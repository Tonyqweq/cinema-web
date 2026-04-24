package org.tonyqwe.cinemaweb.domain.vo;

import lombok.Data;
import org.tonyqwe.cinemaweb.domain.entity.Tags;

import java.util.Date;
import java.util.List;

/**
 * 电影信息展示对象
 */
@Data
public class MovieVO {

    private Long id;
    private String title;
    private String originalTitle;
    private String language;
    private String country;
    private Integer durationMin;
    private Date releaseDate;
    private String description;
    private String posterUrl;
    private String trailerUrl;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
    private Double rating;
    private List<Tags> tags;
    private Integer reviewCount;
}