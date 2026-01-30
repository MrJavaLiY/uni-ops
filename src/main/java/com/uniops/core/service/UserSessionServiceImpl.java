package com.uniops.core.service;// src/main/java/com/uniops/core/service/impl/UserSessionServiceImpl.java

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.uniops.core.entity.UserSession;
import com.uniops.core.mapper.UserSessionMapper;
import com.uniops.core.service.IUserSessionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserSessionServiceImpl extends ServiceImpl<UserSessionMapper, UserSession> implements IUserSessionService {

    private static final long SESSION_TIMEOUT_MINUTES = 120; // 2小时过期

    @Override
    public String createSession(String username, String ipAddress, String userAgent) {
        // 先删除该用户之前的会话
        invalidateUserSession(username);

        // 创建新会话
        UserSession session = new UserSession();
        session.setUsername(username);
        session.setSessionToken(UUID.randomUUID().toString());
        session.setLoginTime(LocalDateTime.now());
        session.setLastAccessTime(LocalDateTime.now());
        session.setExpiresTime(LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES));
        session.setStatus("ACTIVE");
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);

        save(session);
        return session.getSessionToken();
    }

    @Override
    public boolean validateSession(String sessionToken) {
        QueryWrapper<UserSession> wrapper = new QueryWrapper<>();
        wrapper.eq("session_token", sessionToken);
        wrapper.eq("status", "ACTIVE");
        wrapper.gt("expires_time", LocalDateTime.now());

        UserSession session = getOne(wrapper);
        if (session != null) {
            // 更新最后访问时间
            updateSessionAccess(sessionToken);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateSessionAccess(String sessionToken) {
        QueryWrapper<UserSession> wrapper = new QueryWrapper<>();
        wrapper.eq("session_token", sessionToken);
        wrapper.eq("status", "ACTIVE");

        UserSession session = new UserSession();
        session.setLastAccessTime(LocalDateTime.now());
        session.setExpiresTime(LocalDateTime.now().plusMinutes(SESSION_TIMEOUT_MINUTES));

        return update(session, wrapper);
    }

    @Override
    public boolean invalidateSession(String sessionToken) {
        QueryWrapper<UserSession> wrapper = new QueryWrapper<>();
        wrapper.eq("session_token", sessionToken);

        UserSession session = new UserSession();
        session.setStatus("INACTIVE");

        return update(session, wrapper);
    }

    @Override
    public void cleanupExpiredSessions() {
        QueryWrapper<UserSession> wrapper = new QueryWrapper<>();
        wrapper.le("expires_time", LocalDateTime.now());

        UserSession session = new UserSession();
        session.setStatus("INACTIVE");

        update(session, wrapper);
    }

    private void invalidateUserSession(String username) {
        QueryWrapper<UserSession> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        wrapper.eq("status", "ACTIVE");

        UserSession session = new UserSession();
        session.setStatus("INACTIVE");

        update(session, wrapper);
    }
}
