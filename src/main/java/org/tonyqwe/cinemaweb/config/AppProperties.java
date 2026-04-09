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
    private Security security = new Security();

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

    /**
     * 与 sys_roles.name 一致的角色名列表；用于接口路径与 Spring Security hasAuthority("ROLE_xxx")。
     */
    public static class Security {
        private List<String> cinemaApiRoles = List.of("SUPER_ADMIN", "ADMIN", "STAFF");
        private List<String> movieApiRoles = List.of("SUPER_ADMIN", "ADMIN", "STAFF");
        private List<String> orderApiRoles = List.of("SUPER_ADMIN", "ADMIN","STAFF");
        private List<String> userApiRoles = List.of("SUPER_ADMIN", "ADMIN");
        private List<String> settingsApiRoles = List.of("SUPER_ADMIN", "ADMIN","STAFF","USER");

        public List<String> getCinemaApiRoles() { return cinemaApiRoles; }
        public void setCinemaApiRoles(List<String> cinemaApiRoles) { this.cinemaApiRoles = movieApiRoles; }
        public List<String> getMovieApiRoles() { return movieApiRoles; }
        public void setMovieApiRoles(List<String> movieApiRoles) { this.movieApiRoles = movieApiRoles; }
        public List<String> getOrderApiRoles() { return orderApiRoles; }
        public void setOrderApiRoles(List<String> orderApiRoles) { this.orderApiRoles = orderApiRoles; }
        public List<String> getUserApiRoles() { return userApiRoles; }
        public void setUserApiRoles(List<String> userApiRoles) { this.userApiRoles = userApiRoles; }
        public List<String> getSettingsApiRoles() { return settingsApiRoles; }
        public void setSettingsApiRoles(List<String> settingsApiRoles) { this.settingsApiRoles = settingsApiRoles; }
    }

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Token getToken() { return token; }
    public void setToken(Token token) { this.token = token; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public Security getSecurity() { return security; }
    public void setSecurity(Security security) { this.security = security; }
}
