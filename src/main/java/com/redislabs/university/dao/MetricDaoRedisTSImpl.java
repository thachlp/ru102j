package com.redislabs.university.dao;

import com.redislabs.redistimeseries.RedisTimeSeries;
import com.redislabs.redistimeseries.Value;
import com.redislabs.university.api.Measurement;
import com.redislabs.university.api.MeterReading;
import com.redislabs.university.api.MetricUnit;
import redis.clients.jedis.JedisPool;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Retain metrics using the Redis Time Series module
 * (see <a href="https://github.com/RedisLabsModules/RedisTimeSeries">...</a>)
 *
 */
public class MetricDaoRedisTSImpl implements MetricDao {

    private static final Integer RETENTION_MS =
            60 * 60 * 24 * 14 * 1000;
    private final RedisTimeSeries rts;

    public MetricDaoRedisTSImpl(JedisPool pool) {
        rts = new RedisTimeSeries(pool);
    }

    @Override
    public void insert(MeterReading reading) {
        insertMetric(reading.getSiteId(), reading.getWhGenerated(),
                MetricUnit.WH_GENERATED, reading.getDateTime());
        insertMetric(reading.getSiteId(), reading.getWhUsed(),
                MetricUnit.WH_USED, reading.getDateTime());
        insertMetric(reading.getSiteId(), reading.getTempC(),
                MetricUnit.TEMPERATURE_CELSIUS, reading.getDateTime());
    }

    private void insertMetric(Long siteId, Double value, MetricUnit unit,
                              ZonedDateTime dateTime) {
        final String metricKey = RedisSchema.getTSKey(siteId, unit);
        rts.add(metricKey, dateTime.toEpochSecond() * 1000, value, RETENTION_MS);
    }


    // Return the `limit` most-recent minute-level measurements starting at the
    // provided timestamp.
    @Override
    public List<Measurement> getRecent(Long siteId, MetricUnit unit, ZonedDateTime time, Integer limit) {
        final List<Measurement> measurements = new ArrayList<>();
        final String metricKey = RedisSchema.getTSKey(siteId, unit);

        final Long nowMs = time.toEpochSecond() * 1000;
        final Long initialTimestamp = nowMs - limit * 60 * 1000;
        final Value[] values = rts.range(metricKey, initialTimestamp, nowMs);

        for (int j=0; j<limit && j<values.length; j++) {
            final Measurement m = new Measurement();
            m.setSiteId(siteId);
            m.setMetricUnit(unit);
            final Instant i = Instant.ofEpochSecond(values[j].getTime() / 1000);
            m.setDateTime(ZonedDateTime.ofInstant(i, ZoneId.of("UTC")));
            m.setValue(values[j].getValue());
            measurements.add(m);
        }

        return measurements;
    }
}
