package com.uniops.core.controller;

import com.uniops.core.response.ResponseResult;
import com.uniops.core.util.AuthConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * LoginController 类的简要描述
 *
 * @author liyang
 * @since 2026/1/20
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "用户认证", description = "用户登录登出相关接口")
public class LoginController {

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    public ResponseResult<Map<String, Object>> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        // 验证用户名和密码
        if (AuthConstants.USERNAME.equals(loginRequest.getUsername()) &&
            AuthConstants.PASSWORD.equals(loginRequest.getPassword())) {

            // 创建session并保存用户信息
            HttpSession session = request.getSession();
            session.setAttribute("user", loginRequest.getUsername());

            response.put("success", true);
            response.put("message", "登录成功");
            response.put("username", loginRequest.getUsername());

            return ResponseResult.success(response);
        } else {
            response.put("success", false);
            response.put("message", "用户名或密码错误");
            return ResponseResult.error(500,"用户名或密码错误",response);
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();

        // 销毁session
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        response.put("success", true);
        response.put("message", "登出成功");

        return ResponseEntity.ok(response);
    }

    // 登录请求对象
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
