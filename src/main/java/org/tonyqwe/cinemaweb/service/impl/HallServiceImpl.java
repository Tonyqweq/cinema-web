package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.dto.HallDTO;
import org.tonyqwe.cinemaweb.domain.entity.Halls;
import org.tonyqwe.cinemaweb.domain.entity.Seats;
import org.tonyqwe.cinemaweb.mapper.HallMapper;
import org.tonyqwe.cinemaweb.mapper.SeatMapper;
import org.tonyqwe.cinemaweb.service.HallService;

import java.util.Date;

@Service
public class HallServiceImpl implements HallService {

    @Resource
    private HallMapper hallMapper;

    @Resource
    private SeatMapper seatMapper;

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
        hall.setPriceNormal(hallDTO.getPriceNormal());
        hall.setPriceGolden(hallDTO.getPriceGolden());
        hall.setPriceVip(hallDTO.getPriceVip());
        hall.setPriceOther(hallDTO.getPriceOther());
        hall.setCreatedAt(new Date());
        hall.setUpdatedAt(new Date());
        return hallMapper.insert(hall) > 0;
    }

    @Override
    @Transactional
    public boolean updateHall(Long id, HallDTO hallDTO) {
        Halls hall = hallMapper.selectById(id);
        if (hall == null) {
            return false;
        }

        // 检查价格是否发生变化
        boolean priceNormalChanged = !isEqual(hall.getPriceNormal(), hallDTO.getPriceNormal());
        boolean priceGoldenChanged = !isEqual(hall.getPriceGolden(), hallDTO.getPriceGolden());
        boolean priceVipChanged = !isEqual(hall.getPriceVip(), hallDTO.getPriceVip());
        boolean priceOtherChanged = !isEqual(hall.getPriceOther(), hallDTO.getPriceOther());

        hall.setCinemaId(hallDTO.getCinemaId());
        hall.setName(hallDTO.getName());
        hall.setType(hallDTO.getType());
        hall.setCapacity(hallDTO.getCapacity());
        hall.setStatus(hallDTO.getStatus());
        hall.setPriceNormal(hallDTO.getPriceNormal());
        hall.setPriceGolden(hallDTO.getPriceGolden());
        hall.setPriceVip(hallDTO.getPriceVip());
        hall.setPriceOther(hallDTO.getPriceOther());
        hall.setUpdatedAt(new Date());
        
        boolean success = hallMapper.updateById(hall) > 0;
        
        if (success) {
            // 同步更新座位价格
            if (priceNormalChanged) updateSeatPrices(id, 1, hallDTO.getPriceNormal());
            if (priceGoldenChanged) updateSeatPrices(id, 2, hallDTO.getPriceGolden());
            if (priceVipChanged) updateSeatPrices(id, 3, hallDTO.getPriceVip());
            if (priceOtherChanged) updateSeatPrices(id, 4, hallDTO.getPriceOther());
        }
        
        return success;
    }

    private boolean isEqual(java.math.BigDecimal d1, java.math.BigDecimal d2) {
        if (d1 == null && d2 == null) return true;
        if (d1 == null || d2 == null) return false;
        return d1.compareTo(d2) == 0;
    }

    private void updateSeatPrices(Long hallId, Integer seatType, java.math.BigDecimal newPrice) {
        com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<Seats> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
        updateWrapper.eq("hall_id", hallId).eq("seat_type", seatType).set("price", newPrice);
        seatMapper.update(null, updateWrapper);
    }

    @Override
    public boolean deleteHall(Long id) {
        return hallMapper.deleteById(id) > 0;
    }
}
