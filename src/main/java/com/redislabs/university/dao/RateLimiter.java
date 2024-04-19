package com.redislabs.university.dao;

public interface RateLimiter {
    void hit(String name) throws RateLimitExceededException;
}
