package com.uniops.core.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证常量类
 */
public class AuthConstants {
    public static final String USERNAME = "admin";
    public static final String PASSWORD_BASE = "uniops";
    private static final long SESSION_TIMEOUT_MINUTES = 120; // 2小时超时

    // 存储用户最后活跃时间
    private static final ConcurrentHashMap<String, LocalDateTime> USER_ACTIVITY_MAP = new ConcurrentHashMap<>();

    /**
     * 获取当前密码（基础密码+年月）
     *
     * @return 加密后的密码
     */
    public static String getCurrentPassword() {
        String yearMonth = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        return PASSWORD_BASE + yearMonth;
    }

    /**
     * 验证用户名和密码
     *
     * @param username 用户名
     * @param password 密码
     * @return 验证结果
     */
    public static boolean validateCredentials(String username, String password) {
        if (USERNAME.equals(username) && getCurrentPassword().equals(password)) {
            // 更新用户活跃时间
            updateUserActivity(username);
            return true;
        }
        return false;
    }

    /**
     * 更新用户的最后活跃时间
     *
     * @param username 用户名
     */
    public static void updateUserActivity(String username) {
        USER_ACTIVITY_MAP.put(username, LocalDateTime.now());
    }

    /**
     * 检查用户是否活跃（在过去2小时内有活动）
     *
     * @param username 用户名
     * @return 是否活跃
     */
    public static boolean isUserActive(String username) {
        LocalDateTime lastActivity = USER_ACTIVITY_MAP.get(username);
        if (lastActivity == null) {
            return false;
        }

        // 计算时间差
        long minutesDiff = java.time.Duration.between(lastActivity, LocalDateTime.now()).toMinutes();
        return minutesDiff <= SESSION_TIMEOUT_MINUTES;
    }

    /**
     * 获取用户最后活跃时间
     *
     * @param username 用户名
     * @return 最后活跃时间
     */
    public static LocalDateTime getLastActivityTime(String username) {
        return USER_ACTIVITY_MAP.get(username);
    }

    /**
     * 手动清除用户活跃状态
     *
     * @param username 用户名
     */
    public static void clearUserActivity(String username) {
        USER_ACTIVITY_MAP.remove(username);
    }
}
