package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.domain.entity.UserCollection;
import org.tonyqwe.cinemaweb.service.UserCollectionService;
import org.tonyqwe.cinemaweb.service.UserService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user-collections")
public class UserCollectionController {

    @Resource
    private UserCollectionService userCollectionService;

    @Resource
    private UserService userService;

    /**
     * 获取用户收藏列表
     * GET /api/user-collections?page=1&pageSize=12
     */
    @GetMapping
    public ResponseEntity<ResponseResult<Map<String, Object>>> getUserCollections(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "12") long pageSize
    ) {
        try {
            // 获取当前登录用户的用户名
            String username = SecurityUtils.getCurrentUsername();
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseResult.error(401, "用户未登录"));
            }

            // 根据用户名获取用户信息，得到用户 ID
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseResult.error(404, "用户不存在"));
            }

            // 获取用户收藏列表
            IPage<UserCollection> result = userCollectionService.getUserCollections(user.getId().longValue(), page, pageSize);

            // 构建响应数据
            Map<String, Object> response = new HashMap<>();
            response.put("records", result.getRecords());
            response.put("total", result.getTotal());
            response.put("current", result.getCurrent());
            response.put("size", result.getSize());
            response.put("pages", result.getPages());

            return ResponseEntity.ok(ResponseResult.success(response));
        } catch (Exception e) {
            e.printStackTrace(); // 打印详细报错信息到后端控制台
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "获取收藏列表失败：" + e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    /**
     * 取消收藏电影
     * DELETE /api/user-collections/{movieId}
     */
    @DeleteMapping("/{movieId}")
    public ResponseEntity<ResponseResult<Void>> removeCollection(@PathVariable("movieId") Long movieId) {
        try {
            // 获取当前登录用户的用户名
            String username = SecurityUtils.getCurrentUsername();
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseResult.error(401, "用户未登录"));
            }

            // 根据用户名获取用户信息，得到用户 ID
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseResult.error(404, "用户不存在"));
            }

            // 取消收藏
            boolean success = userCollectionService.removeCollection(user.getId().longValue(), movieId);
            if (!success) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseResult.error(404, "收藏记录不存在"));
            }

            return ResponseEntity.ok(ResponseResult.success("取消收藏成功", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "取消收藏失败：" + e.getMessage()));
        }
    }

    /**
     * 添加收藏电影
     * POST /api/user-collections/{movieId}
     */
    @PostMapping("/{movieId}")
    public ResponseEntity<ResponseResult<Void>> addCollection(@PathVariable("movieId") Long movieId) {
        try {
            // 获取当前登录用户的用户名
            String username = SecurityUtils.getCurrentUsername();
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseResult.error(401, "用户未登录"));
            }

            // 根据用户名获取用户信息，得到用户 ID
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseResult.error(404, "用户不存在"));
            }

            // 添加收藏
            boolean success = userCollectionService.addCollection(user.getId().longValue(), movieId);
            if (!success) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ResponseResult.error(500, "添加收藏失败"));
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseResult.success("添加收藏成功", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "添加收藏失败：" + e.getMessage()));
        }
    }

    /**
     * 检查电影是否已收藏
     * GET /api/user-collections/{movieId}/status
     */
    @GetMapping("/{movieId}/status")
    public ResponseEntity<ResponseResult<Map<String, Boolean>>> checkCollectionStatus(@PathVariable("movieId") Long movieId) {
        try {
            // 获取当前登录用户的用户名
            String username = SecurityUtils.getCurrentUsername();
            if (username == null || "anonymousUser".equals(username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseResult.error(401, "用户未登录"));
            }

            // 根据用户名获取用户信息，得到用户 ID
            SysUsers user = userService.getByUsername(username);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseResult.error(404, "用户不存在"));
            }

            // 检查是否已收藏
            boolean isCollected = userCollectionService.isCollected(user.getId().longValue(), movieId);

            Map<String, Boolean> response = new HashMap<>();
            response.put("isCollected", isCollected);

            return ResponseEntity.ok(ResponseResult.success(response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "检查收藏状态失败：" + e.getMessage()));
        }
    }
}
