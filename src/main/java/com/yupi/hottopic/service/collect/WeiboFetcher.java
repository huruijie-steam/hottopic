package com.yupi.hottopic.service.collect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.hottopic.dto.SearchResult;
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
 * 微博抓取(需求 §5:热搜榜公开 API,无需登录)。
 * 通过热搜榜匹配关键词:命中则返回热搜话题条目。
 */
@Service
public class WeiboFetcher implements SourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(WeiboFetcher.class);

    private static final String HOT_SEARCH_API = "https://weibo.com/ajax/side/hotSearch";
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String source() {
        return "weibo";
    }

    @Override
    public List<SearchResult> fetch(String query) {
        rateLimiter.waitIfNeeded();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(HOT_SEARCH_API))
                    .header("User-Agent", randomUserAgent())
                    .header("Accept", "application/json")
                    .header("Referer", "https://weibo.com/")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Weibo hot search status {} for \"{}\"", response.statusCode(), query);
                return List.of();
            }
            List<SearchResult> results = parseHotSearch(response.body(), query);
            log.info("Weibo hot search: {} matches for \"{}\"", results.size(), query);
            return results;
        } catch (IOException | InterruptedException e) {
            log.warn("Weibo hot search error for \"{}\": {}", query, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    /**
     * 解析热搜榜并匹配关键词(包内可见便于测试)。
     * 匹配规则:任一查询词出现在话题中,或话题包含在查询词中(与原项目一致)。
     */
    List<SearchResult> parseHotSearch(String json, String query) {
        List<SearchResult> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.path("ok").asInt(-1) != 1) {
                return results;
            }
            JsonNode realtime = root.path("data").path("realtime");
            if (!realtime.isArray()) {
                return results;
            }
            String queryLower = query.toLowerCase();
            String[] queryWords = queryLower.split("\\s+");

            for (JsonNode item : realtime) {
                String word = item.path("word").asText("");
                String note = item.path("note").asText("");
                String topic = note.isEmpty() ? word : note;
                String wordLower = topic.toLowerCase();

                boolean match = false;
                for (String qw : queryWords) {
                    if (!qw.isEmpty() && (wordLower.contains(qw) || qw.contains(wordLower))) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    match = wordLower.contains(queryLower) || queryLower.contains(wordLower);
                }
                if (!match) {
                    continue;
                }

                SearchResult r = new SearchResult();
                r.setTitle("🔥 微博热搜: " + topic);
                r.setContent("微博热搜话题「" + topic + "」,热度 " + item.path("num").asLong(0));
                r.setUrl("https://s.weibo.com/weibo?q=" + URLEncoder.encode("#" + topic + "#", StandardCharsets.UTF_8));
                r.setSource("weibo");
                r.setViewCount(item.path("num").asLong(0));
                results.add(r);
            }
        } catch (IOException e) {
            log.warn("Weibo hot search JSON 解析失败: {}", e.getMessage());
        }
        return results;
    }

    private String randomUserAgent() {
        return USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)];
    }
}
