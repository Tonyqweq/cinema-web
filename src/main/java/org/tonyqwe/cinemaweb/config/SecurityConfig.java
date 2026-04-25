package org.tonyqwe.cinemaweb.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
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
                        // 公开接口：允许匿名访问
                        .requestMatchers("/sessions/**", "/api/sessions/**", "/api/dashboard/**", "/error").permitAll()
                        // 图片代理接口：允许匿名访问
                        .requestMatchers("/api/movies/proxy-image").permitAll()
                        // 电影列表、影院电影查询：允许匿名访问
                        .requestMatchers("/api/movies", "/api/movies/filters", "/api/movies/home", "/api/movies/init-ratings", "/api/movies/cinema/**").permitAll()
                        // 座位状态接口：允许匿名访问
                        .requestMatchers("/api/seats/showtime").permitAll()
                        // 影院列表和详情：允许所有用户访问（用于购票页面）
                        .requestMatchers("/api/cinemas", "/api/cinemas/page/**", "/api/cinemas/{id}").permitAll()
                        // 影院管理功能 (CINEMA_LIST_ROLES)：仅 SUPER_ADMIN 可访问增删改
                        .requestMatchers(HttpMethod.POST, "/api/cinemas").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/cinemas/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/cinemas/**").hasRole("SUPER_ADMIN")
                        // ADMIN 和 STAFF 只能访问获取绑定影院的接口
                        .requestMatchers("/api/cinemas/all", "/api/cinemas/my-cinema").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                        .requestMatchers("/api/cinemas/**").hasRole("SUPER_ADMIN")
                        // 影厅管理 (HALL_LIST_ROLES)：SUPER_ADMIN, ADMIN, STAFF
                        .requestMatchers("/api/halls/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                        // 总影片管理 (MOVIE_PAGE_ROLES)：仅 SUPER_ADMIN
                        // 注意：保持 /api/movies 为公开，但管理接口仅 SUPER_ADMIN
                        .requestMatchers("/api/movies/admin/**").hasRole("SUPER_ADMIN")
                        // 影片-影院绑定 (CINEMA_MOVIE_ROLES)：SUPER_ADMIN, ADMIN, STAFF
                        .requestMatchers("/api/admin/cinema-movies/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                        // 排片查询：允许所有用户访问（用于购票页面）
                        .requestMatchers(HttpMethod.GET, "/api/showtimes/**").permitAll()
                        // 排片管理 (SCHEDULE_PAGE_ROLES)：SUPER_ADMIN, ADMIN, STAFF（增删改）
                        .requestMatchers(HttpMethod.POST, "/api/showtimes/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/showtimes/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/showtimes/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                        // 订单管理 (ORDER_PAGE_ROLES)：SUPER_ADMIN, ADMIN, STAFF
                        // 用户查看自己的订单：所有已认证用户可访问
                        .requestMatchers("/api/orders", "/api/orders/{id}", "/api/orders/{id}/cancel", "/api/orders/{id}/pay").authenticated()
                        // 管理后台订单列表：仅管理员
                        .requestMatchers("/api/orders/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                        // 用户钱包：已认证用户可访问
                        .requestMatchers("/api/user/wallet/**").authenticated()
                        // 用户昵称修改：已认证用户可访问
                        .requestMatchers("/api/admin/users/nickname").authenticated()
                        // 用户管理 (USER_PAGE_ROLES)：仅 SUPER_ADMIN
                        // 但 /me/cinema 需要所有已认证用户可访问
                        .requestMatchers("/api/admin/users/me/cinema").authenticated()
                        .requestMatchers("/api/admin/users/**").hasRole("SUPER_ADMIN")
                        // 用户模块：已认证用户可访问（包括收藏功能）
                        .requestMatchers("/api/users/**", "/api/user/**").authenticated()
                        .requestMatchers("/api/user-collections/**").authenticated()
                        // 设置 (SETTINGS_PAGE_ROLES)：SUPER_ADMIN, ADMIN, STAFF
                        .requestMatchers("/api/settings/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "STAFF")
                        // 标签接口：已认证用户可访问
                        .requestMatchers("/api/tags/**").authenticated()
                        // 影评接口：已认证用户可访问
                        .requestMatchers("/api/movie-reviews/**").authenticated()
                        // 座位接口：已认证用户可访问
                        .requestMatchers("/api/seats/**").authenticated()
                        // 默认：任意已认证用户
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
