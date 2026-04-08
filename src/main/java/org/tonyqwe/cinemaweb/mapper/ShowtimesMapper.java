package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.tonyqwe.cinemaweb.domain.entity.Showtimes;

/**
 * 排片Mapper
 */
@Mapper
public interface ShowtimesMapper extends BaseMapper<Showtimes> {
}
