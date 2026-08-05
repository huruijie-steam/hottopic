package com.yupi.hottopic.service.collect;

import com.yupi.hottopic.dto.SearchResult;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bing 搜索抓取(需求 §5:HTML 抓取 bing.com/search,li.b_algo)
 */
@Service
public class BingFetcher implements SourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(BingFetcher.class);

    private static final String BING_URL = "https://www.bing.com/search";
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
    };

    private final RateLimiter rateLimiter = new RateLimiter(5000);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String source() {
        return "bing";
    }

    @Override
    public List<SearchResult> fetch(String query) {
        rateLimiter.waitIfNeeded();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BING_URL + "?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8) + "&count=20"))
                    .header("User-Agent", randomUserAgent())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Bing search status {} for \"{}\"", response.statusCode(), query);
                return List.of();
            }
            List<SearchResult> results = parseHtml(response.body());
            log.info("Bing search for \"{}\": found {} results", query, results.size());
            return results;
        } catch (IOException | InterruptedException e) {
            log.warn("Bing search error for \"{}\": {}", query, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    /** 解析 Bing 搜索结果 HTML(包内可见便于测试) */
    List<SearchResult> parseHtml(String html) {
        List<SearchResult> results = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        for (Element item : doc.select("li.b_algo")) {
            Element titleEl = item.selectFirst("h2 a");
            if (titleEl == null) {
                continue;
            }
            String title = titleEl.text().trim();
            String url = titleEl.attr("href");
            Element snippetEl = item.selectFirst(".b_caption p");
            String snippet = snippetEl != null ? snippetEl.text().trim() : "";
            if (!title.isEmpty() && url.startsWith("http")) {
                SearchResult r = new SearchResult();
                r.setTitle(title);
                r.setContent(snippet.isEmpty() ? title : snippet);
                r.setUrl(url);
                r.setSource("bing");
                results.add(r);
            }
        }
        return results;
    }

    private String randomUserAgent() {
        return USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)];
    }
}
