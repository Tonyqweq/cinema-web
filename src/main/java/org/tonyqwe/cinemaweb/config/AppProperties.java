package org.tonyqwe.cinemaweb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 应用级配置：JWT、Token Redis、CORS 等，统一从 application.yaml 读取。
 */
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Token token = new Token();
    private Cors cors = new Cors();

    public static class Jwt {
        private String secret = "demo-secret-key-123456-demo-secret-key-123456";
        private int expirationSeconds = 3600;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public int getExpirationSeconds() { return expirationSeconds; }
        public void setExpirationSeconds(int expirationSeconds) { this.expirationSeconds = expirationSeconds; }
    }

    public static class Token {
        private String redisPrefix = "login:token:";

        public String getRedisPrefix() { return redisPrefix; }
        public void setRedisPrefix(String redisPrefix) { this.redisPrefix = redisPrefix; }
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Token getToken() { return token; }
    public void setToken(Token token) { this.token = token; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
}
