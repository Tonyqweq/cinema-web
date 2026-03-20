package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tonyqwe.cinemaweb.domain.dto.LoginRequest;
import org.tonyqwe.cinemaweb.domain.dto.RegisterRequest;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.mapper.UserMapper;
import org.tonyqwe.cinemaweb.service.LoginService;
import org.tonyqwe.cinemaweb.utils.JwtUtil;
import jakarta.annotation.Resource;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
public class LoginServiceImpl extends ServiceImpl<UserMapper, SysUsers> implements LoginService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;



    // 与 JwtUtil 中的过期时间保持一致（1 小时）
    private static final long EXPIRATION_SECONDS = 60 * 60;
    private static final String LOGIN_TOKEN_KEY_PREFIX = "login:token:";

    @Override
    public String login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        if (username == null || password == null) {
            throw new BadCredentialsException("invalid username or password");
        }

        // 1. 查数据库验证账号密码（注册时使用 BCrypt，这里必须用 matches）
        LambdaQueryWrapper<SysUsers> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUsers::getUsername, username);
        SysUsers user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BadCredentialsException("invalid username or password");
        }

        // 2. 账号状态校验（status: 1-启用，0-禁用）
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new DisabledException("account is disabled");
        }

        // 3. 密码校验（BCrypt）
        String storedPassword = user.getPassword();
        if (storedPassword == null) {
            throw new BadCredentialsException("invalid username or password");
        }

        // 3.1 先用 BCrypt 校验（推荐路径）
        if (!passwordEncoder.matches(password, storedPassword)) {
            // 3.2 兼容历史数据：如果数据库存的不是 BCrypt 哈希，就当明文校验
            // BCrypt 哈希一般形如：$2a$12$xxxx... / $2b$12$xxxx...
            boolean looksLikeBcrypt = Pattern.matches("^\\$2[aby]\\$\\d{2}\\$.*", storedPassword);
            if (!looksLikeBcrypt && storedPassword.equals(password)) {
                // 明文匹配成功：立刻迁移为 BCrypt，避免下一次继续走兼容逻辑
                String encodedPassword = passwordEncoder.encode(password);
                user.setPassword(encodedPassword);
                this.updateById(user);
            } else {
                throw new BadCredentialsException("invalid username or password");
            }
        }

        // 4. Redis 中是否已有未过期 token：有就复用
        String redisKey = LOGIN_TOKEN_KEY_PREFIX + username;
        String cachedToken = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedToken != null && !cachedToken.isEmpty()) {
            return cachedToken;
        }

        // 5. 没有缓存则生成新 token
        String token = JwtUtil.generateToken(username);

        // 6. 写入 Redis，设置过期时间
        stringRedisTemplate
                .opsForValue()
                .set(redisKey, token, EXPIRATION_SECONDS, TimeUnit.SECONDS);

        return token;
    }

    @Override
    public SysUsers getCurrentUser(String token) {
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
        LambdaQueryWrapper<SysUsers> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUsers::getUsername, username);
        SysUsers user = userMapper.selectOne(wrapper);
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

    @Override
    public SysUsers getByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<SysUsers>()
                .eq(SysUsers::getUsername, username));
    }

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 1. 检查用户名是否存在
        SysUsers existUser = this.getByUsername(request.getUsername());
        if (existUser != null) {
            throw new IllegalArgumentException("用户名已存在");
        }

        // 2. 【核心】密码加密
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. 构建用户对象
        SysUsers user = new SysUsers();
        user.setUsername(request.getUsername());
        user.setPassword(encodedPassword);      // 存加密值
        user.setEmail(request.getEmail());      // 如果有邮箱
        user.setStatus(1);                      // 1-启用
        user.setGender(0);                      // 默认性别
        user.setCreatedAt(new Date());          // 创建时间

        // 4. MyBatis-Plus 保存
        this.save(user);
    }
}
