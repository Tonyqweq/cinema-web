package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.dto.ShowtimesDTO;
import org.tonyqwe.cinemaweb.domain.entity.Showtimes;
import org.tonyqwe.cinemaweb.mapper.ShowtimesMapper;
import org.tonyqwe.cinemaweb.service.ShowtimesService;

import java.util.Date;
import java.util.List;

/**
 * 排片服务实现
 */
@Service
public class ShowtimesServiceImpl extends ServiceImpl<ShowtimesMapper, Showtimes> implements ShowtimesService {

    @Resource
    private ShowtimesMapper showtimesMapper;

    @Override
    public com.baomidou.mybatisplus.core.metadata.IPage<Showtimes> pageShowtimes(int page, int pageSize, Long cinemaId, Long hallId, Long movieId) {
        LambdaQueryWrapper<Showtimes> queryWrapper = new LambdaQueryWrapper<>();

        if (cinemaId != null) {
            queryWrapper.eq(Showtimes::getCinemaId, cinemaId);
        }

        if (hallId != null) {
            queryWrapper.eq(Showtimes::getHallId, hallId);
        }

        if (movieId != null) {
            queryWrapper.eq(Showtimes::getMovieId, movieId);
        }

        // 按开始时间降序排序
        queryWrapper.orderByDesc(Showtimes::getStartTime);

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Showtimes> showtimesPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, pageSize);
        return showtimesMapper.selectPage(showtimesPage, queryWrapper);
    }

    @Override
    public Showtimes getById(Long id) {
        return showtimesMapper.selectById(id);
    }

    @Override
    public boolean saveShowtimes(ShowtimesDTO showtimesDTO) {
        // 检查时间冲突
        if (checkTimeConflict(showtimesDTO.getHallId(), showtimesDTO.getStartTime(), showtimesDTO.getEndTime(), showtimesDTO.getId())) {
            return false;
        }

        Showtimes showtimes = new Showtimes();
        if (showtimesDTO.getId() != null) {
            // 更新操作
            showtimes.setId(showtimesDTO.getId());
        }

        showtimes.setCinemaId(showtimesDTO.getCinemaId());
        showtimes.setHallId(showtimesDTO.getHallId());
        showtimes.setMovieId(showtimesDTO.getMovieId());
        showtimes.setStartTime(showtimesDTO.getStartTime());
        showtimes.setEndTime(showtimesDTO.getEndTime());
        showtimes.setPrice(showtimesDTO.getPrice());
        showtimes.setPriceNormal(showtimesDTO.getPriceNormal());
        showtimes.setPriceGolden(showtimesDTO.getPriceGolden());
        showtimes.setPriceVip(showtimesDTO.getPriceVip());
        showtimes.setPriceOther(showtimesDTO.getPriceOther());
        showtimes.setStatus(showtimesDTO.getStatus() != null ? showtimesDTO.getStatus() : 1);

        if (showtimesDTO.getId() != null) {
            return updateById(showtimes);
        } else {
            return save(showtimes);
        }
    }

    @Override
    public boolean deleteShowtimes(Long id) {
        return removeById(id);
    }

    @Override
    public boolean checkTimeConflict(Long hallId, Date startTime, Date endTime, Long excludeId) {
        LambdaQueryWrapper<Showtimes> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Showtimes::getHallId, hallId)
                .eq(Showtimes::getStatus, 1)
                .and(wrapper -> wrapper
                        .lt(Showtimes::getStartTime, endTime)
                        .gt(Showtimes::getEndTime, startTime)
                );
        
        if (excludeId != null) {
            queryWrapper.ne(Showtimes::getId, excludeId);
        }
        
        return count(queryWrapper) > 0;
    }

    @Override
    public List<Showtimes> getByCinemaId(Long cinemaId) {
        LambdaQueryWrapper<Showtimes> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Showtimes::getCinemaId, cinemaId)
                .orderByAsc(Showtimes::getStartTime);
        return list(queryWrapper);
    }

    @Override
    public List<Showtimes> getByHallId(Long hallId) {
        LambdaQueryWrapper<Showtimes> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Showtimes::getHallId, hallId)
                .orderByAsc(Showtimes::getStartTime);
        return list(queryWrapper);
    }

    @Override
    public List<Showtimes> getByMovieId(Long movieId) {
        LambdaQueryWrapper<Showtimes> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Showtimes::getMovieId, movieId)
                .orderByAsc(Showtimes::getStartTime);
        return list(queryWrapper);
    }
}
