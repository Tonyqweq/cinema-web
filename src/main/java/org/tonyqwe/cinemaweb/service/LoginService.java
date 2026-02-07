package org.tonyqwe.cinemaweb.service;

import org.tonyqwe.cinemaweb.domain.dto.LoginRequest;
import org.tonyqwe.cinemaweb.domain.entity.sysUsers;

public interface LoginService {
    /**
     * 登录：
     * 1. 如果 Redis 中已有该账号的有效 token，则直接返回该 token；
     * 2. 否则验证用户名/密码，生成新 token 写入 Redis 后返回。
     */
    String login(LoginRequest request);

    /**
     * 根据 token 获取当前登录用户：
     * 1. 使用 JwtUtil 解析出用户名；
     * 2. 校验 Redis 中存储的 token 是否与当前 token 一致；
     * 3. 返回用户信息（密码字段置空），无效时返回 null。
     */
    sysUsers getCurrentUser(String token);

    /**
     * 登出：
     * 1. 从 token 解析出用户名；
     * 2. 删除 Redis 中对应的 login:token:{username}；
     * 3. 不抛异常，静默处理。
     */
    void logout(String token);
}
