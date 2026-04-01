package org.tonyqwe.cinemaweb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district
    ) {
        IPage<Cinemas> result = cinemaService.pageCinemas(page, pageSize, name, province, city, district);
        CinemaPageResponse response = new CinemaPageResponse(result.getTotal(), result.getRecords());
        return ResponseEntity.ok(ResponseResult.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseResult<Cinemas>> get(@PathVariable Long id) {
        Cinemas cinema = cinemaService.getCinemaById(id);
        if (cinema == null) {
            return ResponseEntity.ok(ResponseResult.error("影院不存在"));
        }
        return ResponseEntity.ok(ResponseResult.success(cinema));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> update(@PathVariable Long id, @RequestBody Cinemas cinema) {
        cinema.setId(id);
        boolean success = cinemaService.saveCinema(cinema);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("更新失败"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> delete(@PathVariable Long id) {
        boolean success = cinemaService.deleteCinema(id);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("删除失败"));
        }
    }

    @PostMapping
    public ResponseEntity<ResponseResult<Void>> add(@RequestBody Cinemas cinema) {
        boolean success = cinemaService.saveCinema(cinema);
        if (success) {
            return ResponseEntity.ok(ResponseResult.success());
        } else {
            return ResponseEntity.ok(ResponseResult.error("添加失败"));
        }
    }
}
