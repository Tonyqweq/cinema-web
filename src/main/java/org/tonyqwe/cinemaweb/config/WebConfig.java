package org.tonyqwe.cinemaweb.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Spring Security 已经接管认证与鉴权，这里暂时不再注册基于拦截器的 JWT 校验
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 留空或仅注册与安全无关的拦截器
    }
}
