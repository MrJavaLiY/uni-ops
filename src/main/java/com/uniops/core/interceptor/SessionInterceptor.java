package com.uniops.core.interceptor;

import com.alibaba.fastjson2.JSONObject;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.util.AuthConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SessionInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 允许OPTIONS请求通过（用于预检请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 排除登录接口和静态资源
        String requestURI = request.getRequestURI();
        //去掉公共前缀
        if (requestURI.startsWith("/uni-ops")) {
            requestURI = requestURI.substring("/uni-ops".length());
        }
        if (requestURI.startsWith("/auth") ||
                requestURI.startsWith("/api-docs") ||
                requestURI.startsWith("/webjars") ||
                requestURI.startsWith("/swagger") ||
                requestURI.startsWith("/system") ||
                requestURI.startsWith("/index.html") ||
                requestURI.contains(".html") ||
                requestURI.contains(".htm") ||
                requestURI.contains(".css") ||
                requestURI.contains(".js")) {

            return true;
        }

        // 检查session令牌
        String sessionToken = request.getHeader("session_token"); // 使用新的会话令牌头
        if (sessionToken == null || !AuthConstants.validateSessionWithCache(sessionToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            ResponseResult result = ResponseResult.error(401, "未授权，请先登录");
            response.getWriter().write(JSONObject.toJSONString(result));
            return false;
        }

        return true;
    }
}
