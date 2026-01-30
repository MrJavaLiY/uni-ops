package com.uniops.core.condition;

import lombok.Data;

/**
 * AuthCondition 类的简要描述
 *
 * @author liyang
 * @since 2026/1/30
 */
@Data
public class AuthCondition {
    private String username;
    private String password;
    private String sessionToken;
}
