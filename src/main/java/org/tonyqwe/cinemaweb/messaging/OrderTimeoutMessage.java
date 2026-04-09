package org.tonyqwe.cinemaweb.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单超时消息
 * 用于RabbitMQ传递订单超时信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderTimeoutMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    private Long orderId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 场次ID
     */
    private Long showtimeId;

    /**
     * 座位ID列表（JSON格式）
     */
    private String seats;

    /**
     * 消息创建时间
     */
    private Long createTime;
}
