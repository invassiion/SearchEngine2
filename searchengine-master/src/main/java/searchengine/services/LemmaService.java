package searchengine.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

@Service("LemmaServiceImpl")
public interface LemmaService {
    Map<String,Integer> getLemmasFromText(String text) throws IOException;
    void getLemmasFromUrl(URL url) throws IOException;
}