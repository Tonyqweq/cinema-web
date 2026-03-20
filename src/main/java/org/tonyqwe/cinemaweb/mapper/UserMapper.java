package org.tonyqwe.cinemaweb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;

@Mapper
public interface UserMapper extends BaseMapper<SysUsers> {
    @Select("SELECT * FROM sys_users WHERE username = #{username}")
    SysUsers selectByUsername(@Param("username") String username);
}
