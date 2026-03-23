package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.Movie;
import org.tonyqwe.cinemaweb.mapper.MovieMapper;
import org.tonyqwe.cinemaweb.service.MovieService;

@Service
public class MovieServiceImpl extends ServiceImpl<MovieMapper, Movie> implements MovieService {

    @Resource
    private MovieMapper movieMapper;

    @Override
    public IPage<Movie> pageMovies(long page, long pageSize) {
        // 按上映日期从新到旧排序（便于列表浏览）
        Page<Movie> mpPage = new Page<>(page, pageSize);
        return movieMapper.selectPage(mpPage, null);
    }

    @Override
    public Movie getMovieById(Long id) {
        if (id == null) return null;
        return movieMapper.selectById(id);
    }

    @Override
    public Movie updateMovieStatus(Long id, Integer status) {
        if (id == null || status == null) return null;
        Movie movie = movieMapper.selectById(id);
        if (movie == null) return null;
        movie.setStatus(status);
        movieMapper.updateById(movie);
        return movie;
    }
}

