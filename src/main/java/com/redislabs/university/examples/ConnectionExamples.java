package com.redislabs.university.examples;

import redis.clients.jedis.*;

import java.util.HashSet;
import java.util.Set;

public final class ConnectionExamples {

    private ConnectionExamples() {
    }

    // Create a basic connection to Redis. Not thread safe!
    public static Jedis getJedis(String host, Integer port, int timeout,
                                 String password) {
        final Jedis jedis = new Jedis(host, port, timeout);
        jedis.auth(password);

        return jedis;
    }

    // Create a pool of connections to Redis. This is thread safe.
    public static JedisPool getPool(String host, Integer port,
                                    int maxConnections, int timeout,
                                    String password) {
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxConnections);
        config.setMaxIdle(maxConnections);

        return new JedisPool(config, host, port, timeout, password);
    }

    // Connect to a Redis Sentinel deployment. Pooled and thread safe.
    public static JedisSentinelPool getJedisSentinelPool(int maxConnections,
                                                         int timeout,
                                                         String password) {
        final String masterName = "redisMaster";
        final Set<String> sentinels = new HashSet<>();
        sentinels.add("localhost:6379");
        sentinels.add("localhost:7379");

        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxConnections);
        config.setMaxIdle(maxConnections);

        return new JedisSentinelPool(masterName, sentinels, config, timeout,
                password);
    }

    /** Connect to a Redis Cluster deployment. Uses a pool connection under
    the hood and is thread safe.
    */
    public static JedisCluster getClusterConnection(int timeout, int maxAttempts,
                                                    int maxConnections) {
        final Set<HostAndPort> nodes = new HashSet<>();
        final JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxConnections);
        config.setMaxIdle(maxConnections);
        return new JedisCluster(nodes, timeout, maxAttempts, config);
    }
}
