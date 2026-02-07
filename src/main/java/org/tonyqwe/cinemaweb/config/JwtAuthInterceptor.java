package org.tonyqwe.cinemaweb.config;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.tonyqwe.cinemaweb.utils.JwtUtil;

/**
 * 简单的 JWT + Redis 登录校验拦截器。
 * 目前仅用于保护 /api/demo/secure 接口。
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        String token = authHeader.substring(7);
        String username = JwtUtil.parseUsername(token);
        if (username == null) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        String redisKey = "login:token:" + username;
        String tokenInRedis = stringRedisTemplate.opsForValue().get(redisKey);
        if (tokenInRedis == null || !token.equals(tokenInRedis)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        // 校验通过，放行
        return true;
    }


}
