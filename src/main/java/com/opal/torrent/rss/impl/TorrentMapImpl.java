package com.opal.torrent.rss.impl;

import com.opal.torrent.rss.model.TBoard;
import com.opal.torrent.rss.model.TMBoard;
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
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service(TorrentMapImpl.SITE_SIMPLE_NAME)
public class TorrentMapImpl implements ITorrentService {

    final static String SITE_SIMPLE_NAME = "tm";

    @Value("${opal.torrent.rss.site.tm.url}")
    private String BASE_URL;

    @Override
    public String getSiteSimpleName() {
        return SITE_SIMPLE_NAME;
    }

    @Override
    public List<Document> queryList(List<String> boards, String search, int page, int maxPage) {
        List<Document> docList = new ArrayList<>();
        if (boards == null || boards.isEmpty()) {
            boards = Arrays.stream(TMBoard.values()).map(TMBoard::name).collect(Collectors.toList());
        }

        boards.stream().parallel().forEach(board -> IntStream.range(page, maxPage).parallel().forEach(p -> {
            Map<String, Object> mapParam = new HashMap<>();
            mapParam.put("bo_table", board);
            mapParam.put("sfl", "wr_subject");
            mapParam.put("sop", "and");
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
    public Elements getTableElements(Document doc) {
        return doc.select("form div table tr[class]");
    }

    @Override
    public List<Category> getCategory(Element element, String board) {
        Category category = new Category();
        category.setDomain("TM");
        category.setValue(TMBoard.valueOf(board).getName());
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
        String dateStr = element.select("td[class=td_datetime]").text();

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
        board.setName(queryParam.get("bo_table"));
        board.setId(queryParam.get("wr_id"));
        return board;
    }

    @Override
    public long getFileSize(Element element) {
        return FileUtil.getFileSize(element.select("td[class=td_size]").text());
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
    public String getMagnet(Document doc, String prefer) {
        Elements fileNameEm = doc.select("section[id=bo_v_file] ul li a strong");
        if (fileNameEm.isEmpty()) {
            return null;
        }
        int magnetIndex = getMagnetIndex(fileNameEm, prefer);
        Elements magnetEm = doc.select("section[id=bo_v_file] ul li div a");
        return magnetEm.get(magnetIndex).attr("href");
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
