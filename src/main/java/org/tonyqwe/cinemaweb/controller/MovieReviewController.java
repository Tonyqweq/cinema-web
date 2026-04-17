package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.entity.MovieReview;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.service.MovieReviewService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/movie-reviews")
public class MovieReviewController {
    
    @Resource
    private MovieReviewService movieReviewService;

    @Resource
    private UserService userService;
    
    /**
     * 获取电影评论列表
     * GET /api/movie-reviews/movie/{movieId}?page=1&pageSize=10
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ResponseResult<Map<String, Object>>> getReviewsByMovieId(
            @PathVariable("movieId") Long movieId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize
    ) {
        try {
            IPage<MovieReview> result = movieReviewService.getReviewsByMovieId(movieId, page, pageSize);
            
            // 为每个评论添加用户头像
            List<MovieReview> reviews = result.getRecords();
            for (MovieReview review : reviews) {
                // 获取用户信息
                if (review.getUserId() != null) {
                    SysUsers user = userService.getById(review.getUserId().intValue());
                    if (user != null && user.getAvatar() != null) {
                        // 使用反射为评论对象添加avatar属性
                        try {
                            java.lang.reflect.Field avatarField = MovieReview.class.getDeclaredField("avatar");
                            avatarField.setAccessible(true);
                            avatarField.set(review, user.getAvatar());
                        } catch (Exception e) {
                            // 如果没有avatar字段，使用Map包装
                            // 这里我们使用另一种方式，创建一个新的Map来包装评论信息
                            break;
                        }
                    }
                }
            }
            
            // 由于MovieReview实体没有avatar字段，我们需要创建一个新的列表，包含评论和头像信息
            List<Map<String, Object>> reviewWithAvatar = new ArrayList<>();
            for (MovieReview review : reviews) {
                Map<String, Object> reviewMap = new HashMap<>();
                reviewMap.put("id", review.getId());
                reviewMap.put("movieId", review.getMovieId());
                reviewMap.put("userId", review.getUserId());
                reviewMap.put("rating", review.getRating());
                reviewMap.put("comment", review.getComment());
                reviewMap.put("createdAt", review.getCreatedAt());
                reviewMap.put("updatedAt", review.getUpdatedAt());
                
                // 获取用户头像
                if (review.getUserId() != null) {
                    SysUsers user = userService.getById(review.getUserId().intValue());
                    if (user != null) {
                        reviewMap.put("avatar", user.getAvatar());
                    }
                }
                
                reviewWithAvatar.add(reviewMap);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("records", reviewWithAvatar);
            response.put("total", result.getTotal());
            response.put("current", result.getCurrent());
            response.put("size", result.getSize());
            response.put("pages", result.getPages());
            
            return ResponseEntity.ok(ResponseResult.success(response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "获取评论列表失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取电影评论统计
     * GET /api/movie-reviews/movie/{movieId}/stats
     */
    @GetMapping("/movie/{movieId}/stats")
    public ResponseEntity<ResponseResult<Map<String, Object>>> getReviewStats(
            @PathVariable("movieId") Long movieId
    ) {
        try {
            Map<String, Object> stats = movieReviewService.getReviewStatsByMovieId(movieId);
            return ResponseEntity.ok(ResponseResult.success(stats));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "获取评论统计失败：" + e.getMessage()));
        }
    }
    
    /**
     * 获取用户对电影的评论
     * GET /api/movie-reviews/user/{movieId}
     */
    @GetMapping("/user/{movieId}")
    public ResponseEntity<ResponseResult<MovieReview>> getUserReview(
            @PathVariable("movieId") Long movieId
    ) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseResult.error(401, "用户未登录"));
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseResult.error(404, "用户不存在"));
            }
            
            MovieReview review = movieReviewService.getReviewByUserAndMovie(user.getId().longValue(), movieId);
            return ResponseEntity.ok(ResponseResult.success(review));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "获取用户评论失败：" + e.getMessage()));
        }
    }
    
    /**
     * 提交评论
     * POST /api/movie-reviews
     */
    @PostMapping
    public ResponseEntity<ResponseResult<Void>> submitReview(
            @RequestBody MovieReview review
    ) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseResult.error(401, "用户未登录"));
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseResult.error(404, "用户不存在"));
            }
            
            review.setUserId(user.getId().longValue());
            
            boolean success = movieReviewService.saveOrUpdateReview(review);
            if (success) {
                return ResponseEntity.ok(ResponseResult.success("评论提交成功", null));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseResult.error(500, "评论提交失败"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "评论提交失败：" + e.getMessage()));
        }
    }
    
    /**
     * 删除评论
     * DELETE /api/movie-reviews/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseResult<Void>> deleteReview(
            @PathVariable("id") Long id
    ) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseResult.error(401, "用户未登录"));
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseResult.error(404, "用户不存在"));
            }
            
            boolean success = movieReviewService.deleteReview(id, user.getId().longValue());
            if (success) {
                return ResponseEntity.ok(ResponseResult.success("评论删除成功", null));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseResult.error(404, "评论不存在或无权限删除"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "评论删除失败：" + e.getMessage()));
        }
    }
}
