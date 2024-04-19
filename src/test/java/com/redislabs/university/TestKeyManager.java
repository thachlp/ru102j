package com.redislabs.university;

import com.redislabs.university.core.KeyHelper;
import redis.clients.jedis.Jedis;

import java.util.Set;

// Provides a consistent key prefix for tests and
// a method for cleaning up these keys.
public class TestKeyManager {

    private final String prefix;

    public TestKeyManager(String prefix) {
        KeyHelper.setPrefix(prefix);
        this.prefix = prefix;
    }

    public void deleteKeys(Jedis jedis) {
        final Set<String> keysToDelete = jedis.keys(getKeyPattern());
        for (String key : keysToDelete) {
           jedis.del(key);
        }
    }

    private String getKeyPattern() {
        return prefix + ":*";
    }
}
