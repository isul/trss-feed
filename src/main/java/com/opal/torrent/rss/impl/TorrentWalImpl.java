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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

@Service(TorrentWalImpl.SITE_SIMPLE_NAME)
public class TorrentWalImpl implements ITorrentService {

    final static String SITE_SIMPLE_NAME = "tw";

    @Value("${opal.torrent.rss.site.tw.url}")
    private String BASE_URL;

    @Override
    public String getSiteSimpleName() {
        return SITE_SIMPLE_NAME;
    }

    @Override
    public List<Document> queryList(List<String> boards, String search, int page, int maxPage) {
        List<Document> docList = new ArrayList<>();
        IntStream.range(page, maxPage).parallel().forEach(p -> {
            Map<String, Object> mapParam = new HashMap<>();
            mapParam.put("k", search);
            mapParam.put("page", page);
            String param = WebUtil.urlEncodeUTF8(mapParam);
            String queryUrl = String.format("%s/bbs/s.php?%s", BASE_URL, param);
            try {
                docList.add(Jsoup.connect(queryUrl).method(Connection.Method.GET).ignoreContentType(true).get());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return docList;
    }

    @Override
    public Elements getTableElements(Document doc) {
        return doc.select("table[class=board_list] tr[class=bg1]");
    }

    @Override
    public List<Category> getCategory(Element element, String board) {
        Category category = new Category();
        category.setDomain("TW");
        Elements subjectElement = element.select("td[class=subject] a");
        if (subjectElement != null) {
            category.setValue(subjectElement.get(0).text());
        } else {
            category.setValue(THBoard.valueOf(board).getName());
        }
        return Collections.singletonList(category);
    }

    @Override
    public TitleLink getTitleAndLink(Element element) {
        TitleLink titleLink = new TitleLink();
        Element titleElem = element.select("td[class=subject] a").get(1);
        titleLink.setTitle(titleElem.text());

        String linkUrl = titleElem.attr("href");
        linkUrl = linkUrl.replaceFirst("../", "");
        titleLink.setLink(String.format("%s/%s", BASE_URL, linkUrl));
        return titleLink;
    }

    @Override
    public Date getDate(Element element) {
        String dateStr = element.select("td[class=datetime]").text();
        LocalDate localDate = LocalDate.now();
        if (StringUtils.isEmpty(dateStr) || !dateStr.contains("-")) {
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        }
        String[] date = dateStr.split("-");
        localDate = localDate.withMonth(Integer.parseInt(date[0]))
                .withDayOfMonth(Integer.parseInt(date[1]));
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public TBoard getBoard(String linkUrl) {
        TBoard board = new TBoard();
        Pattern pattern = Pattern.compile("net/(.+)/(\\d+)\\.html");
        Matcher matcher = pattern.matcher(linkUrl);
        if (matcher.find()) {
            board.setName(matcher.group(1));
            board.setId(matcher.group(2));
            return board;
        }
        return board;
    }

    @Override
    public long getFileSize(Element element) {
        return FileUtil.getFileSize(element.select("td[class=hit]").text());
    }

    @Override
    public Document queryView(String board, String boardId) throws IOException {
        String queryUrl = String.format("%s/%s/%s.html", BASE_URL, board, boardId);
        return Jsoup.connect(queryUrl).method(Connection.Method.GET).ignoreContentType(true).get();
    }

    @Override
    public String getMagnet(Document doc, String prefer) {
        Elements fileTableEm = doc.select("table[id=file_table] td");
        Elements fileNameEm = fileTableEm.select("span");
        if (fileTableEm.isEmpty()) {
            return null;
        }
        int magnetIndex = getMagnetIndex(fileNameEm, prefer);
        Elements magnetEm = doc.select("img[onclick]");
        String onclick = magnetEm.get(magnetIndex).attr("onclick");
        Pattern pattern = Pattern.compile("'(.+)',");
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
