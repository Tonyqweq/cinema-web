package org.tonyqwe.cinemaweb.service;

import java.util.List;

public interface PermissionService {

    /**
     * 根据用户 ID 查询其拥有的全部权限编码集合（去重），例如 ["movie:view", "user:edit"]。
     */
    List<String> getPermissionCodesByUserId(int userId);
}
