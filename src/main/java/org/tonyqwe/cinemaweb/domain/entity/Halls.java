package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
@TableName("halls")
public class Halls {

    @TableId(type = IdType.AUTO)
    private Long id; // 影厅ID（主键）

    private Long cinemaId; // 影院ID（外键，关联到cinemas表的id）

    private String name; // 影厅名称

    private String type; // 影厅类型（如：IMAX、3D、2D、4DX等）

    private Integer capacity; // 座位数

    private Integer status; // 状态：1=正常营业，0=暂停营业

    @JsonProperty("created_at")
    @TableField("created_at")
    private Date createdAt; // 创建时间

    @JsonProperty("updated_at")
    @TableField("updated_at")
    private Date updatedAt; // 更新时间
}
