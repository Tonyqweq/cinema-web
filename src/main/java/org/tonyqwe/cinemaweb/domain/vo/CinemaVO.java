package org.tonyqwe.cinemaweb.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 影院信息展示对象
 */
@Data
public class CinemaVO {

    private Long id;
    private String name;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer status;
    private Date createdAt;
    private Date updatedAt;
}