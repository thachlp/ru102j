package com.redislabs.university.core;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redislabs.university.api.Site;
import com.redislabs.university.dao.SiteDao;
import com.redislabs.university.dao.SiteDaoRedisImpl;
import com.redislabs.university.dao.SiteGeoDao;
import com.redislabs.university.dao.SiteGeoDaoRedisImpl;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DataLoader {

    private final JedisPool jedisPool;
    private final InputStream inputStream;

    public DataLoader(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        final ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        inputStream = classloader.getResourceAsStream("data/sites.json");
    }

    public DataLoader(JedisPool jedisPool, String filename) throws FileNotFoundException {
        this.jedisPool = jedisPool;
        inputStream = new FileInputStream(filename);
    }

    public void load() throws IOException {
        System.out.println("Loading solar sites...");
        final ObjectMapper mapper = new ObjectMapper();
        final List<Site> sites = mapper.readValue(inputStream, new TypeReference<List<Site>>(){});
        final SiteDao siteDao = new SiteDaoRedisImpl(jedisPool);
        final SiteGeoDao siteGeoDao = new SiteGeoDaoRedisImpl(jedisPool);
        for (Site site : sites) {
            siteDao.insert(site);
            siteGeoDao.insert(site);
        }
    }

    public void flush() {
        try (Jedis jedis = jedisPool.getResource()) {
            System.out.println("Flushing database...");
            jedis.flushDB();
        }
    }
}
