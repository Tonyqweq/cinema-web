package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.SysRole;
import org.tonyqwe.cinemaweb.domain.entity.SysUserRole;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.mapper.RoleMapper;
import org.tonyqwe.cinemaweb.mapper.UserMapper;
import org.tonyqwe.cinemaweb.mapper.UserRoleMapper;
import org.tonyqwe.cinemaweb.service.UserService;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUsers> implements UserService {

    @Resource
    private UserRoleMapper userRoleMapper;
    
    @Resource
    private RoleMapper roleMapper;

    @Override
    public SysUsers getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<SysUsers>().eq(SysUsers::getUsername, username));
    }
    
    @Override
    public boolean isSuperAdmin(String username) {
        return hasRole(username, "SUPER_ADMIN");
    }
    
    @Override
    public boolean isAdmin(String username) {
        return hasRole(username, "ADMIN");
    }
    
    /**
     * 检查用户是否拥有指定角色
     */
    private boolean hasRole(String username, String roleName) {
        SysUsers user = getByUsername(username);
        if (user == null) {
            return false;
        }
        
        // 获取用户的角色ID列表
        List<SysUserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, user.getId()));
        
        for (SysUserRole userRole : userRoles) {
            SysRole role = roleMapper.selectById(userRole.getRoleId());
            if (role != null && roleName.equals(role.getName())) {
                return true;
            }
        }
        
        return false;
    }
}
