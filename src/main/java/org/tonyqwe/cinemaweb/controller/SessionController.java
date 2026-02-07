package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.LoginRequest;
import org.tonyqwe.cinemaweb.domain.dto.LoginResponse;
import org.tonyqwe.cinemaweb.domain.entity.sysUsers;
import org.tonyqwe.cinemaweb.service.LoginService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

@RestController
@RequestMapping("/api")
public class SessionController {

    @Resource
    private LoginService loginService;

    /**
     * 创建会话（登录）
     * POST /api/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<ResponseResult<LoginResponse>> createSession(@RequestBody LoginRequest request) {
        String token = loginService.login(request);
        if (token == null) {
            // 未授权：401 + 统一格式
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseResult.error(401, "invalid username or password"));
        }
        // 登录成功：201 + 统一格式
        LoginResponse response = new LoginResponse(token);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ResponseResult.success("login success", response));
    }

    /**
     * 另一种更直观的登出接口：POST /api/logout
     * 前端调用时需要携带 Authorization: Bearer xxx
     */
    @PostMapping("/logout")
    public ResponseEntity<ResponseResult<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            loginService.logout(token);
        }
        // 无论是否拿到 token，都返回 200 + 统一格式
        return ResponseEntity.ok(ResponseResult.success("logout success", null));
    }

    /**
     * 当前会话信息（给前端路由守卫用）
     * GET /api/sessions/current
     */
    @GetMapping("/sessions/current")
    public ResponseEntity<ResponseResult<sysUsers>> getCurrentSession(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseResult.error(401, "missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);
        sysUsers currentUser = loginService.getCurrentUser(token);
        if (currentUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseResult.error(401, "invalid or expired token"));
        }

        currentUser.setPassword(null);
        return ResponseEntity.ok(ResponseResult.success(currentUser));
    }
}