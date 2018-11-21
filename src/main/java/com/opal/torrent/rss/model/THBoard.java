package com.opal.torrent.rss.model;

import lombok.Getter;

@Getter
public enum THBoard {
    torrent_movie("외국영화"),
    torrent_kmovie("한국영화"),
    torrent_ent("예능/오락"),
    torrent_docu("다큐/교양"),
    torrent_video("뮤비/공연");

    private String name;

    THBoard(String name) {
        this.name = name;
    }
}
