package com.uniops.starter.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@ConfigurationProperties(prefix = "uniops")
@Data
@Component
public class UniOpsProperties {

    private boolean enabled = true;
    private String contextPath = "/uni-ops";
    /**
     * 自定义页面的跳转地址，需要从http开始的完整可跳转地址
     */
    private String otherWebPath;
    /**
     * 需要认证的路径前缀,需要登录才能访问的接口的前缀,一般来说不包含content-path
     */
    private List<String> includeAuthPathPrefixes;
}
