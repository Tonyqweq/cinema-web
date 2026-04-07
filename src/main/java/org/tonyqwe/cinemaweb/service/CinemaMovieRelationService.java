package org.tonyqwe.cinemaweb.service;

import java.util.List;

public interface CinemaMovieRelationService {
    /**
     * 根据影院ID获取电影ID列表
     */
    List<Long> getMovieIdsByCinemaId(Long cinemaId);

    /**
     * 根据电影ID获取影院ID列表
     */
    List<Long> getCinemaIdsByMovieId(Long movieId);

    /**
     * 绑定影院与电影
     */
    void bindCinemaToMovie(Long cinemaId, Long movieId);

    /**
     * 解除影院与电影的绑定
     */
    void unbindCinemaFromMovie(Long cinemaId, Long movieId);

    /**
     * 解除影院的所有电影绑定
     */
    void unbindAllMoviesFromCinema(Long cinemaId);

    /**
     * 解除电影的所有影院绑定
     */
    void unbindAllCinemasFromMovie(Long movieId);
}
