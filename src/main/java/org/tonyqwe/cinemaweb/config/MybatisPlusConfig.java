package org.tonyqwe.cinemaweb.config;

import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public PaginationInnerInterceptor paginationInnerInterceptor() {
        // 启用 MyBatis-Plus 分页拦截器，让 selectPage(page, wrapper) 生效
        return new PaginationInnerInterceptor();
    }
}

