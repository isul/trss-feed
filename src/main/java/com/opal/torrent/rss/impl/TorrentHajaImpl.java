package com.opal.torrent.rss.impl;

import com.opal.torrent.rss.model.TBoard;
import com.opal.torrent.rss.model.THBoard;
import com.opal.torrent.rss.model.TitleLink;
import com.opal.torrent.rss.service.ITorrentService;
import com.opal.torrent.rss.util.FileUtil;
import com.opal.torrent.rss.util.WebUtil;
import com.rometools.rome.feed.rss.Category;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Service(TorrentHajaImpl.SITE_SIMPLE_NAME)
public class TorrentHajaImpl implements ITorrentService {

    final static String SITE_SIMPLE_NAME = "th";

    @Value("${opal.torrent.rss.site.th.url}")
    private String BASE_URL;

    @Override
    public String getSiteSimpleName() {
        return SITE_SIMPLE_NAME;
    }

    @Override
    public List<Document> queryList(List<String> boards, String search, int page, int maxPage) {
        List<Document> docList = new ArrayList<>();
        if (boards.isEmpty()) {
            IntStream.range(page, maxPage).parallel().forEach(p -> {
                Map<String, Object> mapParam = new HashMap<>();
                mapParam.put("search_flag", "search");
                mapParam.put("stx", search);
                String param = WebUtil.urlEncodeUTF8(mapParam);
                String queryUrl = String.format("%s/bbs/search.php?%s", BASE_URL, param);
                try {
                    docList.add(Jsoup.connect(queryUrl).method(Connection.Method.GET).ignoreContentType(true).get());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            return docList;
        }
        boards.stream().parallel().forEach(board -> IntStream.range(page, maxPage).parallel().forEach(p -> {
            Map<String, Object> mapParam = new HashMap<>();
            mapParam.put("bo_table", board);
            mapParam.put("sca", "");
            mapParam.put("sop", "and");
            mapParam.put("sfl", "wr_subject");
            mapParam.put("stx", search);
            mapParam.put("page", page);
            String param = WebUtil.urlEncodeUTF8(mapParam);
            String queryUrl = String.format("%s/bbs/board.php?%s", BASE_URL, param);
            try {
                docList.add(Jsoup.connect(queryUrl).method(Connection.Method.GET).ignoreContentType(true).get());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        return docList;
    }

    @Override
    public Document queryView(String board, String boardId) throws IOException {
        Map<String, Object> mapParam = new HashMap<>();
        mapParam.put("bo_table", board);
        mapParam.put("wr_id", boardId);
        String param = WebUtil.urlEncodeUTF8(mapParam);
        String queryUrl = String.format("%s/bbs/board.php?%s", BASE_URL, param);
        return Jsoup.connect(queryUrl).method(Connection.Method.GET).ignoreContentType(true).get();
    }

    @Override
    public Elements getTableElements(Document doc) {
        return doc.select("div[class=board-list-body] tbody tr");
    }

    @Override
    public List<Category> getCategory(Element element, String board) {
        Category category = new Category();
        category.setDomain("TH");
        Elements subjectElement = element.select("td[class=td-num]");
        if (subjectElement != null) {
            category.setValue(subjectElement.text());
        } else {
            category.setValue(THBoard.valueOf(board).getName());
        }
        return Collections.singletonList(category);
    }

    @Override
    public TitleLink getTitleAndLink(Element element) {
        TitleLink titleLink = new TitleLink();
        Element titleElem = element.select("a").get(0);
        titleLink.setTitle(titleElem.text());

        String linkUrl = titleElem.attr("href");
        titleLink.setLink(linkUrl);
        return titleLink;
    }

    @Override
    public Date getDate(Element element) {
        String dateStr = element.select("td[class=td-date hidden-xs]").text();
        if ("오늘".equals(dateStr)) {
            return new Date();
        }
        String[] date = dateStr.split("\\.");
        LocalDateTime localDateTime = LocalDate.now().withMonth(Integer.parseInt(date[0]))
                .withDayOfMonth(Integer.parseInt(date[1]))
                .atTime(0, 0);
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public TBoard getBoard(String linkUrl) throws URISyntaxException {
        if (linkUrl.contains("board.php")) {
            TBoard board = new TBoard();
            Map<String, String> queryParam = WebUtil.convertQueryStringToMap(linkUrl);
            board.setName(queryParam.get("bo_table"));
            board.setId(queryParam.get("wr_id"));
            return board;
        }
        TBoard board = new TBoard();
        Pattern pattern = Pattern.compile("com/(.+)/(\\d+)\\.html");
        Matcher matcher = pattern.matcher(linkUrl);
        if (matcher.find()) {
            board.setName(matcher.group(1));
            board.setId(matcher.group(2));
            return board;
        }
        return null;
    }

    @Override
    public long getFileSize(Element element) {
        return FileUtil.getFileSize(element.select("td[class=td-filesize]").text());
    }

    @Override
    public String getMagnet(Document doc, String prefer) {
        Elements fileNameEm = doc.select("th[class=title]");
        if (fileNameEm.isEmpty()) {
            return null;
        }
        int magnetIndex = getMagnetIndex(fileNameEm, prefer);
        Elements magnetEm = doc.select("tfoot button[type=button]");
        String onclick = magnetEm.get(magnetIndex).attr("onclick");
        Pattern pattern = Pattern.compile("\\('(.+)'\\)");
        Matcher matcher = pattern.matcher(onclick);
        if (!matcher.find()) {
            return null;
        }
        return String.format("magnet:?xt=urn:btih:%s", matcher.group(1));
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
