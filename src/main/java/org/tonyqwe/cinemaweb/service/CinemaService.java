package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.entity.Cinemas;

public interface CinemaService extends IService<Cinemas> {
    IPage<Cinemas> pageCinemas(long page, long pageSize, String name, String province, String city, String district);
    Cinemas getCinemaById(Long id);
    boolean saveCinema(Cinemas cinema);
    boolean deleteCinema(Long id);
}
