package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.entity.MovieReview;

public interface MovieReviewService extends IService<MovieReview> {

    IPage<MovieReview> getReviewsByMovieId(Long movieId, long page, long pageSize);

    MovieReview createOrUpdateReview(Long movieId, Integer userId, Integer rating, String comment);

    MovieReview getUserReview(Long movieId, Integer userId);

    boolean deleteReview(Long reviewId, Integer userId);

    Double getAverageRating(Long movieId);

    Integer getReviewCount(Long movieId);
}