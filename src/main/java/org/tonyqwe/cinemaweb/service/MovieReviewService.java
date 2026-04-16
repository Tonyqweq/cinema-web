package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.tonyqwe.cinemaweb.domain.entity.MovieReview;

import java.util.Map;

public interface MovieReviewService {
    
    IPage<MovieReview> getReviewsByMovieId(Long movieId, long page, long pageSize);
    
    MovieReview getReviewByUserAndMovie(Long userId, Long movieId);
    
    boolean saveOrUpdateReview(MovieReview review);
    
    boolean deleteReview(Long reviewId, Long userId);
    
    Map<String, Object> getReviewStatsByMovieId(Long movieId);
}
