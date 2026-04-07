package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("cinema_movie_relation")
public class CinemaMovieRelation {

    @TableId(type = IdType.AUTO)
    private Long id; // 主键

    @TableField("cinema_id")
    private Long cinemaId; // 影院ID

    @TableField("movie_id")
    private Long movieId; // 电影ID

    @TableField("created_at")
    private Date createdAt; // 创建时间

    @TableField("updated_at")
    private Date updatedAt; // 更新时间
}
