// src/main/java/com/uniops/core/entity/ThirdPartyHttpLog.java
package com.uniops.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("uniops_third_party_http_log")
public class ThirdPartyHttpLog {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("app_id")
    private Long appId;

    @TableField("url")
    private String url;                           // 请求URL

    @TableField("method")
    private String method;                        // HTTP方法

    @TableField("request_headers")
    private String requestHeaders;                // 请求头

    @TableField("request_params")
    private String requestParams;                 // 请求参数

    @TableField("request_body")
    private String requestBody;                   // 请求体

    @TableField("response_status")
    private Integer responseStatus;              // 响应状态码

    @TableField("response_headers")
    private String responseHeaders;               // 响应头

    @TableField("response_body")
    private String responseBody;                  // 响应体

    @TableField("request_time")
    private LocalDateTime requestTime;           // 请求时间

    @TableField("response_time")
    private LocalDateTime responseTime;          // 响应时间

    @TableField("duration")
    private Long duration;                       // 耗时（毫秒）

    @TableField("error_message")
    private String errorMessage;                  // 错误信息

    @TableField("third_party_name")
    private String thirdPartyName;               // 第三方服务名称

    @TableField("created_at")
    private LocalDateTime createdAt;             // 创建时间

    @TableField("updated_at")
    private LocalDateTime updatedAt;             // 更新时间
    @TableField("log_trace_id")
    private String logTraceId;                  // 日志追踪ID
}
