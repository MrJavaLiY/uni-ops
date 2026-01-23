// src/main/java/com/uniops/core/controller/ThirdPartyHttpLogController.java
package com.uniops.core.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.uniops.core.condition.ThirdPartyLogCondition;
import com.uniops.core.entity.ThirdPartyHttpLog;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.service.ThirdPartyHttpLogService;
import com.uniops.core.vo.ThirdPartyHttpLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/business/third-party-http-log")
@Tag(name = "第三方HTTP调用日志管理", description = "管理第三方HTTP调用日志")
public class ThirdPartyHttpLogController {

    @Autowired
    private ThirdPartyHttpLogService thirdPartyHttpLogService;

    @PostMapping("/list")
    @Operation(summary = "获取第三方HTTP调用日志列表", description = "根据条件分页获取第三方HTTP调用日志")
    public ResponseResult<ThirdPartyHttpLogVO> getLogs(
            @RequestBody ThirdPartyLogCondition condition) {
        IPage<ThirdPartyHttpLog> page = thirdPartyHttpLogService.getLogsByCondition(condition);
        ThirdPartyHttpLogVO thirdPartyHttpLogVO = new ThirdPartyHttpLogVO();
        thirdPartyHttpLogVO.setPage(condition.getPage());
        thirdPartyHttpLogVO.setSize(condition.getSize());
        thirdPartyHttpLogVO.setTotal(page.getTotal());
        thirdPartyHttpLogVO.setRecords(page.getRecords());
        thirdPartyHttpLogVO.setTotalPages((int) page.getPages());
        return ResponseResult.success(thirdPartyHttpLogVO);
    }

    @PostMapping("/error-list")
    @Operation(summary = "获取错误日志列表", description = "获取包含错误的第三方HTTP调用日志")
    public ResponseResult<IPage<ThirdPartyHttpLog>> getErrorLogs(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {

        Page<ThirdPartyHttpLog> page = new Page<>(current, size);
        return ResponseResult.success(thirdPartyHttpLogService.getErrorLogs(page));
    }

    @PostMapping("/clean-expired")
    @Operation(summary = "清理过期日志", description = "清理指定时间之前的日志")
    public ResponseResult<Boolean> cleanExpiredLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime expireTime) {

        boolean result = thirdPartyHttpLogService.cleanExpiredLogs(expireTime);
        return ResponseResult.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取单条日志详情", description = "根据ID获取第三方HTTP调用日志详情")
    public ResponseResult<ThirdPartyHttpLog> getLogDetail(@PathVariable Long id) {
        ThirdPartyHttpLog log = thirdPartyHttpLogService.getById(id);
        return ResponseResult.success(log);
    }
}
