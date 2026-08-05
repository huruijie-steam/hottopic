package com.yupi.hottopic.service.collect;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.hottopic.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Hacker News 抓取(需求 §5:官方 Algolia API,只搜最近 24h 的 story)
 */
@Service
public class HackerNewsFetcher implements SourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(HackerNewsFetcher.class);

    private static final String HN_API = "https://hn.algolia.com/api/v1/search";

    private final RateLimiter rateLimiter = new RateLimiter(1000);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String source() {
        return "hackernews";
    }

    @Override
    public List<SearchResult> fetch(String query) {
        rateLimiter.waitIfNeeded();
        try {
            long oneDayAgo = Instant.now().minus(Duration.ofHours(24)).getEpochSecond();
            String url = HN_API + "?query=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
                    + "&tags=story&hitsPerPage=20&numericFilters=created_at_i%3E" + oneDayAgo;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "HotTopicMonitor/1.0")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("HackerNews search status {} for \"{}\"", response.statusCode(), query);
                return List.of();
            }
            List<SearchResult> results = parseJson(response.body());
            log.info("HackerNews search for \"{}\": found {} results", query, results.size());
            return results;
        } catch (IOException | InterruptedException e) {
            log.warn("HackerNews search error for \"{}\": {}", query, e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return List.of();
        }
    }

    /** 解析 HN Algolia 响应 JSON(包内可见便于测试) */
    List<SearchResult> parseJson(String json) {
        List<SearchResult> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode hits = root.path("hits");
            if (!hits.isArray()) {
                return results;
            }
            for (JsonNode hit : hits) {
                String url = hit.path("url").asText("");
                String storyText = hit.path("story_text").asText("");
                String title = hit.path("title").asText("");
                if (url.isEmpty() && storyText.isEmpty()) {
                    continue;
                }
                SearchResult r = new SearchResult();
                r.setTitle(title);
                r.setContent(storyText.isEmpty() ? title : storyText);
                r.setUrl(url.isEmpty() ? "https://news.ycombinator.com/item?id=" + hit.path("objectID").asText() : url);
                r.setSource("hackernews");
                r.setSourceId(hit.path("objectID").asText());
                long created = hit.path("created_at_i").asLong(0);
                if (created > 0) {
                    r.setPublishedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(created), ZoneId.systemDefault()));
                }
                r.setScore(hit.path("points").asInt(0));
                r.setCommentCount((long) hit.path("num_comments").asInt(0));
                r.setAuthorName(hit.path("author").asText());
                r.setAuthorUsername(hit.path("author").asText());
                results.add(r);
            }
        } catch (IOException e) {
            log.warn("HackerNews JSON 解析失败: {}", e.getMessage());
        }
        return results;
    }
}
