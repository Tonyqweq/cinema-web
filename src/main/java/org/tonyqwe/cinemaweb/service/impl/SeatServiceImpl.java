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
                seatDTO.setPrice(null); // 默认使用影厅统一价格
                seatDTOs.add(seatDTO);
            }
        }

        // 批量保存座位
        boolean success = batchSaveSeats(seatDTOs);

        // 更新影厅的座位数
        if (success) {
            org.tonyqwe.cinemaweb.domain.entity.Halls hall = hallMapper.selectById(hallId);
            if (hall != null) {
                hall.setCapacity(rows * columns);
                hallMapper.updateById(hall);
            }
        }

        return success;
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
}
