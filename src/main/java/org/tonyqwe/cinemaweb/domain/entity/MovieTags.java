package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
@TableName("movie_tags")
public class MovieTags {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("movie_id")
    private Long movieId;

    @TableField("tag_id")
    private Long tagId;

    @JsonProperty("created_at")
    @TableField("created_at")
    private Date createdAt;
}