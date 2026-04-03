package org.tonyqwe.cinemaweb.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tonyqwe.cinemaweb.domain.vo.CinemaVO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CinemaPageResponse {
    private Long total;
    private List<CinemaVO> records;
}
