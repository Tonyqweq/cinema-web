package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.Cinemas;
import org.tonyqwe.cinemaweb.mapper.CinemaMapper;
import org.tonyqwe.cinemaweb.service.CinemaService;

@Slf4j
@Service
public class CinemaServiceImpl extends ServiceImpl<CinemaMapper, Cinemas> implements CinemaService {

    @Resource
    private CinemaMapper cinemaMapper;

    @Override
    public IPage<Cinemas> pageCinemas(long page, long pageSize, String name, String province, String city, String district) {
        Page<Cinemas> mpPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Cinemas> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) {
            qw.like(Cinemas::getName, name.trim());
        }
        if (province != null && !province.isBlank()) {
            qw.eq(Cinemas::getProvince, province.trim());
        }
        if (city != null && !city.isBlank()) {
            qw.eq(Cinemas::getCity, city.trim());
        }
        if (district != null && !district.isBlank()) {
            qw.eq(Cinemas::getDistrict, district.trim());
        }

        return cinemaMapper.selectPage(mpPage, qw);
    }

    @Override
    public Cinemas getCinemaById(Long id) {
        return cinemaMapper.selectById(id);
    }

    @Override
    public boolean saveCinema(Cinemas cinema) {
        if (cinema.getId() == null) {
            return save(cinema);
        } else {
            return updateById(cinema);
        }
    }

    @Override
    public boolean deleteCinema(Long id) {
        return removeById(id);
    }
}
