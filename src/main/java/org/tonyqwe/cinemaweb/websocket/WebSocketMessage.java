package org.tonyqwe.cinemaweb.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket消息对象
 * 用于座位状态实时同步的消息格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {

    /**
     * 消息类型
     * PING - 心跳请求
     * PONG - 心跳响应
     * SEAT_LOCKED - 座位被锁定
     * SEAT_RELEASED - 座位被释放
     * SEAT_SOLD - 座位已售出
     * SEAT_SELECT - 用户选择座位
     * SEAT_UNSELECT - 用户取消选择座位
     */
    private String type;

    /**
     * 座位ID列表（JSON格式）
     */
    private String seatIds;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 时间戳
     */
    private Long timestamp = System.currentTimeMillis();

    public WebSocketMessage(String type, String seatIds, Long userId) {
        this.type = type;
        this.seatIds = seatIds;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }
}
