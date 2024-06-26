package com.redislabs.university.examples;

import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.List;

public class UpdateIfLowestScript {
    private final Jedis jedis;
    private final String sha;
    private static final String SCRIPT =
            "local key = KEYS[1] " +
            "local new = ARGV[1] " +
            "local current = redis.call('GET', key) " +
            "if (current == false) or " +
            "   (tonumber(new) < tonumber(current)) then " +
            "  redis.call('SET', key, new) " +
            "  return 1 " +
            "else " +
            "  return 0 " +
            "end";

    // Load the script and cache the sha of the script.
    public UpdateIfLowestScript(Jedis jedis) {
        this.jedis = jedis;
        sha = jedis.scriptLoad(SCRIPT);
    }

    public boolean updateIfLowest(String key, Integer newValue) {
        final List<String> keys = Collections.singletonList(key);
        final List<String> args = Collections.singletonList(String.valueOf(newValue));
        final Object response = jedis.evalsha(sha, keys, args);
        return (Long)response == 1;
    }
}
