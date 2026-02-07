package org.tonyqwe.cinemaweb.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 目前只保护 /api/demo/secure 这个接口，后续有更多受保护接口可以在这里追加
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/demo/secure");
    }
}
