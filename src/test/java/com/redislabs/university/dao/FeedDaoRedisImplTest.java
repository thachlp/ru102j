package com.redislabs.university.dao;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.redislabs.university.HostPort;
import com.redislabs.university.TestKeyManager;
import com.redislabs.university.api.MeterReading;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class FeedDaoRedisImplTest {

    private static JedisPool jedisPool;
    private static Jedis jedis;
    private static TestKeyManager keyManager;

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

    // Challenge #6
    @Test
    public void testBasicInsertReturnsRecent() {
        final FeedDao dao = new FeedDaoRedisImpl(jedisPool);
        final MeterReading reading0 = generateMeterReading(1L, ZonedDateTime.now());
        final MeterReading reading1 = generateMeterReading(1L,
                ZonedDateTime.now().minusMinutes(1));
        dao.insert(reading0);
        dao.insert(reading1);
        final List<MeterReading> globalList = dao.getRecentGlobal(100);
        assertThat(globalList.size(), is(2));
        assertThat(globalList.get(0), is(reading1));
        assertThat(globalList.get(1), is(reading0));

        final List<MeterReading> siteList = dao.getRecentForSite(1, 100);
        assertThat(siteList.size(), is(2));
        assertThat(siteList.get(0), is(reading1));
        assertThat(siteList.get(1), is(reading0));
    }

    private static MeterReading generateMeterReading(long siteId, ZonedDateTime dateTime) {
        final MeterReading reading = new MeterReading();
        reading.setSiteId(siteId);
        reading.setDateTime(dateTime);
        reading.setTempC(15.0);
        reading.setWhGenerated(0.025);
        reading.setWhUsed(0.015);
        return reading;
    }
}