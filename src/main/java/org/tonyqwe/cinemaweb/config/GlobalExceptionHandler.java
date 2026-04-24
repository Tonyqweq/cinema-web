package org.tonyqwe.cinemaweb.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(400, msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ResponseResult<Void>> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(400, msg));
    }

    /** 用户名或密码错误 */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ResponseResult<Void>> handleBadCredentials(BadCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(401, "invalid username or password"));
    }

    /** 账号被禁用 */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ResponseResult<Void>> handleDisabled(DisabledException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(403, "account is disabled"));
    }

    /** 参数/业务不合法（如：日期格式错误、资源不存在说明等） */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseResult<Void>> handleIllegalArg(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(400, e.getMessage()));
    }

    /** 其他未预期异常 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult<Void>> handleOther(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(ResponseResult.error(500, "internal server error"));
    }
}
