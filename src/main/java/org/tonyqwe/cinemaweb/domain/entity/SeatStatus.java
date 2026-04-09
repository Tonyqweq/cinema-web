package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("seat_status")
public class SeatStatus {

    /**
     * 座位状态ID
     */
    private Long id;

    /**
     * 排片ID，关联showtimes表
     */
    private Long showtimeId;

    /**
     * 座位ID，关联seats表
     */
    private Long seatId;

    /**
     * 座位状态：0-空闲，1-已锁定，2-已售出
     */
    private Integer status;

    /**
     * 锁定过期时间
     */
    private Date lockExpireTime;

    /**
     * 创建时间
     */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private Date createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private Date updatedAt;

}
