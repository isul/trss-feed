package com.opal.torrent.rss.impl;

import com.opal.torrent.rss.model.TBoard;
import com.opal.torrent.rss.model.TFBoard;
import com.opal.torrent.rss.model.TitleLink;
import com.opal.torrent.rss.service.FileTenderService;
import com.opal.torrent.rss.service.ITorrentService;
import com.opal.torrent.rss.util.WebUtil;
import com.rometools.rome.feed.rss.Category;
import lombok.RequiredArgsConstructor;
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

@Service(TFreecaImpl.SITE_SIMPLE_NAME)
@RequiredArgsConstructor
public class TFreecaImpl implements ITorrentService {
    final static String SITE_SIMPLE_NAME = "tf";

    private final FileTenderService fileTenderService;

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
    public String getDownloadUrl(Document doc, String prefer) {
        Elements downLinkEm = doc.select("tbody td[align=left] a[class=font11]");
        if (downLinkEm.isEmpty()) {
            return null;
        }
        int index = getTorrentIndex(downLinkEm, prefer);
        String href = downLinkEm.get(index).attr("href");
        return fileTenderService.getDownloadUrl(href);

    }

    private int getTorrentIndex(Elements elements, String prefer) {
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
