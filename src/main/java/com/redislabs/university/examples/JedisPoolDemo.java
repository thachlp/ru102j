package com.redislabs.university.examples;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class JedisPoolDemo {
    public static void main(String[] args) {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(64);
        poolConfig.setMaxIdle(64);
        try(JedisPool jedisPool = new JedisPool(poolConfig, "localhost", 6379)) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.set("hello", "world");
                System.out.println("Active connections: " + jedisPool.getNumActive());
            }
            System.out.println("Idle connections: " + jedisPool.getNumIdle());
        }
    }
}
