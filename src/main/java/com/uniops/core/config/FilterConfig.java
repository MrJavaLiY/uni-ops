package com.uniops.core.config;

import com.uniops.core.interceptor.ContextPathRedirectFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 过滤器配置类
 */
//@Configuration/**/D
public class FilterConfig {

    /**
     * 注册上下文路径重定向过滤器
     */
    @Bean
    public FilterRegistrationBean<ContextPathRedirectFilter> contextPathRedirectFilterRegistration(
            ContextPathRedirectFilter contextPathRedirectFilter) {

        FilterRegistrationBean<ContextPathRedirectFilter> registrationBean =
                new FilterRegistrationBean<>();

        registrationBean.setFilter(contextPathRedirectFilter);

        // 设置过滤器映射模式 - 拦截所有请求
        registrationBean.addUrlPatterns("/*");

        // 设置过滤器顺序（值越小，优先级越高）
        registrationBean.setOrder(1);

        // 设置初始化参数（可选）
        Map<String, String> initParameters = new HashMap<>();
        initParameters.put("excludePaths", "/api,/actuator,/health");
        registrationBean.setInitParameters(initParameters);

        return registrationBean;
    }
}
