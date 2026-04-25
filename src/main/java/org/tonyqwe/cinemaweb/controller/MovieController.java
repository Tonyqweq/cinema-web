package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.tonyqwe.cinemaweb.domain.dto.MovieBodyRequest;
import org.tonyqwe.cinemaweb.domain.dto.MovieImportResult;
import org.tonyqwe.cinemaweb.domain.dto.MoviePageResponse;
import org.tonyqwe.cinemaweb.domain.dto.UpdateMovieStatusRequest;
import org.tonyqwe.cinemaweb.domain.entity.Movies;
import org.tonyqwe.cinemaweb.domain.vo.MovieVO;
import org.tonyqwe.cinemaweb.service.AdminCinemaRelationService;
import org.tonyqwe.cinemaweb.service.MovieService;
import org.tonyqwe.cinemaweb.service.impl.MovieServiceImpl;
import org.tonyqwe.cinemaweb.service.CinemaMovieRelationService;
import org.tonyqwe.cinemaweb.service.MinioService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Calendar;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/api/movies")
public class MovieController {

    @Resource
    private MovieService movieService;

    @Resource
    private CinemaMovieRelationService cinemaMovieRelationService;
    
    @Resource
    private MinioService minioService;

    @Resource
    private org.tonyqwe.cinemaweb.service.TagService tagService;

    @Resource
    private AdminCinemaRelationService adminCinemaRelationService;

    /**
     * 分页查询电影列表
     * GET /api/movies?page=1&pageSize=10&title=xxx&language=中文&country=中国&sortBy=duration_min&sortOrder=desc&tagIds=1,2,3
     */
    @GetMapping
    public ResponseEntity<ResponseResult<MoviePageResponse>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String tagIds
    ) {
        List<Movies> movies;
        
        // 如果有标签筛选参数
        if (tagIds != null && !tagIds.isBlank()) {
            List<Long> tagIdList = java.util.Arrays.stream(tagIds.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            movies = movieService.getMoviesByTagIds(tagIdList);
        } else {
            IPage<Movies> result = movieService.pageMovies(page, pageSize, title, language, country, sortBy, sortOrder);
            movies = result.getRecords();
        }
        
        List<MovieVO> movieVOs = movies.stream().map(this::convertToVO).collect(Collectors.toList());
        
        // 如果有标签筛选且不是分页查询，则movies数量就是总数；否则需要分页
        long total = movies.size();
        if (tagIds == null || tagIds.isBlank()) {
            IPage<Movies> result = movieService.pageMovies(page, pageSize, title, language, country, sortBy, sortOrder);
            total = result.getTotal();
        }
        
        MoviePageResponse response = new MoviePageResponse(total, movieVOs);
        return ResponseEntity.ok(ResponseResult.success(response));
    }

    /**
     * 获取筛选项（语言/国家地区/标签）
     * GET /api/movies/filters     
     */
    @GetMapping("/filters")
    public ResponseEntity<ResponseResult<Map<String, Object>>> filters() {      
        Map<String, Object> map = new HashMap<>();
        map.put("languages", movieService.listLanguages());
        map.put("countries", movieService.listCountries());
        map.put("tags", tagService.getAllTags());
        return ResponseEntity.ok(ResponseResult.success(map));
    }

    /**
     * 新增电影（无需 id,状态默认上架）
     * POST /api/movies
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')") // 总影片管理仅允许 SUPER_ADMIN
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResponseResult<MovieVO>> create(@RequestBody @Valid MovieBodyRequest request) {
        Movies created = movieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseResult.success(convertToVO(created)));
    }

    /**
     * 从 Excel 批量导入（首行可为表头：title, original_title, language, country, duration_min, release_date, description, poster_url, trailer_url）
     * POST /api/movies/import
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')") // 总影片管理仅允许 SUPER_ADMIN
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseResult<MovieImportResult>> importExcel(       
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        MovieImportResult result = movieService.importMoviesFromExcel(file);    
        return ResponseEntity.ok(ResponseResult.success(result));
    }

    /**
     * 上传海报
     * POST /api/movies/upload-poster
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')") // 总影片管理仅允许 SUPER_ADMIN
    @PostMapping("/upload-poster")
    public ResponseEntity<?> uploadPoster(@RequestParam("file") MultipartFile file) {
        try {
            String url = minioService.uploadFile(file);
            return ResponseEntity.ok(ResponseResult.success(url));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "上传失败：" + e.getMessage()));
        }
    }

    /**
     * 获取电影详情
     * GET /api/movies/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ResponseResult<MovieVO>> detail(@PathVariable("id") Long id) {
        Movies movie = movieService.getMovieById(id);
        if (movie == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(404, "movie not found"));        
        }
        return ResponseEntity.ok(ResponseResult.success(convertToVO(movie)));   
    }

    /**
     * 更新电影信息（不可改 id,status）
     * PUT /api/movies/{id}   
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')") // 总影片管理仅允许 SUPER_ADMIN
    @PutMapping("/{id}")
    public ResponseEntity<ResponseResult<MovieVO>> updateInfo(
            @PathVariable("id") Long id,
            @RequestBody @Valid MovieBodyRequest request
    ) {
        Movies updated = movieService.updateMovieInfo(id, request);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(404, "movie not found"));        
        }
        return ResponseEntity.ok(ResponseResult.success(convertToVO(updated))); 
    }

    /**
     * 更新电影状态（0=下架不可售，1=上架可售）
     * PUT /api/movies/{id}/status
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')") // 总影片管理仅允许 SUPER_ADMIN
    @PutMapping("/{id}/status")
    public ResponseEntity<ResponseResult<MovieVO>> updateStatus(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateMovieStatusRequest request
    ) {
        // 检查电影是否已被绑定到影院，如果是，则不允许下架
        if (request.getStatus() == 0) {
            List<Long> cinemaIds = cinemaMovieRelationService.getCinemaIdsByMovieId(id);
            if (cinemaIds != null && !cinemaIds.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseResult.error(400, "该电影已绑定到 影院，无法下架"));
            }
        }

        Movies updated = movieService.updateMovieStatus(id, request.getStatus());
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(404, "movie not found"));        
        }
        return ResponseEntity.ok(ResponseResult.success(convertToVO(updated))); 
    }

    /**
     * 删除电影
     * DELETE /api/movies/{id}
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')") // 总影片管理仅允许 SUPER_ADMIN
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> delete(@PathVariable("id") Long id) {
        // 检查电影是否已被绑定到影院，如果是，则不允许删除
        List<Long> cinemaIds = cinemaMovieRelationService.getCinemaIdsByMovieId(id);
        if (cinemaIds != null && !cinemaIds.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseResult.error(400, "该电影已绑定到影院，无法删除"));
        }

        boolean ok = movieService.deleteMovie(id);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseResult.error(404, "movie not found"));        
        }
        return ResponseEntity.ok(ResponseResult.success("deleted", null));      
    }

    /**
     * 根据影院ID获取绑定的电影列表
     * GET /api/movies/cinema/{cinemaId}
     * 此接口对所有已认证用户开放，不限制影院访问（购票页面需要）
     */
    @GetMapping("/cinema/{cinemaId}")
    public ResponseEntity<ResponseResult<List<MovieVO>>> getMoviesByCinemaId(@PathVariable("cinemaId") Long cinemaId) {
        // 不限制访问，所有用户都可以查看影院的电影（购票页面需要）
        List<Long> movieIds = cinemaMovieRelationService.getMovieIdsByCinemaId(cinemaId);
        if (movieIds == null || movieIds.isEmpty()) {
            return ResponseEntity.ok(ResponseResult.success(List.of()));        
        }

        List<Movies> movies = movieService.getMoviesByIds(movieIds);
        List<MovieVO> movieVOs = movies.stream().map(this::convertToVO).collect(Collectors.toList());
        return ResponseEntity.ok(ResponseResult.success(movieVOs));
    }

    /**
     * 检查用户是否有权限访问指定影院
     */
    private boolean checkCinemaAccess(Long cinemaId) {
        // SUPER_ADMIN可以访问所有影院
        if (SecurityUtils.isSuperAdmin()) {
            return true;
        }

        // ADMIN和STAFF只能访问其绑定的影院
        if (SecurityUtils.isAdmin() || SecurityUtils.isStaff()) {
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                Long userCinemaId = adminCinemaRelationService.getCinemaIdByAdminUsername(username);
                return userCinemaId != null && userCinemaId.equals(cinemaId);
            }
        }

        return false;
    }

    /**
     * 图片代理接口，用于访问MinIO图片
     * GET /api/movies/proxy-image?url=xxx
     */
    @GetMapping("/proxy-image")
    public ResponseEntity<?> proxyImage(@RequestParam("url") String url) {
        try {
            URL imageUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
            connection.setRequestMethod("GET");
            
            int statusCode = connection.getResponseCode();
            if (statusCode != HttpURLConnection.HTTP_OK) {
                return ResponseEntity.status(statusCode).build();
            }
            
            String contentType = connection.getContentType();
            InputStream inputStream = connection.getInputStream();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(inputStream));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "图片代理失败：" + e.getMessage()));
        }
    }

    private MovieVO convertToVO(Movies movie) {
        MovieVO vo = new MovieVO();
        vo.setId(movie.getId());
        vo.setTitle(movie.getTitle());
        vo.setOriginalTitle(movie.getOriginalTitle());
        vo.setLanguage(movie.getLanguage());
        vo.setCountry(movie.getCountry());
        vo.setDurationMin(movie.getDurationMin());
        vo.setReleaseDate(movie.getReleaseDate());
        vo.setDescription(movie.getDescription());
        vo.setPosterUrl(movie.getPosterUrl());
        vo.setTrailerUrl(movie.getTrailerUrl());
        vo.setStatus(movie.getStatus());
        vo.setRating(movie.getRating());
        vo.setCreatedAt(movie.getCreatedAt());
        vo.setUpdatedAt(movie.getUpdatedAt());
        vo.setTags(tagService.getTagsByMovieId(movie.getId()));
        return vo;
    }
    
    /**
     * 批量获取电影信息
     * GET /api/movies/batch?ids=1,2,3
     */
    @GetMapping("/batch")
    public ResponseEntity<ResponseResult<List<MovieVO>>> getMoviesByIds(@RequestParam("ids") String ids) {
        try {
            // 解析 ids 参数，格式为逗号分隔的数字
            List<Long> movieIds = java.util.Arrays.stream(ids.split(","))
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
            
            if (movieIds.isEmpty()) {
                return ResponseEntity.ok(ResponseResult.success(List.of()));
            }
            
            List<Movies> movies = movieService.getMoviesByIds(movieIds);
            List<MovieVO> movieVOs = movies.stream().map(this::convertToVO).collect(Collectors.toList());
            return ResponseEntity.ok(ResponseResult.success(movieVOs));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "批量获取电影失败：" + e.getMessage()));
        }
    }

    /**
     * 更新电影标签
     * PUT /api/movies/{id}/tags
     */
    @PreAuthorize("hasRole('SUPER_ADMIN')") // 总影片管理仅允许 SUPER_ADMIN
    @PutMapping("/{id}/tags")
    public ResponseEntity<ResponseResult<Void>> updateMovieTags(
            @PathVariable("id") Long id,
            @RequestBody Map<String, List<Long>> request) {
        List<Long> tagIds = request.get("tagIds");
        
        // 先删除该电影的所有标签
        tagService.removeAllTagsFromMovie(id);
        
        // 添加新标签
        if (tagIds != null && !tagIds.isEmpty()) {
            tagService.addTagsToMovie(id, tagIds);
        }
        
        return ResponseEntity.ok(ResponseResult.success("标签更新成功", null));
    }

    /**
     * 随机推荐电影
     * GET /api/movies/random?count=4
     */
    @GetMapping("/random")
    public ResponseEntity<ResponseResult<List<MovieVO>>> getRandomMovies(
            @RequestParam(defaultValue = "4") int count
    ) {
        try {
            // 获取所有上架的电影
            List<Movies> allMovies = movieService.lambdaQuery()
                    .eq(Movies::getStatus, 1)
                    .list();

            if (allMovies.isEmpty()) {
                return ResponseEntity.ok(ResponseResult.success(List.of()));
            }

            // 随机打乱顺序并取指定数量
            List<Movies> randomMovies = allMovies.stream()
                    .collect(java.util.stream.Collectors.collectingAndThen(
                            java.util.stream.Collectors.toList(),
                            list -> {
                                java.util.Collections.shuffle(list);
                                return list;
                            }
                    ))
                    .stream()
                    .limit(Math.min(count, allMovies.size()))
                    .collect(Collectors.toList());

            List<MovieVO> movieVOs = randomMovies.stream().map(this::convertToVO).collect(Collectors.toList());
            return ResponseEntity.ok(ResponseResult.success(movieVOs));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "获取随机推荐失败：" + e.getMessage()));
        }
    }

    /**
     * 获取首页数据（正在热映、即将上映）
     * GET /api/movies/home
     */
    @GetMapping("/home")
    public ResponseEntity<ResponseResult<Map<String, Object>>> getHomeData(@RequestParam(value = "sortBy", defaultValue = "rating") String sortBy) {
        try {
            Map<String, Object> homeData = new HashMap<>();
            java.util.Date now = new java.util.Date();

            // 1. 正在热映 - 上映日期在过去或今天的电影
            List<Movies> moviesList;
            
            // 根据排序参数进行排序
            if ("rating".equals(sortBy)) {
                // 按评分降序
                moviesList = movieService.lambdaQuery()
                        .eq(Movies::getStatus, 1)
                        .le(Movies::getReleaseDate, now)
                        .orderByDesc(Movies::getRating)
                        .last("LIMIT 8")
                        .list();
            } else if ("hot".equals(sortBy)) {
                // 这里简化处理，按评论数排序（实际应该按热度字段）
                moviesList = movieService.lambdaQuery()
                        .eq(Movies::getStatus, 1)
                        .le(Movies::getReleaseDate, now)
                        .orderByDesc(Movies::getId)
                        .last("LIMIT 8")
                        .list();
            } else if ("box".equals(sortBy)) {
                // 这里简化处理，按 ID 排序（实际应该按票房字段）
                moviesList = movieService.lambdaQuery()
                        .eq(Movies::getStatus, 1)
                        .le(Movies::getReleaseDate, now)
                        .orderByDesc(Movies::getId)
                        .last("LIMIT 8")
                        .list();
            } else {
                // 默认按评分排序
                moviesList = movieService.lambdaQuery()
                        .eq(Movies::getStatus, 1)
                        .le(Movies::getReleaseDate, now)
                        .orderByDesc(Movies::getRating)
                        .last("LIMIT 8")
                        .list();
            }
            
            List<MovieVO> rankedMovies = moviesList.stream()
                    .map(movie -> {
                        MovieVO vo = convertToVO(movie);
                        vo.setReviewCount(movieService.getMovieReviewCount(movie.getId()));
                        return vo;
                    })
                    .collect(Collectors.toList());
            homeData.put("rankedMovies", rankedMovies);

            // 2. 正在热映 - 当月有排片的电影
            // 计算当月的开始和结束日期
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            Date monthStart = calendar.getTime();
            
            calendar.add(Calendar.MONTH, 1);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            Date monthEnd = calendar.getTime();
            
            List<Movies> upcomingMoviesList = movieService.lambdaQuery()
                    .eq(Movies::getStatus, 1)
                    .le(Movies::getReleaseDate, monthEnd) // 上映日期不晚于当月结束
                    .ge(Movies::getReleaseDate, monthStart) // 上映日期不早于当月开始
                    .orderByAsc(Movies::getReleaseDate)
                    .last("LIMIT 4")
                    .list();
            
            List<MovieVO> upcomingMovies = upcomingMoviesList.stream()
                    .map(movie -> {
                        MovieVO vo = convertToVO(movie);
                        vo.setReviewCount(movieService.getMovieReviewCount(movie.getId()));
                        return vo;
                    })
                    .collect(Collectors.toList());
            homeData.put("upcomingMovies", upcomingMovies);

            return ResponseEntity.ok(ResponseResult.success(homeData));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "获取首页数据失败：" + e.getMessage()));
        }
    }

    /**
     * 初始化所有电影评分（仅用于数据迁移）
     * POST /api/movies/init-ratings
     */
    @PostMapping("/init-ratings")
    public ResponseEntity<ResponseResult<String>> initRatings() {
        try {
            ((MovieServiceImpl) movieService).initializeAllMovieRatings();
            return ResponseEntity.ok(ResponseResult.success("电影评分初始化完成"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "初始化评分失败：" + e.getMessage()));
        }
    }
}