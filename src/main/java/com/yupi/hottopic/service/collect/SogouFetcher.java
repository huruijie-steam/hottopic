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
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 搜狗搜索抓取(需求 §5:HTML 抓取 sogou.com/web,替代百度,反爬宽松)
 */
@Service
public class SogouFetcher implements SourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(SogouFetcher.class);

    private static final String SOGOU_URL = "https://www.sogou.com/web";
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0"
    };

    private final RateLimiter rateLimiter = new RateLimiter(3000);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String source() {
        return "sogou";
    }

    @Override
    public List<SearchResult> fetch(String query) {
        rateLimiter.waitIfNeeded();
        try {
            String url = SOGOU_URL + "?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&ie=utf-8";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", randomUserAgent())
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Sogou search status {} for \"{}\"", response.statusCode(), query);
                return List.of();
            }
            List<SearchResult> results = parseHtml(response.body());
            log.info("Sogou search for \"{}\": found {} results", query, results.size());
            return results;
        } catch (IOException | InterruptedException e) {
            log.warn("Sogou search error for \"{}\": {}", query, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    /** 解析搜狗搜索结果 HTML(包内可见便于测试) */
    List<SearchResult> parseHtml(String html) {
        List<SearchResult> results = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        for (Element item : doc.select(".vrwrap, .rb")) {
            Element titleEl = item.selectFirst("h3 a, .vr-title a, .vrTitle a");
            if (titleEl == null) {
                continue;
            }
            String title = titleEl.text().trim();
            String rawUrl = titleEl.attr("href");
            // 搜狗相对路径转绝对路径
            String url = rawUrl;
            if (rawUrl.startsWith("/link?url=")) {
                url = "https://www.sogou.com" + rawUrl;
            }
            Element snippetEl = item.selectFirst(".space-txt, .str-text-info, .str_info, .text-layout");
            String snippet = snippetEl != null ? snippetEl.text().trim() : "";
            if (snippet.isEmpty()) {
                Element p = item.selectFirst("p");
                snippet = p != null ? p.text().trim() : "";
            }
            // 排除广告和无关结果
            if (!title.isEmpty() && url.startsWith("http") && !title.contains("大家还在搜")) {
                SearchResult r = new SearchResult();
                r.setTitle(title);
                r.setContent(snippet.isEmpty() ? title : snippet);
                r.setUrl(url);
                r.setSource("sogou");
                results.add(r);
            }
        }
        return results;
    }

    private String randomUserAgent() {
        return USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)];
    }
}
