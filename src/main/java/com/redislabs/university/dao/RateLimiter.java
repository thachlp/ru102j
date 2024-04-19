package com.redislabs.university.dao;

@FunctionalInterface
public interface RateLimiter {
    void hit(String name) throws RateLimitExceededException;
}
