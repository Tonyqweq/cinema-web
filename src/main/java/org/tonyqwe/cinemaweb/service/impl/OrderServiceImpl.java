package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.dto.OrderRequest;
import org.tonyqwe.cinemaweb.domain.entity.Orders;
import org.tonyqwe.cinemaweb.domain.entity.SeatStatus;
import org.tonyqwe.cinemaweb.domain.entity.Seats;
import org.tonyqwe.cinemaweb.domain.entity.Showtimes;
import org.tonyqwe.cinemaweb.mapper.OrderMapper;
import org.tonyqwe.cinemaweb.mapper.SeatMapper;
import org.tonyqwe.cinemaweb.mapper.SeatStatusMapper;
import org.tonyqwe.cinemaweb.mapper.ShowtimesMapper;
import org.tonyqwe.cinemaweb.messaging.OrderTimeoutProducer;
import org.tonyqwe.cinemaweb.service.OrderService;
import org.tonyqwe.cinemaweb.utils.RedisLockUtil;
import org.tonyqwe.cinemaweb.websocket.SeatWebSocketHandler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现类
 * 支持高并发的购票流程
 */
@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private ShowtimesMapper showtimesMapper;

    @Resource
    private SeatMapper seatMapper;

    @Resource
    private SeatStatusMapper seatStatusMapper;

    @Resource
    private RedisLockUtil redisLockUtil;

    @Resource
    private OrderTimeoutProducer orderTimeoutProducer;

    @Resource
    private SeatWebSocketHandler seatWebSocketHandler;

    /**
     * 订单超时时间（分钟）
     */
    private static final int ORDER_TIMEOUT_MINUTES = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Orders createOrder(OrderRequest request, Long userId) {
        // 1. 验证场次是否存在
        Showtimes showtime = showtimesMapper.selectById(request.getShowtimeId());
        if (showtime == null) {
            throw new RuntimeException("场次不存在");
        }

        // 2. 使用Redis分布式锁锁定座位（高并发关键）
        List<Long> seatIds = request.getSeats();
        boolean locksAcquired = redisLockUtil.tryLockMultiSeats(
                request.getShowtimeId(),
                seatIds,
                3,  // 等待3秒
                30, // 锁定30秒（足够完成订单创建）
                TimeUnit.SECONDS
        );

        if (!locksAcquired) {
            throw new RuntimeException("座位已被其他用户锁定，请重新选择");
        }

        try {
            // 3. 再次验证座位状态（双重检查，防止并发问题）
            for (Long seatId : seatIds) {
                SeatStatus seatStatus = seatStatusMapper.selectByShowtimeIdAndSeatId(
                        request.getShowtimeId(), seatId);
                if (seatStatus != null && seatStatus.getStatus() != 0) {
                    throw new RuntimeException("座位已被占用，请重新选择");
                }
            }

            // 4. 验证座位是否存在
            List<Seats> seats = seatMapper.selectBatchIds(seatIds);
            if (seats.size() != seatIds.size()) {
                throw new RuntimeException("部分座位不存在");
            }

            // 5. 创建座位状态记录（锁定座位）
            Date lockExpireTime = new Date(System.currentTimeMillis() + ORDER_TIMEOUT_MINUTES * 60 * 1000);
            for (Long seatId : seatIds) {
                SeatStatus seatStatus = new SeatStatus();
                seatStatus.setShowtimeId(request.getShowtimeId());
                seatStatus.setSeatId(seatId);
                seatStatus.setStatus(1); // 1-已锁定
                seatStatus.setLockExpireTime(lockExpireTime);
                seatStatusMapper.insert(seatStatus);
            }

            // 6. 计算总价
            java.math.BigDecimal totalPrice = java.math.BigDecimal.ZERO;
            for (Seats seat : seats) {
                if (seat.getPrice() != null) {
                    totalPrice = totalPrice.add(seat.getPrice());
                } else {
                    totalPrice = totalPrice.add(showtime.getPrice());
                }
            }

            // 7. 创建订单
            Orders order = new Orders();
            order.setUserId(userId);
            order.setShowtimeId(request.getShowtimeId());
            order.setOrderStatus(0); // 0-待支付
            order.setTotalPrice(totalPrice.doubleValue());
            // 将座位ID列表转换为字符串，格式："[1,2,3]"
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < seatIds.size(); i++) {
                sb.append(seatIds.get(i));
                if (i < seatIds.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append("]");
            order.setSeats(sb.toString());

            orderMapper.insert(order);

            // 8. 发送订单超时延迟消息到RabbitMQ
            orderTimeoutProducer.sendOrderTimeoutMessage(
                    order.getId(),
                    userId,
                    request.getShowtimeId(),
                    order.getSeats()
            );

            // 9. 通过WebSocket广播座位锁定消息
            seatWebSocketHandler.broadcastSeatLocked(
                    request.getShowtimeId(),
                    order.getSeats(),
                    userId
            );

            // 10. 关联场次和座位信息
            order.setShowtime(showtime);
            order.setSeatList(seats);

            log.info("订单创建成功: orderId={}, userId={}, showtimeId={}, seats={}",
                    order.getId(), userId, request.getShowtimeId(), order.getSeats());

            return order;

        } finally {
            // 释放Redis分布式锁
            redisLockUtil.unlockMultiSeats(request.getShowtimeId(), seatIds);
        }
    }

    @Override
    public List<Orders> getOrdersByUserId(Long userId) {
        LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Orders::getUserId, userId);
        wrapper.orderByDesc(Orders::getCreatedAt);
        return orderMapper.selectList(wrapper);
    }

    @Override
    public Orders getOrderById(Long orderId) {
        return orderMapper.selectById(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelOrder(Long orderId, Long userId) {
        // 1. 验证订单是否存在
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 2. 验证订单是否属于当前用户
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该订单");
        }

        // 3. 验证订单状态
        if (order.getOrderStatus() != 0) {
            throw new RuntimeException("订单状态不允许取消");
        }

        // 4. 使用分布式锁保护订单操作
        String lockKey = redisLockUtil.getOrderLockKey(orderId);
        Boolean result = redisLockUtil.executeWithLock(lockKey, () -> {
            // 再次检查订单状态
            Orders currentOrder = orderMapper.selectById(orderId);
            if (currentOrder == null || currentOrder.getOrderStatus() != 0) {
                return false;
            }

            // 更新订单状态为已取消
            currentOrder.setOrderStatus(3); // 3-已取消
            orderMapper.updateById(currentOrder);

            // 释放座位锁定状态
            releaseSeats(order.getShowtimeId(), order.getSeats());

            return true;
        });

        if (Boolean.TRUE.equals(result)) {
            // 通知RabbitMQ取消超时检查（实际在消费端检查状态）
            orderTimeoutProducer.cancelOrderTimeoutMessage(orderId);

            // 通过WebSocket广播座位释放消息
            seatWebSocketHandler.broadcastSeatReleased(order.getShowtimeId(), order.getSeats());

            log.info("订单已取消: orderId={}, userId={}", orderId, userId);
        }

        return Boolean.TRUE.equals(result);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payOrder(Long orderId, Long userId) {
        // 1. 验证订单是否存在
        Orders order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }

        // 2. 验证订单是否属于当前用户
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该订单");
        }

        // 3. 验证订单状态
        if (order.getOrderStatus() != 0) {
            throw new RuntimeException("订单状态不允许支付");
        }

        // 4. 使用分布式锁保护订单操作
        String lockKey = redisLockUtil.getOrderLockKey(orderId);
        Boolean result = redisLockUtil.executeWithLock(lockKey, () -> {
            // 再次检查订单状态
            Orders currentOrder = orderMapper.selectById(orderId);
            if (currentOrder == null || currentOrder.getOrderStatus() != 0) {
                return false;
            }

            // 更新订单状态为已支付
            currentOrder.setOrderStatus(1); // 1-已支付
            orderMapper.updateById(currentOrder);

            // 更新座位状态为已售
            updateSeatsToSold(order.getShowtimeId(), order.getSeats());

            return true;
        });

        if (Boolean.TRUE.equals(result)) {
            // 通知RabbitMQ取消超时检查（实际在消费端检查状态）
            orderTimeoutProducer.cancelOrderTimeoutMessage(orderId);

            // 通过WebSocket广播座位已售消息
            seatWebSocketHandler.broadcastSeatSold(order.getShowtimeId(), order.getSeats());

            log.info("订单支付成功: orderId={}, userId={}", orderId, userId);
        }

        return Boolean.TRUE.equals(result);
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
            List<Long> seatIds = new ArrayList<>();
            for (String seatIdStr : cleanJson.split(",")) {
                if (!seatIdStr.isEmpty()) {
                    seatIds.add(Long.parseLong(seatIdStr));
                }
            }

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
     * 更新座位状态为已售
     *
     * @param showtimeId 场次ID
     * @param seatsJson  座位ID列表（JSON格式）
     */
    private void updateSeatsToSold(Long showtimeId, String seatsJson) {
        if (seatsJson == null || seatsJson.isEmpty()) {
            return;
        }

        try {
            // 解析座位ID列表
            String cleanJson = seatsJson.replace("[", "").replace("]", "").replace(" ", "");
            List<Long> seatIds = new ArrayList<>();
            for (String seatIdStr : cleanJson.split(",")) {
                if (!seatIdStr.isEmpty()) {
                    seatIds.add(Long.parseLong(seatIdStr));
                }
            }

            // 更新座位状态为已售
            for (Long seatId : seatIds) {
                SeatStatus seatStatus = seatStatusMapper.selectByShowtimeIdAndSeatId(showtimeId, seatId);
                if (seatStatus != null) {
                    seatStatus.setStatus(2); // 2-已售出
                    seatStatusMapper.updateById(seatStatus);
                }
            }

            log.info("座位已更新为已售状态: showtimeId={}, seats={}", showtimeId, seatIds);
        } catch (Exception e) {
            log.error("更新座位状态失败: showtimeId={}, seats={}, error={}", showtimeId, seatsJson, e.getMessage());
        }
    }
}
