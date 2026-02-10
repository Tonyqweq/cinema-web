package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.SysPermission;
import org.tonyqwe.cinemaweb.domain.entity.SysRolePermission;
import org.tonyqwe.cinemaweb.domain.entity.SysUserRole;
import org.tonyqwe.cinemaweb.mapper.PermissionMapper;
import org.tonyqwe.cinemaweb.mapper.RolePermissionMapper;
import org.tonyqwe.cinemaweb.mapper.UserRoleMapper;
import org.tonyqwe.cinemaweb.service.PermissionService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PermissionServiceImpl implements PermissionService {

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private RolePermissionMapper rolePermissionMapper;

    @Resource
    private PermissionMapper permissionMapper;

    @Override
    public List<String> getPermissionCodesByUserId(int userId) {
        // 1. user -> roles
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        // 2. roles -> permissionIds
        List<SysRolePermission> rolePerms = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getRoleId, roleIds)
        );
        if (rolePerms.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> permIds = rolePerms.stream()
                .map(SysRolePermission::getPermissionId)
                .collect(Collectors.toList());

        // 3. permissionIds -> permission.code
        List<SysPermission> perms = permissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, permIds)
        );

        return perms.stream()
                .map(SysPermission::getCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}
