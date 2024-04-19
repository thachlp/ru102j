package com.redislabs.university.examples;

import redis.clients.jedis.Jedis;

public final class JedisDemo {
    public static void main(String[] args) {
        try(Jedis jedis = new Jedis("localhost", 6379)) {
            final String response = jedis.set("hello", "world");
            System.out.println("Response: " + response);
            System.out.println(jedis.get("hello"));
        }
    }
}
