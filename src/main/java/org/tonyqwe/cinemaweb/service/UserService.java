package org.tonyqwe.cinemaweb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

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
    
    /**
     * 修改密码
     */
    ResponseResult<?> changePassword(String oldPassword, String newPassword);
    
    /**
     * 修改手机号
     */
    ResponseResult<?> changePhone(String phone, String verificationCode);
    
    /**
     * 修改邮箱
     */
    ResponseResult<?> changeEmail(String email, String verificationCode);

    /**
     * 获取用户总数
     */
    long count();
}
