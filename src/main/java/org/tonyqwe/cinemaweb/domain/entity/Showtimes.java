package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 排片表
 */
@Data
@TableName("showtimes")
public class Showtimes {

    /**
     * 排片ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 影院ID，关联cinemas表
     */
    private Long cinemaId;

    /**
     * 影厅ID，关联halls表
     */
    private Long hallId;

    /**
     * 电影ID，关联movies表
     */
    private Long movieId;

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
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updatedAt;
}
