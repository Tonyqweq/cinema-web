package org.tonyqwe.cinemaweb.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * 排片分页响应
 */
@Data
public class ShowtimesPageResponse {

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 排片列表
     */
    private List<org.tonyqwe.cinemaweb.domain.vo.ShowtimesVO> records;

    public ShowtimesPageResponse(Long total, List<org.tonyqwe.cinemaweb.domain.vo.ShowtimesVO> records) {
        this.total = total;
        this.records = records;
    }
}
