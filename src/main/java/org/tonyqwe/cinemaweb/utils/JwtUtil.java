package org.tonyqwe.cinemaweb.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    // 演示用，实际请放配置文件，并使用更安全且足够长度的密钥（>=32 字节）
    private static final String SECRET = "demo-secret-key-123456-demo-secret-key-123456";

    // 预先生成签名用的 Key，整个应用复用
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    // 过期时间：1 小时
    private static final long EXPIRATION = 60 * 60 * 1000L;

    public static String generateToken(String username) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expire)
                .signWith(KEY)
                .compact();
    }

    /**
     * 从 token 中解析出用户名（subject）。解析失败返回 null。
     */
    public static String parseUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
