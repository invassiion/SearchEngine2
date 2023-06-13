package searchengine.services;
import searchengine.model.SiteEntity;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public interface ApiService {
    void startIndexing(AtomicBoolean indexingProcessing);
    void refreshPage(SiteEntity siteEntity, URL url);
}