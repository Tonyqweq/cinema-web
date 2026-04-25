package org.tonyqwe.cinemaweb.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

import java.util.stream.Collectors;

/**
 * 全局异常处理：统一错误码与返回格式，Controller 只需抛异常即可。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数校验失败（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseResult<Void>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        System.err.println("参数校验失败: " + msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(400, msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResponseResult<Void>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        System.err.println("绑定异常: " + msg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(400, msg));
    }

    /** 认证异常 */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResponseResult<Void>> handleAuthentication(AuthenticationException e) {
        System.err.println("认证异常: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(401, "authentication failed"));
    }

    /** 访问被拒绝 */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseResult<Void>> handleAccessDenied(AccessDeniedException e) {
        System.err.println("访问被拒绝: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(403, "access denied"));
    }

    /** 用户名或密码错误 */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseResult<Void>> handleBadCredentials(BadCredentialsException e) {
        System.err.println("认证失败: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(401, "invalid username or password"));
    }

    /** 账号被禁用 */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ResponseResult<Void>> handleDisabled(DisabledException e) {
        System.err.println("账号被禁用: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(403, "account is disabled"));
    }

    /** 参数/业务不合法（如：日期格式错误、资源不存在说明等） */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseResult<Void>> handleIllegalArg(IllegalArgumentException e) {
        System.err.println("非法参数: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(400, e.getMessage()));
    }

    /** 其他未预期异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult<Void>> handleOther(Exception e) {
        System.err.println("未处理异常: " + e.getClass().getName() + " - " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(500, "internal server error: " + e.getMessage()));
    }
}
