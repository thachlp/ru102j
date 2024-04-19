package com.redislabs.university.examples;

import com.redislabs.university.HostPort;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class HelloTest {
    @Test
    public void sayHelloBasic() {
        try (final Jedis jedis = new Jedis(HostPort.getRedisHost(), HostPort.getRedisPort())) {
            if (!HostPort.getRedisPassword().isEmpty()) {
                jedis.auth(HostPort.getRedisPassword());
            }
            jedis.set("hello", "world");
            final String value = jedis.get("hello");
            assertThat(value, is("world"));
        }
    }


    @Test
    public void sayHello() {
        final Jedis jedis = new Jedis(HostPort.getRedisHost(), HostPort.getRedisPort());
        if (!HostPort.getRedisPassword().isEmpty()) {
            jedis.auth(HostPort.getRedisPassword());
        }
        final String result = jedis.set("hello", "world");
        assertThat(result, is("OK"));
        final String value = jedis.get("hello");
        assertThat(value, is("world"));

        jedis.close();
    }

    @Test
    public void sayHelloThreadSafe() {
        final JedisPool jedisPool;
        final String password = HostPort.getRedisPassword();
        if (!password.isEmpty()) {
            jedisPool = new JedisPool(new JedisPoolConfig(),
                    HostPort.getRedisHost(), HostPort.getRedisPort(), 2000, password);
        } else {
            jedisPool = new JedisPool(new JedisPoolConfig(),
                    HostPort.getRedisHost(), HostPort.getRedisPort());
        }
        try (Jedis jedis = jedisPool.getResource()) {
            final String result = jedis.set("hello", "world");
            assertThat(result, is("OK"));
            final String value = jedis.get("hello");
            assertThat(value, is("world"));
        }
        jedisPool.close();
    }
}
