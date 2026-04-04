package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.dto.HallDTO;
import org.tonyqwe.cinemaweb.domain.entity.Halls;
import org.tonyqwe.cinemaweb.mapper.HallMapper;
import org.tonyqwe.cinemaweb.service.HallService;

import java.util.Date;

@Service
public class HallServiceImpl implements HallService {

    @Resource
    private HallMapper hallMapper;

    @Override
    public IPage<Halls> pageHalls(Long cinemaId, int page, int pageSize) {
        LambdaQueryWrapper<Halls> wrapper = new LambdaQueryWrapper<>();
        if (cinemaId != null) {
            wrapper.eq(Halls::getCinemaId, cinemaId);
        }
        return hallMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Halls getById(Long id) {
        return hallMapper.selectById(id);
    }

    @Override
    public boolean saveHall(HallDTO hallDTO) {
        Halls hall = new Halls();
        hall.setCinemaId(hallDTO.getCinemaId());
        hall.setName(hallDTO.getName());
        hall.setType(hallDTO.getType());
        hall.setCapacity(hallDTO.getCapacity());
        hall.setStatus(hallDTO.getStatus());
        hall.setCreatedAt(new Date());
        hall.setUpdatedAt(new Date());
        return hallMapper.insert(hall) > 0;
    }

    @Override
    public boolean updateHall(Long id, HallDTO hallDTO) {
        Halls hall = hallMapper.selectById(id);
        if (hall == null) {
            return false;
        }
        hall.setCinemaId(hallDTO.getCinemaId());
        hall.setName(hallDTO.getName());
        hall.setType(hallDTO.getType());
        hall.setCapacity(hallDTO.getCapacity());
        hall.setStatus(hallDTO.getStatus());
        hall.setUpdatedAt(new Date());
        return hallMapper.updateById(hall) > 0;
    }

    @Override
    public boolean deleteHall(Long id) {
        return hallMapper.deleteById(id) > 0;
    }
}
