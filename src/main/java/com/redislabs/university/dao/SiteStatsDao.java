package com.redislabs.university.dao;

import com.redislabs.university.api.MeterReading;
import com.redislabs.university.api.SiteStats;

import java.time.ZonedDateTime;

public interface SiteStatsDao {
    SiteStats findById(long siteId);
    SiteStats findById(long siteId, ZonedDateTime day);
    void update(MeterReading reading);
}
