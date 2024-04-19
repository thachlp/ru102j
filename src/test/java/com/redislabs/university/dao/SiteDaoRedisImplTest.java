package com.redislabs.university.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

import com.redislabs.university.HostPort;
import com.redislabs.university.TestKeyManager;
import com.redislabs.university.api.Site;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class SiteDaoRedisImplTest {

    private static JedisPool jedisPool;
    private static Jedis jedis;
    private static TestKeyManager keyManager;
    private Set<Site> sites;

    @BeforeClass
    public static void setUp() throws Exception {
        final String password = HostPort.getRedisPassword();
        if (!password.isEmpty()) {
            jedisPool = new JedisPool(new JedisPoolConfig(), HostPort.getRedisHost(), HostPort.getRedisPort(), 2000, password);
        } else {
            jedisPool = new JedisPool(HostPort.getRedisHost(), HostPort.getRedisPort());
        }
        jedis = new Jedis(HostPort.getRedisHost(), HostPort.getRedisPort());

        if (!password.isEmpty()) {
            jedis.auth(password);
        }
        keyManager = new TestKeyManager("test");
    }

    @AfterClass
    public static void tearDown() {
        jedisPool.destroy();
        jedis.close();
    }

    @After
    public void flush() {
        keyManager.deleteKeys(jedis);
    }

    @Before
    public void generateData() {
        sites = new HashSet<>();
        sites.add(new Site(1, 4.5, 3, "123 Willow St.",
                "Oakland", "CA", "94577" ));
        sites.add(new Site(2, 3.0, 2, "456 Maple St.",
                 "Oakland", "CA", "94577" ));
        sites.add(new Site(3, 4.0, 3, "789 Oak St.",
                 "Oakland", "CA", "94577" ));
    }

    /**
     * Challenge #0 Part 1. This challenge is explained in
     * the video "How to Solve a Sample Challenge"
     */
    @Test
    public void findByIdWithExistingSite() {
        final SiteDaoRedisImpl dao = new SiteDaoRedisImpl(jedisPool);
        final Site site = new Site(4L, 5.5, 4, "910 Pine St.",
                "Oakland", "CA", "94577");
        dao.insert(site);
        final Site storedSite = dao.findById(4L);
        assertThat(storedSite, is(site));
    }

    /**
     * Challenge #0 Part 2. This challenge is explained in
     * the video "How to Solve a Sample Challenge"
     */
    @Test
    public void findByIdWithMissingSite() {
        final SiteDaoRedisImpl dao = new SiteDaoRedisImpl(jedisPool);
        assertThat(dao.findById(4L), is(nullValue()));
    }

    /**
     * Challenge #1 Part 1. Use this test case to
     * implement the challenge in Chapter 1.
     */
    @Test
    public void findAllWithMultipleSites() {
        final SiteDaoRedisImpl dao = new SiteDaoRedisImpl(jedisPool);
        // Insert all sites
        for (Site site : sites) {
            dao.insert(site);
        }

        assertThat(dao.findAll(), is(sites));
    }

    /**
     * Challenge #1 Part 2. Use this test case to
     * implement the challenge in Chapter 1.
     */
    @Test
    public void findAllWithEmptySites() {
        final SiteDaoRedisImpl dao = new SiteDaoRedisImpl(jedisPool);
        assertThat(dao.findAll(), is(empty()));
    }

    @Test
    public void insert() {
        final SiteDaoRedisImpl dao = new SiteDaoRedisImpl(jedisPool);
        final Site site = new Site(4, 5.5, 4, "910 Pine St.",
                "Oakland", "CA", "94577");
        dao.insert(site);

        final Map<String, String> siteFields = jedis.hgetAll(RedisSchema.getSiteHashKey(4L));
        assertEquals(siteFields, site.toMap());

        assertThat(jedis.sismember(RedisSchema.getSiteIDsKey(), RedisSchema.getSiteHashKey(4L)),
                is(true));
    }
}