package com.redislabs.university.api;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;

// Site summary stats for a single day.
public class SiteStats {
    private final ZonedDateTime lastReportingTime;
    private final Long meterReadingCount;
    private final Double maxWhGenerated;
    private final Double minWhGenerated;
    private final Double maxCapacity;

    /* These field names will be used by multiple classes, so we define
       them here to abide by DRY (don't repeat yourself). */
    public static final String REPORTING_TIME_FIELD = "lastReportingTime";
    public static final String COUNT_FIELD = "meterReadingCount";
    public static final String MAX_WH_FIELD = "maxWhGenerated";
    public static final String MIN_WH_FIELD = "minWhGenerated";
    public static final String MAX_CAPACITY_FIELD = "maxCapacity";

    public SiteStats(Map<String, String> map) {
        lastReportingTime = parseTime(map.get(REPORTING_TIME_FIELD));
        meterReadingCount = parseLong(map.get(COUNT_FIELD));
        maxWhGenerated = parseDouble(map.get(MAX_WH_FIELD));
        minWhGenerated = parseDouble(map.get(MIN_WH_FIELD));
        maxCapacity = parseDouble(map.get(MAX_CAPACITY_FIELD));
    }

    private static Double parseDouble(String value) {
        if (value == null) {
            return null;
        } else {
            return Double.valueOf(value);
        }
    }

    private static Long parseLong(String value) {
        if (value == null) {
            return null;
        } else {
            return Long.valueOf(value);
        }
    }

    private static ZonedDateTime parseTime(String time) {
        if (time == null) {
            return null;
        } else {
            return ZonedDateTime.parse(time);
        }
    }

    public ZonedDateTime getLastReportingTime() {
        return lastReportingTime;
    }

    public Long getMeterReadingCount() {
        return meterReadingCount;
    }

    public Double getMaxWhGenerated() {
        return maxWhGenerated;
    }

    public Double getMinWhGenerated() {
        return minWhGenerated;
    }

    public Double getMaxCapacity() {
        return maxCapacity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final SiteStats siteStats = (SiteStats) o;
        return Objects.equals(lastReportingTime, siteStats.lastReportingTime) &&
                Objects.equals(meterReadingCount, siteStats.meterReadingCount) &&
                Objects.equals(maxWhGenerated, siteStats.maxWhGenerated) &&
                Objects.equals(minWhGenerated, siteStats.minWhGenerated) &&
                Objects.equals(maxCapacity, siteStats.maxCapacity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastReportingTime, meterReadingCount, maxWhGenerated, minWhGenerated, maxCapacity);
    }

    @Override
    public String toString() {
        return "SiteStats{" +
                "lastReportingTime=" + lastReportingTime +
                ", meterReadingCount=" + meterReadingCount +
                ", maxWhGenerated=" + maxWhGenerated +
                ", minWhGenerated=" + minWhGenerated +
                ", maxCapacity=" + maxCapacity +
                '}';
    }
}
