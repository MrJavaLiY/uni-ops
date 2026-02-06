package com.uniops.core.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class LicenseManager {

    // 盐值：应该保密，可以配置或从安全存储中获取
    private static final String SALT = "cdyx-ptjk-1923";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 1. 根据系统ID生成加密串（内部获取MAC地址）
     * @param systemId 系统ID
     * @return 加密串（Base64编码的SHA-256哈希值）
     */
    public static String buildEncryptionString(String systemId) {
        if (systemId == null || systemId.trim().isEmpty()) {
            throw new IllegalArgumentException("系统ID不能为空");
        }

        String macAddress = getFirstValidMacAddress();
        if (macAddress == null || macAddress.trim().isEmpty()) {
            throw new RuntimeException("无法获取有效的MAC地址");
        }

        // 规范化MAC地址（移除分隔符，统一为大写）
        String normalizedMac = macAddress.replaceAll("[:\\-]", "").toUpperCase();

        String input = systemId + ":" + normalizedMac + ":" + SALT;
        return base64Encode(sha256Hash(input));
    }

    /**
     * 1. 根据系统ID和MAC地址生成加密串（保持原有方法用于向后兼容）
     * @param systemId 系统ID
     * @param macAddress MAC地址
     * @return 加密串（Base64编码的SHA-256哈希值）
     */
    public static String buildEncryptionString(String systemId, String macAddress) {
        if (systemId == null || systemId.trim().isEmpty() ||
                macAddress == null || macAddress.trim().isEmpty()) {
            throw new IllegalArgumentException("系统ID和MAC地址不能为空");
        }

        // 规范化MAC地址（移除分隔符，统一为大写）
        String normalizedMac = macAddress.replaceAll("[:\\-]", "").toUpperCase();

        String input = systemId + ":" + normalizedMac + ":" + SALT;
        return base64Encode(sha256Hash(input));
    }

    /**
     * 获取第一个有效的MAC地址
     * @return MAC地址字符串，如果没有找到则返回null
     */
    private static String getFirstValidMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // 跳过回环接口、虚拟接口和未启用的接口
                if (networkInterface.isLoopback() || networkInterface.isVirtual() || !networkInterface.isUp()) {
                    continue;
                }

                byte[] mac = networkInterface.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X", mac[i]));
                        if (i < mac.length - 1) {
                            sb.append(":");
                        }
                    }
                    return sb.toString();
                }
            }

            // 如果没找到有效MAC地址，尝试获取本地主机地址的网络接口
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(localHost);
            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X", mac[i]));
                        if (i < mac.length - 1) {
                            sb.append(":");
                        }
                    }
                    return sb.toString();
                }
            }
        } catch (SocketException | java.net.UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 2. 根据加密串和授权日期生成授权串
     * 授权串格式：date:encryptedData:signature
     * 其中：encryptedData = base64(date + ":" + encryptionString)
     *      signature = base64(sha256(encryptionString + ":" + date + ":" + SALT))
     * @param encryptionString 加密串（由buildEncryptionString生成）
     * @param dateStr 授权日期，格式为yyyy-MM-dd
     * @return 授权串
     */
    public static String buildAuthorizationString(String encryptionString, String dateStr) {
        if (encryptionString == null || encryptionString.trim().isEmpty() ||
                dateStr == null || dateStr.trim().isEmpty()) {
            throw new IllegalArgumentException("加密串和日期不能为空");
        }

        // 验证日期格式
        try {
            LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期格式必须为yyyy-MM-dd");
        }

        // 构建加密数据：date:encryptionString
        String encryptedDataInput = dateStr + ":" + encryptionString;
        String encryptedData = base64Encode(encryptedDataInput.getBytes(StandardCharsets.UTF_8));

        // 构建签名：使用加密串、日期和盐值计算哈希
        String signatureInput = encryptionString + ":" + dateStr + ":" + SALT;
        String signature = base64Encode(sha256Hash(signatureInput));

        // 授权串格式：date:encryptedData:signature
        return dateStr + ":" + encryptedData + ":" + signature;
    }

    /**
     * 3. 校验授权串是否在有效期内
     * @param authorizationString 授权串（由buildAuthorizationString生成）
     * @param currentDateStr 当前日期，格式为yyyy-MM-dd
     * @return 授权验证结果
     */
    public static AuthorizationResult checkAuthorization(String authorizationString, String currentDateStr) {
        if (authorizationString == null || authorizationString.trim().isEmpty() ||
                currentDateStr == null || currentDateStr.trim().isEmpty()) {
            return new AuthorizationResult(false, "参数不能为空","");
        }

        // 解析授权串
        String[] parts = authorizationString.split(":");
        if (parts.length != 3) {
            return new AuthorizationResult(false, "授权串格式错误","");
        }

        String authorizedDateStr = parts[0];
        String encryptedData = parts[1];
        String storedSignature = parts[2];

        // 验证日期格式
        LocalDate authorizedDate, currentDate;
        try {
            authorizedDate = LocalDate.parse(authorizedDateStr, DATE_FORMAT);
            currentDate = LocalDate.parse(currentDateStr, DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return new AuthorizationResult(false, "日期格式错误,应该为yyyy-MM-dd","");
        }

        // 1. 检查授权日期是否已过期
        if (currentDate.isAfter(authorizedDate)) {
            return new AuthorizationResult(false, "授权已过期","");
        }

        // 2. 验证签名
        try {
            // 解密获取加密串和日期
            String decryptedData = new String(base64Decode(encryptedData), StandardCharsets.UTF_8);
            String[] dataParts = decryptedData.split(":");
            if (dataParts.length != 2) {
                return new AuthorizationResult(false, "加密数据格式错误","");
            }

            String decodedDate = dataParts[0];
            String decodedEncryptionString = dataParts[1];

            // 验证解码后的日期与授权串中的日期一致
            if (!decodedDate.equals(authorizedDateStr)) {
                return new AuthorizationResult(false, "日期数据不一致","");
            }

            // 重新计算签名
            String signatureInput = decodedEncryptionString + ":" + decodedDate + ":" + SALT;
            String calculatedSignature = base64Encode(sha256Hash(signatureInput));

            // 比较签名
            if (!calculatedSignature.equals(storedSignature)) {
                return new AuthorizationResult(false, "签名验证失败",authorizedDateStr);
            }

            // 3. 验证加密串（可选：如果需要验证加密串与系统ID/MAC的对应关系）
            // 注意：这里无法直接验证加密串，因为需要原始系统ID和MAC地址
            // 如果需要验证，可以在AuthorizationResult中返回加密串，由调用方进一步验证

            return new AuthorizationResult(true, "授权验证通过",authorizedDateStr);

        } catch (Exception e) {
            return new AuthorizationResult(false, "授权验证异常: " + e.getMessage(),"");
        }
    }

    /**
     * 4. 辅助方法：验证加密串是否与系统ID和MAC地址匹配
     * 注意：此方法需要原始系统ID和MAC地址
     */
    public static boolean verifyEncryptionString(String encryptionString, String systemId, String macAddress) {
        try {
            String calculated = buildEncryptionString(systemId, macAddress);
            return calculated.equals(encryptionString);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 辅助类：授权验证结果
     */
    public static class AuthorizationResult {
        private final boolean valid;
        private final String message;
        private final String authTime;
        private String encryptionString; // 可选：用于进一步验证

        public AuthorizationResult(boolean valid, String message, String authTime) {
            this.valid = valid;
            this.message = message;
            this.authTime = authTime;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }

        public void setEncryptionString(String encryptionString) {
            this.encryptionString = encryptionString;
        }

        public String getEncryptionString() {
            return encryptionString;
        }
        public String getAuthTime() {
            return authTime;
        }

    }

    /**
     * 辅助方法：计算SHA-256哈希
     */
    private static byte[] sha256Hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    private static byte[] sha256Hash(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 辅助方法：Base64编码
     */
    private static String base64Encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private static String base64Encode(String data) {
        return base64Encode(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 辅助方法：Base64解码
     */
    private static byte[] base64Decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    // 示例用法
    public static void main(String[] args) {

        // 模拟系统信息
        String systemId = "SYSTEM_001";



        // 1. 生成加密串（自动获取MAC地址）
        String encryptionString = LicenseManager.buildEncryptionString(systemId);
        System.out.println("加密串: " + encryptionString);

        // 2. 生成授权串（授权日期为2026-12-31）
        String authorizationString = LicenseManager.buildAuthorizationString("5TJ6psTRAqaroYWMHKyGqgYSrdROpIDLNcWmkk61lcw=", "2026-02-15");
        System.out.println("授权串: " + authorizationString);

        // 3. 校验授权（当前日期为2026-01-21）
        LicenseManager.AuthorizationResult result1 = LicenseManager.checkAuthorization(authorizationString, "2026-01-21");
        System.out.println("授权是否有效: " + result1.isValid() + " - " + result1.getMessage());

        // 4. 校验授权（当前日期为2027-01-01，已过期）
        LicenseManager.AuthorizationResult result2 = LicenseManager.checkAuthorization(authorizationString, "2027-01-01");
        System.out.println("授权是否有效: " + result2.isValid() + " - " + result2.getMessage());

        // 5. 校验加密串是否匹配系统信息
        String macAddress = getFirstValidMacAddress();
        boolean isMatch = LicenseManager.verifyEncryptionString(encryptionString, systemId, macAddress);
        System.out.println("加密串与系统信息匹配: " + isMatch);

        // 6. 模拟篡改的授权串
        String tamperedAuth = "2026-12-31:TAMPERED_DATA:INVALID_SIGNATURE";
        LicenseManager.AuthorizationResult result3 = LicenseManager.checkAuthorization(tamperedAuth, "2026-01-21");
        System.out.println("篡改授权串是否有效: " + result3.isValid() + " - " + result3.getMessage());
    }
}
