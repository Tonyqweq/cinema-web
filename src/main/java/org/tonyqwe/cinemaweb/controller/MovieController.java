package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.MovieBodyRequest;
import org.tonyqwe.cinemaweb.domain.dto.MovieImportResult;
import org.tonyqwe.cinemaweb.domain.dto.MoviePageResponse;
import org.tonyqwe.cinemaweb.domain.dto.UpdateMovieStatusRequest;
import org.tonyqwe.cinemaweb.domain.entity.Movies;
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
     * GET /api/movies?page=1&pageSize=10&title=xxx&language=中文&country=中国&sortBy=duration_min&sortOrder=desc
     */
    @GetMapping
    public ResponseEntity<ResponseResult<MoviePageResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder
    ) {
        IPage<Movies> result = movieService.pageMovies(page, pageSize, title, language, country, sortBy, sortOrder);
        MoviePageResponse response = new MoviePageResponse(result.getTotal(), result.getRecords());
        return ResponseEntity.ok(ResponseResult.success(response));
    }

    /**
     * 获取筛选项（语言/国家地区）
     * GET /api/movies/filters
     */
    @GetMapping("/filters")
    public ResponseEntity<ResponseResult<java.util.Map<String, Object>>> filters() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("languages", movieService.listLanguages());
        map.put("countries", movieService.listCountries());
        return ResponseEntity.ok(ResponseResult.success(map));
    }

    /**
     * 新增电影（无需 id，状态默认上架）
     * POST /api/movies
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseResult<Movies>> create(@RequestBody @Valid MovieBodyRequest request) {
        Movies created = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseResult.success(created));
    }

    /**
     * 从 Excel 批量导入（首行可为表头：title, original_title, language, country, duration_min, release_date, description, poster_url, trailer_url）
     * POST /api/movies/import
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseResult<MovieImportResult>> importExcel(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) throws java.io.IOException {
        MovieImportResult result = movieService.importMoviesFromExcel(file);
        return ResponseEntity.ok(ResponseResult.success(result));
    }

    /**
     * 获取电影详情
     * GET /api/movies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseResult<Movies>> detail(@PathVariable("id") Long id) {
        Movies movie = movieService.getMovieById(id);
        if (movie == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(404, "movie not found"));
        }
        return ResponseEntity.ok(ResponseResult.success(movie));
    }

    /**
     * 更新电影信息（不可改 id、status）
     * PUT /api/movies/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ResponseResult<Movies>> updateInfo(
            @PathVariable("id") Long id,
            @RequestBody @Valid MovieBodyRequest request
    ) {
        Movies updated = movieService.updateMovieInfo(id, request);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(404, "movie not found"));
        }
        return ResponseEntity.ok(ResponseResult.success(updated));
    }

    /**
     * 更新电影状态（0=下架不可售，1=上架可售）
     * PUT /api/movies/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ResponseResult<Movies>> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateMovieStatusRequest request
    ) {
        Movies updated = movieService.updateMovieStatus(id, request.getStatus());
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(404, "movie not found"));
        }
        return ResponseEntity.ok(ResponseResult.success(updated));
    }

    /**
     * 删除电影
     * DELETE /api/movies/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> delete(@PathVariable("id") Long id) {
        boolean ok = movieService.deleteMovie(id);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(404, "movie not found"));
        }
        return ResponseEntity.ok(ResponseResult.success("deleted", null));
    }
}