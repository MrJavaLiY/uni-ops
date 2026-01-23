// src/main/java/com/uniops/core/util/HttpCallLoggerUtil.java
package com.uniops.core.util;

import com.uniops.core.service.ThirdPartyHttpLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class HttpCallLoggerUtil {

    @Autowired
    private ThirdPartyHttpLogService thirdPartyHttpLogService;

    /**
     * 记录HTTP调用日志
     *
     * @param thirdPartyName 第三方服务名称
     * @param url 请求URL
     * @param headers 请求头
     * @param body 请求体
     * @param responseBody 响应体
     * @param errorMessage 错误信息
     * @param duration 耗时
     */
    public void easyLogHttpCallByPost(String thirdPartyName, String url,
                                  Map<String, String> headers, String body,
                                  String responseBody, String errorMessage, Long duration) {
        thirdPartyHttpLogService.recordHttpCall(thirdPartyName, url, "POST", headers, "", body,
                null, null, responseBody, errorMessage, duration);
    }
    /**
     * 记录HTTP调用日志
     *
     * @param thirdPartyName 第三方服务名称
     * @param url 请求URL
     * @param headers 请求头
     * @param body 请求体
     * @param responseStatus 响应状态码
     * @param responseHeaders 响应头
     * @param responseBody 响应体
     * @param errorMessage 错误信息
     * @param duration 耗时
     */
    public void logHttpCallByPost(String thirdPartyName, String url,
                            Map<String, String> headers, String body,
                            Integer responseStatus, Map<String, String> responseHeaders,
                            String responseBody, String errorMessage, Long duration) {
        thirdPartyHttpLogService.recordHttpCall(thirdPartyName, url, "POST", headers, "", body,
                responseStatus, responseHeaders, responseBody, errorMessage, duration);
    }


    /**
     * 记录HTTP调用日志
     *
     * @param thirdPartyName 第三方服务名称
     * @param url 请求URL
     * @param method HTTP方法
     * @param headers 请求头
     * @param params 请求参数
     * @param body 请求体
     * @param responseStatus 响应状态码
     * @param responseHeaders 响应头
     * @param responseBody 响应体
     * @param errorMessage 错误信息
     * @param duration 耗时
     */
    public void logHttpCall(String thirdPartyName, String url, String method,
                           Map<String, String> headers, String params, String body,
                           Integer responseStatus, Map<String, String> responseHeaders,
                           String responseBody, String errorMessage, Long duration) {
        thirdPartyHttpLogService.recordHttpCall(thirdPartyName, url, method, headers, params, body,
                responseStatus, responseHeaders, responseBody, errorMessage, duration);
    }

    /**
     * 记录成功的HTTP调用
     */
    public void logSuccessfulCall(String thirdPartyName, String url, String method,
                                  Map<String, String> headers, String params, String body,
                                  Integer responseStatus, Map<String, String> responseHeaders,
                                  String responseBody, Long startTime) {
        Long duration = System.currentTimeMillis() - startTime;
        logHttpCall(thirdPartyName, url, method, headers, params, body,
                responseStatus, responseHeaders, responseBody, null, duration);
    }

    /**
     * 记录失败的HTTP调用
     */
    public void logFailedCall(String thirdPartyName, String url, String method,
                              Map<String, String> headers, String params, String body,
                              String errorMessage, Long startTime) {
        Long duration = System.currentTimeMillis() - startTime;
        logHttpCall(thirdPartyName, url, method, headers, params, body,
                null, null, null, errorMessage, duration);
    }
}
