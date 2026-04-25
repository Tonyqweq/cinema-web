package org.tonyqwe.cinemaweb.domain.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 创建用户请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    private String username;
    private String password;
    private String email;
    private Integer gender;
    private Integer status;
    private Integer roleId;
}
