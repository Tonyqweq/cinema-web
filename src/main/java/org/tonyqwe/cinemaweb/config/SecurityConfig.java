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
                        .requestMatchers("/sessions/**", "/api/sessions/**", "/api/dashboard/**", "/error").permitAll()
                        // 图片代理接口：允许匿名访问
                        .requestMatchers("/api/movies/proxy-image").permitAll()
                        // 电影列表和标签筛选：允许匿名访问
                        .requestMatchers("/api/movies", "/api/movies/filters").permitAll()
                        // 影院模块：允许所有已认证用户访问
                        .requestMatchers("/api/cinemas/**").authenticated()
                        // 影厅模块：允许所有已认证用户访问
                        .requestMatchers("/api/halls/**").authenticated()
                        // 影片模块其他接口：允许所有已认证用户访问
                        .requestMatchers("/api/movies/**").authenticated()
                        // 订单模块：允许所有已认证用户访问
                        .requestMatchers("/api/orders/**").authenticated()
                        .requestMatchers("/api/user/wallet/**").authenticated()
                        // 用户昵称修改：允许所有已认证用户访问
                        .requestMatchers("/api/admin/users/nickname").authenticated()
                        // 用户模块（其他接口）：仅允许管理员访问
                        .requestMatchers("/api/users/**", "/admin/users/**", "/api/admin/users/**")
                        .hasAnyAuthority(SecurityExpressions.toRoleAuthorities(
                                appProperties.getSecurity().getUserApiRoles()))
                        // 设置模块：允许所有已认证用户访问
                        .requestMatchers("/api/settings/**").authenticated()
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
        config.setAllowedHeaders(java.util.List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
