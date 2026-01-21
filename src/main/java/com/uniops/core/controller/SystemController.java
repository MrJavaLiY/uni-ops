package com.uniops.core.controller;

import com.uniops.core.condition.SystemRequestCondition;
import com.uniops.core.entity.SystemRegister;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.service.ISystemManagerService;
import com.uniops.core.service.ISystemRegisterService;
import com.uniops.core.util.LicenseManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SystemController 类的简要描述
 *
 * @author liyang
 * @since 2026/1/21
 */
@RestController
@RequestMapping("/system")
@Slf4j
@Tag(name = "系统接口")
public class SystemController {
    @Resource
    ISystemRegisterService systemRegisterService;
    @Resource
    ISystemManagerService systemManagerService;

    @PostMapping("/localSystram")
    @Operation(summary = "获取本地系统信息")
    public ResponseResult<SystemRegister> localSystem() {
        return ResponseResult.success(systemRegisterService.localSystem());
    }

    @PostMapping("/searchList")
    @Operation(summary = "获取系统列表")
    public ResponseResult<List<SystemRegister>> searchList(@RequestBody SystemRequestCondition condition) {
        return ResponseResult.success(systemManagerService.searchList(condition));
    }

    @PostMapping("/checkSystemValidity")
    @Operation(summary = "检查系统是否有效")
    public ResponseResult<Boolean> checkSystemValidity( @RequestBody SystemRequestCondition condition) {
        return ResponseResult.success(systemRegisterService.checkSystemValidity(condition));
    }

    @PostMapping("/auth")
    @Operation(summary = "授权系统")
    public ResponseResult<Boolean> auth(@RequestBody SystemRequestCondition condition) {
        systemRegisterService.auth(condition);
        return ResponseResult.success(true);
    }

    @PostMapping("/authStr")
    @Operation(summary = "授权系统1")
    public ResponseResult<String> authStr(@RequestBody SystemRequestCondition condition) {
        return ResponseResult.success(LicenseManager.buildAuthorizationString(condition.getSecretKey(),"2026-02-01"));
    }
}
