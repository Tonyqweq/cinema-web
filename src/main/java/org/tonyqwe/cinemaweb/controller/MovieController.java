package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.MoviePageResponse;
import org.tonyqwe.cinemaweb.domain.dto.UpdateMovieStatusRequest;
import org.tonyqwe.cinemaweb.domain.entity.Movie;
import org.tonyqwe.cinemaweb.service.MovieService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

import com.baomidou.mybatisplus.core.metadata.IPage;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Resource
    private MovieService movieService;

    /**
     * 分页查询电影列表
     * GET /api/movies?page=1&pageSize=10
     */
    @GetMapping
    public ResponseEntity<ResponseResult<MoviePageResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        IPage<Movie> result = movieService.pageMovies(page, pageSize);
        MoviePageResponse response = new MoviePageResponse(result.getTotal(), result.getRecords());
        return ResponseEntity.ok(ResponseResult.success(response));
    }

    /**
     * 获取电影详情
     * GET /api/movies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseResult<Movie>> detail(@PathVariable("id") Long id) {
        Movie movie = movieService.getMovieById(id);
        if (movie == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(404, "movie not found"));
        }
        return ResponseEntity.ok(ResponseResult.success(movie));
    }

    /**
     * 更新电影状态（0=下架不可售，1=上架可售）
     * PUT /api/movies/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ResponseResult<Movie>> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateMovieStatusRequest request
    ) {
        Movie updated = movieService.updateMovieStatus(id, request.getStatus());
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(404, "movie not found"));
        }
        return ResponseEntity.ok(ResponseResult.success(updated));
    }
}