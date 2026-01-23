// src/main/java/com/uniops/core/controller/HttpRequestLogController.java
package com.uniops.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uniops.core.condition.HttpLogRequestCondition;
import com.uniops.core.entity.HttpRequestLog;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.service.HttpRequestLogService;
import com.uniops.core.vo.HttpLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/business/http-request-log")
@Tag(name = "HTTP请求日志管理", description = "管理HTTP请求日志")
public class HttpRequestLogController {

    @Autowired
    private HttpRequestLogService httpRequestLogService;

    @PostMapping("/list")
    @Operation(summary = "获取请求日志列表", description = "根据条件分页获取请求日志")
    public ResponseResult<HttpLogVO> getLogs(
            @RequestBody HttpLogRequestCondition condition) {
        HttpLogVO httpLogVO = new HttpLogVO();
        IPage<HttpRequestLog> pageR = httpRequestLogService.getLogsByCondition(condition);
        httpLogVO.setRecords(pageR.getRecords());
        httpLogVO.setTotal(pageR.getTotal());
        httpLogVO.setTotalPages((int) pageR.getPages());
        httpLogVO.setPage(condition.getPage());
        httpLogVO.setSize(condition.getSize());
        return ResponseResult.success(httpLogVO);
    }

    @PostMapping("/error-list")
    @Operation(summary = "获取错误日志列表", description = "获取包含异常的请求日志")
    public ResponseResult<IPage<HttpRequestLog>> getErrorLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {

        Page<HttpRequestLog> page = new Page<>(current, size);
        return ResponseResult.success(httpRequestLogService.getErrorLogs(page));
    }

    @PostMapping("/clean-expired")
    @Operation(summary = "清理过期日志", description = "清理指定时间之前的日志")
    public ResponseResult<Boolean> cleanExpiredLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expireTime) {

        boolean result = httpRequestLogService.cleanExpiredLogs(expireTime);
        return ResponseResult.success(result);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取统计信息", description = "获取请求日志统计信息")
    public ResponseResult<Object> getStatistics() {
        // 可以添加各种统计逻辑
        return ResponseResult.success("统计功能待实现");
    }
}
