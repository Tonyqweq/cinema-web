package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.tonyqwe.cinemaweb.domain.entity.UserCollection;

@Mapper
public interface UserCollectionMapper extends BaseMapper<UserCollection> {
    @Select("SELECT COUNT(*) FROM user_collections WHERE movie_id = #{movieId}")
    Integer getCollectionCount(Long movieId);
}