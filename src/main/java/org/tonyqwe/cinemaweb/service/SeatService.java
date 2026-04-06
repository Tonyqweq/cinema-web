package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.tonyqwe.cinemaweb.domain.dto.SeatDTO;
import org.tonyqwe.cinemaweb.domain.entity.Seats;
import org.tonyqwe.cinemaweb.domain.vo.SeatVO;

import java.util.List;

public interface SeatService {
    /**
     * 根据影厅ID获取座位列表
     */
    List<SeatVO> getSeatsByHallId(Long hallId);

    /**
     * 保存座位
     */
    boolean saveSeat(SeatDTO seatDTO);

    /**
     * 批量保存座位
     */
    boolean batchSaveSeats(List<SeatDTO> seatDTOs);

    /**
     * 更新座位
     */
    boolean updateSeat(Long id, SeatDTO seatDTO);

    /**
     * 删除座位
     */
    boolean deleteSeat(Long id);

    /**
     * 根据影厅ID删除所有座位
     */
    boolean deleteSeatsByHallId(Long hallId);

    /**
     * 生成影厅座位
     */
    boolean generateSeats(Long hallId, int rows, int columns);
}
