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
import org.tonyqwe.cinemaweb.domain.dto.SessionInfoResponse;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
import org.tonyqwe.cinemaweb.service.AuthService;
import org.tonyqwe.cinemaweb.service.PermissionService;
import org.tonyqwe.cinemaweb.service.RegisterService;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

@RestController
@RequestMapping("/api")
public class SessionController {

    @Resource
    private AuthService authService;

    @Resource
    private RegisterService registerService;

    @Resource
    private PermissionService permissionService;

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
     * 发送验证码
     * POST /api/sessions/verification-code
     */
    @PostMapping("/sessions/verification-code")
    public ResponseEntity<ResponseResult<Void>> sendVerificationCode(@RequestBody String email) {
        // 去除可能的引号
        if (email != null && (email.startsWith("\"") || email.startsWith("'"))) {
            email = email.substring(1, email.length() - 1);
        }
        authService.sendVerificationCode(email);
        return ResponseEntity.ok(ResponseResult.success("验证码已发送", null));
    }

    /**
     * 当前会话信息（给前端路由守卫用）
     * GET /api/sessions/current
     */
    @GetMapping("/sessions/current")
    public ResponseEntity<ResponseResult<SessionInfoResponse>> getCurrentSession(HttpServletRequest request) {
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
        int uid = currentUser.getId() == null ? 0 : currentUser.getId();
        SessionInfoResponse body = new SessionInfoResponse(
                currentUser,
                permissionService.getRoleNamesByUserId(uid),
                permissionService.getPermissionCodesByUserId(uid)
        );
        return ResponseEntity.ok(ResponseResult.success(body));
    }
}
