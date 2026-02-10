package org.tonyqwe.cinemaweb.config;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.tonyqwe.cinemaweb.domain.entity.sysUsers;
import org.tonyqwe.cinemaweb.service.LoginService;
import org.tonyqwe.cinemaweb.service.PermissionService;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 Spring Security 的 JWT 过滤器：
 * - 从 Authorization: Bearer xxx 中解析 token
 * - 使用 JwtUtil + Redis 校验 token
 * - 校验通过后，将当前用户信息放入 Spring Security 上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private LoginService loginService;

    @Resource
    private PermissionService permissionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // 如果上下文中还没有认证信息，则尝试根据 token 认证
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            sysUsers currentUser = loginService.getCurrentUser(token);
            if (currentUser != null) {
                // 查询该用户的权限编码，封装为 Spring Security 的 GrantedAuthority
                List<String> codes = permissionService.getPermissionCodesByUserId(currentUser.getId());
                List<GrantedAuthority> authorities = codes.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                currentUser.getUsername(),
                                null,
                                authorities
                        );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
