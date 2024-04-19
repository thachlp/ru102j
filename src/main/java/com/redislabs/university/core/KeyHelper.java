package com.redislabs.university.core;

public class KeyHelper {

    private static final String DEFAULT_PREFIX = "app";

    private static String prefix = null;

    public static void setPrefix(String keyPrefix) {
        prefix = keyPrefix;
    }

    public static String getKey(String key) {
        return getPrefix() + ":" + key;
    }

    public static String getPrefix() {
        if (prefix != null) {
            return prefix;
        } else {
            return DEFAULT_PREFIX;
        }
    }
}
