package org.tonyqwe.cinemaweb.domain.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 排片DTO
 */
@Data
public class ShowtimesDTO {

    /**
     * 排片ID，用于更新操作
     */
    private Long id;

    /**
     * 影院ID
     */
    private Long cinemaId;

    /**
     * 影厅ID
     */
    private Long hallId;

    /**
     * 电影ID
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
     * 状态：1=正常，0=取消
     */
    private Integer status;
}
