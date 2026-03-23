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
@TableName("movie")
public class Movie {

    @TableId(type = IdType.AUTO)
    private Long id; // 电影ID（主键）

    private String title; // 电影名称（中文名）

    @JsonProperty("original_title")
    @TableField("original_title")
    private String originalTitle; // 原名（可选）

    private String language; // 语言（可选）

    private String country; // 国家/地区（可选）

    @JsonProperty("duration_min")
    @TableField("duration_min")
    private Integer durationMin; // 片长（分钟）

    @JsonProperty("release_date")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    @TableField("release_date")
    private Date releaseDate; // 上映日期（可选）

    private String description; // 剧情简介（可选）

    @JsonProperty("poster_url")
    @TableField("poster_url")
    private String posterUrl; // 海报URL（可选）

    @JsonProperty("trailer_url")
    @TableField("trailer_url")
    private String trailerUrl; // 预告片URL（可选）

    private Integer status; // 状态：1=上架可售，0=下架不可售

    @JsonProperty("created_at")
    @TableField("created_at")
    private Date createdAt; // 创建时间

    @JsonProperty("updated_at")
    @TableField("updated_at")
    private Date updatedAt; // 更新时间
}
