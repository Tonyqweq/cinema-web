package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.tonyqwe.cinemaweb.domain.dto.ShowtimesDTO;
import org.tonyqwe.cinemaweb.domain.entity.Showtimes;

import java.util.List;

/**
 * 排片服务
 */
public interface ShowtimesService {

    /**
     * 分页查询排片列表
     * @param page 页码
     * @param pageSize 每页大小
     * @param cinemaId 影院ID（可选）
     * @param hallId 影厅ID（可选）
     * @param movieId 电影ID（可选）
     * @return 排片列表
     */
    IPage<Showtimes> pageShowtimes(int page, int pageSize, Long cinemaId, Long hallId, Long movieId);

    /**
     * 根据ID查询排片详情
     * @param id 排片ID
     * @return 排片详情
     */
    Showtimes getById(Long id);

    /**
     * 保存排片
     * @param showtimesDTO 排片DTO
     * @return 是否保存成功
     */
    boolean saveShowtimes(ShowtimesDTO showtimesDTO);

    /**
     * 删除排片
     * @param id 排片ID
     * @return 是否删除成功
     */
    boolean deleteShowtimes(Long id);

    /**
     * 检查排片时间冲突
     * @param hallId 影厅ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeId 排除的排片ID（用于更新操作）
     * @return 是否有冲突
     */
    boolean checkTimeConflict(Long hallId, java.util.Date startTime, java.util.Date endTime, Long excludeId);

    /**
     * 根据影院ID查询排片列表
     * @param cinemaId 影院ID
     * @return 排片列表
     */
    List<Showtimes> getByCinemaId(Long cinemaId);

    /**
     * 根据影厅ID查询排片列表
     * @param hallId 影厅ID
     * @return 排片列表
     */
    List<Showtimes> getByHallId(Long hallId);

    /**
     * 根据电影ID查询排片列表
     * @param movieId 电影ID
     * @return 排片列表
     */
    List<Showtimes> getByMovieId(Long movieId);
}
