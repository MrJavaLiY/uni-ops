// src/main/java/com/uniops/core/entity/UserSession.java
package com.uniops.core.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户会话实体
 */
@Data
@TableName("uniops_user_session")
public class UserSession {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("username")
    private String username;

    @TableField("session_token")
    private String sessionToken;

    @TableField("login_time")
    private LocalDateTime loginTime;

    @TableField("last_access_time")
    private LocalDateTime lastAccessTime;

    @TableField("expires_time")
    private LocalDateTime expiresTime;

    @TableField("status")
    private String status; // ACTIVE, INACTIVE

    @TableField("ip_address")
    private String ipAddress;

    @TableField("user_agent")
    private String userAgent;
}
