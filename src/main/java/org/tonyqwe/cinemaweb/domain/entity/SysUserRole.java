package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_user_role")
public class SysUserRole {

    @TableId
    private Integer id;

    private Integer userId;

    private Integer roleId;
}
