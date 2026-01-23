// src/main/java/com/uniops/core/entity/HttpRequestLog.java
package com.uniops.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("uniops_http_request_log")
public class HttpRequestLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("api_path")
    private String apiPath;           // 访问API
    @TableField("app_id")//外键，绑定uniops_system_register的id，这样才能获取到属于本机的日志
    private Long appId;

    @TableField("request_params")
    private String requestParams;     // 请求参数

    @TableField("request_time")
    private LocalDateTime requestTime; // 请求时间

    @TableField("response_message")
    private String responseMessage;   // 响应消息

    @TableField("response_time")
    private LocalDateTime responseTime; // 响应时间

    @TableField("duration")
    private Long duration;            // 耗时（毫秒）

    @TableField("exception_stack")
    private String exceptionStack;    // 异常栈

    @TableField("http_method")
    private String httpMethod;        // HTTP方法

    @TableField("user_agent")
    private String userAgent;         // 用户代理

    @TableField("client_ip")
    private String clientIp;          // 客户端IP

    @TableField("status_code")
    private Integer statusCode;       // 响应状态码
}
