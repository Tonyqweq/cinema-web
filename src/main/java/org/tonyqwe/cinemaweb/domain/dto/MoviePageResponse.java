package org.tonyqwe.cinemaweb.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tonyqwe.cinemaweb.domain.vo.MovieVO;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoviePageResponse {
    private Long total;
    private List<MovieVO> records;
}

