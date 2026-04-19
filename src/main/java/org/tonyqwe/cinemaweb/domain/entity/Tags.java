package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
@TableName("tags")
public class Tags {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String color;

    @JsonProperty("created_at")
    @TableField("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    @TableField("updated_at")
    private Date updatedAt;
}