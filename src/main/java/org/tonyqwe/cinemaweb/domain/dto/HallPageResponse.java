package org.tonyqwe.cinemaweb.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tonyqwe.cinemaweb.domain.vo.HallVO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallPageResponse {
    private Long total;
    private List<HallVO> records;
}
