package searchengine.services.implement;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.data.domain.Page;
import searchengine.config.Connection;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexer;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@AllArgsConstructor
public class PageFinder extends RecursiveAction {
    private final PageIndexer pageIndexer;
    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final AtomicBoolean indexingProcessing;
    private final Connection connection;
    private final Set<String> urlSet = new HashSet<>();
    private final String page;
    private final SiteEntity siteDomain;
    private final ConcurrentHashMap<String, PageEntity> resultForkJoinPoolIndexedPages;

    public PageFinder(SiteRepository siteRepository, PageRepository pageRepository, SiteEntity siteDomain, String page, ConcurrentHashMap<String, PageEntity> resultForkJoinPoolIndexedPages, Connection connection, LemmaService lemmaService, PageIndexer pageIndexer, AtomicBoolean indexingProcessing) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.page = page;
       this.resultForkJoinPoolIndexedPages = resultForkJoinPoolIndexedPages;
        this.connection = connection;
        this.indexingProcessing = indexingProcessing;
        this.siteDomain = siteDomain;
        this.lemmaService = lemmaService;
        this.pageIndexer = pageIndexer;
    }



    @Override
    protected void compute() {

        if (resultForkJoinPoolIndexedPages.get(page) != null || !indexingProcessing.get()) {
            return;
        }
        PageEntity indexingPage = new PageEntity();
        indexingPage.setPath(page);
        indexingPage.setSiteId(siteDomain.getId());
      /*if connection is blocked use ->
        Thread.sleep(1000); */
        try {
            org.jsoup.Connection connect = Jsoup.connect(siteDomain.getUrl() + page).userAgent(connection.getUserAgent()).referrer(connection.getReferer());
            Document doc = connect.timeout(60000).get();

            indexingPage.setContent(doc.head() + String.valueOf(doc.body()));
            Elements pages = doc.getElementsByTag("a");
            for (org.jsoup.nodes.Element element : pages)
                if (!element.attr("href").isEmpty() && element.attr("href").charAt(0) == '/') {
                    if (resultForkJoinPoolIndexedPages.get(page) != null || !indexingProcessing.get()) {
                        return;
                    } else if (resultForkJoinPoolIndexedPages.get(element.attr("href")) == null) {
                        urlSet.add(element.attr("href"));
                    }
                }
            indexingPage.setCode(doc.connection().response().statusCode());
        } catch (Exception ex) {
            String message = ex.toString();
            int errorCode;
            if (message.contains("UnsupportedMimeTypeException")) {
                errorCode = 415;
            } else if (message.contains("Status=401")) {
                errorCode = 401;
            } else if (message.contains("UnknownHostException")) {
                errorCode = 401;
            } else if (message.contains("Status=403")) {
                errorCode = 403;
            } else if (message.contains("Status=404")) {
                errorCode = 404;
            } else if (message.contains("Status=500")) {
                errorCode = 401;
            } else if (message.contains("ConnectException: Connection refused")) {
                errorCode = 500;
            } else if (message.contains("SSLHandshakeException")) {
                errorCode = 525;
            } else if (message.contains("Status=503")) {
                errorCode = 503;
            } else {
                errorCode = -1;
            }
            indexingPage.setCode(errorCode);
            return;
        }
        if (resultForkJoinPoolIndexedPages.get(page) != null || !indexingProcessing.get()) {
            return;
        }
        resultForkJoinPoolIndexedPages.putIfAbsent(indexingPage.getPath(), indexingPage);
        SiteEntity siteEntity = siteRepository.findById(siteDomain.getId()).orElseThrow();
        siteEntity.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
        siteRepository.save(siteEntity);
        pageRepository.save(indexingPage);
        pageIndexer.indexHtml(indexingPage.getContent(), indexingPage);
        List<PageFinder> indexingPagesTasks = new ArrayList<>();
        for (String url : urlSet) {
            if (resultForkJoinPoolIndexedPages.get(url) == null && indexingProcessing.get()) {
                PageFinder task = new PageFinder(siteRepository, pageRepository, siteEntity, url, resultForkJoinPoolIndexedPages, connection, lemmaService, pageIndexer, indexingProcessing);
                task.fork();
                indexingPagesTasks.add(task);
            }
        }
        for (PageFinder page : indexingPagesTasks) {
            if (!indexingProcessing.get()) {
                return;
            }
            page.join();
        }

    }
    public void refreshPage() {

        PageEntity indexingPage = new PageEntity();
        indexingPage.setPath(page);
        indexingPage.setSiteId(siteDomain.getId());
        /*if connection is blocked use ->
        Thread.sleep(1000); */
        try {
            org.jsoup.Connection connect = Jsoup.connect(siteDomain.getUrl() + page).userAgent(connection.getUserAgent()).referrer(connection.getReferer());
            Document doc = connect.timeout(60000).get();

            indexingPage.setContent(doc.head() + String.valueOf(doc.body()));
            indexingPage.setCode(doc.connection().response().statusCode());
        } catch (Exception ex) {
            String message = ex.toString();
            int errorCode;
            if (message.contains("UnsupportedMimeTypeException")) {
                errorCode = 415;
            } else if (message.contains("Status=401")) {
                errorCode = 401;
            } else if (message.contains("UnknownHostException")) {
                errorCode = 401;
            } else if (message.contains("Status=403")) {
                errorCode = 403;
            } else if (message.contains("Status=404")) {
                errorCode = 404;
            } else if (message.contains("Status=500")) {
                errorCode = 401;
            } else if (message.contains("ConnectException: Connection refused")) {
                errorCode = 500;
            } else if (message.contains("SSLHandshakeException")) {
                errorCode = 525;
            } else if (message.contains("Status=503")) {
                errorCode = 503;
            } else {
                errorCode = -1;
            }
            indexingPage.setCode(errorCode);
            return;
        }
        SiteEntity siteEntity = siteRepository.findById(siteDomain.getId()).orElseThrow();
        siteEntity.setStatusTime(Timestamp.valueOf(LocalDateTime.now()));
        siteRepository.save(siteEntity);

        PageEntity pageForRefresh = pageRepository.findPageBySiteIdAndPath(page,siteEntity.getId());
        pageForRefresh.setCode(indexingPage.getCode());
        pageForRefresh.setContent(indexingPage.getContent());
        pageRepository.save(pageForRefresh);

        pageIndexer.refreshIndex(indexingPage.getContent(), pageForRefresh);
    }

}