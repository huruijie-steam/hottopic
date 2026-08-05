package com.yupi.hottopic.service.collect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yupi.hottopic.dto.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BilibiliFetcherTest {

    private final BilibiliFetcher fetcher = new BilibiliFetcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseVideoList_正常解析() throws Exception {
        String json = """
                [{"bvid": "BV1xx411c7mD", "title": "<em class='keyword'>Spring</em> Boot 4 教程",
                  "description": "从零开始", "author": "up主", "mid": 123, "play": 99999,
                  "like": 500, "review": 100, "comment": 120, "danmaku": 888, "pubdate": 1700000000}]""";        List<SearchResult> results = fetcher.parseVideoList(objectMapper.readTree(json));
        assertEquals(1, results.size());
        SearchResult r = results.get(0);
        // 高亮标签被移除
        assertEquals("Spring Boot 4 教程", r.getTitle());
        assertEquals("https://www.bilibili.com/video/BV1xx411c7mD", r.getUrl());
        assertEquals("bilibili", r.getSource());
        assertEquals(99999L, r.getViewCount());
        // comment 优先于 review
        assertEquals(120L, r.getCommentCount());
        assertEquals(888L, r.getDanmakuCount());
        assertNotNull(r.getPublishedAt());
        assertEquals("up主", r.getAuthorName());
    }

    @Test
    void parseVideoList_无bvid被跳过() throws Exception {
        String json = "[{\"title\": \"no bvid\", \"author\": \"x\"}]";
        assertTrue(fetcher.parseVideoList(objectMapper.readTree(json)).isEmpty());
    }
}
