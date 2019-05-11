package com.opal.torrent.rss.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheService {

    static final String CACHE_NAME_RSS = "rss";
    static final String CACHE_NAME_RSS_BY_SITE = "rssBySite";
    static final String CACHE_NAME_DOWNLOAD_LINK = "downloadLink";

    @CacheEvict(allEntries = true, cacheNames = {CacheService.CACHE_NAME_RSS, CacheService.CACHE_NAME_RSS_BY_SITE, CacheService.CACHE_NAME_DOWNLOAD_LINK})
    @Scheduled(fixedDelayString = "${opal.torrent.rss.cache.expire}")
    public void cacheEvict() {
    }
}
