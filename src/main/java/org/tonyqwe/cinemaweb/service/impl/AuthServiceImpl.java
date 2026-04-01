package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.domain.dto.LoginRequest;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.mapper.UserMapper;
import org.tonyqwe.cinemaweb.service.AuthService;
import org.tonyqwe.cinemaweb.service.EmailService;
import org.tonyqwe.cinemaweb.service.TokenService;
import org.tonyqwe.cinemaweb.service.UserService;

import java.util.Random;
import java.util.concurrent.TimeUnit;
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

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private EmailService emailService;

    @Override
    public String login(LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String email = request.getEmail();
        String verificationCode = request.getVerificationCode();
        
        if (username == null || password == null || email == null || verificationCode == null) {
            throw new BadCredentialsException("invalid username, password, email or verification code");
        }
        
        // 验证验证码
        boolean codeValid = verifyCode(email, verificationCode);
        if (!codeValid) {
            throw new BadCredentialsException("invalid or expired verification code");
        }
        
        // 验证码验证通过后，删除Redis中的验证码
        deleteVerificationCode(email);

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

    @Override
    public void sendVerificationCode(String email) {
        // 生成6位随机验证码
        String code = String.format("%06d", new Random().nextInt(999999));
        // 存储到Redis，有效期1分钟
        String key = "verification:code:" + email;
        redisTemplate.opsForValue().set(key, code, 1, TimeUnit.MINUTES);
        // 调用邮件服务发送验证码
        try {
            emailService.sendVerificationCode(email, code);
            System.out.println("验证码已发送到邮箱：" + email + "，验证码：" + code);
        } catch (Exception e) {
            System.err.println("发送邮件失败：" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("发送验证码失败", e);
        }
    }

    @Override
    public boolean verifyCode(String email, String code) {
        String key = "verification:code:" + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        return code != null && code.equals(storedCode);
    }
    
    @Override
    public void deleteVerificationCode(String email) {
        String key = "verification:code:" + email;
        redisTemplate.delete(key);
    }
}
