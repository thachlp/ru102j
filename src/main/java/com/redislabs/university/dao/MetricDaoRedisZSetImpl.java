package com.redislabs.university.dao;

import com.redislabs.university.api.Measurement;
import com.redislabs.university.api.MeterReading;
import com.redislabs.university.api.MetricUnit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Tuple;

import java.text.DecimalFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Retain metrics using Redis sorted sets.
 * <p>
 * In this implementation, we use one sorted set per day for
 * up to 'MAX_METRIC_RETENTION_DAYS' days. Old sorted sets are expired
 * after this number of days.
 *
 */
public class MetricDaoRedisZSetImpl implements MetricDao {

    private static final Integer MAX_METRIC_RETENTION_DAYS = 30;
    private static final Integer MAX_DAYS_TO_RETURN = 7;
    private static final Integer METRICS_PER_DAY = 60 * 24;
    private static final Integer METRIC_EXPIRATION_SECONDS =
            60 * 60 * 24 * MAX_METRIC_RETENTION_DAYS + 1;
    private final JedisPool jedisPool;

    public MetricDaoRedisZSetImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    public void insert(MeterReading reading) {
        try (Jedis jedis = jedisPool.getResource()) {
            insertMetric(jedis, reading.getSiteId(), reading.getWhGenerated(),
                    MetricUnit.WH_GENERATED, reading.getDateTime());
            insertMetric(jedis, reading.getSiteId(), reading.getWhUsed(),
                    MetricUnit.WH_USED, reading.getDateTime());
            insertMetric(jedis, reading.getSiteId(), reading.getTempC(),
                    MetricUnit.TEMPERATURE_CELSIUS, reading.getDateTime());
        }
    }

    // Challenge #2
    private void insertMetric(Jedis jedis, long siteId, double value, MetricUnit unit,
                              ZonedDateTime dateTime) {
        final String metricKey = RedisSchema.getDayMetricKey(siteId, unit, dateTime);
        final Integer minuteOfDay = getMinuteOfDay(dateTime);

        jedis.zadd(metricKey, minuteOfDay, new MeasurementMinute(value, minuteOfDay).toString());
        jedis.pexpire(metricKey, METRIC_EXPIRATION_SECONDS);
    }

    /**
     * Return the N most-recent minute-level measurements starting at the
     * provided day.
     * TODO: Watch out for large data structures when sharding
     * TODO: Or implement your own expiry with zremrange
     */
    @Override
    public List<Measurement> getRecent(Long siteId, MetricUnit unit,
                                       ZonedDateTime time, Integer limit) {
        if (limit > METRICS_PER_DAY * MAX_METRIC_RETENTION_DAYS) {
            throw new IllegalArgumentException("Cannot request more than two weeks" +
                    "of minute-level data");
        }

        final List<Measurement> measurements = new ArrayList<>();
        ZonedDateTime currentDate = time;
        int count = limit;
        int iterations = 0;

        // This loop extracts the elements of successive
        // sorted sets until it reaches the requested limit.
        do {
            final List<Measurement> ms = getMeasurementsForDate(siteId, currentDate,
                    unit, count);
            measurements.addAll(0, ms);
            count -= ms.size();
            currentDate = currentDate.minusDays(1);
            iterations += 1;
        } while (count > 0 && iterations < MAX_DAYS_TO_RETURN);

        return measurements;
    }

    /**
     * Return up to `count` elements from the sorted set corresponding to
     * the siteId, date, and metric unit specified here.
     */
    private List<Measurement> getMeasurementsForDate(Long siteId,
                                                     ZonedDateTime date,
                                                     MetricUnit unit,
                                                     Integer count) {
        // A list of Measurement objects to return.
        final List<Measurement> measurements = new ArrayList<>();

        try (Jedis jedis = jedisPool.getResource()) {
            // Get the metric key for the day implied by the date.
            // metric:[unit-name]:[year-month-day]:[site-id]
            // e.g.: metrics:whU:2020-01-01:1
            final String metricKey = RedisSchema.getDayMetricKey(siteId, unit, date);

            // Return a reverse range so that we're always consuming from the end
            // of the sorted set.
            final Set<Tuple> metrics = jedis.zrevrangeWithScores(metricKey, 0, count - 1L);
            for (Tuple minuteValue : metrics) {
                // Elements of the set are of the form [measurement]:[minute]
                // The MeasurementMinute class abstracts this for us.
                final MeasurementMinute mm = MeasurementMinute.fromZSetValue(
                        minuteValue.getElement());

                // Derive the dateTime for the measurement using the date and
                // the minute of the day.
                final ZonedDateTime dateTime = getDateFromDayMinute(date,
                        mm.getMinuteOfDay());

                // Add a new measurement to the list of measurements.
                measurements.add(new Measurement(siteId, unit, dateTime,
                        mm.getMeasurement()));
            }
        }

        Collections.reverse(measurements);
        return measurements;
    }

    private static ZonedDateTime getDateFromDayMinute(ZonedDateTime dateTime,
                                                      Integer dayMinute) {
       final int minute = dayMinute % 60;
       final int hour = dayMinute / 60;
       return dateTime.withHour(hour).withMinute(minute).
               withZoneSameInstant(ZoneOffset.UTC);
    }

    // Return the minute of the day. For example:
    //  01:12 is the 72nd minute of the day
    //  5:00 is the 300th minute of the day
    public Integer getMinuteOfDay(ZonedDateTime dateTime) {
        final int hour = dateTime.getHour();
        final int minute = dateTime.getMinute();
        return hour * 60 + minute;
    }

    /**
     * Utility class to convert between our sorted set members and their
     * constituent measurement and minute values.
     * Also rounds decimals before storing them.
     */
    public static class MeasurementMinute {
        private final Double measurement;
        private final Integer minuteOfDay;
        private final DecimalFormat decimalFormat;

        // For a sorted set value of "22.0:1", this classes provides
        // the measurement value of 22.0 and the minuteOfDay value of 1.
        public static MeasurementMinute fromZSetValue(String zSetValue) {
            final String[] parts = zSetValue.split(":");
            if (parts.length == 2) {
                return new MeasurementMinute(Double.valueOf(parts[0]),
                        Integer.valueOf(parts[1]));
            } else {
                throw new IllegalArgumentException("Cannot convert zSetValue " +
                        zSetValue + " into MeasurementMinute");
            }
        }

        public MeasurementMinute(Double measurement, Integer minuteOfDay) {
            this.measurement = measurement;
            this.minuteOfDay = minuteOfDay;
            decimalFormat = new DecimalFormat("#.##");
        }

        public Integer getMinuteOfDay() {
            return minuteOfDay;
        }

        public Double getMeasurement() {
            return measurement;
        }

        @Override
        public String toString() {
            return decimalFormat.format(measurement) + ':' + minuteOfDay;
        }
    }
}
