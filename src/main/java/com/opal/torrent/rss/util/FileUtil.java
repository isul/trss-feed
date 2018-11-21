package com.opal.torrent.rss.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtil {

    public static long getFileSize(String fileSize) {
        Pattern pattern = Pattern.compile("(.+)([GMKT])");
        Matcher matcher = pattern.matcher(fileSize);
        if (!matcher.find()) {
            return 0;
        }
        String unit = matcher.group(2);
        if ("G".equals(unit)) {
            return (long) Float.parseFloat(matcher.group(1)) * 1073741824;
        }
        if ("M".equals(unit)) {
            return (long) Float.parseFloat(matcher.group(1)) * 1048576;
        }
        if ("K".equals(unit)) {
            return (long) Float.parseFloat(matcher.group(1)) * 1024;
        }
        if ("T".equals(unit)) {
            return (long) Float.parseFloat(matcher.group(1)) * 1024;
        }
        return (long) Float.parseFloat(matcher.group(1));
    }
}
