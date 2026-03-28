package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.dto.MovieBodyRequest;
import org.tonyqwe.cinemaweb.domain.dto.MovieImportResult;
import org.tonyqwe.cinemaweb.domain.entity.Movie;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface MovieService extends IService<Movie> {
    IPage<Movie> pageMovies(long page, long pageSize, String title, String language, String country, String sortBy, String sortOrder);

    java.util.List<String> listLanguages();

    java.util.List<String> listCountries();

    Movie getMovieById(Long id);

    Movie updateMovieStatus(Long id, Integer status);

    Movie createMovie(MovieBodyRequest request);

    Movie updateMovieInfo(Long id, MovieBodyRequest request);

    MovieImportResult importMoviesFromExcel(MultipartFile file) throws IOException;

    boolean deleteMovie(Long id);
}

