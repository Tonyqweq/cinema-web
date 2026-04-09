package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 订单实体类
 */
@Data
@TableName("orders")
public class Orders {

    /**
     * 订单ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 场次ID
     */
    @TableField("showtime_id")
    private Long showtimeId;

    /**
     * 订单状态：0-待支付，1-已支付，2-已完成，3-已取消
     */
    @TableField("order_status")
    private Integer orderStatus;

    /**
     * 总金额
     */
    @TableField("total_price")
    private Double totalPrice;

    /**
     * 座位ID列表，JSON格式
     */
    @TableField("seats")
    private String seats;

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

    /**
     * 关联的用户信息
     */
    @TableField(exist = false)
    private SysUsers user;

    /**
     * 关联的场次信息
     */
    @TableField(exist = false)
    private Showtimes showtime;

    /**
     * 关联的座位信息列表
     */
    @TableField(exist = false)
    private List<Seats> seatList;
}
