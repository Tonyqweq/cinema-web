package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    public IPage<Movie> pageMovies(long page, long pageSize, String title, String language, String country, String sortBy, String sortOrder) {
        Page<Movie> mpPage = new Page<>(page, pageSize);

        LambdaQueryWrapper<Movie> qw = new LambdaQueryWrapper<>();
        if (title != null && !title.isBlank()) {
            qw.like(Movie::getTitle, title.trim());
        }
        if (language != null && !language.isBlank()) {
            qw.eq(Movie::getLanguage, language.trim());
        }
        if (country != null && !country.isBlank()) {
            qw.eq(Movie::getCountry, country.trim());
        }

        if (sortBy != null && !sortBy.isBlank()) {
            boolean asc = "asc".equalsIgnoreCase(sortOrder);

            if ("duration_min".equalsIgnoreCase(sortBy) || "durationMin".equalsIgnoreCase(sortBy)) {
                if (asc) qw.orderByAsc(Movie::getDurationMin);
                else qw.orderByDesc(Movie::getDurationMin);
            } else if ("release_date".equalsIgnoreCase(sortBy) || "releaseDate".equalsIgnoreCase(sortBy)) {
                if (asc) qw.orderByAsc(Movie::getReleaseDate);
                else qw.orderByDesc(Movie::getReleaseDate);
            }
        }

        return movieMapper.selectPage(mpPage, qw);
    }

    @Override
    public java.util.List<String> listLanguages() {
        return movieMapper.selectDistinctLanguages();
    }

    @Override
    public java.util.List<String> listCountries() {
        return movieMapper.selectDistinctCountries();
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

