package com.yupi.hottopic.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HotspotWebSocketHandlerTest {

    private HotspotWebSocketHandler handler;
    private WebSocketSession session1;
    private WebSocketSession session2;

    @BeforeEach
    void setUp() {
        handler = new HotspotWebSocketHandler();
        session1 = mockSession("s1");
        session2 = mockSession("s2");
        handler.afterConnectionEstablished(session1);
        handler.afterConnectionEstablished(session2);
    }

    private WebSocketSession mockSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    @Test
    void 订阅后_按关键词推送() throws Exception {
        handler.handleTextMessage(session1, new TextMessage(
                "{\"action\":\"subscribe\",\"keywords\":[\"AI\",\"Claude\"]}"));
        handler.sendToKeywordSubscribers("AI", "hotspot:new", Map.of("id", "1"));
        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        // 未订阅该关键词的会话不收
        verify(session2, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void 未订阅关键词_不推送() throws Exception {
        handler.sendToKeywordSubscribers("Java", "hotspot:new", Map.of());
        verify(session1, never()).sendMessage(any(TextMessage.class));
        verify(session2, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void 取消订阅后_不再推送() throws Exception {
        handler.handleTextMessage(session1, new TextMessage(
                "{\"action\":\"subscribe\",\"keywords\":[\"AI\"]}"));
        handler.handleTextMessage(session1, new TextMessage(
                "{\"action\":\"unsubscribe\",\"keywords\":[\"AI\"]}"));
        handler.sendToKeywordSubscribers("AI", "hotspot:new", Map.of());
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void 广播_所有连接都收到() throws Exception {
        handler.broadcast("notification", Map.of("title", "x"));
        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void 断开连接后_不再推送() throws Exception {
        handler.afterConnectionClosed(session2, null);
        handler.broadcast("notification", Map.of());
        verify(session1, times(1)).sendMessage(any(TextMessage.class));
        verify(session2, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void 无效消息_不影响后续() throws Exception {
        handler.handleTextMessage(session1, new TextMessage("not json"));
        handler.handleTextMessage(session1, new TextMessage("{\"action\":\"bad\"}"));
        // 无异常即通过
        assertEquals(2, handler.onlineCount());
    }

    @Test
    void 推送消息格式_含type和data() throws Exception {
        handler.handleTextMessage(session1, new TextMessage(
                "{\"action\":\"subscribe\",\"keywords\":[\"AI\"]}"));
        handler.sendToKeywordSubscribers("AI", "hotspot:new", Map.of("id", "h1"));
        var captor = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session1).sendMessage(captor.capture());
        String payload = captor.getValue().getPayload();
        assertTrue(payload.contains("\"type\":\"hotspot:new\""));
        assertTrue(payload.contains("\"data\""));
        assertTrue(payload.contains("h1"));
    }
}
