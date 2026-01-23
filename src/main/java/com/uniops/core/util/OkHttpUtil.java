// src/main/java/com/uniops/core/util/OkHttpUtil.java
package com.uniops.core.util;

import com.uniops.core.service.ThirdPartyHttpLogService;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Component
public class OkHttpUtil {

    @Autowired
    private ThirdPartyHttpLogService thirdPartyHttpLogService;

    private final OkHttpClient defaultHttpClient;

    public OkHttpUtil() {
        this.defaultHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 发送GET请求并记录日志
     *
     * @param thirdPartyName 第三方服务名称
     * @param url            请求URL
     * @param headers        请求头
     * @return 响应结果
     */
    public HttpResponseResult get(String thirdPartyName, String url, Map<String, String> headers) {
        return executeHttpRequest(thirdPartyName, url, "GET", headers, null, null);
    }

    /**
     * 发送POST请求并记录日志
     *
     * @param thirdPartyName 第三方服务名称
     * @param url            请求URL
     * @param headers        请求头
     * @param body           请求体
     * @return 响应结果
     */
    public HttpResponseResult post(String thirdPartyName, String url, Map<String, String> headers, String body) {
        return executeHttpRequest(thirdPartyName, url, "POST", headers, null, body);
    }

    /**
     * 发送POST请求并记录日志（表单提交）
     *
     * @param thirdPartyName 第三方服务名称
     * @param url            请求URL
     * @param headers        请求头
     * @param formParams     表单参数
     * @return 响应结果
     */
    public HttpResponseResult postForm(String thirdPartyName, String url, Map<String, String> headers, Map<String, String> formParams) {
        return executeHttpRequest(thirdPartyName, url, "POST", headers, formParams, null);
    }

    /**
     * 发送PUT请求并记录日志
     *
     * @param thirdPartyName 第三方服务名称
     * @param url            请求URL
     * @param headers        请求头
     * @param body           请求体
     * @return 响应结果
     */
    public HttpResponseResult put(String thirdPartyName, String url, Map<String, String> headers, String body) {
        return executeHttpRequest(thirdPartyName, url, "PUT", headers, null, body);
    }

    /**
     * 发送DELETE请求并记录日志
     *
     * @param thirdPartyName 第三方服务名称
     * @param url            请求URL
     * @param headers        请求头
     * @return 响应结果
     */
    public HttpResponseResult delete(String thirdPartyName, String url, Map<String, String> headers) {
        return executeHttpRequest(thirdPartyName, url, "DELETE", headers, null, null);
    }

    /**
     * 发送PATCH请求并记录日志
     *
     * @param thirdPartyName 第三方服务名称
     * @param url            请求URL
     * @param headers        请求头
     * @param body           请求体
     * @return 响应结果
     */
    public HttpResponseResult patch(String thirdPartyName, String url, Map<String, String> headers, String body) {
        return executeHttpRequest(thirdPartyName, url, "PATCH", headers, null, body);
    }

    /**
     * 通用的HTTP请求执行方法
     *
     * @param thirdPartyName 第三方服务名称
     * @param url            请求URL
     * @param method         HTTP方法
     * @param headers        请求头
     * @param formParams     表单参数
     * @param body           请求体
     * @return 响应结果
     */
    private HttpResponseResult executeHttpRequest(String thirdPartyName, String url, String method,
                                                  Map<String, String> headers, Map<String, String> formParams, String body) {
        Long startTime = System.currentTimeMillis();

        // 准备请求构建器
        Request.Builder requestBuilder = new Request.Builder().url(url);

        // 添加请求头
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        // 根据方法和参数类型构建请求体
        switch (method.toUpperCase()) {
            case "GET":
                requestBuilder.get();
                break;
            case "POST":
                if (formParams != null && !formParams.isEmpty()) {
                    // 构建表单请求体
                    FormBody.Builder formBuilder = new FormBody.Builder();
                    for (Map.Entry<String, String> entry : formParams.entrySet()) {
                        formBuilder.add(entry.getKey(), entry.getValue());
                    }
                    requestBuilder.post(formBuilder.build());
                } else if (body != null) {
                    // 构建JSON或其他类型的请求体
                    MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                    RequestBody requestBody = RequestBody.create(body, mediaType);
                    requestBuilder.post(requestBody);
                } else {
                    // 空请求体
                    requestBuilder.post(RequestBody.create("", MediaType.parse("text/plain")));
                }
                break;
            case "PUT":
                if (body != null) {
                    MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                    RequestBody requestBody = RequestBody.create(body, mediaType);
                    requestBuilder.put(requestBody);
                } else {
                    requestBuilder.put(RequestBody.create("", MediaType.parse("text/plain")));
                }
                break;
            case "DELETE":
                if (body != null) {
                    MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                    RequestBody requestBody = RequestBody.create(body, mediaType);
                    requestBuilder.delete(requestBody);
                } else {
                    requestBuilder.delete(null);
                }
                break;
            case "PATCH":
                if (body != null) {
                    MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
                    RequestBody requestBody = RequestBody.create(body, mediaType);
                    requestBuilder.patch(requestBody);
                } else {
                    requestBuilder.patch(RequestBody.create("", MediaType.parse("text/plain")));
                }
                break;
            default:
                requestBuilder.method(method.toUpperCase(), null);
                break;
        }

        Request request = requestBuilder.build();

        try {
            // 执行请求
            Response response = defaultHttpClient.newCall(request).execute();

            // 解析响应
            String responseBody = response.body() != null ? response.body().string() : null;
            int responseCode = response.code();
            Map<String, String> responseHeaders = new HashMap<>();
            for (String name : response.headers().names()) {
                responseHeaders.put(name, response.headers().get(name));
            }

            // 计算耗时
            Long duration = System.currentTimeMillis() - startTime;

            // 记录成功日志
            logSuccessfulCall(thirdPartyName, url, method, headers, formParams != null ? formParams.toString() : "", body,
                    responseCode, responseHeaders, responseBody, startTime);

            return new HttpResponseResult(true, responseCode, responseBody, responseHeaders, null);

        } catch (IOException e) {
            // 计算耗时
            Long duration = System.currentTimeMillis() - startTime;

            // 记录失败日志
            logFailedCall(thirdPartyName, url, method, headers, formParams != null ? formParams.toString() : "", body,
                    e.getMessage(), startTime);

            return new HttpResponseResult(false, null, null, null, e.getMessage());
        }
    }

    /**
     * 记录成功的HTTP调用
     */
    private void logSuccessfulCall(String thirdPartyName, String url, String method,
                                   Map<String, String> headers, String params, String body,
                                   Integer responseStatus, Map<String, String> responseHeaders,
                                   String responseBody, Long startTime) {
        Long duration = System.currentTimeMillis() - startTime;

        thirdPartyHttpLogService.recordHttpCall(thirdPartyName, url, method,
                headers, params, body, responseStatus, responseHeaders,
                responseBody, null, duration);
    }

    /**
     * 记录失败的HTTP调用
     */
    private void logFailedCall(String thirdPartyName, String url, String method,
                               Map<String, String> headers, String params, String body,
                               String errorMessage, Long startTime) {
        Long duration = System.currentTimeMillis() - startTime;

        thirdPartyHttpLogService.recordHttpCall(thirdPartyName, url, method,
                headers, params, body, null, null, null,
                errorMessage, duration);
    }
}

// 响应结果封装类
class HttpResponseResult {
    private boolean success;
    private Integer statusCode;
    private String responseBody;
    private Map<String, String> responseHeaders;
    private String errorMessage;

    public HttpResponseResult(boolean success, Integer statusCode, String responseBody,
                              Map<String, String> responseHeaders, String errorMessage) {
        this.success = success;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders;
        this.errorMessage = errorMessage;
    }

    // Getter方法
    public boolean isSuccess() {
        return success;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
