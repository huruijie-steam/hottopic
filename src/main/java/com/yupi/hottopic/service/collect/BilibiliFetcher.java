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

    private final RateLimiter rateLimiter = new RateLimiter(3000);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            // B 站风控对非浏览器的 HTTP/2 指纹敏感(Java 默认协商 HTTP/2 概率 412),强制 HTTP/1.1
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * B 站风控 cookie(buvid3/b_nut 等),由服务端下发,首次访问首页获取。
     * 随机伪造的 buvid3 会触发 412 风控,必须用真实 cookie 链。
     */
    private volatile String riskCookie;
    private volatile long cookieFetchedAt = 0;
    private static final long COOKIE_MAX_AGE_MS = 30 * 60 * 1000; // 30 分钟刷新一次

    /** 获取(必要时刷新)风控 cookie */
    private String getRiskCookie() {
        long now = System.currentTimeMillis();
        if (riskCookie != null && now - cookieFetchedAt < COOKIE_MAX_AGE_MS) {
            return riskCookie;
        }
        synchronized (this) {
            if (riskCookie != null && System.currentTimeMillis() - cookieFetchedAt < COOKIE_MAX_AGE_MS) {
                return riskCookie;
            }
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://www.bilibili.com/"))
                        .header("User-Agent", randomUserAgent())
                        .timeout(Duration.ofSeconds(15))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                StringBuilder cookie = new StringBuilder();
                response.headers().allValues("set-cookie").forEach(sc -> {
                    String pair = sc.split(";")[0];
                    if (pair.startsWith("buvid3=") || pair.startsWith("b_nut=") || pair.startsWith("buvid4=")) {
                        if (cookie.length() > 0) {
                            cookie.append("; ");
                        }
                        cookie.append(pair);
                    }
                });
                if (cookie.length() > 0) {
                    riskCookie = cookie.toString();
                    cookieFetchedAt = now;
                    log.info("已获取 B 站风控 cookie");
                }
            } catch (Exception e) {
                log.warn("获取 B 站 cookie 失败: {}", e.getMessage());
            }
            return riskCookie;
        }
    }

    @Override
    public String source() {
        return "bilibili";
    }

    /** 视频搜索(第 1 页) */
    @Override
    public List<SearchResult> fetch(String query) {
        return fetchPage(query, 1);
    }

    /** 视频搜索指定页(接口 pagesize 上限 20,pubdate 排序) */
    public List<SearchResult> fetchPage(String query, int page) {
        rateLimiter.waitIfNeeded();
        try {
            String url = SEARCH_API + "?keyword=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&search_type=video&order=pubdate&page=" + page + "&pagesize=20";
            String body = get(url, "https://search.bilibili.com/");
            if (body == null) {
                return List.of();
            }
            JsonNode root = objectMapper.readTree(body);
            if (root.path("code").asInt(-1) != 0) {
                log.warn("Bilibili search code != 0 for \"{}\" (page {})", query, page);
                return List.of();
            }
            List<SearchResult> results = parseVideoList(root.path("data").path("result"));
            log.info("Bilibili search for \"{}\" (page {}): found {} results", query, page, results.size());
            return results;
        } catch (Exception e) {
            log.warn("Bilibili search error for \"{}\" (page {}): {}", query, page, e.getMessage());
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
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", randomUserAgent())
                .header("Referer", referer)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(15))
                .GET();
        String cookie = getRiskCookie();
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 412) {
            // 风控:刷新 cookie 重试一次
            log.warn("B 站接口 412 风控,刷新 cookie 重试");
            synchronized (this) {
                riskCookie = null;
                cookieFetchedAt = 0;
            }
            String retryCookie = getRiskCookie();
            if (retryCookie != null) {
                HttpRequest.Builder retry = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", randomUserAgent())
                        .header("Referer", referer)
                        .header("Accept", "application/json")
                        .header("Cookie", retryCookie)
                        .timeout(Duration.ofSeconds(15))
                        .GET();
                response = httpClient.send(retry.build(), HttpResponse.BodyHandlers.ofString());
            }
        }
        return response.statusCode() == 200 ? response.body() : null;
    }

    private String randomUserAgent() {
        return USER_AGENTS[ThreadLocalRandom.current().nextInt(USER_AGENTS.length)];
    }
}
