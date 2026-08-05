package com.yupi.hottopic.util;

import com.yupi.hottopic.dto.SearchResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 采集结果处理(需求 HC-5 ~ HC-7:去重、新鲜度过滤、来源优先级)
 */
public final class HotspotResultUtils {

    private HotspotResultUtils() {
    }

    /** 来源优先级(越小越靠前):Twitter > 微博 > B站 > HN > 搜狗 > Bing > Google > DDG */
    private static final Map<String, Integer> SOURCE_PRIORITY = Map.of(
            "twitter", 1,
            "weibo", 2,
            "bilibili", 3,
            "hackernews", 4,
            "sogou", 5,
            "bing", 6,
            "google", 7,
            "duckduckgo", 8
    );

    /** 标准化 URL 用于跨源去重:去尾部斜杠、www 前缀、http→https */
    public static String normalizeUrl(String url) {
        if (url == null) {
            return "";
        }
        String normalized = url.trim();
        normalized = normalized.replaceFirst("^https?://www\\.", "https://");
        normalized = normalized.replaceFirst("^http://", "https://");
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /** 按标准化 URL 去重,保留首次出现的条目 */
    public static List<SearchResult> deduplicate(List<SearchResult> results) {
        Set<String> seen = new HashSet<>();
        List<SearchResult> unique = new ArrayList<>();
        for (SearchResult r : results) {
            if (r == null || r.getUrl() == null) {
                continue;
            }
            if (seen.add(normalizeUrl(r.getUrl()))) {
                unique.add(r);
            }
        }
        return unique;
    }

    /**
     * 新鲜度过滤:丢弃超过 maxAgeHours 的内容。
     * 没有发布时间的保留(搜索引擎结果通常没有时间)。
     */
    public static List<SearchResult> filterByFreshness(List<SearchResult> results, int maxAgeHours) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(maxAgeHours);
        List<SearchResult> fresh = new ArrayList<>();
        for (SearchResult r : results) {
            if (r.getPublishedAt() == null || !r.getPublishedAt().isBefore(cutoff)) {
                fresh.add(r);
            }
        }
        return fresh;
    }

    /** 按来源优先级排序(稳定排序,保持同源内部顺序) */
    public static List<SearchResult> prioritize(List<SearchResult> results) {
        List<SearchResult> sorted = new ArrayList<>(results);
        sorted.sort((a, b) -> {
            int pa = SOURCE_PRIORITY.getOrDefault(a.getSource(), 99);
            int pb = SOURCE_PRIORITY.getOrDefault(b.getSource(), 99);
            return Integer.compare(pa, pb);
        });
        return sorted;
    }
}
