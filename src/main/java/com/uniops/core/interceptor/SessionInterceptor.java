package com.uniops.core.interceptor;

import com.alibaba.fastjson2.JSONObject;
import com.uniops.core.annotation.RequiresAuth;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.service.ISystemRegisterService;
import com.uniops.core.util.AuthConstants;
import com.uniops.starter.autoconfigure.UniOpsProperties;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class SessionInterceptor implements HandlerInterceptor {
    @Resource
    UniOpsProperties uniOpsProperties;
    @Resource
    ISystemRegisterService systemRegisterService;
    // 缓存需要权限校验的API路径
    private static final Set<String> authRequiredPaths = ConcurrentHashMap.newKeySet();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 允许OPTIONS请求通过（用于预检请求）
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
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

        //-----------------以上是不需要进行控制的，下面的那些，必须要进行授权验证，授权验证完成才允许使用接口
        boolean isLocalValid = systemRegisterService.checkLocalValidity();
        if (!isLocalValid) {
            //系统授权过期
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json;charset=UTF-8");
            ResponseResult<?> result = ResponseResult.error(505, "系统已经过期了，无法继续使用，请联系公司授权");
            response.getWriter().write(JSONObject.toJSONString(result));
            return false;
        }


        boolean isAuth = false;

        // 检查是否是HandlerMethod类型（控制器方法）
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 检查方法或类上是否有RequiresAuth注解
            if (checkRequiresAuthAnnotation(handlerMethod, requestURI)) {
                isAuth = true;
            }

            // 检查是否是我们项目的Controller
            String packageName = handlerMethod.getBeanType().getPackage().getName();
            if (packageName.startsWith("com.uniops")) {
                isAuth = true;
            }
        }

        if (requestURI.startsWith("/uniops")) {
            isAuth = true;
        }

        // 排除登录接口和静态资源


        if (isAuth || shouldIncludePath(requestURI)) {
            // 检查session令牌
            String sessionToken = request.getHeader("session_token"); // 使用新的会话令牌头
            if (sessionToken == null || !AuthConstants.validateSessionWithCache(sessionToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                ResponseResult result = ResponseResult.error(401, "未授权，请先登录");
                response.getWriter().write(JSONObject.toJSONString(result));
                return false;
            }
        }
        return true;
    }

    /**
     * 检查方法或类上是否有RequiresAuth注解
     * @param handlerMethod 处理方法
     * @param requestURI 请求URI
     * @return 是否需要认证
     */
    private boolean checkRequiresAuthAnnotation(HandlerMethod handlerMethod, String requestURI) {
        // 检查方法上是否有注解
        RequiresAuth methodAnnotation = handlerMethod.getMethodAnnotation(RequiresAuth.class);
        if (methodAnnotation != null && methodAnnotation.required()) {
            // 从RequestMapping等注解获取路径
            extractAndCachePaths(handlerMethod);
            return true;
        }

        // 检查类上是否有注解
        RequiresAuth classAnnotation = handlerMethod.getBeanType().getAnnotation(RequiresAuth.class);
        if (classAnnotation != null && classAnnotation.required()) {
            // 从RequestMapping等注解获取路径
            extractAndCachePaths(handlerMethod);
            return true;
        }

        return false;
    }

    /**
     * 从RequestMapping等注解提取路径并缓存
     * @param handlerMethod 处理方法
     */
    private void extractAndCachePaths(HandlerMethod handlerMethod) {
        // 获取方法上的RequestMapping注解
        RequestMapping methodMapping = handlerMethod.getMethodAnnotation(RequestMapping.class);
        if (methodMapping != null) {
            String[] methodPaths = methodMapping.value();
            String[] classPaths = {};

            // 获取类上的RequestMapping注解
            RequestMapping classMapping = handlerMethod.getBeanType().getAnnotation(RequestMapping.class);
            if (classMapping != null) {
                classPaths = classMapping.value();
            }

            // 组合类路径和方法路径
            for (String methodPath : methodPaths) {
                if (classPaths.length > 0) {
                    for (String classPath : classPaths) {
                        String fullPath = combinePaths(classPath, methodPath);
                        authRequiredPaths.add(fullPath);
                    }
                } else {
                    authRequiredPaths.add(methodPath);
                }
            }
        } else {
            // 如果方法上没有RequestMapping，但类上有，且类有RequiresAuth注解
            RequestMapping classMapping = handlerMethod.getBeanType().getAnnotation(RequestMapping.class);
            if (classMapping != null) {
                String[] classPaths = classMapping.value();
                for (String classPath : classPaths) {
                    authRequiredPaths.add(classPath);
                }
            }
        }
    }

    /**
     * 组合路径
     * @param parentPath 父路径
     * @param childPath 子路径
     * @return 组合后的路径
     */
    private String combinePaths(String parentPath, String childPath) {
        if (parentPath.endsWith("/") && childPath.startsWith("/")) {
            return parentPath + childPath.substring(1);
        } else if (!parentPath.endsWith("/") && !childPath.startsWith("/")) {
            return parentPath + "/" + childPath;
        } else {
            return parentPath + childPath;
        }
    }

    private boolean shouldIncludePath(String requestURI) {
        if (uniOpsProperties.getIncludeAuthPathPrefixes() == null) {
            return false;
        }
        return uniOpsProperties.getIncludeAuthPathPrefixes().stream()
                .anyMatch(prefix -> requestURI.startsWith(prefix));
    }
}
