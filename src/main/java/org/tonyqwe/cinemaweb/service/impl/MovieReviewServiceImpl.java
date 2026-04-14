package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.entity.MovieReview;
import org.tonyqwe.cinemaweb.mapper.MovieReviewMapper;
import org.tonyqwe.cinemaweb.service.MovieReviewService;

@Service
public class MovieReviewServiceImpl extends ServiceImpl<MovieReviewMapper, MovieReview> implements MovieReviewService {

    @Resource
    private MovieReviewMapper movieReviewMapper;

    @Override
    public IPage<MovieReview> getReviewsByMovieId(Long movieId, long page, long pageSize) {
        Page<MovieReview> mpPage = new Page<>(page, pageSize);
        LambdaQueryWrapper<MovieReview> qw = new LambdaQueryWrapper<>();
        qw.eq(MovieReview::getMovieId, movieId);
        qw.orderByDesc(MovieReview::getCreatedAt);
        return movieReviewMapper.selectPage(mpPage, qw);
    }

    @Override
    @Transactional
    public MovieReview createOrUpdateReview(Long movieId, Integer userId, Integer rating, String comment) {
        LambdaQueryWrapper<MovieReview> qw = new LambdaQueryWrapper<>();
        qw.eq(MovieReview::getMovieId, movieId);
        qw.eq(MovieReview::getUserId, userId);
        
        MovieReview existingReview = movieReviewMapper.selectOne(qw);
        
        if (existingReview != null) {
            existingReview.setRating(rating);
            existingReview.setComment(comment);
            movieReviewMapper.updateById(existingReview);
            return existingReview;
        } else {
            MovieReview newReview = new MovieReview();
            newReview.setMovieId(movieId);
            newReview.setUserId(userId);
            newReview.setRating(rating);
            newReview.setComment(comment);
            movieReviewMapper.insert(newReview);
            return newReview;
        }
    }

    @Override
    public MovieReview getUserReview(Long movieId, Integer userId) {
        LambdaQueryWrapper<MovieReview> qw = new LambdaQueryWrapper<>();
        qw.eq(MovieReview::getMovieId, movieId);
        qw.eq(MovieReview::getUserId, userId);
        return movieReviewMapper.selectOne(qw);
    }

    @Override
    public boolean deleteReview(Long reviewId, Integer userId) {
        LambdaQueryWrapper<MovieReview> qw = new LambdaQueryWrapper<>();
        qw.eq(MovieReview::getId, reviewId);
        qw.eq(MovieReview::getUserId, userId);
        return movieReviewMapper.delete(qw) > 0;
    }

    @Override
    public Double getAverageRating(Long movieId) {
        return movieReviewMapper.getAverageRating(movieId);
    }

    @Override
    public Integer getReviewCount(Long movieId) {
        return movieReviewMapper.getReviewCount(movieId);
    }
}