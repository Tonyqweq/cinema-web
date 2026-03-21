package org.tonyqwe.cinemaweb.service;

/**
 * Token 管理：生成、解析、Redis 缓存与删除。
 */
public interface TokenService {

    String generateToken(String username);

    String parseUsername(String token);

    void cacheToken(String username, String token);

    String getCachedToken(String username);

    void deleteToken(String username);

    boolean validate(String username, String token);
}
