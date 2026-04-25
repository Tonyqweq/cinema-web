package org.tonyqwe.cinemaweb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tonyqwe.cinemaweb.domain.entity.SysRole;
import org.tonyqwe.cinemaweb.domain.entity.SysUserRole;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.domain.dto.ChangePasswordRequest;
import org.tonyqwe.cinemaweb.domain.dto.ChangePhoneRequest;
import org.tonyqwe.cinemaweb.domain.dto.ChangeEmailRequest;
import org.tonyqwe.cinemaweb.domain.dto.CreateUserRequest;
import org.tonyqwe.cinemaweb.domain.vo.RoleVO;
import org.tonyqwe.cinemaweb.domain.vo.UserListVO;
import org.tonyqwe.cinemaweb.domain.vo.UserVO;
import org.tonyqwe.cinemaweb.service.AdminCinemaRelationService;
import org.tonyqwe.cinemaweb.service.MinioService;
import org.tonyqwe.cinemaweb.service.RoleService;
import org.tonyqwe.cinemaweb.service.UserRoleService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')") // 用户管理仅允许 SUPER_ADMIN
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @Resource
    private UserRoleService userRoleService;

    @Resource
    private AdminCinemaRelationService adminCinemaRelationService;

    @Resource
    private MinioService minioService;

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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')") // 允许管理员角色查看角色列表
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
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')") // 允许管理员角色查看用户角色
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
     * 获取当前用户绑定的影院ID
     */
    @PreAuthorize("isAuthenticated()") // 所有已认证用户可访问
    @GetMapping("/me/cinema")
    public ResponseResult<?> getMyCinema() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            return ResponseResult.error("Not logged in");
        }
        SysUsers user = userService.getByUsername(username);
        if (user == null) {
            return ResponseResult.error("User not found");
        }
        Long cinemaId = adminCinemaRelationService.getCinemaIdByAdminId(user.getId());
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

    /**
     * 修改密码
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @PutMapping("/password")
    public ResponseResult<?> changePassword(@RequestBody ChangePasswordRequest request) {
        return userService.changePassword(request.getOldPassword(), request.getNewPassword());
    }

    /**
     * 修改手机号
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @PutMapping("/phone")
    public ResponseResult<?> changePhone(@RequestBody ChangePhoneRequest request) {
        return userService.changePhone(request.getPhone(), request.getVerificationCode());
    }

    /**
     * 修改邮箱
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @PutMapping("/email")
    public ResponseResult<?> changeEmail(@RequestBody ChangeEmailRequest request) {
        return userService.changeEmail(request.getEmail(), request.getVerificationCode());
    }

    /**
     * 修改昵称
     */
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/nickname")
    public ResponseResult<?> changeNickname(@RequestBody Map<String, String> request) {
        try {
            // 获取当前用户
            String username = SecurityUtils.getCurrentUsername();
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseResult.error(401, "用户未登录");
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error(404, "用户不存在");
            }
            
            // 更新用户昵称
            String nickname = request.get("nickname");
            if (nickname == null || nickname.isEmpty()) {
                return ResponseResult.error(400, "昵称不能为空");
            }
            
            user.setNickname(nickname);
            userService.updateById(user);
            
            return ResponseResult.success("昵称修改成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error(500, "昵称修改失败：" + e.getMessage());
        }
    }

    /**
     * 创建用户
     * SUPER_ADMIN 可以创建 ADMIN, STAFF, USER
     * ADMIN 可以创建 STAFF, USER
     * STAFF 可以创建 USER
     */
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'STAFF')")
    @PostMapping("/create")
    public ResponseResult<?> createUser(@RequestBody CreateUserRequest request) {
        try {
            // 获取当前用户的角色信息
            String currentUsername = SecurityUtils.getCurrentUsername();
            if (currentUsername == null || "anonymousUser".equals(currentUsername)) {
                return ResponseResult.error(401, "用户未登录");
            }
            
            SysUsers currentUser = userService.getByUsername(currentUsername);
            if (currentUser == null) {
                return ResponseResult.error(404, "用户不存在");
            }
            
            // 获取当前用户角色
            List<SysUserRole> currentUserRoles = userRoleService.list(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, currentUser.getId()));
            if (currentUserRoles.isEmpty()) {
                return ResponseResult.error(403, "用户没有分配角色");
            }
            
            // 获取目标角色
            SysRole targetRole = roleService.getById(request.getRoleId());
            if (targetRole == null) {
                return ResponseResult.error(400, "目标角色不存在");
            }
            
            // 角色等级: SUPER_ADMIN(1) > ADMIN(2) > STAFF(3) > USER(4)
            Integer currentMaxLevel = getRoleMaxLevel(currentUserRoles);
            Integer targetLevel = getRoleLevelByName(targetRole.getName());
            
            // 检查权限: 当前用户不能创建比自己等级高（数字小）的角色
            if (targetLevel < currentMaxLevel) {
                return ResponseResult.error(403, "无权创建比自身权限等级更高的用户");
            }
            
            // 检查用户名是否已存在
            if (userService.getByUsername(request.getUsername()) != null) {
                return ResponseResult.error(400, "用户名已存在");
            }
            
            // 创建用户
            SysUsers newUser = new SysUsers();
            newUser.setUsername(request.getUsername());
            newUser.setPassword(request.getPassword()); // 应该加密，但当前实现可能已处理
            newUser.setEmail(request.getEmail());
            newUser.setGender(request.getGender() != null ? request.getGender() : 0);
            newUser.setStatus(request.getStatus() != null ? request.getStatus() : 1);
            newUser.setCreatedAt(new java.util.Date());
            
            userService.save(newUser);
            
            // 分配角色
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(newUser.getId());
            userRole.setRoleId(request.getRoleId());
            userRoleService.save(userRole);
            
            return ResponseResult.success("用户创建成功");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error(500, "用户创建失败：" + e.getMessage());
        }
    }
    
    /**
     * 根据角色名称获取角色等级
     * 数字越小，等级越高
     */
    private Integer getRoleLevelByName(String roleName) {
        if (roleName == null) return 999;
        return switch (roleName) {
            case "SUPER_ADMIN" -> 1;
            case "ADMIN" -> 2;
            case "STAFF" -> 3;
            case "USER" -> 4;
            default -> 999;
        };
    }
    
    /**
     * 获取用户可创建的最高角色等级（基于用户当前角色）
     */
    private Integer getRoleMaxLevel(List<SysUserRole> userRoles) {
        Integer maxLevel = 999;
        for (SysUserRole ur : userRoles) {
            SysRole role = roleService.getById(ur.getRoleId());
            if (role != null) {
                Integer level = getRoleLevelByName(role.getName());
                if (level < maxLevel) {
                    maxLevel = level;
                }
            }
        }
        return maxLevel;
    }

    /**
     * 上传用户头像
     */
    @PostMapping("/avatar")
    public ResponseResult<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            // 上传文件到MinIO
            String avatarUrl = minioService.uploadFile(file);
            
            // 获取当前用户
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            String username = null;
            if (principal instanceof UserDetails) {
                username = ((UserDetails) principal).getUsername();
            }
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseResult.error(401, "用户未登录");
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error(404, "用户不存在");
            }
            
            // 更新用户头像
            user.setAvatar(avatarUrl);
            userService.updateById(user);
            
            return ResponseResult.success(avatarUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseResult.error(500, "头像上传失败：" + e.getMessage());
        }
    }
}
