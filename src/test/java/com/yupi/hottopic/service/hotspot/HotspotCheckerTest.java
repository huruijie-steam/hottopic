package com.yupi.hottopic.service.hotspot;

import com.yupi.hottopic.config.MonitorProperties;
import com.yupi.hottopic.dto.AIAnalysis;
import com.yupi.hottopic.dto.SearchResult;
import com.yupi.hottopic.entity.Hotspot;
import com.yupi.hottopic.entity.Keyword;
import com.yupi.hottopic.mapper.HotspotMapper;
import com.yupi.hottopic.service.NotificationService;
import com.yupi.hottopic.service.ai.AiClient;
import com.yupi.hottopic.service.collect.AccountDetector;
import com.yupi.hottopic.service.collect.CollectService;
import com.yupi.hottopic.service.mail.EmailService;
import com.yupi.hottopic.util.KeywordUtils;
import com.yupi.hottopic.ws.HotspotWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HotspotCheckerTest {

    private CollectService collectService;
    private AiClient aiClient;
    private AccountDetector accountDetector;
    private HotspotMapper hotspotMapper;
    private NotificationService notificationService;
    private EmailService emailService;
    private HotspotWebSocketHandler webSocketHandler;
    private HotspotChecker checker;

    @BeforeEach
    void setUp() {
        collectService = mock(CollectService.class);
        aiClient = mock(AiClient.class);
        accountDetector = mock(AccountDetector.class);
        hotspotMapper = mock(HotspotMapper.class);
        notificationService = mock(NotificationService.class);
        emailService = mock(EmailService.class);
        webSocketHandler = mock(HotspotWebSocketHandler.class);
        MonitorProperties props = new MonitorProperties();
        checker = new HotspotChecker(collectService, aiClient, accountDetector, hotspotMapper, notificationService,
                emailService, webSocketHandler, props);

        // 默认:AI 分析全部通过,采集返回一条
        when(aiClient.expandKeyword(anyString())).thenReturn(List.of("Spring Boot 4"));
        when(aiClient.analyzeContent(anyString(), anyString(), any()))
                .thenReturn(analysis(true, 80, true, "high"));
        when(hotspotMapper.selectCount(any())).thenReturn(0L);
        SearchResult r = new SearchResult();
        r.setTitle("Spring Boot 4 Released");
        r.setContent("内容");
        r.setUrl("https://example.com/1");
        r.setSource("bing");
        when(collectService.collect(anyString())).thenReturn(List.of(r));
        when(accountDetector.detectAndFetch(anyString()))
                .thenReturn(AccountDetector.DetectResult.empty());
    }

    private AIAnalysis analysis(boolean isReal, int relevance, boolean mentioned, String importance) {
        AIAnalysis a = new AIAnalysis();
        a.setIsReal(isReal);
        a.setRelevance(relevance);
        a.setKeywordMentioned(mentioned);
        a.setImportance(importance);
        a.setSummary("与关键词直接相关");
        return a;
    }

    private Keyword keyword() {
        Keyword k = new Keyword();
        k.setId("kw-1");
        k.setText("Spring Boot 4");
        k.setIsActive(true);
        return k;
    }

    @Test
    void 正常热点_入库并通知() {
        int count = checker.checkKeyword(keyword());
        assertEquals(1, count);
        ArgumentCaptor<Hotspot> captor = ArgumentCaptor.forClass(Hotspot.class);
        verify(hotspotMapper, times(1)).insert(captor.capture());
        assertEquals("https://example.com/1", captor.getValue().getUrl());
        assertEquals("kw-1", captor.getValue().getKeywordId());
        verify(notificationService, times(1)).create(eq("hotspot"), anyString(), anyString(), any());
    }

    @Test
    void 虚假内容被过滤() {
        when(aiClient.analyzeContent(anyString(), anyString(), any()))
                .thenReturn(analysis(false, 80, true, "high"));
        assertEquals(0, checker.checkKeyword(keyword()));
        verify(hotspotMapper, never()).insert(any(Hotspot.class));
        verify(notificationService, never()).create(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void 相关性低于阈值被过滤() {
        when(aiClient.analyzeContent(anyString(), anyString(), any()))
                .thenReturn(analysis(true, 40, true, "low"));
        assertEquals(0, checker.checkKeyword(keyword()));
    }

    @Test
    void 未提及关键词且相关性低于严格阈值被过滤() {
        when(aiClient.analyzeContent(anyString(), anyString(), any()))
                .thenReturn(analysis(true, 55, false, "medium"));
        assertEquals(0, checker.checkKeyword(keyword()));
    }

    @Test
    void 未提及关键词但相关性高_保留() {
        when(aiClient.analyzeContent(anyString(), anyString(), any()))
                .thenReturn(analysis(true, 70, false, "high"));
        assertEquals(1, checker.checkKeyword(keyword()));
    }

    @Test
    void 已存在热点_跳过() {
        when(hotspotMapper.selectCount(any())).thenReturn(1L);
        assertEquals(0, checker.checkKeyword(keyword()));
        verify(hotspotMapper, never()).insert(any(Hotspot.class));
    }

    @Test
    void 配额限制_其他来源超过10条后不再处理() {
        List<SearchResult> many = new java.util.ArrayList<>();
        for (int i = 0; i < 15; i++) {
            SearchResult r = new SearchResult();
            r.setTitle("item-" + i);
            r.setContent("c");
            r.setUrl("https://example.com/" + i);
            r.setSource("bing");
            many.add(r);
        }
        when(collectService.collect(anyString())).thenReturn(many);
        int count = checker.checkKeyword(keyword());
        assertEquals(10, count); // other-quota 默认 10
        verify(hotspotMapper, times(10)).insert(any(Hotspot.class));
    }

    @Test
    void 预匹配结果传入AI分析() {
        checker.checkKeyword(keyword());
        ArgumentCaptor<KeywordUtils.PreMatchResult> captor = ArgumentCaptor.forClass(KeywordUtils.PreMatchResult.class);
        verify(aiClient).analyzeContent(anyString(), eq("Spring Boot 4"), captor.capture());
        // 内容包含 "Spring Boot 4",应命中扩展词
        assertTrue(captor.getValue().matched());
    }

    @Test
    void 新热点_触发WebSocket推送与广播() {
        checker.checkKeyword(keyword());
        verify(webSocketHandler).sendToKeywordSubscribers(eq("Spring Boot 4"), eq("hotspot:new"), any());
        verify(webSocketHandler).broadcast(eq("notification"), any());
    }

    @Test
    void high热点_触发邮件() {
        when(aiClient.analyzeContent(anyString(), anyString(), any()))
                .thenReturn(analysis(true, 90, true, "high"));
        checker.checkKeyword(keyword());
        verify(emailService).sendHotspotEmail(any());
    }

    @Test
    void low热点_不触发邮件() {
        when(aiClient.analyzeContent(anyString(), anyString(), any()))
                .thenReturn(analysis(true, 80, true, "low"));
        checker.checkKeyword(keyword());
        verify(emailService, never()).sendHotspotEmail(any());
    }

    @Test
    void 账号检测内容_豁免严格阈值() {
        // 账号内容:标题不含关键词、AI 降级 50 分,但仍应入库(HC-4 语义)
        SearchResult account = new SearchResult();
        account.setTitle("这是一只美人鱼");
        account.setContent("内容");
        account.setUrl("https://example.com/account-video");
        account.setSource("bilibili");
        account.setAccountContent(true);
        when(collectService.collect(anyString())).thenReturn(List.of());
        when(accountDetector.detectAndFetch(anyString()))
                .thenReturn(new AccountDetector.DetectResult(List.of(), List.of(account)));
        when(aiClient.analyzeContent(anyString(), anyString(), any()))
                .thenReturn(analysis(true, 50, false, "low"));

        int count = checker.checkKeyword(keyword());
        assertEquals(1, count);
    }

    @Test
    void 账号内容_相关性低于普通阈值仍被过滤() {
        SearchResult account = new SearchResult();
        account.setTitle("低相关账号内容");
        account.setContent("内容");
        account.setUrl("https://example.com/low");
        account.setSource("bilibili");
        account.setAccountContent(true);
        when(collectService.collect(anyString())).thenReturn(List.of());
        when(accountDetector.detectAndFetch(anyString()))
                .thenReturn(new AccountDetector.DetectResult(List.of(), List.of(account)));
        when(aiClient.analyzeContent(anyString(), anyString(), any()))
                .thenReturn(analysis(true, 30, false, "low"));

        assertEquals(0, checker.checkKeyword(keyword()));
    }
}
