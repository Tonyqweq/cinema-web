package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.tonyqwe.cinemaweb.domain.entity.MovieTags;

@Mapper
public interface MovieTagMapper extends BaseMapper<MovieTags> {
}