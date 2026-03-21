package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.dto.LoginRequest;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.mapper.UserMapper;
import org.tonyqwe.cinemaweb.service.AuthService;
import org.tonyqwe.cinemaweb.service.TokenService;
import org.tonyqwe.cinemaweb.service.UserService;

import java.util.regex.Pattern;

@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private UserService userService;

    @Resource
    private UserMapper userMapper;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Resource
    private TokenService tokenService;

    @Override
    public String login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        if (username == null || password == null) {
            throw new BadCredentialsException("invalid username or password");
        }

        SysUsers user = userService.getByUsername(username);
        if (user == null) {
            throw new BadCredentialsException("invalid username or password");
        }

        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new DisabledException("account is disabled");
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null) {
            throw new BadCredentialsException("invalid username or password");
        }

        if (!passwordEncoder.matches(password, storedPassword)) {
            boolean looksLikeBcrypt = Pattern.matches("^\\$2[aby]\\$\\d{2}\\$.*", storedPassword);
            if (!looksLikeBcrypt && storedPassword.equals(password)) {
                String encodedPassword = passwordEncoder.encode(password);
                user.setPassword(encodedPassword);
                userService.updateById(user);
            } else {
                throw new BadCredentialsException("invalid username or password");
            }
        }

        String cached = tokenService.getCachedToken(username);
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        String token = tokenService.generateToken(username);
        tokenService.cacheToken(username, token);
        return token;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isEmpty()) return;
        String username = tokenService.parseUsername(token);
        if (username != null) {
            tokenService.deleteToken(username);
        }
    }

    @Override
    public SysUsers getCurrentUser(String token) {
        if (token == null || token.isEmpty()) return null;
        String username = tokenService.parseUsername(token);
        if (username == null) return null;
        if (!tokenService.validate(username, token)) return null;

        SysUsers user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUsers>().eq(SysUsers::getUsername, username));
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
