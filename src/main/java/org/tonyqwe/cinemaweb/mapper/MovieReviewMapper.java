package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.tonyqwe.cinemaweb.domain.entity.MovieReview;

public interface MovieReviewMapper extends BaseMapper<MovieReview> {
    
    IPage<MovieReview> getReviewsByMovieId(Page<MovieReview> page, Long movieId);
    
    MovieReview getReviewByUserAndMovie(Long userId, Long movieId);
    
    Double getAverageRatingByMovieId(Long movieId);
    
    Integer getReviewCountByMovieId(Long movieId);
}
