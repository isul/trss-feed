package com.opal.torrent.rss.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheService {

    static final String CACHE_NAME_RSS = "rss";
    static final String CACHE_NAME_RSS_BY_SITE = "rssBySite";

    @CacheEvict(allEntries = true, cacheNames = {CacheService.CACHE_NAME_RSS, CacheService.CACHE_NAME_RSS_BY_SITE})
    @Scheduled(fixedDelayString = "${opal.torrent.rss.cache.expire}")
    public void cacheEvict() {
        System.out.println("cacheEvict");
    }
}
