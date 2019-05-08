package com.opal.torrent.rss.model;

import lombok.Getter;

@Getter
public enum TMBoard {
    movie_new("최신영화"),
    movie_old("이전영화"),
    kr_ent("예능/오락"),
    kr_daq("시사/요양"),
    kr_drama("드라마"),
    eng_drama("외국드라마"),
    ani("애니"),
    music("음악");

    private String name;

    TMBoard(String name) {
        this.name = name;
    }
}
