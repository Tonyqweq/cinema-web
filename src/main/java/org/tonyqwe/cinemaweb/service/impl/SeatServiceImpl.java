package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.dto.SeatDTO;
import org.tonyqwe.cinemaweb.domain.entity.Seats;
import org.tonyqwe.cinemaweb.domain.vo.SeatVO;
import org.tonyqwe.cinemaweb.mapper.HallMapper;
import org.tonyqwe.cinemaweb.mapper.SeatMapper;
import org.tonyqwe.cinemaweb.service.SeatService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SeatServiceImpl implements SeatService {

    @Resource
    private SeatMapper seatMapper;

    @Resource
    private HallMapper hallMapper;

    @Override
    public List<SeatVO> getSeatsByHallId(Long hallId) {
        QueryWrapper<Seats> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("hall_id", hallId);
        queryWrapper.orderByAsc("row_number", "column_number");
        List<Seats> seats = seatMapper.selectList(queryWrapper);
        return seats.stream().map(this::toSeatVO).collect(Collectors.toList());
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
