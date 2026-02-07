package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.tonyqwe.cinemaweb.domain.entity.sysUsers;

@Mapper
public interface UserMapper extends BaseMapper<sysUsers> {
}
