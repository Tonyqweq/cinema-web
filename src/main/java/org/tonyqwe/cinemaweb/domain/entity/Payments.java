package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("payments")
public class Payments {

    /**
     * 支付ID
     */
    private Long id;

    /**
     * 订单ID，关联orders表
     */
    private Long orderId;

    /**
     * 支付金额
     */
    private BigDecimal paymentAmount;

    /**
     * 支付方式：1-微信，2-支付宝，3-银行卡
     */
    private Integer paymentMethod;

    /**
     * 支付状态：0-待支付，1-支付成功，2-支付失败
     */
    private Integer paymentStatus;

    /**
     * 第三方支付平台交易ID
     */
    private String transactionId;

    /**
     * 支付时间
     */
    private Date paymentTime;

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
