package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("order_items")
public class OrderItems {

    /**
     * 订单详情ID
     */
    private Long id;

    /**
     * 订单ID，关联orders表
     */
    private Long orderId;

    /**
     * 座位ID，关联seats表
     */
    private Long seatId;

    /**
     * 座位单价
     */
    private BigDecimal price;

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
