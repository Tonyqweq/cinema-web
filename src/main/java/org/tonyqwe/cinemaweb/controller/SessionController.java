package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.LoginRequest;
import org.tonyqwe.cinemaweb.domain.dto.LoginResponse;
import org.tonyqwe.cinemaweb.domain.dto.RegisterRequest;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.service.AuthService;
import org.tonyqwe.cinemaweb.service.RegisterService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

@RestController
@RequestMapping("/api")
public class SessionController {

    @Resource
    private AuthService authService;

    @Resource
    private RegisterService registerService;

    /**
     * 创建会话（登录）
     * POST /api/sessions
     */
    @PostMapping("/sessions")
    public ResponseEntity<ResponseResult<LoginResponse>> createSession(
            @RequestBody @Valid LoginRequest request) {
        String token = authService.login(request);
        LoginResponse response = new LoginResponse(token, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseResult.success("login success", response));
    }

    /**
     * 注册
     * POST /api/sessions/register
     */
    @PostMapping("/sessions/register")
    public ResponseEntity<ResponseResult<Void>> register(@RequestBody @Valid RegisterRequest request) {
        registerService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseResult.success("register success", null));
    }

    /**
     * 登出
     * POST /api/sessions/logout
     */
    @PostMapping("/sessions/logout")
    public ResponseEntity<ResponseResult<Void>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            authService.logout(authHeader.substring(7));
        }
        return ResponseEntity.ok(ResponseResult.success("logout success", null));
    }

    /**
     * 当前会话信息（给前端路由守卫用）
     * GET /api/sessions/current
     */
    @GetMapping("/sessions/current")
    public ResponseEntity<ResponseResult<SysUsers>> getCurrentSession(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseResult.error(401, "missing or invalid Authorization header"));
        }
        String token = authHeader.substring(7);
        SysUsers currentUser = authService.getCurrentUser(token);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseResult.error(401, "invalid or expired token"));
        }
        currentUser.setPassword(null);
        return ResponseEntity.ok(ResponseResult.success(currentUser));
    }
}
