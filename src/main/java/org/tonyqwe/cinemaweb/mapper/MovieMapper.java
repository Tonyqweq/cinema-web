package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.tonyqwe.cinemaweb.domain.entity.Movie;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;

@Mapper
public interface MovieMapper extends BaseMapper<Movie> {
    @Select("SELECT * FROM movie WHERE title = #{title}")
    SysUsers selectByTitle(@Param("title") String title);
}

