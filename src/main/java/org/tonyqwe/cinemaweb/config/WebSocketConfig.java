package org.tonyqwe.cinemaweb.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.tonyqwe.cinemaweb.websocket.SeatWebSocketHandler;
import org.tonyqwe.cinemaweb.websocket.SeatWebSocketInterceptor;

/**
 * WebSocket配置类
 * 配置座位实时同步的WebSocket端点
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private SeatWebSocketHandler seatWebSocketHandler;

    @Resource
    private SeatWebSocketInterceptor seatWebSocketInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册座位WebSocket处理器
        registry.addHandler(seatWebSocketHandler, "/ws/seats/{showtimeId}")
                .addInterceptors(seatWebSocketInterceptor)
                .setAllowedOrigins("*");
    }
}
