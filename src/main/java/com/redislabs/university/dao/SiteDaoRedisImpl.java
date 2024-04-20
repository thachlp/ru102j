package com.redislabs.university.dao;

import com.redislabs.university.api.Site;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nullable;
import java.util.*;

public class SiteDaoRedisImpl implements SiteDao {
    private final JedisPool jedisPool;

    public SiteDaoRedisImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    // When we insert a site, we set all of its values into a single hash.
    // We then store the site's id in a set for easy access.
    @Override
    public void insert(Site site) {
        try (Jedis jedis = jedisPool.getResource()) {
            final String hashKey = RedisSchema.getSiteHashKey(site.getId());
            final String siteIdKey = RedisSchema.getSiteIDsKey();
            jedis.hmset(hashKey, site.toMap());
            jedis.sadd(siteIdKey, hashKey);
        }
    }

    @Override
    @Nullable
    public Site findById(long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            final String key = RedisSchema.getSiteHashKey(id);
            final Map<String, String> fields = jedis.hgetAll(key);
            if (fields == null || fields.isEmpty()) {
                return null;
            } else {
                return new Site(fields);
            }
        }
    }

    // Challenge #1
    @Override
    public Set<Site> findAll() {
        try (Jedis jedis = jedisPool.getResource()) {
            final Set<String> keys = jedis.smembers(RedisSchema.getSiteIDsKey());
            final Set<Site> sites = new HashSet<>(keys.size());
            for (String key : keys) {
                final Map<String, String> fields = jedis.hgetAll(key);
                if (fields != null && !fields.isEmpty()) {
                    sites.add(new Site(fields));
                }
            }
            return sites;
        }
    }
}
