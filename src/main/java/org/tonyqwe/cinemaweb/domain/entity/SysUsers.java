package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "sys_users")
public class SysUsers {

    @TableId(type = IdType.AUTO)
    private Integer id;             // 主键 ID

    @TableField("username")
    private String username;        // 用户名

    @TableField("password")
    private String password;        // BCrypt 加密后的密码

    @TableField("email")
    private String email;           // 邮箱

    @TableField("phone")
    private String phone;           // 手机号

    @TableField("status")
    private Integer status;         // 状态（0-禁用，1-启用）

    @TableField("gender")
    private Integer gender;         // 性别（0-男，1-女）

    @TableField("created_at")
    private Date createdAt;         // 创建日期

    @TableField("update_time")
    private Date updateTime;         // 创建日期
}
