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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bilibili 抓取(需求 §5:公开 API,需 buvid3 cookie 防 412 风控)
 * 三个能力:视频搜索(search_type=video)、用户搜索(search_type=bili_user)、UP 主空间视频
 */
@Service
public class BilibiliFetcher implements SourceFetcher {

    private static final Logger log = LoggerFactory.getLogger(BilibiliFetcher.class);

    private static final String SEARCH_API = "https://api.bilibili.com/x/web-interface/search/type";
    private static final String SPACE_API = "https://api.bilibili.com/x/space/arc/search";
    private static final String[] USER_AGENTS = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0"
    };

    private final RateLimiter rateLimiter = new RateLimiter(2000);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String source() {
        return "bilibili";
    }

    /** 视频搜索 */
    @Override
    public List<SearchResult> fetch(String query) {
        rateLimiter.waitIfNeeded();
        try {
            String url = SEARCH_API + "?keyword=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&search_type=video&order=pubdate&page=1&pagesize=20";
            String body = get(url, "https://search.bilibili.com/");
            if (body == null) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(body);
            if (root.path("code").asInt(-1) != 0) {
                log.warn("Bilibili search code != 0 for \"{}\"", query);
                return List.of();
            }
            List<SearchResult> results = parseVideoList(root.path("data").path("result"));
            log.info("Bilibili search for \"{}\": found {} results", query, results.size());
            return results;
        } catch (Exception e) {
            log.warn("Bilibili search error for \"{}\": {}", query, e.getMessage());
            return List.of();
        }
    }

    /** B 站用户信息(账号检测用,需求 HC-4) */
    public record BilibiliUser(String mid, String name, long fans, int verifiedType, String sign, String avatar) {
    }

    /**
     * 搜索 B 站用户:名字精确匹配优先;否则粉丝 > 1000 且名字包含关键词才认(与原项目一致)
     */
    public BilibiliUser searchUser(String keyword) {
        rateLimiter.waitIfNeeded();
        try {
            String url = SEARCH_API + "?keyword=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&search_type=bili_user&page=1&pagesize=5";
            String body = get(url, "https://search.bilibili.com/");
            if (body == null) {
                return null;
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode users = root.path("data").path("result");
            if (root.path("code").asInt(-1) != 0 || !users.isArray() || users.isEmpty()) {
                return null;
            }
            for (JsonNode u : users) {
                if (u.path("uname").asText("").equalsIgnoreCase(keyword)) {
                    return toUser(u);
                }
            }
            JsonNode top = users.get(0);
            String topName = top.path("uname").asText("");
            long topFans = top.path("fans").asLong(0);
            if (topFans > 1000 && topName.contains(keyword)) {
                return toUser(top);
            }
            return null;
        } catch (Exception e) {
            log.warn("Bilibili user search error for \"{}\": {}", keyword, e.getMessage());
            return null;
        }
    }

    /** 获取 UP 主最新视频(账号检测用) */
    public List<SearchResult> getUserVideos(String mid) {
        rateLimiter.waitIfNeeded();
        try {
            String url = SPACE_API + "?mid=" + mid + "&pn=1&ps=10&order=pubdate";
            String body = get(url, "https://space.bilibili.com/" + mid);
            if (body == null) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(body);
            JsonNode vlist = root.path("data").path("list").path("vlist");
            if (root.path("code").asInt(-1) != 0 || !vlist.isArray()) {
                return List.of();
            }
            List<SearchResult> results = new ArrayList<>();
            for (JsonNode v : vlist) {
                SearchResult r = toVideoResult(v);
                if (r != null) {
                    results.add(r);
                }
            }
            log.info("Bilibili user {} videos: found {} results", mid, results.size());
            return results;
        } catch (Exception e) {
            log.warn("Bilibili user videos error for mid {}: {}", mid, e.getMessage());
            return List.of();
        }
    }

    // ========== 解析(包内可见便于测试) ==========

    /** 解析视频搜索/空间视频列表 */
    List<SearchResult> parseVideoList(JsonNode vlist) {
        List<SearchResult> results = new ArrayList<>();
        if (!vlist.isArray()) {
            return results;
        }
        for (JsonNode v : vlist) {
            SearchResult r = toVideoResult(v);
            if (r != null) {
                results.add(r);
            }
        }
        return results;
    }

    private SearchResult toVideoResult(JsonNode v) {
        String bvid = v.path("bvid").asText("");
        if (bvid.isEmpty()) {
            return null;
        }
        SearchResult r = new SearchResult();
        // 去掉搜索接口返回的 <em> 高亮标签
        String title = v.path("title").asText("").replaceAll("</?em[^>]*>", "");
        r.setTitle(title);
        String desc = v.path("description").asText("");
        r.setContent(desc.isEmpty() ? title : desc);
        r.setUrl("https://www.bilibili.com/video/" + bvid);
        r.setSource("bilibili");
        r.setSourceId(bvid);
        long pubdate = v.path("pubdate").asLong(0);
        if (pubdate > 0) {
            r.setPublishedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(pubdate), ZoneId.systemDefault()));
        }
        r.setViewCount(v.path("play").asLong(0));
        r.setLikeCount(v.path("like").asLong(0));
        long review = v.path("review").asLong(0);
        long comment = v.path("comment").asLong(0);
        r.setCommentCount(comment > 0 ? comment : review);
        r.setDanmakuCount(v.path("danmaku").asLong(0));
        r.setAuthorName(v.path("author").asText(""));
        r.setAuthorUsername(v.path("mid").asText(""));
        return r;
    }

    private BilibiliUser toUser(JsonNode u) {
        return new BilibiliUser(
                u.path("mid").asText(),
                u.path("uname").asText(""),
                u.path("fans").asLong(0),
                u.path("official_verify").path("type").asInt(-1),
                u.path("usign").asText(""),
                u.path("upic").asText(""));
    }

    private String get(String url, String referer) throws IOException, InterruptedException {
        // 生成 buvid3 cookie 以避免 412 错误
        String buvid3 = UUID.randomUUID().toString().replace("-", "") + "infoc";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", randomUserAgent())
                .header("Referer", referer)
                .header("Accept", "application/json")
                .header("Cookie", "buvid3=" + buvid3)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200 ? response.body() : null;
    }

    private String randomUserAgent() {
        return USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)];
    }
}
