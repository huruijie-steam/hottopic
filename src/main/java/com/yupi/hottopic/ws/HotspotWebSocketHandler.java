package com.yupi.hottopic.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 热点 WebSocket 推送(需求 NT-1,协议见需求文档 §7.2)。
 *
 * 客户端消息(JSON):{"action":"subscribe","keywords":["AI"]} / {"action":"unsubscribe","keywords":["AI"]}
 * 服务端推送(JSON):{"type":"hotspot:new","data":{...}} / {"type":"notification","data":{...}}
 */
@Component
public class HotspotWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(HotspotWebSocketHandler.class);

    /** sessionId → WebSocketSession */
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /** sessionId → 订阅的关键词集合 */
    private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        subscriptions.put(session.getId(), ConcurrentHashMap.newKeySet());
        log.info("WebSocket 客户端连接: {} (当前 {} 个连接)", session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        subscriptions.remove(session.getId());
        log.info("WebSocket 客户端断开: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            String action = node.path("action").asText("");
            JsonNode keywordsNode = node.path("keywords");
            if (!keywordsNode.isArray()) {
                log.warn("无效的订阅消息(缺少 keywords 数组): {}", message.getPayload());
                return;
            }
            List<String> keywords = new java.util.ArrayList<>();
            for (JsonNode k : keywordsNode) {
                String s = k.asText("");
                if (!s.isBlank()) {
                    keywords.add(s.trim());
                }
            }
            Set<String> subs = subscriptions.computeIfAbsent(session.getId(), k -> ConcurrentHashMap.newKeySet());
            switch (action) {
                case "subscribe" -> {
                    subs.addAll(keywords);
                    log.info("客户端 {} 订阅关键词: {}", session.getId(), keywords);
                }
                case "unsubscribe" -> {
                    subs.removeAll(keywords);
                    log.info("客户端 {} 取消订阅关键词: {}", session.getId(), keywords);
                }
                default -> log.warn("未知动作: {}", action);
            }
        } catch (Exception e) {
            log.warn("解析 WebSocket 消息失败: {}", e.getMessage());
        }
    }

    /**
     * 向订阅了指定关键词的所有客户端推送消息(需求 NT-1)。
     *
     * @param keyword 关键词(订阅维度)
     * @param payload 推送内容(任意对象,序列化为 JSON)
     */
    public void sendToKeywordSubscribers(String keyword, String type, Object payload) {
        if (keyword == null || sessions.isEmpty()) {
            return;
        }
        String json = toJson(type, payload);
        for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
            Set<String> subs = subscriptions.get(entry.getKey());
            if (subs != null && subs.contains(keyword)) {
                sendQuietly(entry.getValue(), json);
            }
        }
    }

    /** 向所有客户端广播(如站内通知) */
    public void broadcast(String type, Object payload) {
        String json = toJson(type, payload);
        for (WebSocketSession session : sessions.values()) {
            sendQuietly(session, json);
        }
    }

    private String toJson(String type, Object payload) {
        try {
            Map<String, Object> wrapper = new java.util.HashMap<>();
            wrapper.put("type", type);
            wrapper.put("data", payload);
            return objectMapper.writeValueAsString(wrapper);
        } catch (Exception e) {
            log.warn("序列化推送消息失败: {}", e.getMessage());
            return "{}";
        }
    }

    private void sendQuietly(WebSocketSession session, String json) {
        try {
            if (session.isOpen()) {
                synchronized (session) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.warn("向客户端 {} 推送失败: {}", session.getId(), e.getMessage());
        }
    }

    /** 当前在线连接数(供统计/测试) */
    int onlineCount() {
        return sessions.size();
    }
}
