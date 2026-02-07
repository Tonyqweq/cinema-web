package org.tonyqwe.cinemaweb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 示例接口，用于演示需要登录才能访问的资源。
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /**
     * 受保护接口：/api/demo/secure
     * 必须携带有效的 Authorization: Bearer <token> 才能访问。
     */
    @GetMapping("/secure")
    public ResponseEntity<String> secure() {
        return ResponseEntity.ok("This is a secure demo resource.");
    }
}
