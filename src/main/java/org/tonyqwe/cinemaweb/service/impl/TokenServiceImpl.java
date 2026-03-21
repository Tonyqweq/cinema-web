package org.tonyqwe.cinemaweb.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.tonyqwe.cinemaweb.config.AppProperties;
import org.tonyqwe.cinemaweb.service.TokenService;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
public class TokenServiceImpl implements TokenService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private AppProperties appProperties;

    @Override
    public String generateToken(String username) {
        String secret = appProperties.getJwt().getSecret();
        int expSeconds = appProperties.getJwt().getExpirationSeconds();

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expire = new Date(now.getTime() + expSeconds * 1000L);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key)
                .compact();
    }

    @Override
    public String parseUsername(String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            String secret = appProperties.getJwt().getSecret();
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void cacheToken(String username, String token) {
        String key = appProperties.getToken().getRedisPrefix() + username;
        int ttl = appProperties.getJwt().getExpirationSeconds();
        stringRedisTemplate.opsForValue().set(key, token, ttl, TimeUnit.SECONDS);
    }

    @Override
    public String getCachedToken(String username) {
        String key = appProperties.getToken().getRedisPrefix() + username;
        return stringRedisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteToken(String username) {
        String key = appProperties.getToken().getRedisPrefix() + username;
        stringRedisTemplate.delete(key);
    }

    @Override
    public boolean validate(String username, String token) {
        String cached = getCachedToken(username);
        return cached != null && cached.equals(token);
    }
}
