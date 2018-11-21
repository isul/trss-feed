package com.opal.torrent.rss.util;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WebUtil {

    private static final Charset BASE_URL_CHARSET = Charset.forName("UTF-8");

    public static String getRequestUrl(HttpServletRequest request) {
        StringBuffer requestUrl = request.getRequestURL();
        requestUrl.append("?");
        requestUrl.append(request.getQueryString());
        return requestUrl.toString();
    }

    public static URI getRequestBaseUri(HttpServletRequest request) throws URISyntaxException {
        URI requestUri = new URI(request.getRequestURL().toString());
        return new URI(requestUri.getScheme(), requestUri.getAuthority(), request.getContextPath(), null, null);
    }

    public static String urlEncodeUTF8(Map<String, Object> map) {
        List<NameValuePair> paramList = new ArrayList<>();
        for (String key : map.keySet()) {
            paramList.add(new BasicNameValuePair(key, map.get(key).toString()));
        }
        return URLEncodedUtils.format(paramList, BASE_URL_CHARSET);
    }

    public static Map<String, String> convertQueryStringToMap(String url) throws URISyntaxException {
        List<NameValuePair> params = URLEncodedUtils.parse(new URI(url), BASE_URL_CHARSET);
        Map<String, String> queryStringMap = new HashMap<>();
        for (NameValuePair param : params) {
            queryStringMap.put(param.getName(), param.getValue());
        }
        return queryStringMap;
    }
}
