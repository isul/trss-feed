package com.opal.torrent.rss.model;

import lombok.Getter;

@Getter
public enum THBoard {
    torrent_movie("외국영화"),
    torrent_kmovie("한국영화"),
    torrent_drama("드라마"),
    torrent_ent("예능/오락"),
    torrent_docu("다큐/교양"),
    torrent_video("뮤비/공연"),
    torrent_sports("스포츠"),
    torrent_ani("애니"),
    torrent_music("음악");

    private String name;

    THBoard(String name) {
        this.name = name;
    }
}
