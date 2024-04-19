package com.redislabs.university.dao;

import com.redislabs.university.api.CapacityReport;
import com.redislabs.university.api.MeterReading;

public interface CapacityDao {
    void update(MeterReading reading);
    CapacityReport getReport(Integer limit);
    Long getRank(Long siteId);
}
