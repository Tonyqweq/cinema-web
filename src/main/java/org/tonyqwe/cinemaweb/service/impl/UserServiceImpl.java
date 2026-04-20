package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.entity.SysRole;
import org.tonyqwe.cinemaweb.domain.entity.SysUserRole;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.mapper.RoleMapper;
import org.tonyqwe.cinemaweb.mapper.UserMapper;
import org.tonyqwe.cinemaweb.mapper.UserRoleMapper;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUsers> implements UserService {

    @Resource
    private UserRoleMapper userRoleMapper;
    
    @Resource
    private RoleMapper roleMapper;
    
    @Resource
    private PasswordEncoder passwordEncoder;
    
    @Resource
    private RedisTemplate<String, String> redisTemplate;

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
    
    /**
     * 获取当前用户
     */
    private SysUsers getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String username = authentication.getName();
        return getByUsername(username);
    }
    
    @Override
    public ResponseResult<?> changePassword(String oldPassword, String newPassword) {
        SysUsers user = getCurrentUser();
        if (user == null) {
            return ResponseResult.error("用户未登录");
        }
        
        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseResult.error("旧密码错误");
        }
        
        // 加密新密码
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);
        
        // 更新密码
        if (updateById(user)) {
            return ResponseResult.success("密码修改成功");
        } else {
            return ResponseResult.error("密码修改失败");
        }
    }
    
    /**
     * 验证验证码
     */
    private boolean verifyCode(String email, String code) {
        String key = "verification:code:" + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        return code != null && code.equals(storedCode);
    }
    
    /**
     * 删除验证码
     */
    private void deleteVerificationCode(String email) {
        String key = "verification:code:" + email;
        redisTemplate.delete(key);
    }
    
    @Override
    public ResponseResult<?> changePhone(String phone, String verificationCode) {
        SysUsers user = getCurrentUser();
        if (user == null) {
            return ResponseResult.error("用户未登录");
        }
        
        // 验证验证码
        boolean codeValid = verifyCode(user.getEmail(), verificationCode);
        if (!codeValid) {
            return ResponseResult.error("验证码无效或已过期");
        }
        
        // 更新手机号
        user.setPhone(phone);
        if (updateById(user)) {
            // 验证码验证通过后，删除Redis中的验证码
            deleteVerificationCode(user.getEmail());
            return ResponseResult.success("手机号修改成功");
        } else {
            return ResponseResult.error("手机号修改失败");
        }
    }
    
    @Override
    public ResponseResult<?> changeEmail(String email, String verificationCode) {
        SysUsers user = getCurrentUser();
        if (user == null) {
            return ResponseResult.error("用户未登录");
        }
        
        // 验证验证码
        boolean codeValid = verifyCode(email, verificationCode);
        if (!codeValid) {
            return ResponseResult.error("验证码无效或已过期");
        }
        
        // 检查邮箱是否已被使用
        SysUsers existingUser = getOne(new LambdaQueryWrapper<SysUsers>().eq(SysUsers::getEmail, email));
        if (existingUser != null && !existingUser.getId().equals(user.getId())) {
            return ResponseResult.error("邮箱已被使用");
        }
        
        // 更新邮箱
        user.setEmail(email);
        if (updateById(user)) {
            // 验证码验证通过后，删除Redis中的验证码
            deleteVerificationCode(email);
            return ResponseResult.success("邮箱修改成功");
        } else {
            return ResponseResult.error("邮箱修改失败");
        }
    }

    @Override
    public long count() {
        return super.count();
    }
}
