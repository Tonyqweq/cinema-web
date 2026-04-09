package org.tonyqwe.cinemaweb.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 订单请求DTO
 */
@Data
public class OrderRequest {

    /**
     * 场次ID
     */
    private Long showtimeId;

    /**
     * 座位ID列表
     */
    private List<Long> seats;

    /**
     * 总金额
     */
    private Double totalPrice;
}
