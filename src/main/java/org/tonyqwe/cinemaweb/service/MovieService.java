package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.entity.Movie;

public interface MovieService extends IService<Movie> {
    IPage<Movie> pageMovies(long page, long pageSize);

    Movie getMovieById(Long id);

    Movie updateMovieStatus(Long id, Integer status);
}

