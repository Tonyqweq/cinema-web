package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("admin_cinema_relation")
public class AdminCinemaRelation {

    @TableId(type = IdType.AUTO)
    private Long id; // 主键

    @TableField("admin_id")
    private Integer adminId; // 管理员ID

    @TableField("cinema_id")
    private Long cinemaId; // 影院ID

    @TableField("created_at")
    private Date createdAt; // 创建时间

    @TableField("updated_at")
    private Date updatedAt; // 更新时间
}
