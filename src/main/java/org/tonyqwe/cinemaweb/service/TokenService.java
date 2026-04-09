package org.tonyqwe.cinemaweb.service;

/**
 * Token 管理：生成、解析、Redis 缓存与删除。
 */
public interface TokenService {

    /**
     * 生成token
     * @param username 用户名
     * @param userId 用户ID
     * @return token
     */
    String generateToken(String username, Long userId);

    String parseUsername(String token);

    void cacheToken(String username, String token);

    String getCachedToken(String username);

    void deleteToken(String username);

    boolean validate(String username, String token);

    /**
     * 从token中获取用户ID
     * @param token token
     * @return 用户ID
     */
    Long getUserIdFromToken(String token);
}
