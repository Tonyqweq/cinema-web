package org.tonyqwe.cinemaweb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.tonyqwe.cinemaweb.utils.ResponseResult;

@RestController
public class TestController {
    
    @GetMapping("/test")
    public ResponseEntity<ResponseResult<String>> test(
            @RequestParam(required = false) String username) {
        System.out.println("测试请求：username = " + username);
        if (username == null || username.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ResponseResult.error(400, "用户名不能为空"));
        }
        return ResponseEntity.ok(ResponseResult.success("测试成功：" + username));
    }
}