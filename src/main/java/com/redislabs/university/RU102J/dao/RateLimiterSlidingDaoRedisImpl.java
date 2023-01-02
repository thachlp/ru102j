package com.redislabs.university.RU102J.dao;

import java.util.Random;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RateLimiterSlidingDaoRedisImpl implements RateLimiter {

    private final JedisPool jedisPool;
    private final long windowSizeMS;
    private final long maxHits;

    public RateLimiterSlidingDaoRedisImpl(JedisPool pool, long windowSizeMS,
                                          long maxHits) {
        this.jedisPool = pool;
        this.windowSizeMS = windowSizeMS;
        this.maxHits = maxHits;
    }

    // Challenge #7
    @Override
    public void hit(String name) throws RateLimitExceededException {
        // START CHALLENGE #7
        try (Jedis jedis = jedisPool.getResource()) {
            String key = getKey(name);
            jedis.zadd(key,
                System.currentTimeMillis(),
                String.valueOf(System.currentTimeMillis() - Math.random()));
            jedis.zremrangeByScore(key,
                -1,
                System.currentTimeMillis() - (double)windowSizeMS);
            Long currentHits = jedis.zcard(key);
            if (currentHits > maxHits) {
                throw new RateLimitExceededException();
            }
        }
        // END CHALLENGE #7
    }

    private String getKey(String name) {
        return RedisSchema.getRateSlideWindowLimiterKey(name, windowSizeMS, maxHits);
    }
}
