package org.tonyqwe.cinemaweb.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("cinemas")
public class Cinemas {

    @TableId(type = IdType.AUTO)
    private Long id; // 电影ID（主键）

    private String name; // 电影名称（中文名）

    private String phone;//影院电话（可选）

    private String province;//影院电话（可选）

    private String city;//影院电话（可选）

    private String district;//影院电话（可选）

    private String address;//影院联系方式（可选）

    private BigDecimal latitude;//纬度（可选）

    private BigDecimal longitude;//经度（可选）

    private Integer status; // 状态：1=正常营业，0=暂停营业

    @JsonProperty("created_at")
    @TableField("created_at")
    private Date createdAt; // 创建时间

    @JsonProperty("updated_at")
    @TableField("updated_at")
    private Date updatedAt; // 更新时间
}
