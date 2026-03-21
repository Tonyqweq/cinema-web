package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.mapper.UserMapper;
import org.tonyqwe.cinemaweb.service.UserService;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUsers> implements UserService {

    @Override
    public SysUsers getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUsers>().eq(SysUsers::getUsername, username));
    }
}
