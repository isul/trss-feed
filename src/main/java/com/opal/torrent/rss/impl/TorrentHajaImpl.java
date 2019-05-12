package com.opal.torrent.rss.impl;

import com.opal.torrent.rss.model.TBoard;
import com.opal.torrent.rss.model.THBoard;
import com.opal.torrent.rss.model.TitleLink;
import com.opal.torrent.rss.service.ITorrentService;
import com.opal.torrent.rss.util.FileUtil;
import com.opal.torrent.rss.util.WebUtil;
import com.rometools.rome.feed.rss.Category;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.IntStream;

@Service(TorrentHajaImpl.SITE_SIMPLE_NAME)
@RequiredArgsConstructor
public class TorrentHajaImpl implements ITorrentService {

    final static String SITE_SIMPLE_NAME = "th";

    private final RestTemplate restTemplate;

    @Value("${opal.torrent.rss.site.th.url}")
    private String BASE_URL;

    @Override
    public String getSiteSimpleName() {
        return SITE_SIMPLE_NAME;
    }

    @Override
    public List<Document> queryList(List<String> boards, String search, int page, int maxPage) {
        List<Document> docList = new ArrayList<>();
        if (boards == null || boards.isEmpty()) {
            IntStream.range(page, maxPage).parallel().forEach(p -> {
                Map<String, Object> mapParam = new HashMap<>();
                mapParam.put("mid", "search");
                mapParam.put("q", search);
                mapParam.put("page", page);
                String param = WebUtil.urlEncodeUTF8(mapParam);
                String queryUrl = String.format("%s/index.php?%s", BASE_URL, param);
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
            mapParam.put("mid", board);
            mapParam.put("q", search);
            mapParam.put("page", page);
            String param = WebUtil.urlEncodeUTF8(mapParam);
            String queryUrl = String.format("%s/index.php?%s", BASE_URL, param);
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
        mapParam.put("mid", board);
        mapParam.put("document_srl", boardId);
        String param = WebUtil.urlEncodeUTF8(mapParam);
        String queryUrl = String.format("%s/index.php?%s", BASE_URL, param);
        return Jsoup.connect(queryUrl).method(Connection.Method.GET).ignoreContentType(true).get();
    }

    @Override
    public Elements getTableElements(Document doc) {
        Elements elements = new Elements();
        doc.select("div[class=board_list] tbody tr").forEach(element -> {
            if (StringUtils.isEmpty(element.attr("class"))) {
                elements.add(element);
            }
        });
        return elements;
    }

    @Override
    public List<Category> getCategory(Element element, String board) {
        Category category = new Category();
        category.setDomain("TH");
        Elements subjectElement = element.select("td[class=no]");
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
        if (linkUrl.startsWith("http")) {
            titleLink.setLink(linkUrl);
        } else {
            linkUrl = linkUrl.replaceFirst("./", "");
            titleLink.setLink(String.format("%s/%s", BASE_URL, linkUrl));
        }
        return titleLink;
    }

    @Override
    public Date getDate(Element element) {
        String dateStr = element.select("td[class=time]").text();

        LocalDate localNowDate = LocalDate.now();
        if (StringUtils.isEmpty(dateStr) || !dateStr.contains(".")) {
            return Date.from(localNowDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        String[] date = dateStr.split("\\.");

        LocalDate localDate;
        if (date.length == 3) {
            localDate = localNowDate.withYear(Integer.valueOf(date[0]))
                    .withMonth(Integer.valueOf(date[1]))
                    .withDayOfMonth(Integer.valueOf(date[2]));
        } else {
            localDate = localNowDate.withMonth(Integer.valueOf(date[0]))
                    .withDayOfMonth(Integer.valueOf(date[1]));
        }
        if (!localDate.isEqual(localNowDate) && localDate.isAfter(localNowDate)) {
            localDate = localDate.withYear(localDate.getYear() - 1);
        }
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public TBoard getBoard(String linkUrl) throws URISyntaxException {
        TBoard board = new TBoard();
        Map<String, String> queryParam = WebUtil.convertQueryStringToMap(linkUrl);
        board.setName(queryParam.get("mid"));
        board.setId(queryParam.get("document_srl"));
        return board;
    }

    @Override
    public long getFileSize(Element element) {
        return FileUtil.getFileSize(element.select("td[class=torrent]").text());
    }

    @Override
    public String getDownloadUrl(Document doc, String prefer) {
        Elements fileNameEm = doc.select("div[class=read_torrent] table");

        int magnetIndex = getMagnetIndex(fileNameEm, prefer);
        Elements magnetEm = fileNameEm.select("tfoot a[class=magnet]");
        String href = magnetEm.get(magnetIndex).attr("href");
        ResponseEntity<String> entity = restTemplate.getForEntity(href, String.class);

        List<String> location = entity.getHeaders().get("location");
        if (location == null || location.isEmpty()) {
            return null;
        }
        return location.get(0);
    }

    private int getMagnetIndex(Elements elements, String prefer) {
        if (StringUtils.isEmpty(prefer)) {
            return 0;
        }
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            String title = element.select("thead").text();
            if (title.contains(prefer)) {
                return i;
            }
        }
        return 0;
    }

}
