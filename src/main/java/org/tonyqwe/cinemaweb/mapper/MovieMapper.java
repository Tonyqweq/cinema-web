package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.tonyqwe.cinemaweb.domain.entity.Movies;

import java.util.List;

@Mapper
public interface MovieMapper extends BaseMapper<Movies> {
    @Select("SELECT DISTINCT language FROM movies WHERE language IS NOT NULL AND language <> '' ORDER BY language")
    java.util.List<String> selectDistinctLanguages();

    @Select("SELECT DISTINCT country FROM movies WHERE country IS NOT NULL AND country <> '' ORDER BY country")
    java.util.List<String> selectDistinctCountries();

    @Select("SELECT DISTINCT m.* FROM movies m INNER JOIN movie_tags mt ON m.id = mt.movie_id WHERE mt.tag_id IN (${tagIds})")
    List<Movies> selectMoviesByTagIds(@Param("tagIds") String tagIds);
}

