package org.tonyqwe.cinemaweb.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.entity.SeatStatus;
import org.tonyqwe.cinemaweb.mapper.SeatStatusMapper;
import org.tonyqwe.cinemaweb.websocket.SeatWebSocketHandler;

import java.util.Date;
import java.util.List;

/**
 * 订单超时处理服务
 * 
 * 注意：订单超时处理已迁移到RabbitMQ延迟队列
 * 此类现在仅用于清理过期的座位锁定状态
 */
@Slf4j
@Service
public class OrderTimeoutService {

    @Resource
    private SeatStatusMapper seatStatusMapper;

    @Resource
    private SeatWebSocketHandler seatWebSocketHandler;

    /**
     * 每60秒清理一次过期的座位锁定
     * 作为RabbitMQ延迟队列的兜底机制
     */
    @Scheduled(fixedRate = 60000)
    @Transactional(rollbackFor = Exception.class)
    public void cleanExpiredSeatLocks() {
        log.debug("开始清理过期座位锁定...");

        Date now = new Date();
        List<SeatStatus> expiredLocks = seatStatusMapper.selectExpiredLocks(now);

        if (expiredLocks.isEmpty()) {
            log.debug("没有过期座位锁定需要清理");
            return;
        }

        log.info("发现 {} 个过期座位锁定需要清理", expiredLocks.size());

        for (SeatStatus seatStatus : expiredLocks) {
            try {
                // 删除过期的座位锁定记录
                seatStatusMapper.deleteById(seatStatus.getId());

                // 通过WebSocket通知座位已释放
                String seatIds = "[" + seatStatus.getSeatId() + "]";
                seatWebSocketHandler.broadcastSeatReleased(seatStatus.getShowtimeId(), seatIds);

                log.info("过期座位锁定已清理: showtimeId={}, seatId={}",
                        seatStatus.getShowtimeId(), seatStatus.getSeatId());
            } catch (Exception e) {
                log.error("清理过期座位锁定失败: showtimeId={}, seatId={}, error={}",
                        seatStatus.getShowtimeId(), seatStatus.getSeatId(), e.getMessage());
            }
        }
    }
}
