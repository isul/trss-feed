package com.opal.torrent.rss.service;

import com.opal.torrent.rss.model.TBoard;
import com.opal.torrent.rss.model.TitleLink;
import com.rometools.rome.feed.rss.Category;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

public interface ITorrentService {

    String getSiteSimpleName();

    List<Document> queryList(List<String> boards, String search, int page, int maxPage) throws IOException;

    Elements getTableElements(Document doc);

    List<Category> getCategory(Element element, String board);

    TitleLink getTitleAndLink(Element element);

    Date getDate(Element element);

    TBoard getBoard(String linkUrl) throws URISyntaxException;

    long getFileSize(Element element);

    Document queryView(String board, String boardId) throws IOException;

    String getMagnet(Document doc, String prefer);
}
