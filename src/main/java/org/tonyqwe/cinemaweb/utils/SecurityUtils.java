package org.tonyqwe.cinemaweb.utils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public class SecurityUtils {

    /**
     * 获取当前登录用户的用户名
     */
    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }

    /**
     * 检查当前用户是否拥有指定角色
     */
    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    /**
     * 检查当前用户是否是超级管理员
     */
    public static boolean isSuperAdmin() {
        return hasRole("SUPER_ADMIN");
    }

    /**
     * 检查当前用户是否是管理员
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * 检查当前用户是否是管理员
     */
    public static boolean isStaff() {
        return hasRole("STAFF");
    }
}
