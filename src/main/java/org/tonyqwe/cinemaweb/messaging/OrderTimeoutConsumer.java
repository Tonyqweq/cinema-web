package org.tonyqwe.cinemaweb.messaging;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.config.RabbitMQConfig;
import org.tonyqwe.cinemaweb.domain.entity.Orders;
import org.tonyqwe.cinemaweb.domain.entity.SeatStatus;
import org.tonyqwe.cinemaweb.mapper.OrderMapper;
import org.tonyqwe.cinemaweb.mapper.SeatStatusMapper;
import org.tonyqwe.cinemaweb.websocket.SeatWebSocketHandler;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单超时消息消费者
 * 监听订单超时队列，处理超时未支付订单
 */
@Slf4j
@Component
public class OrderTimeoutConsumer {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private SeatStatusMapper seatStatusMapper;

    @Resource
    private SeatWebSocketHandler seatWebSocketHandler;

    /**
     * 消费订单超时消息
     * 检查订单状态，如果仍为待支付则自动取消
     *
     * @param message 订单超时消息
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_TIMEOUT_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void handleOrderTimeout(OrderTimeoutMessage message) {
        log.info("收到订单超时消息: orderId={}", message.getOrderId());

        try {
            // 1. 查询订单当前状态
            Orders order = orderMapper.selectById(message.getOrderId());

            if (order == null) {
                log.warn("订单不存在: orderId={}", message.getOrderId());
                return;
            }

            // 2. 检查订单状态，只有待支付状态(0)才需要取消
            if (order.getOrderStatus() != 0) {
                log.info("订单状态已变更，无需取消: orderId={}, status={}",
                        message.getOrderId(), order.getOrderStatus());
                return;
            }

            // 3. 更新订单状态为已取消(3)
            order.setOrderStatus(3);
            orderMapper.updateById(order);

            // 4. 释放座位锁定状态
            releaseSeats(message.getShowtimeId(), message.getSeats());

            // 5. 通过WebSocket通知所有在线用户座位已释放
            notifySeatReleased(message.getShowtimeId(), message.getSeats());

            log.info("订单已自动取消并释放座位: orderId={}", message.getOrderId());

        } catch (Exception e) {
            log.error("处理订单超时消息失败: orderId={}, error={}", message.getOrderId(), e.getMessage());
            throw e; // 抛出异常，触发消息重试
        }
    }

    /**
     * 释放座位锁定状态
     *
     * @param showtimeId 场次ID
     * @param seatsJson  座位ID列表（JSON格式）
     */
    private void releaseSeats(Long showtimeId, String seatsJson) {
        if (seatsJson == null || seatsJson.isEmpty()) {
            return;
        }

        try {
            // 解析座位ID列表
            String cleanJson = seatsJson.replace("[", "").replace("]", "").replace(" ", "");
            List<Long> seatIds = Arrays.stream(cleanJson.split(","))
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());

            // 删除座位状态记录（释放锁定）
            for (Long seatId : seatIds) {
                seatStatusMapper.deleteByShowtimeIdAndSeatId(showtimeId, seatId);
            }

            log.info("座位已释放: showtimeId={}, seats={}", showtimeId, seatIds);
        } catch (Exception e) {
            log.error("释放座位失败: showtimeId={}, seats={}, error={}", showtimeId, seatsJson, e.getMessage());
        }
    }

    /**
     * 通知所有在线用户座位已释放
     *
     * @param showtimeId 场次ID
     * @param seatsJson  座位ID列表
     */
    private void notifySeatReleased(Long showtimeId, String seatsJson) {
        try {
            seatWebSocketHandler.broadcastSeatReleased(showtimeId, seatsJson);
        } catch (Exception e) {
            log.error("发送座位释放通知失败: showtimeId={}, error={}", showtimeId, e.getMessage());
        }
    }
}
