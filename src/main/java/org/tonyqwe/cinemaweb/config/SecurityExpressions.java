package org.tonyqwe.cinemaweb.config;

import java.util.List;

/**
 * 将 sys_roles.name 转为 Spring Security 使用的权限串（ROLE_ 前缀）。
 */
public final class SecurityExpressions {

    private SecurityExpressions() {
    }

    public static String[] toRoleAuthorities(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            // 保证 hasAnyAuthority 至少有一个占位，避免无参；无人拥有该角色即等效拒绝整段路径
            return new String[] { "ROLE____CONFIGURE_APP_SECURITY_ROLES____" };
        }
        return roleNames.stream()
                .filter(n -> n != null && !n.isBlank())
                .map(String::trim)
                .map(n -> "ROLE_" + n)
                .distinct()
                .toArray(String[]::new);
    }
}
