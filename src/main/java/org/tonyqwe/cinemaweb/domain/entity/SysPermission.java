package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("sys_permissions")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Integer id;

    private String name;

    /**
     * 权限标识码，例如 "movie:view"、"user:edit"
     */
    private String code;
}
