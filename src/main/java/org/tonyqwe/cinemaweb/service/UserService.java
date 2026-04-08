package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;

/**
 * 用户基础服务：按用户名查询、保存等。
 */
public interface UserService extends IService<SysUsers> {

    SysUsers getByUsername(String username);
    
    /**
     * 检查用户是否为超级管理员
     */
    boolean isSuperAdmin(String username);
    
    /**
     * 检查用户是否为管理员
     */
    boolean isAdmin(String username);
}
