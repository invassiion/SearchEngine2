package searchengine.services.implement;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.config.Connection;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.IndexedStatus;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.ApiService;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexer;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
public class ApiServiceImpl implements ApiService {
    @Autowired
    private PageIndexer pageIndexer;
    @Autowired
    private LemmaService lemmaService;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final SitesList sitesToIndexing;
    private final Set<SiteEntity> sitePagesAllFromDB;
    private final Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(ApiServiceImpl.class);
    private AtomicBoolean indexingProcessing;



    @Override
    public void startIndexing(AtomicBoolean indexingProcessing) {
        this.indexingProcessing = indexingProcessing;
        try {
            deleteSitePagesAndPagesInDB();
            addSitePagesToDB();
            indexAllSitePages();
        } catch (RuntimeException | InterruptedException ex) {
            logger.error("Error: ", ex);
        }
    }

    private void addSitePagesToDB() {
        for (Site siteApp : sitesToIndexing.getSites()) {
            SiteEntity siteEntityDAO = new SiteEntity();
            siteEntityDAO.setStatus(IndexedStatus.INDEXING);
            siteEntityDAO.setName(siteApp.getName());
            siteEntityDAO.setUrl(siteApp.getUrl().toString());
            siteRepository.save(siteEntityDAO);
        }

    }
    private void indexAllSitePages() throws InterruptedException {
        sitePagesAllFromDB.addAll(siteRepository.findAll());
        List<String> urlToIndexing = new ArrayList<>();
        for (Site siteApp : sitesToIndexing.getSites()) {
            urlToIndexing.add(siteApp.getUrl().toString());
        }
        sitePagesAllFromDB.removeIf(sitePage -> !urlToIndexing.contains(sitePage.getUrl()));
        List<Thread> indexingThreadList = new ArrayList<>();
        for (SiteEntity siteDomain :sitePagesAllFromDB) {
            Runnable indexSite = () -> {
                ConcurrentHashMap<String, PageEntity> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
                try {
                    System.out.println("Запущена индексация "+siteDomain.getUrl());
                    new ForkJoinPool().invoke(new PageFinder(siteRepository, pageRepository,siteDomain, "" , resultForkJoinPageIndexer, connection, lemmaService, pageIndexer, indexingProcessing ));
                } catch (SecurityException ex) {
                    SiteEntity siteEntity = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    siteEntity.setStatus(IndexedStatus.FAILED);
                    siteEntity.setLastError(ex.getMessage());
                    siteRepository.save(siteEntity);
                }
                if (!indexingProcessing.get()) {
                    SiteEntity siteEntity = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    siteEntity.setStatus(IndexedStatus.FAILED);
                    siteEntity.setLastError("Indexing stopped by user");
                    siteRepository.save(siteEntity);
                } else {
                    System.out.println("Проиндексирован сайт: " + siteDomain.getName());
                    SiteEntity siteEntity = siteRepository.findById(siteDomain.getId()).orElseThrow();
                    siteEntity.setStatus(IndexedStatus.INDEXED);
                    siteRepository.save(siteEntity);
                }

            };
            Thread thread = new Thread(indexSite);
            indexingThreadList.add(thread);
            thread.start();
        }
        for (Thread thread :indexingThreadList) {
            thread.join();
        }
        indexingProcessing.set(false);
    }

    @Override
    public void refreshPage(SiteEntity siteDomain, URL url) {
        SiteEntity existSiteEntity = siteRepository.getSitePageByUrl(siteDomain.getUrl());
        siteDomain.setId(existSiteEntity.getId());
        ConcurrentHashMap<String, PageEntity> resultForkJoinPageIndexer = new ConcurrentHashMap<>();
        try {
            System.out.println("Запущена переиндексация " + url.getHost());
            PageFinder f = new PageFinder(siteRepository, pageRepository, siteDomain, url.getPath(), resultForkJoinPageIndexer, connection, lemmaService, pageIndexer, indexingProcessing);
            f.refreshPage();
        } catch (SecurityException ex) {
            SiteEntity siteEntity = siteRepository.findById(siteDomain.getId()).orElseThrow();
            siteEntity.setStatus(IndexedStatus.FAILED);
            siteEntity.setLastError(ex.getMessage());
            siteRepository.save(siteEntity);
        }

        System.out.println("Проиндексирован сайт: " + siteDomain.getName());
        SiteEntity siteEntity = siteRepository.findById(siteDomain.getId()).orElseThrow();
        siteEntity.setStatus(IndexedStatus.INDEXED);
        siteRepository.save(siteEntity);
    }

    private void deleteSitePagesAndPagesInDB() {
        List<SiteEntity> sitesFromDB = siteRepository.findAll();
        for (SiteEntity siteEntityDB : sitesFromDB) {
            for (Site siteApp : sitesToIndexing.getSites()) {
                if (siteEntityDB.getUrl().equals(siteApp.getUrl())) {
                    siteRepository.deleteById(siteEntityDB.getId());
                }
            }
        }
    }

}