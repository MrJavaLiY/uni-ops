package com.uniops.core.config;

import com.uniops.core.interceptor.SessionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
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
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 配置静态资源，Vue的dist目录放在src/main/resources/static下
        // Spring Boot默认会处理classpath:/static/下的资源
        // 所以不需要额外配置，但如果你有其他静态资源目录，可以在这里添加
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
