package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.entity.Cinemas;

public interface CinemaService extends IService<Cinemas> {
    IPage<Cinemas> pageCinemas(long page, long pageSize, String name);
}
