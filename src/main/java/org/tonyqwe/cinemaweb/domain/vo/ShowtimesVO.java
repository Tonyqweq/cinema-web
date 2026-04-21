package org.tonyqwe.cinemaweb.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 排片VO
 */
@Data
public class ShowtimesVO {

    /**
     * 排片ID
     */
    private Long id;

    /**
     * 影院ID
     */
    private Long cinemaId;

    /**
     * 影院名称
     */
    private String cinemaName;

    /**
     * 影厅ID
     */
    private Long hallId;

    /**
     * 影厅名称
     */
    private String hallName;

    /**
     * 电影ID
     */
    private Long movieId;

    /**
     * 电影名称
     */
    private String movieName;

    /**
     * 电影海报
     */
    private String moviePoster;

    /**
     * 放映开始时间
     */
    private Date startTime;

    /**
     * 放映结束时间
     */
    private Date endTime;

    /**
     * 票价
     */
    private BigDecimal price;

    /**
     * 普通座价格
     */
    private BigDecimal priceNormal;

    /**
     * 黄金座价格
     */
    private BigDecimal priceGolden;

    /**
     * VIP座价格
     */
    private BigDecimal priceVip;

    /**
     * 其他座价格
     */
    private BigDecimal priceOther;

    /**
     * 状态：1=正常，0=取消
     */
    private Integer status;

    /**
     * 状态文本
     */
    private String statusText;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}
