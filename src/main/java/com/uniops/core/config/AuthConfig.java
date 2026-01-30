// src/main/java/com/uniops/core/config/AuthConfig.java
package com.uniops.core.config;

import com.uniops.core.cache.SessionCacheManager;
import com.uniops.core.service.IUserSessionService;
import com.uniops.core.util.AuthConstants;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 认证配置类
 */
@Component
public class AuthConfig {

    @Autowired
    private IUserSessionService userSessionService;

    @Autowired
    private SessionCacheManager sessionCacheManager;

    @PostConstruct
    public void initAuthConstants() {
        // 初始化认证常量类的服务引用
        AuthConstants.setUserSessionService(userSessionService);
        AuthConstants.setSessionCacheManager(sessionCacheManager);
    }
}
