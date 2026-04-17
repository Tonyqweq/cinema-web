package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.tonyqwe.cinemaweb.domain.entity.MovieReview;
import java.util.List;
import java.util.Map;

public interface MovieReviewMapper extends BaseMapper<MovieReview> {
    
    IPage<MovieReview> getReviewsByMovieId(Page<MovieReview> page, Long movieId);
    
    MovieReview getReviewByUserAndMovie(Long userId, Long movieId);
    
    Double getAverageRatingByMovieId(Long movieId);
    
    Integer getReviewCountByMovieId(Long movieId);
    
    /**
     * 获取评分分布
     * @param movieId 电影ID
     * @return 评分分布列表，每个元素包含 rating 和 count
     */
    List<Map<String, Object>> getRatingDistributionByMovieId(Long movieId);
}
