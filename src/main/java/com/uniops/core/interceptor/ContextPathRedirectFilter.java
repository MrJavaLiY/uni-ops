package com.uniops.core.interceptor;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * 自动上下文路径重定向过滤器
 * 解决静态资源访问时缺少上下文路径的问题
 */
//@Component
public class ContextPathRedirectFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(ContextPathRedirectFilter.class);

    // 需要重定向的资源路径前缀
    private static final String[] RESOURCE_PATHS = {
            "/assets/",
            "/static/",
            "/public/",
            "/favicon.ico",
            "/robots.txt"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        String contextPath = httpRequest.getContextPath();

        // 调试日志
        logger.debug("收到请求: {}", requestURI);
        logger.debug("当前上下文路径: {}", contextPath);

        // 检查是否需要重定向
        if (shouldRedirect(requestURI, contextPath)) {
            // 构建新的URL，加上上下文路径
            String newPath = contextPath + requestURI;

            logger.info("重定向: {} -> {}", requestURI, newPath);

            // 使用302临时重定向（也可以使用301永久重定向）
            httpResponse.sendRedirect(newPath);
            return;
        }

        // 不需要重定向，继续过滤器链
        chain.doFilter(request, response);
    }

    /**
     * 判断是否需要重定向
     */
    private boolean shouldRedirect(String requestURI, String contextPath) {
        // 如果上下文路径为空（即部署在根路径），不需要重定向
        if (contextPath == null || contextPath.isEmpty() || contextPath.equals("/")) {
            return false;
        }

        // 如果请求已经包含上下文路径，不需要重定向
        if (requestURI.startsWith(contextPath + "/")) {
            return false;
        }

        // 检查是否是需要重定向的资源路径
        for (String resourcePath : RESOURCE_PATHS) {
            if (requestURI.startsWith(resourcePath)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("ContextPathRedirectFilter 初始化");
    }

    @Override
    public void destroy() {
        logger.info("ContextPathRedirectFilter 销毁");
    }
}
