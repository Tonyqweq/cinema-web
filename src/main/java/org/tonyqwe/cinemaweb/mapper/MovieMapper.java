package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.tonyqwe.cinemaweb.domain.entity.Movies;

@Mapper
public interface MovieMapper extends BaseMapper<Movies> {
    @Select("SELECT DISTINCT language FROM movies WHERE language IS NOT NULL AND language <> '' ORDER BY language")
    java.util.List<String> selectDistinctLanguages();

    @Select("SELECT DISTINCT country FROM movies WHERE country IS NOT NULL AND country <> '' ORDER BY country")
    java.util.List<String> selectDistinctCountries();
}

