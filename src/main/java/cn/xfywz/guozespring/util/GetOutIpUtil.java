package cn.xfywz.guozespring.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetOutIpUtil {
//    public static String getOutIp() {
//        String ip = "";
//        try {
//            URL url = new URL("https://txt.go.sohu.com/ip/soip");
//            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8));
//            String line;
//            while ((line = in.readLine()) != null) {
//                ip=line;
//            }
//            in.close();
//            String pattern = "sohu_user_ip=\"(\\d+\\.\\d+\\.\\d+\\.\\d+)\"";
//            Pattern regex = Pattern.compile(pattern);
//            Matcher matcher = regex.matcher(ip);
//
//            if (matcher.find()) {
//                return matcher.group(1); // 提取IP地址
//            }
//            return null;
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }

    public static String getOutIp() {
        try {
            URL url = new URL("https://api.ipify.org");
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
                return in.readLine();
            }
        } catch (Exception e) {
            // 记录日志，但不要抛出 RuntimeException 阻断登录！
            System.err.println("获取公网IP失败，使用本地回环地址: " + e.getMessage());
            return "127.0.0.1";
        }
    }
}
