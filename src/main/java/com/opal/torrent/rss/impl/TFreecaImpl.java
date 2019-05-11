package com.opal.torrent.rss.impl;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.opal.torrent.rss.model.QqCaptchaResult;
import com.opal.torrent.rss.model.TBoard;
import com.opal.torrent.rss.model.TFBoard;
import com.opal.torrent.rss.model.TitleLink;
import com.opal.torrent.rss.service.ITorrentService;
import com.opal.torrent.rss.util.WebUtil;
import com.rometools.rome.feed.rss.Category;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.boot.json.BasicJsonParser;
import org.springframework.boot.json.JsonParser;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service(TFreecaImpl.SITE_SIMPLE_NAME)
public class TFreecaImpl implements ITorrentService {
    final Logger logger = LoggerFactory.getLogger(getClass());
    final static String SITE_SIMPLE_NAME = "tf";
    final static String QQ_CAPTCHA_URL = "https://ssl.captcha.qq.com/cap_union_prehandle";

    @Value("${opal.torrent.rss.site.tf.url}")
    private String BASE_URL;

    @Override
    public String getSiteSimpleName() {
        return SITE_SIMPLE_NAME;
    }

    @Override
    public List<Document> queryList(List<String> boards, String search, int page, int maxPage) {
        List<Document> docList = new ArrayList<>();
        if (boards == null || boards.isEmpty()) {
            boards = Arrays.stream(TFBoard.values()).map(TFBoard::name).collect(Collectors.toList());
        }

        boards.stream().parallel().forEach(board -> IntStream.range(page, maxPage).parallel().forEach(p -> {
            Map<String, Object> mapParam = new HashMap<>();
            mapParam.put("mode", "list");
            mapParam.put("b_id", board);
            mapParam.put("sc", search);
            mapParam.put("x", 0);
            mapParam.put("y", 0);
            mapParam.put("page", page);
            String param = WebUtil.urlEncodeUTF8(mapParam);
            String queryUrl = String.format("%s/board.php?%s", BASE_URL, param);
            try {
                docList.add(Jsoup.connect(queryUrl).method(Connection.Method.GET).ignoreContentType(true).get());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        return docList;
    }

    @Override
    public Elements getTableElements(Document doc) {
        Elements trEm = new Elements();
        Elements tableEm = doc.select("table[class=b_list] tr");
        tableEm.forEach(element -> {
            if ("notice".equals(element.select("td[class=num]").text())) {
                return;
            }
            if (StringUtils.isEmpty(element.select("td[class=datetime]").text())) {
                return;
            }
            trEm.add(element);
        });
        return trEm;
    }

    @Override
    public List<Category> getCategory(Element element, String board) {
        Category category = new Category();
        category.setDomain("TF");
        category.setValue(TFBoard.valueOf(board).getName());
        return Collections.singletonList(category);
    }

    @Override
    public TitleLink getTitleAndLink(Element element) {
        TitleLink titleLink = new TitleLink();
        Element titleElem = element.select("a").get(1);
        titleLink.setTitle(titleElem.text());

        String linkUrl = titleElem.attr("href");
        titleLink.setLink(String.format("%s/%s", BASE_URL, linkUrl));
        return titleLink;
    }

    @Override
    public Date getDate(Element element) {
        String dateStr = element.select("td[class=datetime]").text();

        LocalDate localNowDate = LocalDate.now();
        if (StringUtils.isEmpty(dateStr) || !dateStr.contains("-")) {
            return Date.from(localNowDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        String[] date = dateStr.split("-");
        LocalDate localDate = localNowDate.withMonth(Integer.parseInt(date[0]))
                .withDayOfMonth(Integer.parseInt(date[1]));
        if (!localDate.isEqual(localNowDate) && localDate.isAfter(localNowDate)) {
            localDate = localDate.withYear(localDate.getYear() - 1);
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public TBoard getBoard(String linkUrl) throws URISyntaxException {
        TBoard board = new TBoard();
        Map<String, String> queryParam = WebUtil.convertQueryStringToMap(linkUrl);
        board.setName(queryParam.get("b_id"));
        board.setId(queryParam.get("id"));
        return board;
    }

    @Override
    public long getFileSize(Element element) {
        return 1;
    }

    @Override
    public Document queryView(String board, String boardId) throws IOException {
        Map<String, Object> mapParam = new HashMap<>();
        mapParam.put("mode", "view");
        mapParam.put("page", "1");
        mapParam.put("b_id", board);
        mapParam.put("id", boardId);
        String param = WebUtil.urlEncodeUTF8(mapParam);
        String queryUrl = String.format("%s/board.php?%s", BASE_URL, param);
        return Jsoup.connect(queryUrl).method(Connection.Method.GET).ignoreContentType(true).get();
    }

    @Override
    public String getMagnet(Document doc, String prefer) {
        Elements elements = doc.select("body");
        if (elements.isEmpty()) {
            return null;
        }
        String torrent = getTorrentLink(elements);
        logger.info("getMagnet(): TorrentLink-> {}", torrent);
        try (final WebClient webClient = new WebClient()) {
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setRedirectEnabled(true);
            webClient.getOptions().setCssEnabled(false);
            webClient.getOptions().setUseInsecureSSL(true);
            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            
            final HtmlPage page = webClient.getPage(torrent);
            webClient.waitForBackgroundJavaScript(1000);
            
            DomElement anchor = page.getElementByName("download");
            anchor.setAttribute("style", "display: block; width: 100px;");
            anchor.click();
            webClient.waitForBackgroundJavaScript(2000);
            String url = page.getElementById("Down").getAttribute("action");
            String key = page.getElementByName("key").getAttribute("value");
            String ticket = page.getElementById("Ticket").getAttribute("value");
            String randstr = page.getElementById("Randstr").getAttribute("value");
            String userIP = page.getElementByName("UserIP").getAttribute("value");
            Map<String, Object> mapParam = new HashMap<>();
            mapParam.put("key", key);
            mapParam.put("Ticket", ticket);
            mapParam.put("Randstr", randstr);
            mapParam.put("UserIP", userIP);
            logger.debug("getMagnet(): mapParam-> {}", mapParam);
            if (!StringUtils.isEmpty(url)
                    && !StringUtils.isEmpty(key)
                    && !StringUtils.isEmpty(ticket)
                    && !StringUtils.isEmpty(randstr)
                    && !StringUtils.isEmpty(userIP)) {
                String param = WebUtil.urlEncodeUTF8(mapParam);
                torrent = String.format("%s?%s", url, param);
            }
            else {
                if (StringUtils.isEmpty(url))
                    url = "http://file.filetender.com/file.php";
                String appId = anchor.getAttribute("data-appid");
                QqCaptchaResult qqCaptchaResult = getCaptchaResult(torrent, appId);
                if (qqCaptchaResult != null) {
                    mapParam.put("Ticket", qqCaptchaResult.getTicket());
                    mapParam.put("Randstr", qqCaptchaResult.getRandstr());
                    String param = WebUtil.urlEncodeUTF8(mapParam);
                    torrent = String.format("%s?%s", url, param);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("getMagnet(): torrent-> {}", torrent);
        return torrent;
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
            logger.debug("getCaptchaResult(): {}", document.text());
            String contents = document.text().substring(document.text().indexOf("(")+1).replace(")", "");
            JsonParser parser = new BasicJsonParser();
            Map<String, Object> map = parser.parseMap(contents);
            return QqCaptchaResult.builder()
                                    .ticket((String)map.get("ticket"))
                                    .randstr((String)map.get("randstr"))
                                    .build();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getTorrentLink(Elements elements) {
        for (Element el: elements) {
            if (el.hasAttr("href") && el.attr("href").contains("filetender")) {
                return el.attr("href");
            }
            Elements children = el.children();
            if (children.size() > 0) {
                String torrent = getTorrentLink(children);
                if (!StringUtils.isEmpty(torrent))
                    return torrent;
            }
        }
        return null;
    }

    private int getMagnetIndex(Elements elements, String prefer) {
        if (StringUtils.isEmpty(prefer)) {
            return 0;
        }
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            if (element.text().contains(prefer)) {
                return i;
            }
        }
        return 0;
    }

}
