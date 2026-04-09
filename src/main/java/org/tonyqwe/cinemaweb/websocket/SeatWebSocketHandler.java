package org.tonyqwe.cinemaweb.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 座位WebSocket处理器
 * 处理座位状态的实时同步
 */
@Slf4j
@Component
public class SeatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 存储每个场次的WebSocket会话
     * key: showtimeId, value: 该场次的所有会话
     */
    private final Map<Long, Set<WebSocketSession>> showtimeSessions = new ConcurrentHashMap<>();

    /**
     * 存储每个会话对应的场次ID
     */
    private final Map<String, Long> sessionShowtimeMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long showtimeId = getShowtimeIdFromSession(session);
        if (showtimeId == null) {
            log.warn("WebSocket连接建立失败，未找到场次ID: sessionId={}", session.getId());
            session.close();
            return;
        }

        // 将会话添加到对应场次的会话集合中
        showtimeSessions.computeIfAbsent(showtimeId, k -> new CopyOnWriteArraySet<>()).add(session);
        sessionShowtimeMap.put(session.getId(), showtimeId);

        log.info("WebSocket连接已建立: sessionId={}, showtimeId={}, 当前场次在线人数={}",
                session.getId(), showtimeId, showtimeSessions.get(showtimeId).size());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到WebSocket消息: sessionId={}, message={}", session.getId(), payload);

        try {
            // 解析消息
            WebSocketMessage wsMessage = objectMapper.readValue(payload, WebSocketMessage.class);

            switch (wsMessage.getType()) {
                case "PING":
                    // 心跳响应
                    sendMessage(session, new WebSocketMessage("PONG", null, null));
                    break;
                case "SEAT_SELECT":
                    // 座位选择通知
                    handleSeatSelect(session, wsMessage);
                    break;
                case "SEAT_UNSELECT":
                    // 座位取消选择通知
                    handleSeatUnselect(session, wsMessage);
                    break;
                default:
                    log.warn("未知的消息类型: {}", wsMessage.getType());
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long showtimeId = sessionShowtimeMap.remove(session.getId());
        if (showtimeId != null) {
            Set<WebSocketSession> sessions = showtimeSessions.get(showtimeId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    showtimeSessions.remove(showtimeId);
                }
                log.info("WebSocket连接已关闭: sessionId={}, showtimeId={}, 剩余在线人数={}",
                        session.getId(), showtimeId, sessions.size());
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: sessionId={}, error={}", session.getId(), exception.getMessage());
        session.close();
    }

    /**
     * 广播座位锁定消息
     * 当用户选择座位时，通知同一场次的其他用户
     *
     * @param showtimeId 场次ID
     * @param seatIds    被锁定的座位ID列表
     * @param userId     操作用户ID
     */
    public void broadcastSeatLocked(Long showtimeId, String seatIds, Long userId) {
        Set<WebSocketSession> sessions = showtimeSessions.get(showtimeId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        WebSocketMessage message = new WebSocketMessage("SEAT_LOCKED", seatIds, userId);
        broadcastMessage(showtimeId, message, null);
    }

    /**
     * 广播座位释放消息
     * 当订单取消或超时时，通知同一场次的所有用户
     *
     * @param showtimeId 场次ID
     * @param seatIds    被释放的座位ID列表
     */
    public void broadcastSeatReleased(Long showtimeId, String seatIds) {
        Set<WebSocketSession> sessions = showtimeSessions.get(showtimeId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        WebSocketMessage message = new WebSocketMessage("SEAT_RELEASED", seatIds, null);
        broadcastMessage(showtimeId, message, null);
    }

    /**
     * 广播座位已售消息
     * 当订单支付成功时，通知同一场次的所有用户
     *
     * @param showtimeId 场次ID
     * @param seatIds    已售座位ID列表
     */
    public void broadcastSeatSold(Long showtimeId, String seatIds) {
        Set<WebSocketSession> sessions = showtimeSessions.get(showtimeId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        WebSocketMessage message = new WebSocketMessage("SEAT_SOLD", seatIds, null);
        broadcastMessage(showtimeId, message, null);
    }

    /**
     * 向指定场次的所有用户广播消息
     *
     * @param showtimeId    场次ID
     * @param message       消息对象
     * @param excludeSession 需要排除的会话（可选）
     */
    private void broadcastMessage(Long showtimeId, WebSocketMessage message, WebSocketSession excludeSession) {
        Set<WebSocketSession> sessions = showtimeSessions.get(showtimeId);
        if (sessions == null) {
            return;
        }

        String messageJson;
        try {
            messageJson = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("序列化WebSocket消息失败: {}", e.getMessage());
            return;
        }

        for (WebSocketSession session : sessions) {
            if (session.isOpen() && !session.equals(excludeSession)) {
                try {
                    session.sendMessage(new TextMessage(messageJson));
                } catch (IOException e) {
                    log.error("发送WebSocket消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
                }
            }
        }
    }

    /**
     * 处理座位选择消息
     */
    private void handleSeatSelect(WebSocketSession session, WebSocketMessage message) {
        Long showtimeId = sessionShowtimeMap.get(session.getId());
        if (showtimeId == null) {
            return;
        }

        // 广播座位锁定消息给其他用户
        broadcastSeatLocked(showtimeId, message.getSeatIds(), message.getUserId());
    }

    /**
     * 处理座位取消选择消息
     */
    private void handleSeatUnselect(WebSocketSession session, WebSocketMessage message) {
        Long showtimeId = sessionShowtimeMap.get(session.getId());
        if (showtimeId == null) {
            return;
        }

        // 广播座位释放消息给所有用户
        broadcastSeatReleased(showtimeId, message.getSeatIds());
    }

    /**
     * 发送消息给指定会话
     */
    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        if (!session.isOpen()) {
            return;
        }

        try {
            String messageJson = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(messageJson));
        } catch (Exception e) {
            log.error("发送WebSocket消息失败: sessionId={}, error={}", session.getId(), e.getMessage());
        }
    }

    /**
     * 从会话属性中获取场次ID
     */
    private Long getShowtimeIdFromSession(WebSocketSession session) {
        String showtimeIdStr = (String) session.getAttributes().get("showtimeId");
        if (showtimeIdStr == null) {
            return null;
        }
        try {
            return Long.parseLong(showtimeIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取指定场次的在线人数
     */
    public int getOnlineCount(Long showtimeId) {
        Set<WebSocketSession> sessions = showtimeSessions.get(showtimeId);
        return sessions == null ? 0 : sessions.size();
    }
}
