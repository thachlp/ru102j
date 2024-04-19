package com.redislabs.university.dao;

import com.redislabs.university.api.Site;

import java.util.Set;

public interface SiteDao {
    void insert(Site site);
    Site findById(long id);
    Set<Site> findAll();
}
