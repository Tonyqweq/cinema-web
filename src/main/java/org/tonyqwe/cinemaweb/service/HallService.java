package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.tonyqwe.cinemaweb.domain.dto.HallDTO;
import org.tonyqwe.cinemaweb.domain.entity.Halls;

public interface HallService {
    IPage<Halls> pageHalls(Long cinemaId, int page, int pageSize);
    Halls getById(Long id);
    boolean saveHall(HallDTO hallDTO);
    boolean updateHall(Long id, HallDTO hallDTO);
    boolean deleteHall(Long id);
}
