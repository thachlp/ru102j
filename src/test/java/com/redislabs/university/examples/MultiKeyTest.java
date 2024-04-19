package com.redislabs.university.examples;

import com.redislabs.university.HostPort;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisDataException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

public class MultiKeyTest {
    private Jedis jedis;
    private String statusKey;
    private String availableKey;

    @Before
    public void setUp() {
        jedis = new Jedis(HostPort.getRedisHost(), HostPort.getRedisPort());

        if (!HostPort.getRedisPassword().isEmpty()) {
            jedis.auth(HostPort.getRedisPassword());
        }

        statusKey = "test:sites:status";
        availableKey = "test:sites:available";
    }

    @After
    public void tearDown() {
        jedis.del(statusKey);
        jedis.del(availableKey);
        jedis.del("a");
        jedis.del("b");
        jedis.del("c");
        jedis.close();
    }

    @Test
    public void testPipeline() {
        final Long siteId = 1L;
        final Pipeline p = jedis.pipelined();

        final Response<Long> hsetResponse = p.hset(statusKey, "available", "true");
        final Response<Long> expireResponse = p.pexpire(statusKey, 1000);
        final Response<Long> saddResponse = p.sadd(availableKey,
                String.valueOf(siteId));

        p.sync();

        assertThat(hsetResponse.get(), is(1L));
        assertThat(expireResponse.get(), is(1L));
        assertThat(saddResponse.get(), is(1L));
    }

    @Test
    public void testTransaction() {
        final Long siteId = 1L;
        final Transaction t = jedis.multi();

        final Response<Long> hsetResponse = t.hset(statusKey, "available", "true");
        final Response<Long> expireResponse = t.pexpire(statusKey, 1000);
        final Response<Long> saddResponse = t.sadd(availableKey,
                String.valueOf(siteId));

        t.exec();

        assertThat(hsetResponse.get(), is(1L));
        assertThat(expireResponse.get(), is(1L));
        assertThat(saddResponse.get(), is(1L));
    }

    @Test public void testTransactionWithErrors() {
        jedis.set("a", "foo");
        jedis.set("c", "bar");
        final Transaction t = jedis.multi();

        final Response<String> r1 = t.set("b", "1");
        final Response<Long> r2 = t.incr("a");
        final Response<String> r3 = t.set("c", "100");
        t.exec();
        assertThat(r1.get(), is("OK"));
        assertThat(r3.get(), is("OK"));
        assertThrows(JedisDataException.class, r2::get);
    }
}
