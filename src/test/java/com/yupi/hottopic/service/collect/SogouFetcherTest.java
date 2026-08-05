package com.yupi.hottopic.service.collect;

import com.yupi.hottopic.dto.SearchResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SogouFetcherTest {

    private final SogouFetcher fetcher = new SogouFetcher();

    @Test
    void parseHtml_正常结果() {
        String html = """
                <html><body>
                  <div class="vrwrap">
                    <h3 class="vr-title"><a href="/link?url=abc123">Spring Boot 4 新特性解析</a></h3>
                    <div class="str_info">一文看懂 Spring Boot 4 的模块变化</div>
                  </div>
                  <div class="rb">
                    <h3><a href="https://example.com/direct">直接链接标题</a></h3>
                    <p>摘要文本</p>
                  </div>
                </body></html>""";
        List<SearchResult> results = fetcher.parseHtml(html);
        assertEquals(2, results.size());

        // 相对路径转绝对
        assertEquals("https://www.sogou.com/link?url=abc123", results.get(0).getUrl());
        assertEquals("sogou", results.get(0).getSource());
        assertEquals("一文看懂 Spring Boot 4 的模块变化", results.get(0).getContent());

        assertEquals("https://example.com/direct", results.get(1).getUrl());
    }

    @Test
    void parseHtml_广告和无效链接被过滤() {
        String html = """
                <html><body>
                  <div class="vrwrap"><h3><a href="/link?url=ad">大家还在搜:xxx</a></h3></div>
                  <div class="vrwrap"><h3><a>无链接</a></h3></div>
                  <div class="vrwrap"><h3><a href="javascript:void(0)">坏链接</a></h3></div>
                </body></html>""";
        assertTrue(fetcher.parseHtml(html).isEmpty());
    }
}
