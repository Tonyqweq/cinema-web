package org.tonyqwe.cinemaweb.service;

import org.tonyqwe.cinemaweb.domain.dto.RegisterRequest;

/**
 * 注册服务：用户名校验、密码加密、落库。
 */
public interface RegisterService {

    String register(RegisterRequest request);
}
