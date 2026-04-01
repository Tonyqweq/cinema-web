package org.tonyqwe.cinemaweb.service.impl;

import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.dto.RegisterRequest;
import org.tonyqwe.cinemaweb.domain.entity.SysUserRole;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.mapper.UserRoleMapper;
import org.tonyqwe.cinemaweb.service.AuthService;
import org.tonyqwe.cinemaweb.service.RegisterService;
import org.tonyqwe.cinemaweb.service.UserService;

import java.util.Date;

@Service
public class RegisterServiceImpl implements RegisterService {

    @Resource
    private UserService userService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private UserRoleMapper userRoleMapper;

    @Resource
    private AuthService authService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 验证验证码
        boolean codeValid = authService.verifyCode(request.getEmail(), request.getVerificationCode());
        if (!codeValid) {
            throw new IllegalArgumentException("验证码无效或已过期");
        }
        
        // 验证码验证通过后，删除Redis中的验证码
        authService.deleteVerificationCode(request.getEmail());

        SysUsers existUser = userService.getByUsername(request.getUsername());
        if (existUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        SysUsers user = new SysUsers();
        user.setUsername(request.getUsername());
        user.setPassword(encodedPassword);
        user.setEmail(request.getEmail());
        user.setStatus(1);
        user.setGender(0);
        user.setCreatedAt(new Date());

        userService.save(user);

        if (user.getId() == null) {
            throw new IllegalStateException("用户创建失败");
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(4);
        userRoleMapper.insert(userRole);
    }
}
