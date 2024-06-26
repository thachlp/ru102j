package com.redislabs.university.script;

import com.redislabs.university.HostPort;
import com.redislabs.university.TestKeyManager;
import com.redislabs.university.dao.RedisSchema;
import org.junit.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Transaction;

import java.time.ZonedDateTime;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CompareAndUpdateScriptTest {
    private static JedisPool jedisPool;
    private static Jedis jedis;
    private static TestKeyManager keyManager;
    private CompareAndUpdateScript cu;
    private String key;
    private String field;
    private String emptyKey;

    @BeforeClass
    public static void setUp() throws Exception {
        final String password = HostPort.getRedisPassword();
        jedis = new Jedis(HostPort.getRedisHost(), HostPort.getRedisPort());
        if (!password.isEmpty()) {
            jedisPool = new JedisPool(new JedisPoolConfig(), HostPort.getRedisHost(), HostPort.getRedisPort(), 2000, password);
            jedis.auth(password);
        } else {
            jedisPool = new JedisPool(HostPort.getRedisHost(), HostPort.getRedisPort());
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
    public void prepare() {
        cu = new CompareAndUpdateScript(jedisPool);
        key = RedisSchema.getSiteStatsKey(1L, ZonedDateTime.now());
        emptyKey = RedisSchema.getSiteStatsKey(1000L, ZonedDateTime.now());
        field = "n";
        jedis.hset(key, field, "1.0");
    }

    @Test
    public void updateWhenNull() {
        jedis.del(emptyKey);
        final Transaction t1 = jedis.multi();
        cu.updateIfGreater(t1, emptyKey, "n", 1.0);
        t1.exec();
        assertThat(jedis.hget(emptyKey, "n"), is("1.0"));
    }

    @Test
    public void updateIfGreater() {
        final Transaction t1 = jedis.multi();
        cu.updateIfGreater(t1, key, "n", 0.0);
        t1.exec();
        assertThat(jedis.hget(key, field), is("1.0"));

        final Transaction t2 = jedis.multi();
        cu.updateIfGreater(t2, key, "n", 2.0);
        t2.exec();
        assertThat(jedis.hget(key, field), is("2.0"));
    }

    @Test
    public void updateIfLess() {
        final Transaction t1 = jedis.multi();
        cu.updateIfLess(t1, key, "n", 0.0);
        t1.exec();
        assertThat(jedis.hget(key, field), is("0.0"));

        final Transaction t2 = jedis.multi();
        cu.updateIfLess(t2, key, "n", 2.0);
        t2.exec();
        assertThat(jedis.hget(key, field), is("0.0"));
    }
}