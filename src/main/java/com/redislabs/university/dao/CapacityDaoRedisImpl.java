package com.redislabs.university.dao;

import com.redislabs.university.api.CapacityReport;
import com.redislabs.university.api.MeterReading;
import com.redislabs.university.api.SiteCapacityTuple;
import redis.clients.jedis.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CapacityDaoRedisImpl implements CapacityDao {

    private final JedisPool jedisPool;

    public CapacityDaoRedisImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public void update(MeterReading reading) {
        final String capacityRankingKey = RedisSchema.getCapacityRankingKey();
        final Long siteId = reading.getSiteId();

        final double currentCapacity = reading.getWhGenerated() - reading.getWhUsed();

        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zadd(capacityRankingKey, currentCapacity, String.valueOf(siteId));
        }
    }

    @Override
    public CapacityReport getReport(Integer limit) {
        final CapacityReport report;
        final String key = RedisSchema.getCapacityRankingKey();

        try (Jedis jedis = jedisPool.getResource()) {
            final Pipeline p = jedis.pipelined();
            final Response<Set<Tuple>> lowCapacity = p.zrangeWithScores(key, 0L, limit - 1L);
            final Response<Set<Tuple>> highCapacity = p.zrevrangeWithScores(key, 0L,limit - 1L);
            p.sync();

            final List<SiteCapacityTuple> lowCapacityList = lowCapacity.get().stream()
                    .map(SiteCapacityTuple::new)
                    .collect(Collectors.toList());

            final List<SiteCapacityTuple> highCapacityList = highCapacity.get().stream()
                    .map(SiteCapacityTuple::new)
                    .collect(Collectors.toList());

            report = new CapacityReport(highCapacityList, lowCapacityList);
        }

        return report;
    }

    // Challenge #4
    @Override
    public Long getRank(Long siteId) {
        final String key = RedisSchema.getCapacityRankingKey();
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrevrank(key, String.valueOf(siteId));
        }
    }
}
