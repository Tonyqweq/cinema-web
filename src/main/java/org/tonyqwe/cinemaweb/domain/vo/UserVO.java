package org.tonyqwe.cinemaweb.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息展示对象
 */
@Data
public class UserVO {

    private Integer id;
    private String username;
    private String email;
    private Integer gender;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updateTime;
    private String avatar;           // 头像URL
    private List<RoleVO> roles;
}
