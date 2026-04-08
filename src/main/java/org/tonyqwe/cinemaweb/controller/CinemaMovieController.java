package org.tonyqwe.cinemaweb.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.service.CinemaMovieRelationService;
import org.tonyqwe.cinemaweb.service.AdminCinemaRelationService;
import org.tonyqwe.cinemaweb.utils.SecurityUtils;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/cinema-movies")
public class CinemaMovieController {

    @Autowired
    private CinemaMovieRelationService cinemaMovieRelationService;

    @Autowired
    private AdminCinemaRelationService adminCinemaRelationService;

    /**
     * 获取影院绑定的电影ID列表
     */
    @GetMapping("/bind/{cinemaId}")
    public ResponseEntity<ResponseResult<List<Long>>> getBoundMovies(@PathVariable Long cinemaId) {
        // 检查权限
        if (!checkCinemaAccess(cinemaId)) {
            return ResponseEntity.ok(ResponseResult.error("无权访问该影院"));
        }

        List<Long> movieIds = cinemaMovieRelationService.getMovieIdsByCinemaId(cinemaId);
        return ResponseEntity.ok(ResponseResult.success(movieIds));
    }

    /**
     * 绑定/解除绑定影院与电影
     */
    @PutMapping("/bind")
    public ResponseEntity<ResponseResult<?>> bindCinemaMovie(@RequestBody Map<String, Object> params) {
        Long cinemaId = null;
        Long movieId = null;
        Boolean bind = null;

        // 解析参数
        try {
            Object cinemaIdObj = params.get("cinemaId");
            Object movieIdObj = params.get("movieId");
            Object bindObj = params.get("bind");

            if (cinemaIdObj != null) {
                if (cinemaIdObj instanceof Integer) {
                    cinemaId = ((Integer) cinemaIdObj).longValue();
                } else if (cinemaIdObj instanceof Long) {
                    cinemaId = (Long) cinemaIdObj;
                } else if (cinemaIdObj instanceof String) {
                    cinemaId = Long.parseLong((String) cinemaIdObj);
                }
            }

            if (movieIdObj != null) {
                if (movieIdObj instanceof Integer) {
                    movieId = ((Integer) movieIdObj).longValue();
                } else if (movieIdObj instanceof Long) {
                    movieId = (Long) movieIdObj;
                } else if (movieIdObj instanceof String) {
                    movieId = Long.parseLong((String) movieIdObj);
                }
            }

            if (bindObj != null) {
                if (bindObj instanceof Boolean) {
                    bind = (Boolean) bindObj;
                } else if (bindObj instanceof String) {
                    bind = Boolean.parseBoolean((String) bindObj);
                }
            }
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseResult.error("参数格式错误"));
        }

        // 检查参数
        if (cinemaId == null || movieId == null || bind == null) {
            return ResponseEntity.ok(ResponseResult.error("参数不能为空"));
        }

        // 检查权限
        if (!checkCinemaAccess(cinemaId)) {
            return ResponseEntity.ok(ResponseResult.error("无权操作该影院"));
        }

        try {
            if (bind) {
                cinemaMovieRelationService.bindCinemaToMovie(cinemaId, movieId);
            } else {
                cinemaMovieRelationService.unbindCinemaFromMovie(cinemaId, movieId);
            }
            return ResponseEntity.ok(ResponseResult.success());
        } catch (Exception e) {
            return ResponseEntity.ok(ResponseResult.error("操作失败: " + e.getMessage()));
        }
    }

    /**
     * 检查用户是否有权限访问指定影院
     */
    private boolean checkCinemaAccess(Long cinemaId) {
        // SUPER_ADMIN和ADMIN可以访问所有影院
        if (SecurityUtils.isSuperAdmin()||SecurityUtils.isAdmin()) {
            return true;
        }

        // STAFF只能访问其绑定的影院
        if (SecurityUtils.isStaff()) {
            String username = SecurityUtils.getCurrentUsername();
            if (username != null) {
                // 这里需要通过userService获取用户信息，然后获取其绑定的影院ID
                // 暂时简化处理，假设adminCinemaRelationService有获取用户绑定影院的方法
                Long userCinemaId = adminCinemaRelationService.getCinemaIdByAdminUsername(username);
                return userCinemaId != null && userCinemaId.equals(cinemaId);
            }
        }

        return false;
    }
}
