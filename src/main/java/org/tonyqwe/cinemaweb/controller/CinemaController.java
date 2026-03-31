package org.tonyqwe.cinemaweb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tonyqwe.cinemaweb.domain.dto.CinemaPageResponse;
import org.tonyqwe.cinemaweb.domain.entity.Cinemas;
import org.tonyqwe.cinemaweb.service.CinemaService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;


@RestController
@RequestMapping("/api/cinemas")
public class CinemaController {
    @Resource
    private CinemaService cinemaService;

    @GetMapping
    public ResponseEntity<ResponseResult<CinemaPageResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String name
    ) {
        IPage<Cinemas> result = cinemaService.pageCinemas(page, pageSize, name);
        CinemaPageResponse response = new CinemaPageResponse(result.getTotal(), result.getRecords());
        return ResponseEntity.ok(ResponseResult.success(response));
    }
}
