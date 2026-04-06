package org.tonyqwe.cinemaweb.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeatDTO {
    private Long hallId;
    private Integer rowNumber;
    private Integer columnNumber;
    private String seatNumber;
    private Integer seatType;
    private Integer status;
    private BigDecimal price;
}
