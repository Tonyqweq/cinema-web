package org.tonyqwe.cinemaweb.domain.dto;

import lombok.Data;

@Data
public class HallDTO {
    private Long cinemaId;
    private String name;
    private String type;
    private Integer capacity;
    private Integer status;
}
