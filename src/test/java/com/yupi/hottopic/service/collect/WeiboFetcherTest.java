package com.yupi.hottopic.service.collect;

import com.yupi.hottopic.dto.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WeiboFetcherTest {

    private final WeiboFetcher fetcher = new WeiboFetcher();

    private String hotSearchJson(String... topics) {
        StringBuilder items = new StringBuilder();
        for (String t : topics) {
            items.append("{\"word\": \"").append(t).append("\", \"num\": 1234567},");
        }
        if (items.length() > 0) {
            items.setLength(items.length() - 1);
        }
        return "{\"ok\": 1, \"data\": {\"realtime\": [" + items + "]}}";
    }

    @Test
    void 热搜话题命中关键词() {
        String json = hotSearchJson("Claude Sonnet 4.6 发布", "GPT-5 即将到来", "无关话题");
        List<SearchResult> results = fetcher.parseHotSearch(json, "Claude Sonnet");
        assertEquals(1, results.size());
        SearchResult r = results.get(0);
        assertTrue(r.getTitle().contains("Claude Sonnet 4.6 发布"));
        assertEquals("weibo", r.getSource());
        assertEquals(1234567L, r.getViewCount());
        assertTrue(r.getUrl().contains("s.weibo.com"));
    }

    @Test
    void 查询词包含话题_也命中() {
        String json = hotSearchJson("AI");
        List<SearchResult> results = fetcher.parseHotSearch(json, "AI 编程");
        assertEquals(1, results.size());
    }

    @Test
    void 无匹配返回空() {
        String json = hotSearchJson("完全无关的话题");
        assertTrue(fetcher.parseHotSearch(json, "Spring Boot").isEmpty());
    }

    @Test
    void 非ok响应返回空() {
        assertTrue(fetcher.parseHotSearch("{\"ok\": 0}", "x").isEmpty());
    }
}
