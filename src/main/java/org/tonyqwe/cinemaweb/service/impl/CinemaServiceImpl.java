package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.Cinemas;
import org.tonyqwe.cinemaweb.mapper.CinemaMapper;
import org.tonyqwe.cinemaweb.service.CinemaService;

@Service
public class CinemaServiceImpl extends ServiceImpl<CinemaMapper, Cinemas> implements CinemaService {

    @Resource
    private CinemaMapper cinemaMapper;

    @Override
    public IPage<Cinemas> pageCinemas(long page, long pageSize, String name) {
        Page<Cinemas> mpPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Cinemas> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            qw.like(Cinemas::getName, name.trim());
        }

        return cinemaMapper.selectPage(mpPage, qw);
    }
}
