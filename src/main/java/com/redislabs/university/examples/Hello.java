package com.redislabs.university.examples;

import redis.clients.jedis.Jedis;

public class Hello {

    private final String host;
    private final Integer port;

    public Hello(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public void say() {
        final Jedis jedis = new Jedis(host, port);
        final String response = jedis.set("hello", "world");
        final String saying = jedis.get("hello");
        System.out.println("Hello, " + saying);
        jedis.close();
    }
}
