// src/main/java/com/uniops/core/aspect/HttpRequestLogAspect.java
package com.uniops.core.aspect;

import com.uniops.core.entity.HttpRequestLog;
import com.uniops.core.service.HttpRequestLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uniops.core.service.ISystemRegisterService;
import com.uniops.core.util.HttpCallLoggerUtil;
import com.uniops.core.util.MDCUtil;
import com.uniops.core.util.OkHttpUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
public class HttpRequestLogAspect {

    @Autowired
    private HttpRequestLogService httpRequestLogService;

    @Autowired
    private ObjectMapper objectMapper;
    @Resource
    ISystemRegisterService systemRegisterService;
    @Resource
    HttpCallLoggerUtil httpCallLoggerUtil;

    @Around("execution(* com..controller..*(..)) && " +
            "!execution(* com.uniops.core.controller.HttpRequestLogController.*(..))")
    public Object logHttpRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            String traceId = MDCUtil.generateTraceId();
            MDC.put(MDCUtil.TRACE_ID, traceId);
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = null;
            if (attributes != null) {
                request = attributes.getRequest();
            }
            HttpRequestLog logEntry = new HttpRequestLog();
            if (request != null) {
                logEntry.setApiPath(request.getRequestURI());
            }
            if (request != null) {
                logEntry.setHttpMethod(request.getMethod());
            }
            if (request != null) {
                logEntry.setUserAgent(request.getHeader("User-Agent"));
            }
            if (request != null) {
                logEntry.setClientIp(getClientIpAddress(request));
            }
            logEntry.setRequestTime(LocalDateTime.now());
            logEntry.setAppId(systemRegisterService.localSystem().getId());
            logEntry.setLogTraceId(traceId);
            // 记录请求参数
            try {
                logEntry.setRequestParams(objectMapper.writeValueAsString(getRequestParameters(joinPoint)));
            } catch (Exception e) {
                logEntry.setRequestParams("无法序列化参数");
            }
            long startTime = System.currentTimeMillis();
            Object result = null;
            String exceptionStack = null;

            try {
                log.info("""
                        ===========================
                        接收到请求：
                        请求路径:{}
                        入参:{}
                        链路id:{}
                        ===========================""", logEntry.getApiPath(), logEntry.getRequestParams(), traceId);
                result = joinPoint.proceed();
                return result;
            } catch (Exception e) {
                exceptionStack = getStackTraceAsString(e);
                throw e;
            } finally {
                long endTime = System.currentTimeMillis();
                logEntry.setDuration(endTime - startTime);
                logEntry.setResponseTime(LocalDateTime.now());

                if (result != null) {
                    try {
                        logEntry.setResponseMessage(objectMapper.writeValueAsString(result));
                    } catch (Exception e) {
                        logEntry.setResponseMessage("无法序列化响应");
                    }
                }

                if (exceptionStack != null) {
                    logEntry.setExceptionStack(exceptionStack);
                }

                // 获取响应状态码（如果有ResponseEntity等）
                logEntry.setStatusCode(200); // 默认成功状态

                // 保存日志
                try {
                    httpRequestLogService.save(logEntry);
                } catch (Exception e) {
                    // 避免日志记录失败影响主要业务逻辑
                    e.printStackTrace();
                }
                log.info("""
                        ===========================
                        请求处理完毕：
                        请求路径:{}
                        返参:{}
                        链路id:{}
                        ===========================""", logEntry.getApiPath(), logEntry.getResponseMessage(), traceId);


                //TODO
                //写一个调用第三方的测试例子
                httpCallLoggerUtil.easyLogHttpCallByPost("测试",
                        "http://localhost:8080/uni-ops",
                        null,
                        "测试测试测试", "返回返回返回", "", 100L);

            }
        } finally {
            MDC.remove(MDCUtil.TRACE_ID);
        }

    }

    private Object getRequestParameters(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return null;
        }
        // 过滤掉HttpServletRequest、HttpServletResponse等特殊参数
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof HttpServletRequest) {
                continue;
            }
        }
        return args.length == 1 ? args[0] : args;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 处理多级代理的情况，取第一个非unknown的有效IP
            int index = xForwardedFor.indexOf(",");
            if (index != -1) {
                return xForwardedFor.substring(0, index);
            } else {
                return xForwardedFor;
            }
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    private String getStackTraceAsString(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
