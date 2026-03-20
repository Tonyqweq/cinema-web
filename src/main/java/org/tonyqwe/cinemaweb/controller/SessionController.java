package org.tonyqwe.cinemaweb.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.*;
import org.tonyqwe.cinemaweb.domain.dto.LoginRequest;
import org.tonyqwe.cinemaweb.domain.dto.LoginResponse;
import org.tonyqwe.cinemaweb.domain.dto.RegisterRequest;
import org.tonyqwe.cinemaweb.domain.entity.SysUsers;
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
//

    @PostMapping("/sessions")
    public ResponseEntity<ResponseResult<LoginResponse>> createSession(
            @RequestBody @Valid LoginRequest request) {

        try {
            // 使用 Spring Security 进行认证
            String token = loginService.login(request);
            if (token == null) {
                throw new BadCredentialsException("invalid username or password");
            }

            // LoginResponse 目前包含 token + username，这里不返回 username
            LoginResponse response = new LoginResponse(token, null);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseResult.success("login success", response));

        } catch (BadCredentialsException e) {
            // 用户名或密码错误
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseResult.error(401, "invalid username or password"));
        } catch (DisabledException e) {
            // 账号被禁用
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ResponseResult.error(403, "account is disabled"));
        } catch (Exception e) {
            // 其他异常
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "login failed"));
        }
    }

    /**
     * 注册
     * POST /api/sessions/register
     */
    @PostMapping("/sessions/register")
    public ResponseEntity<ResponseResult<Void>> register(@RequestBody @Valid RegisterRequest request) {
        try {
            loginService.register(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ResponseResult.success("register success", null));
        } catch (IllegalArgumentException e) {
            // 参数正确但业务冲突（如：用户名已存在）
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ResponseResult.error(409, e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseResult.error(500, "register failed"));
        }
    }

    /**
     * 登出接口：POST /api/logout
     * 前端调用时需要携带 Authorization: Bearer xxx
     */
    @PostMapping("/sessions/logout")
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
    public ResponseEntity<ResponseResult<SysUsers>> getCurrentSession(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseResult.error(401, "missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);
        SysUsers currentUser = loginService.getCurrentUser(token);
        if (currentUser == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseResult.error(401, "invalid or expired token"));
        }

        currentUser.setPassword(null);
        return ResponseEntity.ok(ResponseResult.success(currentUser));
    }
}