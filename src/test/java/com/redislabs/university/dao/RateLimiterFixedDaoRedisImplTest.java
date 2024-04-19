package com.redislabs.university.dao;

import com.redislabs.university.HostPort;
import com.redislabs.university.TestKeyManager;
import org.junit.*;
import org.junit.rules.ExpectedException;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class RateLimiterFixedDaoRedisImplTest {

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

    @Test
    public void hit() {
        int exceptionCount = 0;
        final RateLimiter limiter = new RateLimiterFixedDaoRedisImpl(jedisPool,
                MinuteInterval.ONE, 10);
        for (int i=0; i<10; i++) {
            try {
                limiter.hit("foo");
            } catch (RateLimitExceededException e) {
                exceptionCount += 1;
            }
        }
        assertEquals(0, exceptionCount);
    }
}