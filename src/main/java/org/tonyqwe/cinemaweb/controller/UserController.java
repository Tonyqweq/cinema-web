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
import org.tonyqwe.cinemaweb.service.RoleService;
import org.tonyqwe.cinemaweb.service.UserRoleService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

import java.time.LocalDateTime;
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

    /**
     * 获取用户列表
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @GetMapping("/list")
    public ResponseResult<?> getUserList(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer limit) {
        Page<SysUsers> userPage = new Page<>(page, limit);
        Page<SysUsers> result = userService.page(userPage);
        
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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
        userVO.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
        userVO.setUpdateTime(user.getUpdateTime() != null ? user.getUpdateTime().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null);
        
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
        // 再删除用户
        userService.removeById(id);
        return ResponseResult.success();
    }
}
