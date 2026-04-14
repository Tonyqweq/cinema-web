package org.tonyqwe.cinemaweb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.entity.MovieReview;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.service.MovieReviewService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;


@RestController
@RequestMapping("/api/movie-reviews")
public class MovieReviewController {

    @Resource
    private MovieReviewService movieReviewService;

    @Resource
    private UserService userService;

    @GetMapping("/movie/{movieId}")
    public ResponseResult<?> getReviewsByMovieId(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        try {
            IPage<MovieReview> reviews = movieReviewService.getReviewsByMovieId(movieId, page, pageSize);
            
            // 为每个评论添加用户信息
            List<Map<String, Object>> records = new ArrayList<>();
            for (MovieReview review : reviews.getRecords()) {
                Map<String, Object> reviewMap = new HashMap<>();
                reviewMap.put("id", review.getId());
                reviewMap.put("movieId", review.getMovieId());
                reviewMap.put("userId", review.getUserId());
                reviewMap.put("rating", review.getRating());
                reviewMap.put("comment", review.getComment());
                reviewMap.put("createdAt", review.getCreatedAt());
                reviewMap.put("updatedAt", review.getUpdatedAt());
                
                // 获取用户信息
                SysUsers user = userService.getById(review.getUserId());
                if (user != null) {
                    reviewMap.put("username", user.getUsername());
                    reviewMap.put("avatar", user.getAvatar());
                } else {
                    reviewMap.put("username", "未知用户");
                    reviewMap.put("avatar", null);
                }
                
                records.add(reviewMap);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", records);
            result.put("total", reviews.getTotal());
            result.put("pages", reviews.getPages());
            result.put("current", reviews.getCurrent());
            result.put("size", reviews.getSize());
            
            return ResponseResult.success(result);
        } catch (Exception e) {
            return ResponseResult.error("获取评论失败: " + e.getMessage());
        }
    }

    @GetMapping("/movie/{movieId}/stats")
    public ResponseResult<?> getMovieReviewStats(@PathVariable Long movieId) {
        try {
            Double avgRating = movieReviewService.getAverageRating(movieId);
            Integer reviewCount = movieReviewService.getReviewCount(movieId);
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("averageRating", avgRating != null ? avgRating : 0.0);
            stats.put("reviewCount", reviewCount != null ? reviewCount : 0);
            
            return ResponseResult.success(stats);
        } catch (Exception e) {
            return ResponseResult.error("获取统计数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/user/{movieId}")
    public ResponseResult<?> getUserReview(@PathVariable Long movieId) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null) {
                return ResponseResult.error("用户未登录");
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error("用户不存在");
            }
            
            MovieReview review = movieReviewService.getUserReview(movieId, user.getId());
            return ResponseResult.success(review);
        } catch (Exception e) {
            return ResponseResult.error("获取用户评论失败: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseResult<?> createOrUpdateReview(@RequestBody Map<String, Object> request) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null) {
                return ResponseResult.error("用户未登录");
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error("用户不存在");
            }
            
            Long movieId = Long.valueOf(request.get("movieId").toString());
            Integer rating = Integer.valueOf(request.get("rating").toString());
            String comment = request.get("comment") != null ? request.get("comment").toString() : null;
            
            if (rating < 1 || rating > 5) {
                return ResponseResult.error("评分必须在1-5之间");
            }
            
            MovieReview review = movieReviewService.createOrUpdateReview(movieId, user.getId(), rating, comment);
            return ResponseResult.success(review);
        } catch (Exception e) {
            return ResponseResult.error("提交评论失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseResult<?> deleteReview(@PathVariable Long reviewId) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null) {
                return ResponseResult.error("用户未登录");
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error("用户不存在");
            }
            
            boolean deleted = movieReviewService.deleteReview(reviewId, user.getId());
            if (deleted) {
                return ResponseResult.success("删除成功");
            } else {
                return ResponseResult.error("删除失败，评论不存在或无权删除");
            }
        } catch (Exception e) {
            return ResponseResult.error("删除评论失败: " + e.getMessage());
        }
    }
}