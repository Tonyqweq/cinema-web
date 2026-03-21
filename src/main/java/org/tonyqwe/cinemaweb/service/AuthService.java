package org.tonyqwe.cinemaweb.service;

import org.tonyqwe.cinemaweb.domain.dto.LoginRequest;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;

/**
 * 认证服务：登录、登出、当前会话查询。
 */
public interface AuthService {

    String login(LoginRequest request);

    void logout(String token);

    SysUsers getCurrentUser(String token);
}
