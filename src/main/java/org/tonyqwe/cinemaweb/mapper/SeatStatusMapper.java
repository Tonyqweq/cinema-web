package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.tonyqwe.cinemaweb.domain.entity.SeatStatus;

import java.util.List;

/**
 * 座位状态Mapper接口
 */
public interface SeatStatusMapper extends BaseMapper<SeatStatus> {

    /**
     * 根据场次ID和座位ID查询座位状态
     *
     * @param showtimeId 场次ID
     * @param seatId     座位ID
     * @return 座位状态
     */
    @Select("SELECT * FROM seat_status WHERE showtime_id = #{showtimeId} AND seat_id = #{seatId}")
    SeatStatus selectByShowtimeIdAndSeatId(@Param("showtimeId") Long showtimeId, @Param("seatId") Long seatId);

    /**
     * 根据场次ID查询所有座位状态
     *
     * @param showtimeId 场次ID
     * @return 座位状态列表
     */
    @Select("SELECT * FROM seat_status WHERE showtime_id = #{showtimeId}")
    List<SeatStatus> selectByShowtimeId(@Param("showtimeId") Long showtimeId);

    /**
     * 根据场次ID和座位ID删除座位状态记录
     *
     * @param showtimeId 场次ID
     * @param seatId     座位ID
     * @return 影响行数
     */
    @Delete("DELETE FROM seat_status WHERE showtime_id = #{showtimeId} AND seat_id = #{seatId}")
    int deleteByShowtimeIdAndSeatId(@Param("showtimeId") Long showtimeId, @Param("seatId") Long seatId);

    /**
     * 批量插入座位状态
     *
     * @param seatStatuses 座位状态列表
     * @return 影响行数
     */
    int batchInsert(@Param("list") List<SeatStatus> seatStatuses);

    /**
     * 根据场次ID删除所有座位状态
     *
     * @param showtimeId 场次ID
     * @return 影响行数
     */
    @Delete("DELETE FROM seat_status WHERE showtime_id = #{showtimeId}")
    int deleteByShowtimeId(@Param("showtimeId") Long showtimeId);

    /**
     * 查询已过期的锁定座位
     *
     * @param currentTime 当前时间
     * @return 座位状态列表
     */
    @Select("SELECT * FROM seat_status WHERE status = 1 AND lock_expire_time < #{currentTime}")
    List<SeatStatus> selectExpiredLocks(@Param("currentTime") java.util.Date currentTime);
}
