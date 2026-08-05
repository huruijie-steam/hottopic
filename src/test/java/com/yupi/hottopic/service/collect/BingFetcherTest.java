package com.yupi.hottopic.service.collect;

import com.yupi.hottopic.dto.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BingFetcherTest {

    private final BingFetcher fetcher = new BingFetcher();

    @Test
    void parseHtml_正常结果() {
        String html = """
                <html><body><ol id="b_results">
                  <li class="b_algo">
                    <h2><a href="https://example.com/spring-boot-4">Spring Boot 4 Released</a></h2>
                    <div class="b_caption"><p>Spring Boot 4 带来了全新特性</p></div>
                  </li>
                  <li class="b_algo">
                    <h2><a href="https://example.com/spring-ai">Spring AI 1.1</a></h2>
                    <div class="b_caption"><p>Spring AI 与 Boot 4 集成</p></div>
                  </li>
                </ol></body></html>""";
        List<SearchResult> results = fetcher.parseHtml(html);
        assertEquals(2, results.size());
        assertEquals("Spring Boot 4 Released", results.get(0).getTitle());
        assertEquals("https://example.com/spring-boot-4", results.get(0).getUrl());
        assertEquals("bing", results.get(0).getSource());
        assertEquals("Spring Boot 4 带来了全新特性", results.get(0).getContent());
    }

    @Test
    void parseHtml_无标题或非http链接被跳过() {
        String html = """
                <html><body>
                  <li class="b_algo"><h2><a>no href</a></h2></li>
                  <li class="b_algo"><h2><a href="javascript:void(0)">bad url</a></h2></li>
                  <li class="b_algo"><h2><a href="https://good.com">Good</a></h2><div class="b_caption"><p>s</p></div></li>
                </body></html>""";
        List<SearchResult> results = fetcher.parseHtml(html);
        assertEquals(1, results.size());
        assertEquals("https://good.com", results.get(0).getUrl());
    }
}
