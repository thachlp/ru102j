package com.redislabs.university.dao;

import com.redislabs.university.api.GeoQuery;
import com.redislabs.university.api.Site;

import java.util.Set;

public interface SiteGeoDao extends SiteDao {
    Set<Site> findByGeo(GeoQuery query);
}
