package com.uniops.core.config;

import com.uniops.core.interceptor.SessionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SessionInterceptor sessionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns("/login", "/login/**", "/", "/error", "/v3/api-docs/**", "/webjars/**", "/swagger-ui/**", "/doc.html"); // 除了登录接口和其他公共接口
    }
}
