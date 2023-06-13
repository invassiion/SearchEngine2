package searchengine.services.implement;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexxSearch;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.repository.IndexxRepository;
import searchengine.repository.LemmaRepository;
import searchengine.services.LemmaService;
import searchengine.services.PageIndexer;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class PageIndexerImpl implements PageIndexer {
    private LemmaService lemmaService;
    private LemmaRepository lemmaRepository;
    private IndexxRepository indexSearchRepository;

    @Override
    public void indexHtml(String html, PageEntity indexingPage) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmasFromText(html);
            lemmas.entrySet().parallelStream().forEach(entry -> saveLemma(entry.getKey(), entry.getValue(), indexingPage));
            log.warn("Индексация страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }

    }

    @Transactional
    private void saveLemma(String k, Integer v, PageEntity indexingPage) {
        LemmaEntity existLemmaInDB = lemmaRepository.lemmaExist(k);
        if (existLemmaInDB != null) {
            existLemmaInDB.setFrequency(existLemmaInDB.getFrequency() + v);
            lemmaRepository.saveAndFlush(existLemmaInDB);
            createIndex(indexingPage, existLemmaInDB, v);
        } else {
            try {
                LemmaEntity newLemmaToDB = new LemmaEntity();
                newLemmaToDB.setSiteId(indexingPage.getSiteId());
                newLemmaToDB.setLemma(k);
                newLemmaToDB.setFrequency(v);
                newLemmaToDB.setSiteEntity(indexingPage.getSiteEntity());
                lemmaRepository.saveAndFlush(newLemmaToDB);
                createIndex(indexingPage, newLemmaToDB, v);
            } catch (DataIntegrityViolationException ex) {
                log.error("Данная лемма уже существует. Вызов повторного сохранения.");
                saveLemma(k, v, indexingPage);
            }
        }
    }

    private void createIndex(PageEntity indexingPage, LemmaEntity lemmaInDB, Integer rank) {
        IndexxSearch indexSearchExist = indexSearchRepository.indexSearchExist(indexingPage.getId(), lemmaInDB.getId());
        if (indexSearchExist != null) {
            indexSearchExist.setLemmaCount(indexSearchExist.getLemmaCount() + rank);
            indexSearchRepository.save(indexSearchExist);
        } else {
            IndexxSearch index = new IndexxSearch();
            index.setPageId(indexingPage.getId());
            index.setLemmaId(lemmaInDB.getId());
            index.setLemmaCount(rank);
            index.setLemma(lemmaInDB);
            index.setPage(indexingPage);
            indexSearchRepository.save(index);
        }
    }
    @Transactional
    private void refreshLemma(String k, Integer v, PageEntity refreshPage) {
        LemmaEntity existLemmaInDB = lemmaRepository.lemmaExist(k);
        if (existLemmaInDB != null) {
            IndexxSearch indexForRefresh = indexSearchRepository.indexSearchExist(refreshPage.getId(), existLemmaInDB.getId());
            if (indexForRefresh != null) {
                existLemmaInDB.setFrequency((int) (existLemmaInDB.getFrequency() - indexForRefresh.getLemmaCount()));
                lemmaRepository.saveAndFlush(existLemmaInDB);
                indexSearchRepository.delete(indexForRefresh);
                LemmaEntity newLemmaToDB = new LemmaEntity();
                newLemmaToDB.setSiteId(refreshPage.getSiteId());
                newLemmaToDB.setLemma(k);
                newLemmaToDB.setFrequency(v);
                newLemmaToDB.setSiteEntity(refreshPage.getSiteEntity());
                lemmaRepository.saveAndFlush(newLemmaToDB);
                createIndex(refreshPage, newLemmaToDB, v);
                return;
            }
        }
        LemmaEntity newLemmaToDB = new LemmaEntity();
        newLemmaToDB.setSiteId(refreshPage.getSiteId());
        newLemmaToDB.setLemma(k);
        newLemmaToDB.setFrequency(v);
        newLemmaToDB.setSiteEntity(refreshPage.getSiteEntity());
        lemmaRepository.saveAndFlush(newLemmaToDB);
        createIndex(refreshPage, newLemmaToDB, v);
    }
    @Override
    public void refreshIndex(String html, PageEntity refreshPage) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Integer> lemmas = lemmaService.getLemmasFromText(html);
            lemmas.entrySet().parallelStream().forEach(entry -> refreshLemma(entry.getKey(), entry.getValue(), refreshPage));
            log.warn("Обновление индекса страницы " + (System.currentTimeMillis() - start) + " lemmas:" + lemmas.size());
        } catch (IOException e) {
            log.error(String.valueOf(e));
            throw new RuntimeException(e);
        }
    }

}