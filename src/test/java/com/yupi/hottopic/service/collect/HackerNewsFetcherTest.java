package com.yupi.hottopic.service.collect;

import com.yupi.hottopic.dto.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HackerNewsFetcherTest {

    private final HackerNewsFetcher fetcher = new HackerNewsFetcher();

    @Test
    void parseJson_正常响应() {
        String json = """
                {"hits": [
                  {"objectID": "1", "title": "Spring Boot 4.0 Released", "url": "https://spring.io/blog",
                   "story_text": "", "author": "alice", "points": 120, "num_comments": 45, "created_at_i": 1700000000},
                  {"objectID": "2", "title": "Ask HN: no url", "url": "", "story_text": "讨论帖内容",
                   "author": "bob", "points": 5, "num_comments": 3, "created_at_i": 1700000100}
                ]}""";
        List<SearchResult> results = fetcher.parseJson(json);
        assertEquals(2, results.size());

        SearchResult first = results.get(0);
        assertEquals("hackernews", first.getSource());
        assertEquals("1", first.getSourceId());
        assertEquals("https://spring.io/blog", first.getUrl());
        assertEquals(120, first.getScore());
        assertEquals(45L, first.getCommentCount());
        assertNotNull(first.getPublishedAt());

        // 无 url 时用 HN 讨论页链接
        assertEquals("https://news.ycombinator.com/item?id=2", results.get(1).getUrl());
    }

    @Test
    void parseJson_无url无正文被跳过() {
        String json = """
                {"hits": [{"objectID": "1", "title": "empty", "url": "", "story_text": "", "author": "x"}]}""";
        assertTrue(fetcher.parseJson(json).isEmpty());
    }

    @Test
    void parseJson_非法JSON返回空() {
        assertTrue(fetcher.parseJson("not json").isEmpty());
    }
}
