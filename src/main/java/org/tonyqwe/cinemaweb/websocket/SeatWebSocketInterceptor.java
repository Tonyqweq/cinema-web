package org.tonyqwe.cinemaweb.websocket;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.tonyqwe.cinemaweb.service.TokenService;

import java.util.Map;

/**
 * 座位WebSocket握手拦截器
 * 用于验证用户身份并提取场次ID
 */
@Slf4j
@Component
public class SeatWebSocketInterceptor implements HandshakeInterceptor {

    @Resource
    private TokenService tokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

            // 1. 从URL路径参数中获取场次ID
            String path = servletRequest.getServletRequest().getRequestURI();
            Long showtimeId = extractShowtimeId(path);
            if (showtimeId == null) {
                log.warn("WebSocket握手失败，未找到场次ID: path={}", path);
                return false;
            }
            attributes.put("showtimeId", showtimeId.toString());

            // 2. 从请求参数中获取Token并验证用户身份
            String token = servletRequest.getServletRequest().getParameter("token");
            if (token != null && !token.isEmpty()) {
                Long userId = tokenService.getUserIdFromToken(token);
                if (userId != null) {
                    attributes.put("userId", userId.toString());
                    log.debug("WebSocket用户认证成功: userId={}, showtimeId={}", userId, showtimeId);
                }
            }

            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手后的处理，无需额外操作
    }

    /**
     * 从URL路径中提取场次ID
     * URL格式: /ws/seats/{showtimeId}
     */
    private Long extractShowtimeId(String path) {
        try {
            String[] parts = path.split("/");
            if (parts.length >= 4) {
                return Long.parseLong(parts[parts.length - 1]);
            }
        } catch (NumberFormatException e) {
            log.warn("解析场次ID失败: path={}", path);
        }
        return null;
    }
}
