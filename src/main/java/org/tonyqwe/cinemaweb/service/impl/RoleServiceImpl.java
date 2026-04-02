package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.SysRole;
import org.tonyqwe.cinemaweb.mapper.RoleMapper;
import org.tonyqwe.cinemaweb.service.RoleService;

@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, SysRole> implements RoleService {
}
