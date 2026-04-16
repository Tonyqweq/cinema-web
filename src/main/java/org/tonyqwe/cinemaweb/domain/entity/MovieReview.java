package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("movie_reviews")
public class MovieReview {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long movieId;
    
    private Long userId;
    
    private Integer rating;
    
    private String comment;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
