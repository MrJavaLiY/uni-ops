package com.uniops.core.condition;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import javax.annotation.PostConstruct;
import java.net.*;
import java.util.Enumeration;

/**
 * SystemCondition 类的简要描述
 *
 * @author liyang
 * @since 2026/1/16
 */
@Data
@Configuration
public class SystemCondition {
    @Value("${spring.application.name}")
    private String applicationName;
    @Value("${spring.application.chinese.name}")
    private String applicationChineseName;
    private String ip;
    @Value("${server.port}")
    private String port;
    @Value("${server.servlet.context-path}")
    private String servletPath;

    @PostConstruct
    public void init() throws SocketException {
        this.ip = getIpAddress();
    }

    private String getIpAddress() throws SocketException {
        String localIp = null;
        String serverIp = null;

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();

            // 跳过回环接口、虚拟接口和未启用的接口
            if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress address = addresses.nextElement();

                // 只考虑 IPv4 地址
                if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                    String ipAddress = address.getHostAddress();

                    // 优先考虑非内网 IP 地址
                    if (isPrivateIp(ipAddress)) {
                        localIp = ipAddress;
                    } else {
                        serverIp = ipAddress;
                        break; // 如果找到公网 IP，直接返回
                    }
                }
            }
        }

        // 优先返回公网 IP，否则返回内网 IP，最后返回默认本地地址
        if (serverIp != null) {
            return serverIp;
        } else if (localIp != null) {
            return localIp;
        } else {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                return "127.0.0.1";
            }
        }
    }

    /**
     * 判断是否为私有 IP 地址
     * 私有 IP 地址范围:
     * 10.0.0.0 - 10.255.255.255
     * 172.16.0.0 - 172.31.255.255
     * 192.168.0.0 - 192.168.255.255
     */
    private boolean isPrivateIp(String ip) {
        try {
            InetAddress inetAddr = InetAddress.getByName(ip);
            return inetAddr.isSiteLocalAddress() || inetAddr.isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
