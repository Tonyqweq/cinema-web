package org.tonyqwe.cinemaweb.service.impl;

import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.dto.RegisterRequest;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.service.RegisterService;
import org.tonyqwe.cinemaweb.service.UserService;

import java.util.Date;

@Service
public class RegisterServiceImpl implements RegisterService {

    @Resource
    private UserService userService;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
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
    }
}
