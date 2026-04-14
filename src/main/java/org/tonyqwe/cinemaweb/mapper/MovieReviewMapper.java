package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.tonyqwe.cinemaweb.domain.entity.MovieReview;

@Mapper
public interface MovieReviewMapper extends BaseMapper<MovieReview> {
    @Select("SELECT AVG(rating) FROM movie_reviews WHERE movie_id = #{movieId}")
    Double getAverageRating(Long movieId);

    @Select("SELECT COUNT(*) FROM movie_reviews WHERE movie_id = #{movieId}")
    Integer getReviewCount(Long movieId);
}