package org.tonyqwe.cinemaweb.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;

import java.util.List;

/**
 * 当前会话：用户基本信息 + 角色名（与 sys_roles.name 一致）+ 权限码。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoResponse {

    private SysUsers user;
    /**
     * 角色 name 列表，例如 ["SUPER_ADMIN", "MOVIE_ADMIN"]，与库中 sys_roles.name 一致。
     */
    private List<String> roles;
    private List<String> permissions;
}
