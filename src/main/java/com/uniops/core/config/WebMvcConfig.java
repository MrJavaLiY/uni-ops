package com.uniops.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.util.AntPathMatcher;

//@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射静态资源到根路径，使其不受 context-path 影响
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/", "classpath:/assets/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/", "classpath:/css/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/", "classpath:/js/")
                .setCachePeriod(3600);
        registry.addResourceHandler("/images/**", "/img/**")
                .addResourceLocations("classpath:/static/images/", "classpath:/static/img/", "classpath:/images/", "classpath:/img/")
                .setCachePeriod(3600);
    }
}
