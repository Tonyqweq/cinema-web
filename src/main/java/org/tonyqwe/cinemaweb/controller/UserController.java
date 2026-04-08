package org.tonyqwe.cinemaweb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.entity.SysRole;
import org.tonyqwe.cinemaweb.domain.entity.SysUserRole;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.domain.vo.RoleVO;
import org.tonyqwe.cinemaweb.domain.vo.UserListVO;
import org.tonyqwe.cinemaweb.domain.vo.UserVO;
import org.tonyqwe.cinemaweb.service.AdminCinemaRelationService;
import org.tonyqwe.cinemaweb.service.RoleService;
import org.tonyqwe.cinemaweb.service.UserRoleService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @Resource
    private UserRoleService userRoleService;

    @Resource
    private AdminCinemaRelationService adminCinemaRelationService;

    /**
     * 获取用户列表
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @GetMapping("/list")
    public ResponseResult<?> getUserList(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer limit,
                                         @RequestParam(required = false) String username,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) Integer role) {
        LambdaQueryWrapper<SysUsers> queryWrapper = new LambdaQueryWrapper<>();
        
        // 按用户名筛选
        if (username != null && !username.isEmpty()) {
            queryWrapper.like(SysUsers::getUsername, username);
        }
        
        // 按状态筛选
        if (status != null) {
            queryWrapper.eq(SysUsers::getStatus, status);
        }
        
        // 按角色筛选
        if (role != null) {
            // 先查询拥有该角色的用户ID
            List<SysUserRole> userRoles = userRoleService.list(new LambdaQueryWrapper<SysUserRole>()
                    .eq(SysUserRole::getRoleId, role));
            if (!userRoles.isEmpty()) {
                List<Integer> userIds = userRoles.stream()
                        .map(SysUserRole::getUserId)
                        .collect(Collectors.toList());
                queryWrapper.in(SysUsers::getId, userIds);
            } else {
                // 如果没有用户拥有该角色，返回空结果
                Page<SysUsers> userPage = new Page<>(page, limit);
                userPage.setRecords(List.of());
                userPage.setTotal(0);
                
                UserListVO userListVO = new UserListVO();
                userListVO.setRecords(List.of());
                userListVO.setTotal(0);
                userListVO.setCurrent(page);
                userListVO.setSize(limit);
                
                return ResponseResult.success(userListVO);
            }
        }
        
        Page<SysUsers> userPage = new Page<>(page, limit);
        Page<SysUsers> result = userService.page(userPage, queryWrapper);
        
        // 转换为VO
        UserListVO userListVO = new UserListVO();
        userListVO.setRecords(result.getRecords().stream()
                .map(this::convertToUserVO)
                .collect(Collectors.toList()));
        userListVO.setTotal(result.getTotal());
        userListVO.setCurrent((int) result.getCurrent());
        userListVO.setSize((int) result.getSize());
        
        return ResponseResult.success(userListVO);
    }

    /**
     * 获取所有角色
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @GetMapping("/roles")
    public ResponseResult<?> getRoles() {
        List<SysRole> roles = roleService.list();
        List<RoleVO> roleVOs = roles.stream()
                .map(this::convertToRoleVO)
                .collect(Collectors.toList());
        return ResponseResult.success(roleVOs);
    }

    /**
     * 获取用户角色
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @GetMapping("/role/{userId}")
    public ResponseResult<?> getUserRole(@PathVariable Integer userId) {
        List<SysUserRole> userRoles = userRoleService.list(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        return ResponseResult.success(userRoles);
    }

    /**
     * 将SysUsers转换为UserVO
     */
    private UserVO convertToUserVO(SysUsers user) {
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setEmail(user.getEmail());
        userVO.setGender(user.getGender());
        userVO.setStatus(user.getStatus());
        userVO.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null);
        userVO.setUpdateTime(user.getUpdateTime() != null ? user.getUpdateTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime() : null);
        
        // 获取用户角色
        List<SysUserRole> userRoles = userRoleService.list(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, user.getId()));
        if (!userRoles.isEmpty()) {
            List<Integer> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
            List<SysRole> roles = roleService.listByIds(roleIds);
            userVO.setRoles(roles.stream()
                    .map(this::convertToRoleVO)
                    .collect(Collectors.toList()));
        }
        
        return userVO;
    }

    /**
     * 将SysRole转换为RoleVO
     */
    private RoleVO convertToRoleVO(SysRole role) {
        RoleVO roleVO = new RoleVO();
        roleVO.setId(role.getId());
        roleVO.setName(role.getName());
        roleVO.setDescription(role.getDescription());
        return roleVO;
    }

    /**
     * 修改用户角色
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @PutMapping("/role")
    public ResponseResult<?> updateUserRole(@RequestBody Map<String, Object> params) {
        Integer userId = (Integer) params.get("userId");
        Integer roleId = (Integer) params.get("roleId");

        // 删除原有角色
        userRoleService.remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));

        // 新增角色
        if (roleId != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            userRoleService.save(userRole);
        }

        return ResponseResult.success();
    }

    /**
     * 修改用户信息
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @PutMapping("/update")
    public ResponseResult<?> updateUser(@RequestBody SysUsers user) {
        userService.updateById(user);
        return ResponseResult.success();
    }

    /**
     * 删除用户
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @DeleteMapping("/delete/{id}")
    public ResponseResult<?> deleteUser(@PathVariable Integer id) {
        // 先删除用户角色关联
        userRoleService.remove(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, id));
        // 删除用户与影院的绑定
        adminCinemaRelationService.unbindAdminFromCinema(id);
        // 再删除用户
        userService.removeById(id);
        return ResponseResult.success();
    }

    /**
     * 获取用户绑定的影院ID
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @GetMapping("/cinema/{userId}")
    public ResponseResult<?> getUserCinema(@PathVariable Integer userId) {
        Long cinemaId = adminCinemaRelationService.getCinemaIdByAdminId(userId);
        return ResponseResult.success(cinemaId);
    }

    /**
     * 绑定用户与影院
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @PutMapping("/cinema")
    public ResponseResult<?> updateUserCinema(@RequestBody Map<String, Object> params) {
        try {
            Integer userId = (Integer) params.get("userId");
            Long cinemaId = null;
            Object cinemaIdObj = params.get("cinemaId");
            if (cinemaIdObj != null) {
                if (cinemaIdObj instanceof Integer) {
                    cinemaId = ((Integer) cinemaIdObj).longValue();
                } else if (cinemaIdObj instanceof Long) {
                    cinemaId = (Long) cinemaIdObj;
                } else if (cinemaIdObj instanceof String) {
                    try {
                        cinemaId = Long.parseLong((String) cinemaIdObj);
                    } catch (NumberFormatException e) {
                        return ResponseResult.error("Invalid cinemaId format");
                    }
                }
            }

            if (userId == null) {
                return ResponseResult.error("userId cannot be null");
            }

            if (cinemaId != null) {
                adminCinemaRelationService.bindAdminToCinema(userId, cinemaId);
            } else {
                adminCinemaRelationService.unbindAdminFromCinema(userId);
            }

            return ResponseResult.success();
        } catch (Exception e) {
            return ResponseResult.error("Failed to update user cinema binding: " + e.getMessage());
        }
    }
}
