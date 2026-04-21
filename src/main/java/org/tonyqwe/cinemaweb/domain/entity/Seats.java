package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;


import java.util.Date;

@Data
@TableName("seats")
public class Seats {

    @TableId(type = IdType.AUTO)
    private Long id; // 座位ID（主键）

    private Long hallId; // 影厅ID（外键，关联到halls表的id）

    private Integer rowNumber; // 行号

    private Integer columnNumber; // 列号

    private String seatNumber; // 座位号（如A1、A2、B1等）

    private Integer seatType; // 座位类型：1=普通座，2=黄金座，3=VIP座，4=其他

    private Integer status; // 座位状态：1=可选，2=已售，3=已锁定，4=维修中

    private java.math.BigDecimal price; // 座位价格

    @JsonProperty("created_at")
    @TableField("created_at")
    private Date createdAt; // 创建时间

    @JsonProperty("updated_at")
    @TableField("updated_at")
    private Date updatedAt; // 更新时间
}
