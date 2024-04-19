package com.redislabs.university.dao;

import com.redislabs.university.api.Measurement;
import com.redislabs.university.api.MeterReading;
import com.redislabs.university.api.MetricUnit;

import java.time.ZonedDateTime;
import java.util.List;

public interface MetricDao {
    void insert(MeterReading reading);
    List<Measurement> getRecent(Long siteId, MetricUnit unit,
                                ZonedDateTime time, Integer limit);
}
