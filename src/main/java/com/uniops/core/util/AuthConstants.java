// src/main/java/com/uniops/core/util/AuthConstants.java
package com.uniops.core.util;

import com.uniops.core.cache.SessionCacheManager;
import com.uniops.core.service.IUserSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 认证常量类
 */
@Component
public class AuthConstants {
    public static final String DEFAULT_USERNAME = "admin";
    public static final String PASSWORD_BASE = "uniops";

    @Autowired
    private static IUserSessionService userSessionService;

    @Autowired
    private static SessionCacheManager sessionCacheManager;

    /**
     * 设置服务实例（通过setter注入，避免循环依赖）
     */
    public static void setUserSessionService(IUserSessionService service) {
        userSessionService = service;
    }

    /**
     * 设置缓存管理器实例
     */
    public static void setSessionCacheManager(SessionCacheManager manager) {
        sessionCacheManager = manager;
    }

    /**
     * 获取当前密码（基础密码+年月）
     * @return 加密后的密码
     */
    public static String getCurrentPassword() {
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return PASSWORD_BASE + yearMonth;
    }

    /**
     * 验证用户名和密码并创建会话
     * @param username 用户名
     * @param password 密码
     * @param ipAddress IP地址
     * @param userAgent User Agent
     * @return 会话令牌，如果验证失败则返回null
     */
    public static String authenticateAndCreateSession(String username, String password, String ipAddress, String userAgent) {
        if (DEFAULT_USERNAME.equals(username) && getCurrentPassword().equals(password)) {
            // 创建数据库会话
            return userSessionService.createSession(username, ipAddress, userAgent);
        }
        return null;
    }

    /**
     * 验证会话是否有效（带本地缓存）
     * @param sessionToken 会话令牌
     * @return 验证结果
     */
    public static boolean validateSessionWithCache(String sessionToken) {
        // 首先检查本地缓存
        Boolean cachedResult = sessionCacheManager.getCachedSessionValidity(sessionToken);
        if (cachedResult != null) {
            if (cachedResult) {
                // 如果缓存有效，更新访问时间
                userSessionService.updateSessionAccess(sessionToken);
            }
            return cachedResult;
        }

        // 缓存未命中，查询数据库
        boolean isValid = userSessionService.validateSession(sessionToken);

        // 将结果缓存
        sessionCacheManager.cacheSessionValidity(sessionToken, isValid);

        return isValid;
    }

    /**
     * 注销会话
     * @param sessionToken 会话令牌
     */
    public static void logoutSession(String sessionToken) {
        userSessionService.invalidateSession(sessionToken);
        sessionCacheManager.removeCachedSession(sessionToken);
    }

    /**
     * 清理会话缓存和数据库中的过期会话
     */
    public static void cleanupSessions() {
        userSessionService.cleanupExpiredSessions();
        sessionCacheManager.cleanupExpiredCache();
    }
}
