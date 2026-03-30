package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.dto.MovieBodyRequest;
import org.tonyqwe.cinemaweb.domain.dto.MovieImportResult;
import org.tonyqwe.cinemaweb.domain.entity.Movies;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface MovieService extends IService<Movies> {
    IPage<Movies> pageMovies(long page, long pageSize, String title, String language, String country, String sortBy, String sortOrder);

    java.util.List<String> listLanguages();

    java.util.List<String> listCountries();

    Movies getMovieById(Long id);

    Movies updateMovieStatus(Long id, Integer status);

    Movies createMovie(MovieBodyRequest request);

    Movies updateMovieInfo(Long id, MovieBodyRequest request);

    MovieImportResult importMoviesFromExcel(MultipartFile file) throws IOException;

    boolean deleteMovie(Long id);
}

