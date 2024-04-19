package com.redislabs.university.dao;

import com.redislabs.university.JedisDaoTestBase;
import com.redislabs.university.api.Measurement;
import com.redislabs.university.api.MeterReading;
import com.redislabs.university.api.MetricUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class MetricDaoRedisZsetImplTest extends JedisDaoTestBase {

    private List<MeterReading> readings;
    private static final Long siteId = 1L;
    private final ZonedDateTime startingDate = ZonedDateTime.now(ZoneOffset.UTC);

    @After
    public void flush() {
        keyManager.deleteKeys(jedis);
    }

    /**
     * Generate 72 hours worth of data.
     */
    @Before
    public void generateData() {
        readings = new ArrayList<>();
        ZonedDateTime time = startingDate;
        for (int i=0; i <  72 * 60; i++) {
            final MeterReading reading = new MeterReading();
            reading.setSiteId(siteId);
            reading.setTempC(i * 1.0);
            reading.setWhUsed(i * 1.0);
            reading.setWhGenerated(i * 1.0);
            reading.setDateTime(time);
            readings.add(reading);
            time = time.minusMinutes(1);
        }
    }

    @Test
    public void testSmall() {
        testInsertAndRetrieve(1);
    }

    @Test
    public void testOneDay() {
        testInsertAndRetrieve(60 * 24);
    }

    @Test
    public void testMultipleDays() {
        testInsertAndRetrieve(60 * 70);
    }

    private void testInsertAndRetrieve(int limit) {
        final MetricDao metricDao = new MetricDaoRedisZSetImpl(jedisPool);
        for (MeterReading reading : readings) {
            metricDao.insert(reading);
        }

        final List<Measurement> measurements = metricDao.getRecent(siteId, MetricUnit.WH_GENERATED,
         startingDate, limit);
        assertEquals(limit, measurements.size());
        int i = limit;
        for (Measurement measurement : measurements) {
            assertEquals((i - 1) * 1.0, measurement.getValue(), 0.001);
            i -= 1;
        }
    }
}