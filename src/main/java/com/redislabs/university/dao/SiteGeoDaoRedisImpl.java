package com.redislabs.university.dao;

import com.redislabs.university.api.Coordinate;
import com.redislabs.university.api.GeoQuery;
import com.redislabs.university.api.Site;
import redis.clients.jedis.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class SiteGeoDaoRedisImpl implements SiteGeoDao {
    private final JedisPool jedisPool;
    private static final Double CAPACITY_THRESHOLD = 0.2;

    public SiteGeoDaoRedisImpl(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
    }

    @Override
    @Nullable
    public Site findById(long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            final Map<String, String> fields =
                    jedis.hgetAll(RedisSchema.getSiteHashKey(id));
            if (fields == null || fields.isEmpty()) {
                return null;
            }
            return new Site(fields);
        }
    }

    @Override
    public Set<Site> findAll() {
        try (Jedis jedis = jedisPool.getResource()) {
            final Set<String> keys = jedis.zrange(RedisSchema.getSiteGeoKey(), 0, -1);
            final Pipeline pipeline = jedis.pipelined();
            final Set<Site> sites = new HashSet<>(keys.size());
            final Set<Response<Map<String, String>>> responses = new HashSet<>();
            for (String key : keys) {
                final Response<Map<String, String>> response = pipeline.hgetAll(key);
                if (response != null) {
                    responses.add(response);
                }
            }
            pipeline.sync();
            for (Response<Map<String, String>> response : responses) {
                sites.add(new Site(response.get()));
            }

            return sites;
        }
    }

    @Override
    public Set<Site> findByGeo(GeoQuery query) {
        if (query.onlyExcessCapacity()) {
            return findSitesByGeoWithCapacity(query);
        } else {
            return findSitesByGeo(query);
        }
    }

    // Challenge #5
//     private Set<Site> findSitesByGeoWithCapacity(GeoQuery query) {
//         return Collections.emptySet();
//     }
    // Comment out the above, and uncomment what's below
    private Set<Site> findSitesByGeoWithCapacity(GeoQuery query) {
        final Set<Site> results = new HashSet<>();
        final Coordinate coord = query.getCoordinate();
        final Double radius = query.getRadius();
        final GeoUnit radiusUnit = query.getRadiusUnit();

         try (Jedis jedis = jedisPool.getResource()) {
             // START Challenge #5
             // TODO: Challenge #5: Get the sites matching the geo query, store them
             //Set<String> keys = jedis.zrange(RedisSchema.getSiteGeoKey(), 0, -1);
             final List<GeoRadiusResponse> radiusResponses = jedis.georadius(RedisSchema.getSiteGeoKey(), coord.getLng(),
                 coord.getLat(), radius, radiusUnit);
             // END Challenge #5

             final Set<Site> sites = radiusResponses.stream()
                     .map(response -> jedis.hgetAll(response.getMemberByString()))
                     .filter(Objects::nonNull)
                     .map(Site::new).collect(Collectors.toSet());

             // START Challenge #5
             final Pipeline pipeline = jedis.pipelined();
             final Map<Long, Response<Double>> scores = new HashMap<>(sites.size());
             // TODO: Challenge #5: Add the code that populates the scores HashMap...
             for (Site site : sites) {
                 final Response<Double> score = pipeline.zscore(RedisSchema.getCapacityRankingKey(),
                     String.valueOf(site.getId()));
                 scores.put(site.getId(), score);
             }
             pipeline.sync();
             // END Challenge #5

             for (Site site : sites) {
                 if (scores.get(site.getId()).get() >= CAPACITY_THRESHOLD) {
                     results.add(site);
                 }
             }
         }

         return results;
    }

    private Set<Site> findSitesByGeo(GeoQuery query) {
        final Coordinate coord = query.getCoordinate();
        final Double radius = query.getRadius();
        final GeoUnit radiusUnit = query.getRadiusUnit();

        try (Jedis jedis = jedisPool.getResource()) {
            final List<GeoRadiusResponse> radiusResponses =
                    jedis.georadius(RedisSchema.getSiteGeoKey(), coord.getLng(),
                            coord.getLat(), radius, radiusUnit);

            return radiusResponses.stream()
                    .map(response -> jedis.hgetAll(response.getMemberByString()))
                    .filter(Objects::nonNull)
                    .map(Site::new).collect(Collectors.toSet());
        }
    }

    @Override
    public void insert(Site site) {
         try (Jedis jedis = jedisPool.getResource()) {
             final String key = RedisSchema.getSiteHashKey(site.getId());
             jedis.hmset(key, site.toMap());

             if (site.getCoordinate() == null) {
                 throw new IllegalArgumentException("Coordinate required for Geo " +
                         "insert.");
             }
             final double longitude = site.getCoordinate().getGeoCoordinate().getLongitude();
             final double latitude = site.getCoordinate().getGeoCoordinate().getLatitude();
             jedis.geoadd(RedisSchema.getSiteGeoKey(), longitude, latitude,
                     key);
         }
    }
}
