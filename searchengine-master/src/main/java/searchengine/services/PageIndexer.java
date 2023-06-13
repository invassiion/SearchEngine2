package searchengine.services;

import searchengine.model.PageEntity;

public interface PageIndexer {
    void indexHtml(String html, PageEntity indexingPage);
    void  refreshIndex(String html, PageEntity refreshPage);
}