package org.tonyqwe.cinemaweb.messaging;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.tonyqwe.cinemaweb.config.RabbitMQConfig;

/**
 * 订单超时消息生产者
 * 负责发送订单超时延迟消息到RabbitMQ
 */
@Slf4j
@Component
public class OrderTimeoutProducer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送订单超时延迟消息
     * 消息将在5分钟后被消费，用于自动取消未支付订单
     *
     * @param orderId    订单ID
     * @param userId     用户ID
     * @param showtimeId 场次ID
     * @param seats      座位ID列表（JSON格式）
     */
    public void sendOrderTimeoutMessage(Long orderId, Long userId, Long showtimeId, String seats) {
        OrderTimeoutMessage message = new OrderTimeoutMessage(
                orderId,
                userId,
                showtimeId,
                seats,
                System.currentTimeMillis()
        );

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_TIMEOUT_EXCHANGE,
                    RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                    message
            );
            log.info("订单超时消息已发送: orderId={}, 将在{}分钟后超时", orderId, 5);
        } catch (Exception e) {
            log.error("发送订单超时消息失败: orderId={}, error={}", orderId, e.getMessage());
            throw new RuntimeException("发送订单超时消息失败", e);
        }
    }

    /**
     * 取消订单超时消息（通过发送取消标记）
     * 实际实现中，我们在消费端检查订单状态，如果已支付则忽略
     *
     * @param orderId 订单ID
     */
    public void cancelOrderTimeoutMessage(Long orderId) {
        // 由于RabbitMQ无法直接删除已发送的消息
        // 我们在消费端检查订单状态，如果已支付或已取消则忽略
        log.info("订单超时消息取消标记: orderId={}", orderId);
    }
}
