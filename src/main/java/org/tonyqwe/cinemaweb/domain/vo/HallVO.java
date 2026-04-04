package org.tonyqwe.cinemaweb.domain.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

@Data
public class HallVO {
    private Long id;
    private Long cinemaId;
    private String cinemaName;
    private String name;
    private String type;
    private Integer capacity;
    private Integer status;
    private String statusText;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    public String getStatusText() {
        return status == 1 ? "正常营业" : "暂停营业";
    }
}
