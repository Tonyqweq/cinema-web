package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.dto.SeatDTO;
import org.tonyqwe.cinemaweb.domain.entity.Orders;
import org.tonyqwe.cinemaweb.domain.entity.SeatStatus;
import org.tonyqwe.cinemaweb.domain.entity.Seats;
import org.tonyqwe.cinemaweb.domain.entity.Showtimes;
import org.tonyqwe.cinemaweb.domain.vo.SeatVO;
import org.tonyqwe.cinemaweb.mapper.HallMapper;
import org.tonyqwe.cinemaweb.mapper.OrderMapper;
import org.tonyqwe.cinemaweb.mapper.SeatMapper;
import org.tonyqwe.cinemaweb.mapper.SeatStatusMapper;
import org.tonyqwe.cinemaweb.mapper.ShowtimesMapper;
import org.tonyqwe.cinemaweb.service.SeatService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 座位服务实现类
 * 支持实时座位状态同步
 */
@Slf4j
@Service
public class SeatServiceImpl implements SeatService {

    @Resource
    private SeatMapper seatMapper;

    @Resource
    private HallMapper hallMapper;

    @Resource
    private ShowtimesMapper showtimesMapper;

    @Resource
    private OrderMapper orderMapper;

    @Resource
    private SeatStatusMapper seatStatusMapper;

    @Override
    public List<SeatVO> getSeatsByHallId(Long hallId) {
        QueryWrapper<Seats> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("hall_id", hallId);
        queryWrapper.orderByAsc("row_number", "column_number");
        List<Seats> seats = seatMapper.selectList(queryWrapper);
        return seats.stream().map(this::toSeatVO).collect(Collectors.toList());
    }

    @Override
    public List<SeatVO> getSeatsByShowtimeId(Long showtimeId) {
        // 1. 获取场次信息
        Showtimes showtime = showtimesMapper.selectById(showtimeId);
        if (showtime == null) {
            return new ArrayList<>();
        }

        // 2. 获取影厅的所有座位
        Long hallId = showtime.getHallId();
        QueryWrapper<Seats> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("hall_id", hallId);
        queryWrapper.orderByAsc("row_number", "column_number");
        List<Seats> seats = seatMapper.selectList(queryWrapper);

        // 3. 从seat_status表获取该场次的座位状态（优先使用）
        List<SeatStatus> seatStatuses = seatStatusMapper.selectByShowtimeId(showtimeId);

        // 将座位状态转换为Map，便于快速查找
        java.util.Map<Long, SeatStatus> statusMap = seatStatuses.stream()
                .collect(Collectors.toMap(SeatStatus::getSeatId, status -> status));

        // 4. 同时获取订单信息作为备用（兼容旧数据）
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Orders::getShowtimeId, showtimeId);
        orderWrapper.in(Orders::getOrderStatus, 0, 1); // 0-待支付，1-已支付
        List<Orders> orders = orderMapper.selectList(orderWrapper);

        java.util.Set<Long> occupiedSeatIds = new java.util.HashSet<>();
        for (Orders order : orders) {
            String seatsStr = order.getSeats();
            if (seatsStr != null && !seatsStr.isEmpty()) {
                try {
                    seatsStr = seatsStr.substring(1, seatsStr.length() - 1);
                    String[] seatIdStrs = seatsStr.split(",");
                    for (String seatIdStr : seatIdStrs) {
                        if (!seatIdStr.isEmpty()) {
                            Long seatId = Long.parseLong(seatIdStr.trim());
                            occupiedSeatIds.add(seatId);
                        }
                    }
                } catch (Exception e) {
                    log.error("解析订单座位信息失败: orderId={}, seats={}", order.getId(), seatsStr, e);
                }
            }
        }

        // 5. 合并座位状态信息
        List<SeatVO> seatVOs = new ArrayList<>();
        Date now = new Date();

        for (Seats seat : seats) {
            SeatVO seatVO = toSeatVO(seat);
            Long seatId = seat.getId();

            // 计算该场次的实际价格 (场次特定价格 > 影厅默认价格)
            java.math.BigDecimal actualPrice = java.math.BigDecimal.ZERO;
            
            // 1. 尝试获取场次中设定的特定座位类型价格
            java.math.BigDecimal showtimeTypePrice = null;
            switch (seat.getSeatType()) {
                case 1: showtimeTypePrice = showtime.getPriceNormal(); break;
                case 2: showtimeTypePrice = showtime.getPriceGolden(); break;
                case 3: showtimeTypePrice = showtime.getPriceVip(); break;
                case 4: showtimeTypePrice = showtime.getPriceOther(); break;
            }
            
            if (showtimeTypePrice != null && showtimeTypePrice.compareTo(java.math.BigDecimal.ZERO) > 0) {
                actualPrice = showtimeTypePrice;
            } else if (showtime.getPrice() != null && showtime.getPrice().compareTo(java.math.BigDecimal.ZERO) > 0) {
                // 2. 如果没有特定类型价格，尝试使用场次统一价
                actualPrice = showtime.getPrice();
            } else {
                // 3. 如果场次未设置价格，则使用影厅设定的默认价格
                org.tonyqwe.cinemaweb.domain.entity.Halls hall = hallMapper.selectById(hallId);
                if (hall != null) {
                    switch (seat.getSeatType()) {
                        case 1: actualPrice = hall.getPriceNormal(); break;
                        case 2: actualPrice = hall.getPriceGolden(); break;
                        case 3: actualPrice = hall.getPriceVip(); break;
                        case 4: actualPrice = hall.getPriceOther(); break;
                    }
                }
                // 4. 最后兜底：使用座位自身的价格
                if ((actualPrice == null || actualPrice.compareTo(java.math.BigDecimal.ZERO) == 0) && seat.getPrice() != null) {
                    actualPrice = seat.getPrice();
                }
            }
            
            seatVO.setPrice(actualPrice != null ? actualPrice : java.math.BigDecimal.ZERO);

            // 优先从seat_status表获取状态
            SeatStatus seatStatus = statusMap.get(seatId);
            if (seatStatus != null) {
                // 检查锁定是否已过期
                if (seatStatus.getStatus() == 1 && seatStatus.getLockExpireTime() != null) {
                    if (seatStatus.getLockExpireTime().before(now)) {
                        // 锁定已过期，视为可选
                        seatVO.setStatus(1);
                    } else {
                        // 锁定有效
                        seatVO.setStatus(3); // 3-已锁定
                    }
                } else if (seatStatus.getStatus() == 2) {
                    // 已售出
                    seatVO.setStatus(2); // 2-已售
                }
            } else if (occupiedSeatIds.contains(seatId)) {
                // 兼容旧数据：从订单信息判断
                seatVO.setStatus(3); // 3-已锁定
            }

            seatVOs.add(seatVO);
        }

        return seatVOs;
    }

    /**
     * 清理过期的座位锁定
     * 可由定时任务调用
     */
    public void cleanExpiredSeatLocks() {
        Date now = new Date();
        List<SeatStatus> expiredLocks = seatStatusMapper.selectExpiredLocks(now);

        for (SeatStatus seatStatus : expiredLocks) {
            // 检查对应的订单是否已超时
            LambdaQueryWrapper<Orders> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Orders::getShowtimeId, seatStatus.getShowtimeId());
            wrapper.eq(Orders::getOrderStatus, 0); // 待支付
            List<Orders> orders = orderMapper.selectList(wrapper);

            boolean shouldRelease = true;
            for (Orders order : orders) {
                String seatsStr = order.getSeats();
                if (seatsStr != null && seatsStr.contains(String.valueOf(seatStatus.getSeatId()))) {
                    // 该座位有对应的待支付订单，不释放
                    shouldRelease = false;
                    break;
                }
            }

            if (shouldRelease) {
                seatStatusMapper.deleteById(seatStatus.getId());
                log.info("清理过期座位锁定: showtimeId={}, seatId={}",
                        seatStatus.getShowtimeId(), seatStatus.getSeatId());
            }
        }
    }

    @Override
    public boolean saveSeat(SeatDTO seatDTO) {
        Seats seat = new Seats();
        seat.setHallId(seatDTO.getHallId());
        seat.setRowNumber(seatDTO.getRowNumber());
        seat.setColumnNumber(seatDTO.getColumnNumber());
        seat.setSeatNumber(seatDTO.getSeatNumber());
        seat.setSeatType(seatDTO.getSeatType());
        seat.setStatus(seatDTO.getStatus());
        seat.setPrice(seatDTO.getPrice());
        return seatMapper.insert(seat) > 0;
    }

    @Override
    @Transactional
    public boolean batchSaveSeats(List<SeatDTO> seatDTOs) {
        if (seatDTOs == null || seatDTOs.isEmpty()) {
            return false;
        }
        for (SeatDTO dto : seatDTOs) {
            Seats seat = new Seats();
            seat.setHallId(dto.getHallId());
            seat.setRowNumber(dto.getRowNumber());
            seat.setColumnNumber(dto.getColumnNumber());
            seat.setSeatNumber(dto.getSeatNumber());
            seat.setSeatType(dto.getSeatType());
            seat.setStatus(dto.getStatus());
            seat.setPrice(dto.getPrice());
            seatMapper.insert(seat);
        }
        return true;
    }

    @Override
    public boolean updateSeat(Long id, SeatDTO seatDTO) {
        Seats seat = seatMapper.selectById(id);
        if (seat == null) {
            return false;
        }
        seat.setRowNumber(seatDTO.getRowNumber());
        seat.setColumnNumber(seatDTO.getColumnNumber());
        seat.setSeatNumber(seatDTO.getSeatNumber());
        seat.setSeatType(seatDTO.getSeatType());
        seat.setStatus(seatDTO.getStatus());
        seat.setPrice(seatDTO.getPrice());
        return seatMapper.updateById(seat) > 0;
    }

    @Override
    public boolean deleteSeat(Long id) {
        return seatMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean deleteSeatsByHallId(Long hallId) {
        QueryWrapper<Seats> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("hall_id", hallId);
        return seatMapper.delete(queryWrapper) > 0;
    }

    @Override
    @Transactional
    public boolean generateSeats(Long hallId, int rows, int columns) {
        // 先删除该影厅的所有座位
        deleteSeatsByHallId(hallId);

        // 获取影厅信息，用于设置默认价格
        org.tonyqwe.cinemaweb.domain.entity.Halls hall = hallMapper.selectById(hallId);
        java.math.BigDecimal defaultPrice = java.math.BigDecimal.ZERO;
        if (hall != null && hall.getPriceNormal() != null) {
            defaultPrice = hall.getPriceNormal();
        }

        // 生成新的座位
        List<SeatDTO> seatDTOs = new ArrayList<>();
        for (int row = 1; row <= rows; row++) {
            char rowChar = (char) ('A' + row - 1); // 将行号转换为字母，如1->A, 2->B
            for (int col = 1; col <= columns; col++) {
                SeatDTO seatDTO = new SeatDTO();
                seatDTO.setHallId(hallId);
                seatDTO.setRowNumber(row);
                seatDTO.setColumnNumber(col);
                seatDTO.setSeatNumber(rowChar + String.valueOf(col));
                seatDTO.setSeatType(1); // 默认普通座
                seatDTO.setStatus(1); // 默认可选
                seatDTO.setPrice(defaultPrice); // 默认使用影厅普通座价格
                seatDTOs.add(seatDTO);
            }
        }

        // 批量保存座位
        boolean success = batchSaveSeats(seatDTOs);

        // 更新影厅的座位数
        if (success && hall != null) {
            hall.setCapacity(rows * columns);
            hallMapper.updateById(hall);
        }

        return success;
    }

    @Override
    @Transactional
    public boolean batchUpdateSeats(List<Long> ids, SeatDTO seatDTO) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            Seats seat = seatMapper.selectById(id);
            if (seat != null) {
                if (seatDTO.getSeatType() != null) seat.setSeatType(seatDTO.getSeatType());
                if (seatDTO.getStatus() != null) seat.setStatus(seatDTO.getStatus());
                if (seatDTO.getPrice() != null) seat.setPrice(seatDTO.getPrice());
                seatMapper.updateById(seat);
            }
        }
        return true;
    }

    @Override
    public Seats getSeatById(Long id) {
        return seatMapper.selectById(id);
    }

    private SeatVO toSeatVO(Seats seat) {
        SeatVO vo = new SeatVO();
        vo.setId(seat.getId());
        vo.setHallId(seat.getHallId());
        vo.setRowNumber(seat.getRowNumber());
        vo.setColumnNumber(seat.getColumnNumber());
        vo.setSeatNumber(seat.getSeatNumber());
        vo.setSeatType(seat.getSeatType());
        vo.setStatus(seat.getStatus());
        vo.setPrice(seat.getPrice());
        vo.setCreatedAt(seat.getCreatedAt());
        vo.setUpdatedAt(seat.getUpdatedAt());
        return vo;
    }

    @Override
    @Transactional
    public boolean lockSeats(Long showtimeId, List<Long> seatIds) {
        if (showtimeId == null || seatIds == null || seatIds.isEmpty()) {
            return false;
        }

        Date now = new Date();
        Date lockExpireTime = new Date(now.getTime() + 15 * 60 * 1000); // 15分钟锁定时间

        // 获取订单信息作为备用（兼容旧数据）
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Orders::getShowtimeId, showtimeId);
        orderWrapper.in(Orders::getOrderStatus, 0, 1); // 0-待支付，1-已支付
        List<Orders> orders = orderMapper.selectList(orderWrapper);

        java.util.Set<Long> occupiedSeatIds = new java.util.HashSet<>();
        for (Orders order : orders) {
            String seatsStr = order.getSeats();
            if (seatsStr != null && !seatsStr.isEmpty()) {
                try {
                    seatsStr = seatsStr.substring(1, seatsStr.length() - 1);
                    String[] seatIdStrs = seatsStr.split(",");
                    for (String seatIdStr : seatIdStrs) {
                        if (!seatIdStr.isEmpty()) {
                            Long seatId = Long.parseLong(seatIdStr.trim());
                            occupiedSeatIds.add(seatId);
                        }
                    }
                } catch (Exception e) {
                    log.error("解析订单座位信息失败: orderId={}, seats={}", order.getId(), seatsStr, e);
                }
            }
        }

        for (Long seatId : seatIds) {
            // 检查座位是否已被锁定或售出
            LambdaQueryWrapper<SeatStatus> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SeatStatus::getShowtimeId, showtimeId);
            wrapper.eq(SeatStatus::getSeatId, seatId);
            wrapper.in(SeatStatus::getStatus, 1, 2); // 1-锁定, 2-售出
            List<SeatStatus> existingStatuses = seatStatusMapper.selectList(wrapper);

            boolean isAvailable = true;
            if (!existingStatuses.isEmpty()) {
                // 检查锁定是否已过期
                SeatStatus existingStatus = existingStatuses.get(0);
                if (existingStatus.getStatus() == 1 && existingStatus.getLockExpireTime() != null) {
                    if (existingStatus.getLockExpireTime().after(now)) {
                        // 锁定未过期，不能重复锁定
                        isAvailable = false;
                    }
                } else if (existingStatus.getStatus() == 2) {
                    // 已售出，不能锁定
                    isAvailable = false;
                }
            } else if (occupiedSeatIds.contains(seatId)) {
                // 兼容旧数据：从订单信息判断
                isAvailable = false;
            }

            if (!isAvailable) {
                return false;
            }

            // 创建或更新座位锁定状态
            SeatStatus seatStatus = new SeatStatus();
            seatStatus.setShowtimeId(showtimeId);
            seatStatus.setSeatId(seatId);
            seatStatus.setStatus(1); // 1-锁定
            seatStatus.setLockExpireTime(lockExpireTime);
            seatStatus.setCreatedAt(now);
            seatStatus.setUpdatedAt(now);

            // 尝试插入，如果已存在则更新
            try {
                if (existingStatuses.isEmpty()) {
                    seatStatusMapper.insert(seatStatus);
                } else {
                    seatStatus.setId(existingStatuses.get(0).getId());
                    seatStatusMapper.updateById(seatStatus);
                }
            } catch (Exception e) {
                log.error("锁定座位失败: showtimeId={}, seatId={}", showtimeId, seatId, e);
                return false;
            }
        }

        log.info("成功锁定座位: showtimeId={}, seatIds={}", showtimeId, seatIds);
        return true;
    }

    @Override
    @Transactional
    public boolean unlockSeats(Long showtimeId, List<Long> seatIds) {
        if (showtimeId == null || seatIds == null || seatIds.isEmpty()) {
            return false;
        }

        for (Long seatId : seatIds) {
            LambdaQueryWrapper<SeatStatus> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SeatStatus::getShowtimeId, showtimeId);
            wrapper.eq(SeatStatus::getSeatId, seatId);
            wrapper.eq(SeatStatus::getStatus, 1); // 只解锁锁定状态的座位

            int deleted = seatStatusMapper.delete(wrapper);
            if (deleted == 0) {
                log.warn("解锁座位失败: showtimeId={}, seatId={}", showtimeId, seatId);
            }
        }

        log.info("成功解锁座位: showtimeId={}, seatIds={}", showtimeId, seatIds);
        return true;
    }

    @Override
    public java.util.Map<String, Object> checkSeatsLockable(Long showtimeId, List<Long> seatIds) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        java.util.List<Long> lockableSeats = new java.util.ArrayList<>();
        java.util.List<Long> unavailableSeats = new java.util.ArrayList<>();

        if (showtimeId == null || seatIds == null || seatIds.isEmpty()) {
            result.put("lockable", false);
            result.put("lockableSeats", lockableSeats);
            result.put("unavailableSeats", unavailableSeats);
            return result;
        }

        Date now = new Date();

        // 获取订单信息作为备用（兼容旧数据）
        LambdaQueryWrapper<Orders> orderWrapper = new LambdaQueryWrapper<>();
        orderWrapper.eq(Orders::getShowtimeId, showtimeId);
        orderWrapper.in(Orders::getOrderStatus, 0, 1); // 0-待支付，1-已支付
        List<Orders> orders = orderMapper.selectList(orderWrapper);

        java.util.Set<Long> occupiedSeatIds = new java.util.HashSet<>();
        for (Orders order : orders) {
            String seatsStr = order.getSeats();
            if (seatsStr != null && !seatsStr.isEmpty()) {
                try {
                    seatsStr = seatsStr.substring(1, seatsStr.length() - 1);
                    String[] seatIdStrs = seatsStr.split(",");
                    for (String seatIdStr : seatIdStrs) {
                        if (!seatIdStr.isEmpty()) {
                            Long seatId = Long.parseLong(seatIdStr.trim());
                            occupiedSeatIds.add(seatId);
                        }
                    }
                } catch (Exception e) {
                    log.error("解析订单座位信息失败: orderId={}, seats={}", order.getId(), seatsStr, e);
                }
            }
        }

        for (Long seatId : seatIds) {
            LambdaQueryWrapper<SeatStatus> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SeatStatus::getShowtimeId, showtimeId);
            wrapper.eq(SeatStatus::getSeatId, seatId);
            wrapper.in(SeatStatus::getStatus, 1, 2); // 1-锁定, 2-售出
            List<SeatStatus> existingStatuses = seatStatusMapper.selectList(wrapper);

            boolean isAvailable = true;
            if (!existingStatuses.isEmpty()) {
                SeatStatus existingStatus = existingStatuses.get(0);
                if (existingStatus.getStatus() == 1 && existingStatus.getLockExpireTime() != null) {
                    if (existingStatus.getLockExpireTime().after(now)) {
                        // 锁定未过期
                        isAvailable = false;
                    }
                } else if (existingStatus.getStatus() == 2) {
                    // 已售出
                    isAvailable = false;
                }
            } else if (occupiedSeatIds.contains(seatId)) {
                // 兼容旧数据：从订单信息判断
                isAvailable = false;
            }

            if (isAvailable) {
                lockableSeats.add(seatId);
            } else {
                unavailableSeats.add(seatId);
            }
        }

        result.put("lockable", unavailableSeats.isEmpty());
        result.put("lockableSeats", lockableSeats);
        result.put("unavailableSeats", unavailableSeats);
        return result;
    }
}
