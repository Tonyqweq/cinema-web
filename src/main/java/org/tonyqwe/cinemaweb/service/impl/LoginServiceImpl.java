package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.dto.LoginRequest;
import org.tonyqwe.cinemaweb.domain.entity.sysUsers;
import org.tonyqwe.cinemaweb.mapper.UserMapper;
import org.tonyqwe.cinemaweb.service.LoginService;
import org.tonyqwe.cinemaweb.utils.JwtUtil;
import jakarta.annotation.Resource;

import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 与 JwtUtil 中的过期时间保持一致（1 小时）
    private static final long EXPIRATION_SECONDS = 60 * 60;
    private static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";

    @Override
    public String login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        if (username == null || password == null) {
            return null;
        }

        // 1. 先查 Redis：该账号是否已经有未过期的 token
        String redisKey = LOGIN_TOKEN_KEY_PREFIX + username;
        String cachedToken = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedToken != null && !cachedToken.isEmpty()) {
            // 已经登录过，且 Redis 中还有 token，直接返回
            return cachedToken;
        }

        // 2. Redis 中没有，再查数据库验证账号密码
        LambdaQueryWrapper<sysUsers> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(sysUsers::getUsername, username)
                .eq(sysUsers::getPassword, password); // 演示用明文密码，实际建议加密

        sysUsers user = userMapper.selectOne(wrapper);
        if (user == null) {
            return null;
        }

        // 3. 登录成功，生成新 token
        String token = JwtUtil.generateToken(username);

        // 4. 写入 Redis，设置过期时间
        stringRedisTemplate
                .opsForValue()
                .set(redisKey, token, EXPIRATION_SECONDS, TimeUnit.SECONDS);

        return token;
    }

    @Override
    public sysUsers getCurrentUser(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        // 1. 用 JwtUtil 解析用户名（subject）
        String username = JwtUtil.parseUsername(token);
        if (username == null) {
            return null; // token 非法或已过期
        }

        // 2. 校验 Redis 中该用户的 token 是否与当前一致
        String redisKey = LOGIN_TOKEN_KEY_PREFIX + username;
        String cachedToken = stringRedisTemplate.opsForValue().get(redisKey);
        if (!token.equals(cachedToken)) {
            return null; // Redis 中没有，或 token 已被替换/失效
        }

        // 3. 从数据库查当前用户信息
        LambdaQueryWrapper<sysUsers> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(sysUsers::getUsername, username);
        sysUsers user = userMapper.selectOne(wrapper);
        if (user != null) {
            // 不把密码暴露给前端
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }

        String username = JwtUtil.parseUsername(token);
        if (username == null) {
            return;
        }

        String redisKey = LOGIN_TOKEN_KEY_PREFIX + username;
        stringRedisTemplate.delete(redisKey);
    }
}
