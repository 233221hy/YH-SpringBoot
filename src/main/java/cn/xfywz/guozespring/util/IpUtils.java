package cn.xfywz.guozespring.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

public class IpUtils {

    private static final List<String> LOCAL_ADDRESSES = Arrays.asList("127.0.0.1", "0:0:0:0:0:0:0:1", "localhost");

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";

        // 1. X-Forwarded-For（最常用，支持多层代理）
        String ip = request.getHeader("X-Forwarded-For");
        if (isNotBlank(ip)) {
            // X-Forwarded-For 格式：client, proxy1, proxy2
            // 取第一个非 unknown 的 IP
            for (String candidate : ip.split(",")) {
                candidate = candidate.trim();
                if (!"unknown".equalsIgnoreCase(candidate) && !LOCAL_ADDRESSES.contains(candidate)) {
                    return candidate;
                }
            }
        }

        // 2. 其他常见代理头
        ip = request.getHeader("X-Real-IP");
        if (isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) return ip;

        ip = request.getHeader("Proxy-Client-IP");
        if (isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) return ip;

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) return ip;

        // 3. 最后 fallback
        ip = request.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip != null ? ip : "unknown";
    }

    private static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }
}