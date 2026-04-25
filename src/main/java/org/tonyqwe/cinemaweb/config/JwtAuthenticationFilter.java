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
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.service.AuthService;
import org.tonyqwe.cinemaweb.service.PermissionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 Spring Security 的 JWT 过滤器：
 * - 从 Authorization: Bearer xxx 中解析 token
 * - 使用 TokenService + Redis 校验 token
 * - 校验通过后，将当前用户信息放入 Spring Security 上下文
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Resource
    private AuthService authService;

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
            try {
                SysUsers currentUser = authService.getCurrentUser(token);
                if (currentUser != null) {
                    // 权限码（细粒度） + 角色名 ROLE_ 前缀（与 sys_roles.name 一致，供页面/API 区域鉴权）
                    List<String> codes = new ArrayList<>();
                    List<String> roleNames = new ArrayList<>();
                    try {
                        codes = permissionService.getPermissionCodesByUserId(currentUser.getId());
                    } catch (Exception e) {
                        System.err.println("获取用户权限码失败: " + e.getMessage());
                    }
                    try {
                        roleNames = permissionService.getRoleNamesByUserId(currentUser.getId());
                    } catch (Exception e) {
                        System.err.println("获取用户角色失败: " + e.getMessage());
                    }
                    List<GrantedAuthority> authorities = codes.stream()
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());
                    for (String roleName : roleNames) {
                        if (roleName != null && !roleName.isBlank()) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName.trim()));
                        }
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    currentUser.getUsername(),
                                    null,
                                    authorities
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                System.err.println("JWT认证过程发生异常: " + e.getMessage());
                // 不阻止请求继续，让后续的安全检查处理
            }
        }

        filterChain.doFilter(request, response);
    }
}
