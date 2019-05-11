package com.opal.torrent.rss.service;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.opal.torrent.rss.model.QqCaptchaResult;
import com.opal.torrent.rss.util.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.json.BasicJsonParser;
import org.springframework.boot.json.JsonParser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class FileTenderService {

    private final static String QQ_CAPTCHA_URL = "https://ssl.captcha.qq.com/cap_union_prehandle";

    public String getDownloadUrl(String fileTenderUrl) {

        try (final WebClient webClient = new WebClient()) {
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setRedirectEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setUseInsecureSSL(true);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);

            final HtmlPage page = webClient.getPage(fileTenderUrl);
            webClient.waitForBackgroundJavaScript(1000);

            DomElement anchor = page.getElementByName("download");
            anchor.setAttribute("style", "display: block; width: 100px;");
            anchor.click();

            webClient.waitForBackgroundJavaScript(2000);


            String url = page.getElementById("Down").getAttribute("action");
            if (StringUtils.isEmpty(url))
                url = "http://file.filetender.com/file.php";

            Map<String, Object> params = new HashMap<>();
            params.put("key", page.getElementByName("key").getAttribute("value"));
            params.put("Ticket", page.getElementById("Ticket").getAttribute("value"));
            params.put("Randstr", page.getElementById("Randstr").getAttribute("value"));
            params.put("UserIP", page.getElementByName("UserIP").getAttribute("value"));
            log.debug("param-1: {}", params);

            if (StringUtils.isEmpty(params.get("key"))) {
                return null;
            }
            if (StringUtils.isEmpty(params.get("Ticket")) || StringUtils.isEmpty(params.get("Randstr"))) {
                String appId = anchor.getAttribute("data-appid");
                QqCaptchaResult qqCaptchaResult = getCaptchaResult(fileTenderUrl, appId);
                if (qqCaptchaResult != null) {
                    params.put("Ticket", qqCaptchaResult.getTicket());
                    params.put("Randstr", qqCaptchaResult.getRandstr());
                    log.debug("param-2: {}", params);
                }
            }
            fileTenderUrl = String.format("%s?%s", url, WebUtil.urlEncodeUTF8(params));
            log.info("torrentUrl: {}", fileTenderUrl);
            return fileTenderUrl;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private QqCaptchaResult getCaptchaResult(String referrer, String appId) {
        final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:66.0) Gecko/20100101 Firefox/66.0";
        try {
            Document document = Jsoup.connect(QQ_CAPTCHA_URL)
                    .referrer(referrer)
                    .userAgent(USER_AGENT)
                    .data("aid", appId)
                    .data("accver", "1")
                    .data("showtype", "popup")
                    .data("ua", Base64.getEncoder().encodeToString(USER_AGENT.getBytes()))
                    .data("noheader", "1")
                    .data("fb", "1")
                    .data("tkid", appId)
                    .data("grayscale", "1")
                    .data("clientype", "2")
                    .data("subsid", "1")
                    .data("callback", "_aq_" + appId)
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .get();
            log.debug("getCaptchaResult(): {}", document.text());
            String contents = document.text().substring(document.text().indexOf("(") + 1).replace(")", "");
            JsonParser parser = new BasicJsonParser();
            Map<String, Object> map = parser.parseMap(contents);
            return QqCaptchaResult.builder()
                    .ticket((String) map.get("ticket"))
                    .randstr((String) map.get("randstr"))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
