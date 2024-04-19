package com.redislabs.university.dao;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Response;
import redis.clients.jedis.Transaction;

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
            Transaction trans = jedis.multi();
            trans.zadd(key,
                System.currentTimeMillis(),
                String.valueOf(System.currentTimeMillis() - Math.random()));
            trans.zremrangeByScore(key,
                -1,
                System.currentTimeMillis() - (double)windowSizeMS);
            Response<Long> response = trans.zcard(key);
            trans.exec();
            if (response != null && response.get() > maxHits) {
                throw new RateLimitExceededException();
            }
        }
        // END CHALLENGE #7
    }

    private String getKey(String name) {
        return RedisSchema.getRateSlideWindowLimiterKey(name, windowSizeMS, maxHits);
    }
}
