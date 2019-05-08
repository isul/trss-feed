package com.opal.torrent.rss.model;

import lombok.Getter;

@Getter
public enum THBoard {
    mos("영화"),
    tvs("국내/해외TV"),
    mvs("음악/영상");

    private String name;

    THBoard(String name) {
        this.name = name;
    }
}
