package org.tonyqwe.cinemaweb.service;

import java.util.List;

public interface PermissionService {

    /**
     * 根据用户 ID 查询其拥有的全部权限编码集合（去重），例如 ["movie:view", "user:edit"]。
     */
    List<String> getPermissionCodesByUserId(int userId);

    /**
     * 根据用户 ID 查询角色 name 列表（与 sys_roles.name 一致），用于 ROLE_ 前缀授权与前端菜单。
     */
    List<String> getRoleNamesByUserId(int userId);
}
