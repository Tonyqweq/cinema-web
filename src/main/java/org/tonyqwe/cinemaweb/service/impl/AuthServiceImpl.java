package org.tonyqwe.cinemaweb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        String email = request.getEmail();
        log.info("用户登录，用户名: {}, 邮箱: {}", username, email);
        
        String password = request.getPassword();
        String verificationCode = request.getVerificationCode();
        
        if (password == null || email == null || verificationCode == null) {
            log.error("登录参数缺失，密码: {}, 邮箱: {}, 验证码: {}", password, email, verificationCode);
            throw new BadCredentialsException("invalid password, email or verification code");
        }
        
        SysUsers user = null;
        if (username != null && !username.isEmpty()) {
            // 根据用户名获取用户信息
            user = userService.getByUsername(username);
        } else {
            // 根据邮箱获取用户信息
            LambdaQueryWrapper<SysUsers> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysUsers::getEmail, email);
            user = userMapper.selectOne(wrapper);
        }
        
        if (user == null) {
            log.warn("用户不存在，用户名: {}, 邮箱: {}", username, email);
            throw new BadCredentialsException("invalid username or password");
        }
        
        log.info("用户存在，用户ID: {}", user.getId());
        
        // 验证邮箱是否与用户数据库中的邮箱一致
        String userEmail = user.getEmail();
        if (userEmail == null || userEmail.isEmpty()) {
            log.error("用户邮箱未设置，用户ID: {}", user.getId());
            throw new BadCredentialsException("user email not set");
        }
        if (!userEmail.equals(email)) {
            log.warn("邮箱不匹配，用户邮箱: {}, 输入邮箱: {}", userEmail, email);
            throw new BadCredentialsException("email does not match user's email");
        }
        
        // 验证验证码
        boolean codeValid = verifyCode(email, verificationCode);
        if (!codeValid) {
            log.warn("验证码无效或已过期，邮箱: {}", email);
            throw new BadCredentialsException("invalid or expired verification code");
        }
        
        log.info("验证码验证通过，邮箱: {}", email);
        // 验证码验证通过后，删除Redis中的验证码
        deleteVerificationCode(email);

        // 用户信息已经在前面获取过了，不需要重复获取

        if (user.getStatus() != null && user.getStatus() != 1) {
            log.warn("账户已禁用，用户ID: {}", user.getId());
            throw new DisabledException("account is disabled");
        }

        String storedPassword = user.getPassword();
        if (storedPassword == null) {
            log.error("用户密码未设置，用户ID: {}", user.getId());
            throw new BadCredentialsException("invalid username or password");
        }

        if (!passwordEncoder.matches(password, storedPassword)) {
            boolean looksLikeBcrypt = Pattern.matches("^\\$2[aby]\\$\\d{2}\\$.*", storedPassword);
            if (!looksLikeBcrypt && storedPassword.equals(password)) {
                log.info("密码格式升级，用户ID: {}", user.getId());
                String encodedPassword = passwordEncoder.encode(password);
                user.setPassword(encodedPassword);
                userService.updateById(user);
            } else {
                log.warn("密码错误，用户ID: {}", user.getId());
                throw new BadCredentialsException("invalid username or password");
            }
        }

        log.info("密码验证通过，用户ID: {}", user.getId());
        
        String cached = tokenService.getCachedToken(username);
        if (cached != null && !cached.isEmpty()) {
            log.info("使用缓存的token，用户ID: {}", user.getId());
            return cached;
        }

        String token = tokenService.generateToken(username, (long) user.getId());
        log.info("生成新token，用户ID: {}", user.getId());

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
    
    @Override
    public String getMaskedEmail(String username) {
        // 根据用户名获取用户信息
        SysUsers user = userService.getByUsername(username);
        if (user == null) {
            return null;
        }
        
        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            return null;
        }
        
        // 对邮箱进行打码处理
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            // 邮箱前缀太短，全部打码
            return "****" + email.substring(atIndex);
        }
        
        // 保留前两位和后两位，中间打码
        String prefix = email.substring(0, 2);
        String suffix = email.substring(atIndex - 2, atIndex);
        String domain = email.substring(atIndex);
        return prefix + "****" + suffix + domain;
    }
    
    @Override
    public String getEmailByUsername(String username) {
        // 根据用户名获取用户信息
        SysUsers user = userService.getByUsername(username);
        if (user == null) {
            return null;
        }
        
        return user.getEmail();
    }
}
