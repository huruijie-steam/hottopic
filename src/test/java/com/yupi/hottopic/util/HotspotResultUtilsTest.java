package com.yupi.hottopic.util;

import com.yupi.hottopic.dto.SearchResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HotspotResultUtilsTest {

    private SearchResult result(String url, String source, LocalDateTime publishedAt) {
        SearchResult r = new SearchResult();
        r.setUrl(url);
        r.setSource(source);
        r.setPublishedAt(publishedAt);
        return r;
    }

    @Test
    void normalizeUrl_统一协议和www() {
        assertEquals("https://example.com/a", HotspotResultUtils.normalizeUrl("http://www.example.com/a/"));
        assertEquals("https://example.com/a", HotspotResultUtils.normalizeUrl("https://example.com/a/"));
        assertEquals("https://example.com", HotspotResultUtils.normalizeUrl("http://example.com"));
    }

    @Test
    void deduplicate_跨源同URL去重() {
        List<SearchResult> list = List.of(
                result("http://www.example.com/a/", "bing", null),
                result("https://example.com/a", "google", null),
                result("https://other.com/b", "bing", null));
        List<SearchResult> unique = HotspotResultUtils.deduplicate(list);
        assertEquals(2, unique.size());
        assertEquals("bing", unique.get(0).getSource()); // 保留首次出现
    }

    @Test
    void filterByFreshness_丢弃过期保留无时间() {
        LocalDateTime now = LocalDateTime.now();
        List<SearchResult> list = List.of(
                result("https://a.com", "bing", now.minusHours(1)),   // 保留
                result("https://b.com", "bing", now.minusHours(200)), // 丢弃(超过7天)
                result("https://c.com", "bing", null));               // 保留
        List<SearchResult> fresh = HotspotResultUtils.filterByFreshness(list, 168);
        assertEquals(2, fresh.size());
        assertTrue(fresh.stream().anyMatch(r -> r.getUrl().equals("https://a.com")));
        assertTrue(fresh.stream().anyMatch(r -> r.getUrl().equals("https://c.com")));
    }

    @Test
    void prioritize_按来源优先级排序() {
        List<SearchResult> list = List.of(
                result("https://a.com", "bing", null),
                result("https://b.com", "twitter", null),
                result("https://c.com", "bilibili", null));
        List<SearchResult> sorted = HotspotResultUtils.prioritize(list);
        assertEquals("twitter", sorted.get(0).getSource());
        assertEquals("bilibili", sorted.get(1).getSource());
        assertEquals("bing", sorted.get(2).getSource());
    }
}
