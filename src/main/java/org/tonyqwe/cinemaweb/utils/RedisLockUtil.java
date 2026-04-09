package org.tonyqwe.cinemaweb.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Redis分布式锁工具类
 * 基于RedisTemplate实现，用于高并发场景下的资源互斥访问
 */
@Slf4j
@Component
public class RedisLockUtil {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 座位锁的Key前缀
     */
    private static final String SEAT_LOCK_PREFIX = "lock:seat:";

    /**
     * 订单锁的Key前缀
     */
    private static final String ORDER_LOCK_PREFIX = "lock:order:";

    /**
     * 默认等待时间（秒）
     */
    private static final long DEFAULT_WAIT_TIME = 3;

    /**
     * 默认租约时间（秒）
     */
    private static final long DEFAULT_LEASE_TIME = 10;

    /**
     * 锁的唯一标识
     */
    private static final String LOCK_VALUE = "locked";

    /**
     * 获取座位锁的Key
     *
     * @param showtimeId 场次ID
     * @param seatId     座位ID
     * @return 锁的Key
     */
    public String getSeatLockKey(Long showtimeId, Long seatId) {
        return SEAT_LOCK_PREFIX + showtimeId + ":" + seatId;
    }

    /**
     * 获取订单锁的Key
     *
     * @param orderId 订单ID
     * @return 锁的Key
     */
    public String getOrderLockKey(Long orderId) {
        return ORDER_LOCK_PREFIX + orderId;
    }

    /**
     * 尝试获取锁
     *
     * @param lockKey     锁的Key
     * @param waitTime    等待时间
     * @param leaseTime   租约时间
     * @param timeUnit    时间单位
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit) {
        long startTime = System.currentTimeMillis();
        long waitMillis = timeUnit.toMillis(waitTime);
        long leaseMillis = timeUnit.toMillis(leaseTime);

        while (System.currentTimeMillis() - startTime < waitMillis) {
            Boolean acquired = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, LOCK_VALUE, leaseMillis, TimeUnit.MILLISECONDS);
            if (Boolean.TRUE.equals(acquired)) {
                log.debug("获取锁成功: {}", lockKey);
                return true;
            }

            try {
                Thread.sleep(100); // 短暂休眠后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        log.debug("获取锁失败: {}", lockKey);
        return false;
    }

    /**
     * 尝试获取锁（使用默认时间）
     *
     * @param lockKey 锁的Key
     * @return 是否获取成功
     */
    public boolean tryLock(String lockKey) {
        return tryLock(lockKey, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
    }

    /**
     * 释放锁
     *
     * @param lockKey 锁的Key
     */
    public void unlock(String lockKey) {
        try {
            stringRedisTemplate.delete(lockKey);
            log.debug("释放锁成功: {}", lockKey);
        } catch (Exception e) {
            log.error("释放锁失败: {}", lockKey, e);
        }
    }

    /**
     * 使用锁执行操作
     *
     * @param lockKey  锁的Key
     * @param supplier 要执行的操作
     * @param <T>      返回值类型
     * @return 操作结果，如果获取锁失败返回null
     */
    public <T> T executeWithLock(String lockKey, Supplier<T> supplier) {
        boolean locked = false;
        try {
            locked = tryLock(lockKey);
            if (!locked) {
                log.warn("获取锁失败，操作未执行: {}", lockKey);
                return null;
            }
            return supplier.get();
        } finally {
            if (locked) {
                unlock(lockKey);
            }
        }
    }

    /**
     * 批量获取多个座位锁
     * 注意：这种实现不是原子的，可能会导致部分锁定
     * 但在实际场景中，这种概率较低
     *
     * @param showtimeId 场次ID
     * @param seatIds    座位ID列表
     * @param waitTime   等待时间
     * @param leaseTime  租约时间
     * @param timeUnit   时间单位
     * @return 是否全部获取成功
     */
    public boolean tryLockMultiSeats(Long showtimeId, List<Long> seatIds, long waitTime, long leaseTime, TimeUnit timeUnit) {
        List<String> lockKeys = seatIds.stream()
                .map(seatId -> getSeatLockKey(showtimeId, seatId))
                .collect(Collectors.toList());

        boolean allAcquired = true;
        List<String> acquiredLocks = new java.util.ArrayList<>();
        long startTime = System.currentTimeMillis();
        long waitMillis = timeUnit.toMillis(waitTime);

        try {
            for (String lockKey : lockKeys) {
                // 计算剩余等待时间
                long remainingWaitTime = Math.max(0, waitMillis - (System.currentTimeMillis() - startTime));
                if (remainingWaitTime == 0) {
                    allAcquired = false;
                    break;
                }

                boolean acquired = tryLock(lockKey, remainingWaitTime, leaseTime, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    allAcquired = false;
                    break;
                }
                acquiredLocks.add(lockKey);
            }

            if (allAcquired) {
                log.debug("批量获取座位锁成功: showtimeId={}, seats={}", showtimeId, seatIds);
            } else {
                log.warn("批量获取座位锁失败: showtimeId={}, seats={}", showtimeId, seatIds);
            }

            return allAcquired;
        } finally {
            if (!allAcquired) {
                // 释放已获取的锁
                for (String lockKey : acquiredLocks) {
                    unlock(lockKey);
                }
            }
        }
    }

    /**
     * 批量释放多个座位锁
     *
     * @param showtimeId 场次ID
     * @param seatIds    座位ID列表
     */
    public void unlockMultiSeats(Long showtimeId, List<Long> seatIds) {
        for (Long seatId : seatIds) {
            unlock(getSeatLockKey(showtimeId, seatId));
        }
        log.debug("批量释放座位锁完成: showtimeId={}, seats={}", showtimeId, seatIds);
    }
}
