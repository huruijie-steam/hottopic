package com.yupi.hottopic.service.ai;

import com.yupi.hottopic.dto.AIAnalysis;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiClientTest {

    private AiClient aiClient;

    @BeforeEach
    void setUp() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        aiClient = new AiClient(builder, "test-key");
    }

    @Test
    void parseAnalysis_正常JSON() throws Exception {
        String json = """
                {"isReal": true, "relevance": 82, "relevanceReason": "直接讨论关键词",
                 "keywordMentioned": true, "importance": "high", "summary": "此内容与关键词强相关"}""";
        AIAnalysis a = aiClient.parseAnalysis(json);
        assertTrue(a.getIsReal());
        assertEquals(82, a.getRelevance());
        assertEquals("high", a.getImportance());
        assertTrue(a.getKeywordMentioned());
    }

    @Test
    void parseAnalysis_带前后缀文本() throws Exception {
        String json = "好的,分析结果如下:\n```json\n{\"isReal\":false,\"relevance\":12,\"importance\":\"low\"}\n```\n完毕";
        AIAnalysis a = aiClient.parseAnalysis(json);
        assertFalse(a.getIsReal());
        assertEquals(12, a.getRelevance());
        assertEquals("low", a.getImportance());
    }

    @Test
    void parseAnalysis_相关性越界被钳制() throws Exception {
        AIAnalysis a = aiClient.parseAnalysis("{\"relevance\": 150}");
        assertEquals(100, a.getRelevance());
        AIAnalysis b = aiClient.parseAnalysis("{\"relevance\": -5}");
        assertEquals(0, b.getRelevance());
    }

    @Test
    void parseAnalysis_非法重要程度回落low() throws Exception {
        AIAnalysis a = aiClient.parseAnalysis("{\"importance\": \"urgent\"}");
        assertEquals("urgent", a.getImportance());
        AIAnalysis b = aiClient.parseAnalysis("{\"importance\": \"super\"}");
        assertEquals("low", b.getImportance());
    }

    @Test
    void parseAnalysis_非JSON抛异常() {
        assertThrows(Exception.class, () -> aiClient.parseAnalysis("不是 JSON"));
    }

    @Test
    void parseExpansion_正常数组() {
        List<String> list = aiClient.parseExpansion(
                "[\"Claude Sonnet 4.6\", \"Claude Sonnet\", \"Sonnet 4.6\", \"claude-sonnet-4.6\"]");
        assertEquals(4, list.size());
        assertTrue(list.contains("Claude Sonnet"));
    }

    @Test
    void parseExpansion_带前后缀() {
        List<String> list = aiClient.parseExpansion("以下是扩展结果:\n[\"A\", \"B\"]\n请查收");
        assertEquals(2, list.size());
    }

    @Test
    void parseExpansion_非数组返回空() {
        assertTrue(aiClient.parseExpansion("{\"a\": 1}").isEmpty());
        assertTrue(aiClient.parseExpansion(null).isEmpty());
    }

    @Test
    void 无key时expandKeyword降级为纯文本拆词() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(mock(ChatClient.class));
        AiClient noKeyClient = new AiClient(builder, "  ");

        List<String> expanded = noKeyClient.expandKeyword("Spring Boot 4");
        assertTrue(expanded.contains("Spring Boot 4"));
        assertTrue(expanded.contains("Spring"));
        // 不调用 AI 也应有兜底结果
        assertFalse(expanded.isEmpty());
    }
}
