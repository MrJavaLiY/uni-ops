// src/main/java/com/uniops/core/controller/LoginController.java
package com.uniops.core.controller;

import com.uniops.core.condition.AuthCondition;
import com.uniops.core.response.ResponseResult;
import com.uniops.core.util.AuthConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "用户认证和会话管理")
public class LoginController {

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录并获取会话令牌")
    public ResponseResult<Map<String, Object>> login(@RequestBody AuthCondition credentials,
                                                     HttpServletRequest request) {
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        String sessionToken = AuthConstants.authenticateAndCreateSession(
                username, password,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));

        if (sessionToken != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("sessionToken", sessionToken);
            result.put("username", username);
            return ResponseResult.success(result);
        } else {
            return ResponseResult.error("用户名或密码错误");
        }
    }

    @PostMapping("/validate-session")
    @Operation(summary = "验证会话", description = "验证会话令牌是否有效")
    public ResponseResult<Boolean> validateSession(@RequestBody AuthCondition request) {
        String sessionToken = request.getSessionToken();
        boolean isValid = AuthConstants.validateSessionWithCache(sessionToken);
        return ResponseResult.success(isValid);
    }

    @PostMapping("/logout")
    @Operation(summary = "用户注销", description = "注销当前会话")
    public ResponseResult<String> logout(@RequestBody AuthCondition request) {
        String sessionToken = request.getSessionToken();
        AuthConstants.logoutSession(sessionToken);
        return ResponseResult.success("注销成功");
    }
}
