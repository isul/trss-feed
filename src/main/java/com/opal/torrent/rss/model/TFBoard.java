package com.opal.torrent.rss.model;

import lombok.Getter;

@Getter
public enum TFBoard {
    tmovie("영화"),
    tdrama("드라마"),
    tent("예능"),
    tv("TV"),
    tani("애니"),
    tmusic("음악");

    private String name;

    TFBoard(String name) {
        this.name = name;
    }
}
