package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.MovieReview;
import org.tonyqwe.cinemaweb.mapper.MovieReviewMapper;
import org.tonyqwe.cinemaweb.service.MovieReviewService;
import org.tonyqwe.cinemaweb.service.MovieService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MovieReviewServiceImpl implements MovieReviewService {
    
    @Resource
    private MovieReviewMapper movieReviewMapper;
    
    @Resource
    private MovieService movieService;
    
    @Override
    public IPage<MovieReview> getReviewsByMovieId(Long movieId, long page, long pageSize) {
        Page<MovieReview> pagination = new Page<>(page, pageSize);
        return movieReviewMapper.getReviewsByMovieId(pagination, movieId);
    }
    
    @Override
    public MovieReview getReviewByUserAndMovie(Long userId, Long movieId) {
        return movieReviewMapper.getReviewByUserAndMovie(userId, movieId);
    }
    
    @Override
    public boolean saveOrUpdateReview(MovieReview review) {
        MovieReview existingReview = movieReviewMapper.getReviewByUserAndMovie(review.getUserId(), review.getMovieId());
        if (existingReview != null) {
            // 更新现有评论
            existingReview.setRating(review.getRating());
            existingReview.setComment(review.getComment());
            existingReview.setUpdatedAt(LocalDateTime.now());
            boolean result = movieReviewMapper.updateById(existingReview) > 0;
            // 更新电影评分
            if (result) {
                movieService.updateMovieRating(review.getMovieId());
            }
            return result;
        } else {
            // 创建新评论
            review.setCreatedAt(LocalDateTime.now());
            review.setUpdatedAt(LocalDateTime.now());
            boolean result = movieReviewMapper.insert(review) > 0;
            // 更新电影评分
            if (result) {
                movieService.updateMovieRating(review.getMovieId());
            }
            return result;
        }
    }
    
    @Override
    public boolean deleteReview(Long reviewId, Long userId) {
        MovieReview review = movieReviewMapper.selectById(reviewId);
        if (review != null && review.getUserId().equals(userId)) {
            Long movieId = review.getMovieId();
            boolean result = movieReviewMapper.deleteById(reviewId) > 0;
            // 删除评论后更新电影评分
            if (result && movieId != null) {
                movieService.updateMovieRating(movieId);
            }
            return result;
        }
        return false;
    }
    
    @Override
    public Map<String, Object> getReviewStatsByMovieId(Long movieId) {
        Map<String, Object> stats = new HashMap<>();
        Double averageRating = movieReviewMapper.getAverageRatingByMovieId(movieId);
        Integer reviewCount = movieReviewMapper.getReviewCountByMovieId(movieId);
        List<Map<String, Object>> ratingDistributionList = movieReviewMapper.getRatingDistributionByMovieId(movieId);
        
        // 构建评分分布Map
        Map<Integer, Integer> ratingDistribution = new HashMap<>();
        for (Map<String, Object> item : ratingDistributionList) {
            Integer rating = (Integer) item.get("rating");
            Integer count = ((Number) item.get("count")).intValue();
            ratingDistribution.put(rating, count);
        }
        
        // 确保返回完整的评分分布（1-5分）
        Map<Integer, Integer> completeDistribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            completeDistribution.put(i, ratingDistribution.getOrDefault(i, 0));
        }
        
        stats.put("averageRating", averageRating != null ? averageRating : 0.0);
        stats.put("reviewCount", reviewCount != null ? reviewCount : 0);
        stats.put("ratingDistribution", completeDistribution);
        
        return stats;
    }
}
