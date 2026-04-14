package org.tonyqwe.cinemaweb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.domain.entity.UserCollection;
import org.tonyqwe.cinemaweb.service.UserCollectionService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user-collections")
public class UserCollectionController {

    @Resource
    private UserCollectionService userCollectionService;

    @Resource
    private UserService userService;

    @PostMapping
    public ResponseResult<?> addCollection(@RequestBody Map<String, Object> request) {
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
            boolean added = userCollectionService.addCollection(user.getId(), movieId);
            
            if (added) {
                return ResponseResult.success("收藏成功");
            } else {
                return ResponseResult.error("收藏失败");
            }
        } catch (Exception e) {
            return ResponseResult.error("收藏失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{movieId}")
    public ResponseResult<?> removeCollection(@PathVariable Long movieId) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null) {
                return ResponseResult.error("用户未登录");
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error("用户不存在");
            }
            
            boolean removed = userCollectionService.removeCollection(user.getId(), movieId);
            if (removed) {
                return ResponseResult.success("取消收藏成功");
            } else {
                return ResponseResult.error("取消收藏失败");
            }
        } catch (Exception e) {
            return ResponseResult.error("取消收藏失败: " + e.getMessage());
        }
    }

    @GetMapping("/check/{movieId}")
    public ResponseResult<?> checkCollection(@PathVariable Long movieId) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null) {
                return ResponseResult.error("用户未登录");
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error("用户不存在");
            }
            
            boolean isCollected = userCollectionService.isCollected(user.getId(), movieId);
            Map<String, Boolean> result = new HashMap<>();
            result.put("isCollected", isCollected);
            
            return ResponseResult.success(result);
        } catch (Exception e) {
            return ResponseResult.error("检查收藏状态失败: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseResult<?> getUserCollections(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null) {
                return ResponseResult.error("用户未登录");
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error("用户不存在");
            }
            
            IPage<UserCollection> collections = userCollectionService.getUserCollections(user.getId(), page, pageSize);
            
            Map<String, Object> result = new HashMap<>();
            result.put("records", collections.getRecords());
            result.put("total", collections.getTotal());
            result.put("pages", collections.getPages());
            result.put("current", collections.getCurrent());
            result.put("size", collections.getSize());
            
            return ResponseResult.success(result);
        } catch (Exception e) {
            return ResponseResult.error("获取收藏列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/movie-ids")
    public ResponseResult<?> getUserCollectedMovieIds() {
        try {
            String username = SecurityUtils.getCurrentUsername();
            if (username == null) {
                return ResponseResult.error("用户未登录");
            }
            
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseResult.error("用户不存在");
            }
            
            List<Long> movieIds = userCollectionService.getUserCollectedMovieIds(user.getId());
            return ResponseResult.success(movieIds);
        } catch (Exception e) {
            return ResponseResult.error("获取收藏电影ID列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/movie/{movieId}/count")
    public ResponseResult<?> getCollectionCount(@PathVariable Long movieId) {
        try {
            Integer count = userCollectionService.getCollectionCount(movieId);
            Map<String, Integer> result = new HashMap<>();
            result.put("count", count);
            return ResponseResult.success(result);
        } catch (Exception e) {
            return ResponseResult.error("获取收藏数量失败: " + e.getMessage());
        }
    }
}