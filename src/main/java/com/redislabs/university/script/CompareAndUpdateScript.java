package com.redislabs.university.script;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/* Encapsulates a server-side Lua script to compare
 * a value stored in a hash field and update if
 * greater than or less than the provided value,
 * as requested.
 */
public class CompareAndUpdateScript {

    private final String sha;
    public static final String SCRIPT = "local key = KEYS[1] " +
            "local field = ARGV[1] " +
            "local value = ARGV[2] " +
            "local op = ARGV[3] " +
            "local current = redis.call('hget', key, field) " +
            "if (current == false or current == nil) then " +
            "  redis.call('hset', key, field, value)" +
            "elseif op == '>' then" +
            "  if tonumber(value) > tonumber(current) then" +
            "    redis.call('hset', key, field, value)" +
            "  end " +
            "elseif op == '<' then" +
            "  if tonumber(value) < tonumber(current) then" +
            "    redis.call('hset', key, field, value)" +
            "  end " +
            "end ";

    public CompareAndUpdateScript(JedisPool jedisPool) {
        try (Jedis jedis = jedisPool.getResource()) {
            sha = jedis.scriptLoad(SCRIPT);
        }
    }

    public void updateIfGreater(Transaction jedis, String key, String field,
                               Double value) {
        update(jedis, key, field, value, ScriptOperation.GREATER_THAN);
    }

    public void updateIfLess(Transaction jedis, String key, String field,
                               Double value) {
        update(jedis, key, field, value, ScriptOperation.LESS_THAN);
    }

    private void update(Transaction jedis, String key, String field, Double value,
                           ScriptOperation op) {
        if (sha != null) {
            final List<String> keys = Collections.singletonList(key);
            final List<String> args = Arrays.asList(field, String.valueOf(value),
                    op.getSymbol());
            jedis.evalsha(sha, keys, args);
        }
    }
}
