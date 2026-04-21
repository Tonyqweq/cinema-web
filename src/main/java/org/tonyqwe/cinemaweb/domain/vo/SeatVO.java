package org.tonyqwe.cinemaweb.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SeatVO {
    private Long id;
    private Long hallId;
    private Integer rowNumber;
    private Integer columnNumber;
    private String seatNumber;
    private Integer seatType;
    private String seatTypeText;
    private Integer status;
    private String statusText;
    private BigDecimal price;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    public String getSeatTypeText() {
        switch (seatType) {
            case 1: return "普通座";
            case 2: return "黄金座";
            case 3: return "VIP座";
            case 4: return "其他";
            default: return "未知";
        }
    }

    public String getStatusText() {
        switch (status) {
            case 1: return "可选";
            case 2: return "已售";
            case 3: return "已锁定";
            case 4: return "维修中";
            default: return "未知";
        }
    }
}
