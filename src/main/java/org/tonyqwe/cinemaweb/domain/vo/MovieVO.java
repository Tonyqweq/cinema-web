package org.tonyqwe.cinemaweb.domain.vo;

import lombok.Data;

import java.util.Date;

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
}