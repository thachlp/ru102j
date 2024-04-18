package com.redislabs.university.RU102J.examples;

import com.redislabs.university.RU102J.HostPort;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class UpdateIfLowestScriptTest {
    private Jedis jedis;

    @Before
    public void setUp() {
        jedis = new Jedis(HostPort.getRedisHost(), HostPort.getRedisPort());
        if (!HostPort.getRedisPassword().isEmpty()) {
            jedis.auth(HostPort.getRedisPassword());
        }
    }

    @After
    public void tearDown() {
        jedis.del("testLua");
        jedis.close();
    }

    @Test
    public void updateIfLowest() {
        jedis.set("testLua", "100");
        final UpdateIfLowestScript script = new UpdateIfLowestScript(jedis);

        final boolean result = script.updateIfLowest("testLua", 50);
        assertTrue(result);
        assertEquals("50", jedis.get("testLua"));
    }

    @Test
    public void updateIfLowestUnchanged() {
        jedis.set("testLua", "100");
        final UpdateIfLowestScript script = new UpdateIfLowestScript(jedis);

        final boolean result = script.updateIfLowest("testLua", 200);
        assertFalse(result);
        assertEquals("100", jedis.get("testLua"));
    }

    @Test
    public void updateIfLowestWithNoKey() {
        jedis.del("testLua");
        final UpdateIfLowestScript script = new UpdateIfLowestScript(jedis);

        final boolean result = script.updateIfLowest("testLua", 200);
        assertTrue(result);
        assertEquals("200", jedis.get("testLua"));
    }
}