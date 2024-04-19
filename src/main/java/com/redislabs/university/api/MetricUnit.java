package com.redislabs.university.api;

public enum MetricUnit {
    WH_GENERATED("whG"),
    WH_USED("whU"),
    TEMPERATURE_CELSIUS("tempC");

    private final String shortName;

    MetricUnit(String shortName) {
        this.shortName = shortName;
    }

    public String getShortName() {
        return shortName;
    }
}
