package org.tonyqwe.cinemaweb.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Resource
    private AppProperties appProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // REST API，一般关闭 CSRF
                .csrf(csrf -> csrf.disable())
                // 使用自定义的 CORS 配置
                .cors(Customizer.withDefaults())
                // 不创建 HTTP Session，完全基于 token
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 认证与授权规则（先匹配更具体的路径）
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/sessions", "/api/sessions/register", "/error").permitAll()
                        //影院模块
//                        .requestMatchers("")
//                        .hasAnyAuthority(SecurityExpressions.toRoleAuthorities(
//                                appProperties.getSecurity().getCinemaApiRoles()))
                        // 影片模块：仅允许配置的角色访问（与 app.security.movie-api-roles / sys_roles.name 一致）
                        .requestMatchers("/api/movies/**")
                        .hasAnyAuthority(SecurityExpressions.toRoleAuthorities(
                                appProperties.getSecurity().getMovieApiRoles()))
                        .requestMatchers("/api/orders/**")
                        .hasAnyAuthority(SecurityExpressions.toRoleAuthorities(
                                appProperties.getSecurity().getOrderApiRoles()))
                        .requestMatchers("/api/users/**")
                        .hasAnyAuthority(SecurityExpressions.toRoleAuthorities(
                                appProperties.getSecurity().getUserApiRoles()))
                        .requestMatchers("/api/settings/**")
                        .hasAnyAuthority(SecurityExpressions.toRoleAuthorities(
                                appProperties.getSecurity().getSettingsApiRoles()))
                        // 示例与其它已登录接口：任意已认证用户
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                // 将 JWT 过滤器加在用户名密码过滤器之前
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 配置：从 application.yaml app.cors 读取。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
