package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.entity.Movie;

public interface MovieService extends IService<Movie> {
    IPage<Movie> pageMovies(long page, long pageSize, String title, String language, String country, String sortBy, String sortOrder);

    java.util.List<String> listLanguages();

    java.util.List<String> listCountries();

    Movie getMovieById(Long id);

    Movie updateMovieStatus(Long id, Integer status);
}

