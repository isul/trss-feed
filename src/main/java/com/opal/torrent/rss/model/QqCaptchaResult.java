package com.opal.torrent.rss.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QqCaptchaResult {
    private String ticket;
    private String randstr;
}
