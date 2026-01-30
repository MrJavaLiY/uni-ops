// src/main/java/com/uniops/core/service/IUserSessionService.java
package com.uniops.core.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.uniops.core.entity.UserSession;

public interface IUserSessionService extends IService<UserSession> {
    /**
     * 创建用户会话
     */
    String createSession(String username, String ipAddress, String userAgent);

    /**
     * 验证会话是否有效
     */
    boolean validateSession(String sessionToken);

    /**
     * 更新会话访问时间
     */
    boolean updateSessionAccess(String sessionToken);

    /**
     * 注销会话
     */
    boolean invalidateSession(String sessionToken);

    /**
     * 清理过期会话
     */
    void cleanupExpiredSessions();
}
