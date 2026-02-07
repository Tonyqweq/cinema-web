package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName(value = "sys_users")
public class sysUsers {

// 如果不自主设置自增，而你往数据库添加新值的时候不传Id，那么就会走ASSIGN_ID，雪花算法
    @TableId(type = IdType.AUTO)
    private int id;             // 主键ID
    private String username;    // 编号
    private String password;    // 姓名
    private String email;       // 密码
    private int status;         // 性别（0-禁用, 1-启用）
    private int gender;         // 性别（0-男，1-女）
    @TableField("created_at")
    private Date createdAt;     //创建日期


}
