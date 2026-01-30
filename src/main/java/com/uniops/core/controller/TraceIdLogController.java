package com.uniops.core.controller;

import com.uniops.core.condition.TraceIdLogCondition;
import com.uniops.core.entity.LogInfo;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.service.TraceIdLogService;
import com.uniops.core.vo.LogInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * TraceIdLogController 控制器
 * 提供根据traceId和时间范围检索日志文件的接口
 *
 * @author liyang
 * @since 2026/1/30
 */
@RestController
@RequestMapping("/trace-log")
@Tag(name = "链路日志查询", description = "根据traceId和时间范围检索日志文件")
public class TraceIdLogController {

    @Autowired
    private TraceIdLogService traceIdLogService;

    @PostMapping("/file-logs")
    @Operation(summary = "根据traceId查询日志文件", description = "根据traceId和时间范围查询日志文件内容，支持分页")
    public ResponseResult<LogInfoVO> getFileLogsByTraceId(@RequestBody TraceIdLogCondition condition) {
        LogInfoVO result = traceIdLogService.getLogLinesByTraceId(condition);
        return ResponseResult.success(result);
    }

    @PostMapping("/full-trace-file")
    @Operation(summary = "获取完整链路日志文件", description = "获取指定traceId的完整链路日志（从日志文件中），支持分页")
    public ResponseResult<LogInfoVO> getFullTraceLogFile(@RequestBody TraceIdLogCondition condition) {
        LogInfoVO fullTraceLogs = traceIdLogService.getFullTraceLog(
            condition.getTraceId(),
            condition.getStartTime(),
            condition.getEndTime(),
            condition.getPage() != 0 ? condition.getPage() : 1,
            condition.getSize() !=  0 ? condition.getSize() : 10
        );
        return ResponseResult.success(fullTraceLogs);
    }
}
