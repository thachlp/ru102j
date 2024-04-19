package com.redislabs.university.api;

import java.util.List;

public class CapacityReport {
    private final List<SiteCapacityTuple> highestCapacity;
    private final List<SiteCapacityTuple> lowestCapacity;

    public CapacityReport(List<SiteCapacityTuple> highest, List<SiteCapacityTuple> lowest) {
        highestCapacity = highest;
        lowestCapacity = lowest;
    }

    public List<SiteCapacityTuple> getHighestCapacity() {
        return highestCapacity;
    }

    public List<SiteCapacityTuple> getLowestCapacity() {
        return lowestCapacity;
    }
}
