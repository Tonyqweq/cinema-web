package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user_role")
public class SysUserRole {

    private Integer userId;

    private Integer roleId;
}
