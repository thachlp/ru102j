package com.redislabs.university.dao;

import com.redislabs.university.api.MeterReading;
import com.redislabs.university.api.SiteStats;
import com.redislabs.university.script.CompareAndUpdateScript;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

public class SiteStatsDaoRedisImpl implements SiteStatsDao {

    private final int weekSeconds = 60 * 60 * 24 * 7;
    private final JedisPool jedisPool;
    private final CompareAndUpdateScript compareAndUpdateScript;

    public SiteStatsDaoRedisImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.compareAndUpdateScript = new CompareAndUpdateScript(jedisPool);
    }

    // Returns the site stats for the current day
    @Override
    public SiteStats findById(long siteId) {
        return findById(siteId, ZonedDateTime.now());
    }

    @Override
    public SiteStats findById(long siteId, ZonedDateTime day) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = RedisSchema.getSiteStatsKey(siteId, day);
            Map<String, String> fields = jedis.hgetAll(key);
            if (fields == null || fields.isEmpty()) {
                return null;
            }
            return new SiteStats(fields);
        }
    }

    @Override
    public void update(MeterReading reading) {
        try (Jedis jedis = jedisPool.getResource()) {
            Long siteId = reading.getSiteId();
            ZonedDateTime day = reading.getDateTime();
            String key = RedisSchema.getSiteStatsKey(siteId, day);

            updateOptimized(jedis, key, reading);
        }
    }

    // A naive implementation of update. This implementation has
    // potential race conditions and makes several round trips to Redis.
    private void updateBasic(Jedis jedis, String key, MeterReading reading) {
        String reportingTime = ZonedDateTime.now(ZoneOffset.UTC).toString();
        jedis.hset(key, SiteStats.REPORTING_TIME_FIELD, reportingTime);
        jedis.hincrBy(key, SiteStats.COUNT_FIELD, 1);
        jedis.expire(key, weekSeconds);

        String maxWh = jedis.hget(key, SiteStats.MAX_WH_FIELD);
        if (maxWh == null || reading.getWhGenerated() > Double.parseDouble(maxWh)) {
            jedis.hset(key, SiteStats.MAX_WH_FIELD,
                    String.valueOf(reading.getWhGenerated()));
        }

        String minWh = jedis.hget(key, SiteStats.MIN_WH_FIELD);
        if (minWh == null || reading.getWhGenerated() < Double.parseDouble(minWh)) {
            jedis.hset(key, SiteStats.MIN_WH_FIELD,
                    String.valueOf(reading.getWhGenerated()));
        }

        String maxCapacity = jedis.hget(key, SiteStats.MAX_CAPACITY_FIELD);
        if (maxCapacity == null || getCurrentCapacity(reading) > Double.parseDouble(maxCapacity)) {
            jedis.hset(key, SiteStats.MAX_CAPACITY_FIELD,
                    String.valueOf(getCurrentCapacity(reading)));
        }
    }

    // Challenge #3
    private void updateOptimized(Jedis jedis, String key, MeterReading reading) {
        try (Transaction trans = jedis.multi()) {
            String reportingTime = ZonedDateTime.now(ZoneOffset.UTC).toString();
            trans.hset(key, SiteStats.REPORTING_TIME_FIELD, reportingTime);
            trans.hincrBy(key, SiteStats.COUNT_FIELD, 1);
            trans.expire(key, weekSeconds);
            this.compareAndUpdateScript.updateIfGreater(trans, key, SiteStats.MAX_WH_FIELD, reading.getWhGenerated());
            this.compareAndUpdateScript.updateIfLess(trans, key, SiteStats.MIN_WH_FIELD, reading.getWhGenerated());
            this.compareAndUpdateScript.updateIfGreater(trans, key, SiteStats.MAX_CAPACITY_FIELD, getCurrentCapacity(reading));
            trans.exec();
        }
    }

    private Double getCurrentCapacity(MeterReading reading) {
        return reading.getWhGenerated() - reading.getWhUsed();
    }
}
