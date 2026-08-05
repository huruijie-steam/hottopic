package com.yupi.hottopic.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeywordUtilsTest {

    @Test
    void extractCoreTerms_英文短语_拆词并组合() {
        List<String> terms = KeywordUtils.extractCoreTerms("Claude Sonnet 4.6");
        assertTrue(terms.contains("Claude"));
        assertTrue(terms.contains("Sonnet"));
        assertTrue(terms.contains("4.6") || terms.contains("4"));
        assertTrue(terms.contains("Claude Sonnet"));
        assertTrue(terms.contains("Sonnet 4.6"));
    }

    @Test
    void extractCoreTerms_排除原始关键词() {
        List<String> terms = KeywordUtils.extractCoreTerms("AI");
        assertFalse(terms.contains("AI"));
    }

    @Test
    void extractCoreTerms_中文关键词_不拆分() {
        List<String> terms = KeywordUtils.extractCoreTerms("人工智能");
        assertTrue(terms.isEmpty() || !terms.contains("人工智能"));
    }

    @Test
    void preMatchKeyword_命中变体() {
        var result = KeywordUtils.preMatchKeyword("Anthropic just released Claude Sonnet",
                List.of("Claude Sonnet", "Claude", "claude-sonnet"));
        assertTrue(result.matched());
        assertTrue(result.matchedTerms().contains("Claude"));
    }

    @Test
    void preMatchKeyword_不区分大小写() {
        var result = KeywordUtils.preMatchKeyword("hello CLAUDE sonnet world", List.of("claude sonnet"));
        assertTrue(result.matched());
    }

    @Test
    void preMatchKeyword_未命中() {
        var result = KeywordUtils.preMatchKeyword("OpenAI released GPT-5", List.of("Claude Sonnet"));
        assertFalse(result.matched());
        assertTrue(result.matchedTerms().isEmpty());
    }

    @Test
    void preMatchKeyword_空输入安全() {
        assertFalse(KeywordUtils.preMatchKeyword(null, List.of("a")).matched());
        assertFalse(KeywordUtils.preMatchKeyword("text", null).matched());
        assertFalse(KeywordUtils.preMatchKeyword("text", List.of()).matched());
    }
}
