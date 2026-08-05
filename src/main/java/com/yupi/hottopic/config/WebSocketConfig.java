package com.yupi.hottopic.config;

import com.yupi.hottopic.ws.HotspotWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置(需求 NT-1,端点 /ws)
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final HotspotWebSocketHandler hotspotWebSocketHandler;

    public WebSocketConfig(HotspotWebSocketHandler hotspotWebSocketHandler) {
        this.hotspotWebSocketHandler = hotspotWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(hotspotWebSocketHandler, "/ws")
                .setAllowedOriginPatterns("*");
    }
}
