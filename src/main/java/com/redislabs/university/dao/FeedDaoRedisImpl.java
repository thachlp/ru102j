package com.redislabs.university.dao;

import com.redislabs.university.api.MeterReading;
import redis.clients.jedis.*;

import java.util.ArrayList;
import java.util.List;

public class FeedDaoRedisImpl implements FeedDao {

    private final JedisPool jedisPool;
    private static final long GLOBAL_MAX_FEED_LENGTH = 10000;
    private static final long SITE_MAX_FEED_LENGTH = 2440;

    public FeedDaoRedisImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    // Challenge #6
    @Override
    public void insert(MeterReading meterReading) {
        // START Challenge #6
        // END Challenge #6
        try (Jedis jedis = jedisPool.getResource()) {
            final String globalFeedKey = RedisSchema.getGlobalFeedKey();
            final String siteFeedKey = RedisSchema.getFeedKey(meterReading.getSiteId());
            final Pipeline pipeline = jedis.pipelined();
            pipeline.xadd(globalFeedKey,
                StreamEntryID.NEW_ENTRY,
                meterReading.toMap(),
                GLOBAL_MAX_FEED_LENGTH,
                true);
            pipeline.xadd(siteFeedKey,
                StreamEntryID.NEW_ENTRY,
                meterReading.toMap(),
                SITE_MAX_FEED_LENGTH,
                true);
            pipeline.sync();
        }
    }

    @Override
    public List<MeterReading> getRecentGlobal(int limit) {
        return getRecent(RedisSchema.getGlobalFeedKey(), limit);
    }

    @Override
    public List<MeterReading> getRecentForSite(long siteId, int limit) {
        return getRecent(RedisSchema.getFeedKey(siteId), limit);
    }

    public List<MeterReading> getRecent(String key, int limit) {
        final List<MeterReading> readings = new ArrayList<>(limit);
        try (Jedis jedis = jedisPool.getResource()) {
            final List<StreamEntry> entries = jedis.xrevrange(key, null,
                    null, limit);
            for (StreamEntry entry : entries) {
                readings.add(new MeterReading(entry.getFields()));
            }
            return readings;
        }
    }
}
