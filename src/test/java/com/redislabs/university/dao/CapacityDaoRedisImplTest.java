package com.redislabs.university.dao;

import com.redislabs.university.JedisDaoTestBase;
import com.redislabs.university.api.CapacityReport;
import com.redislabs.university.api.MeterReading;
import com.redislabs.university.api.SiteCapacityTuple;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Tuple;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class CapacityDaoRedisImplTest extends JedisDaoTestBase {

    private List<MeterReading> readings;

    @After
    public void flush() {
        keyManager.deleteKeys(jedis);
    }

    @Before
    public void generateData() {
        readings = new ArrayList<>();
        final ZonedDateTime time = ZonedDateTime.now(ZoneOffset.UTC);
        for (int i=0; i < 10; i++) {
            final MeterReading reading = new MeterReading((long) i, time, 1.2,
                    (double) i, 22.0);
            readings.add(reading);
        }
    }

    @Test
    public void update() {
        final CapacityDao dao = new CapacityDaoRedisImpl(jedisPool);
        for (MeterReading reading : readings) {
            dao.update(reading);
        }
        final Set<Tuple> results = jedis.zrevrangeWithScores(RedisSchema.getCapacityRankingKey(), 0, 20);
        assertThat(results.size(), is(10));
    }

    @Test
    public void getReport() {
        final CapacityDao dao = new CapacityDaoRedisImpl(jedisPool);
        for (MeterReading reading : readings) {
            dao.update(reading);
        }
        final CapacityReport results = dao.getReport(5);
        final List<SiteCapacityTuple> highest = results.getHighestCapacity();
        assertThat(highest.size(), is(5));
        final List<SiteCapacityTuple> lowest = results.getLowestCapacity();
        assertThat(lowest.size(), is(5));

        assertThat(highest.get(0).getCapacity(),
                greaterThan(highest.get(1).getCapacity()));
        assertThat(lowest.get(0).getCapacity(),
                lessThan(lowest.get(1).getCapacity()));
        assertThat(lowest.get(4).getCapacity(),
                greaterThan(lowest.get(0).getCapacity()));
    }

    // Challenge #4
    @Test
    public void getRank() {
        final CapacityDao dao = new CapacityDaoRedisImpl(jedisPool);
        for (MeterReading reading : readings) {
            dao.update(reading);
        }

        assertThat(dao.getRank(readings.get(0).getSiteId()), is(9L));
        assertThat(dao.getRank(readings.get(9).getSiteId()), is(0L));
    }
}