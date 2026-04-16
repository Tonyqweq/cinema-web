package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;
import org.tonyqwe.cinemaweb.domain.entity.UserCollection;

import java.util.List;

public interface UserCollectionMapper extends BaseMapper<UserCollection> {

    IPage<UserCollection> selectByUserId(IPage<UserCollection> page, @Param("userId") Long userId);
    
    UserCollection selectByUserIdAndMovieId(@Param("userId") Long userId, @Param("movieId") Long movieId);
    
    int deleteByUserIdAndMovieId(@Param("userId") Long userId, @Param("movieId") Long movieId);
}
