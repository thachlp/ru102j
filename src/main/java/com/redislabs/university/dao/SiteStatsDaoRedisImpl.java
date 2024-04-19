package com.redislabs.university.dao;

import com.redislabs.university.api.MeterReading;
import com.redislabs.university.api.SiteStats;
import com.redislabs.university.script.CompareAndUpdateScript;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import javax.annotation.Nullable;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

public class SiteStatsDaoRedisImpl implements SiteStatsDao {

    private static final int WEEK_SECONDS = 60 * 60 * 24 * 7;
    private final JedisPool jedisPool;
    private final CompareAndUpdateScript compareAndUpdateScript;

    public SiteStatsDaoRedisImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        compareAndUpdateScript = new CompareAndUpdateScript(jedisPool);
    }

    // Returns the site stats for the current day
    @Override
    public SiteStats findById(long siteId) {
        return findById(siteId, ZonedDateTime.now());
    }

    @Override
    @Nullable
    public SiteStats findById(long siteId, ZonedDateTime day) {
        try (Jedis jedis = jedisPool.getResource()) {
            final String key = RedisSchema.getSiteStatsKey(siteId, day);
            final Map<String, String> fields = jedis.hgetAll(key);
            if (fields == null || fields.isEmpty()) {
                return null;
            }
            return new SiteStats(fields);
        }
    }

    @Override
    public void update(MeterReading reading) {
        try (Jedis jedis = jedisPool.getResource()) {
            final Long siteId = reading.getSiteId();
            final ZonedDateTime day = reading.getDateTime();
            final String key = RedisSchema.getSiteStatsKey(siteId, day);

            updateOptimized(jedis, key, reading);
        }
    }

    // A naive implementation of update. This implementation has
    // potential race conditions and makes several round trips to Redis.
    private void updateBasic(Jedis jedis, String key, MeterReading reading) {
        final String reportingTime = ZonedDateTime.now(ZoneOffset.UTC).toString();
        jedis.hset(key, SiteStats.REPORTING_TIME_FIELD, reportingTime);
        jedis.hincrBy(key, SiteStats.COUNT_FIELD, 1);
        jedis.pexpire(key, WEEK_SECONDS);

        final String maxWh = jedis.hget(key, SiteStats.MAX_WH_FIELD);
        if (maxWh == null || reading.getWhGenerated() > Double.parseDouble(maxWh)) {
            jedis.hset(key, SiteStats.MAX_WH_FIELD,
                    String.valueOf(reading.getWhGenerated()));
        }

        final String minWh = jedis.hget(key, SiteStats.MIN_WH_FIELD);
        if (minWh == null || reading.getWhGenerated() < Double.parseDouble(minWh)) {
            jedis.hset(key, SiteStats.MIN_WH_FIELD,
                    String.valueOf(reading.getWhGenerated()));
        }

        final String maxCapacity = jedis.hget(key, SiteStats.MAX_CAPACITY_FIELD);
        if (maxCapacity == null || getCurrentCapacity(reading) > Double.parseDouble(maxCapacity)) {
            jedis.hset(key, SiteStats.MAX_CAPACITY_FIELD,
                    String.valueOf(getCurrentCapacity(reading)));
        }
    }

    // Challenge #3
    private void updateOptimized(Jedis jedis, String key, MeterReading reading) {
        try (Transaction trans = jedis.multi()) {
            final String reportingTime = ZonedDateTime.now(ZoneOffset.UTC).toString();
            trans.hset(key, SiteStats.REPORTING_TIME_FIELD, reportingTime);
            trans.hincrBy(key, SiteStats.COUNT_FIELD, 1);
            trans.pexpire(key, WEEK_SECONDS);
            compareAndUpdateScript.updateIfGreater(trans, key, SiteStats.MAX_WH_FIELD, reading.getWhGenerated());
            compareAndUpdateScript.updateIfLess(trans, key, SiteStats.MIN_WH_FIELD, reading.getWhGenerated());
            compareAndUpdateScript.updateIfGreater(trans, key, SiteStats.MAX_CAPACITY_FIELD, getCurrentCapacity(reading));
            trans.exec();
        }
    }

    private static Double getCurrentCapacity(MeterReading reading) {
        return reading.getWhGenerated() - reading.getWhUsed();
    }
}
