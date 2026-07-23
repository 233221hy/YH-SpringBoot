package cn.xfywz.guozespring.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * 密码工具类
 */
public class EncodePasswordUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String encodePassword(String rawPassword) {
        if (!StringUtils.hasText(rawPassword)) {
            throw new IllegalArgumentException("密码不能为空");
        }
        return encoder.encode(rawPassword);
    }

    /**
     * 验证密码
     * @param rawPassword 原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(encodedPassword)) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 生成随机密码
     * @param length 密码长度（默认8位）
     * @return 随机密码
     */
    public static String generateRandomPassword(int length) {
        if (length <= 0) {
            length = 8;
        }

        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();

        // 确保至少包含一个字母和一个数字
        password.append(chars.charAt((int) (Math.random() * 52))); // 字母
        password.append(chars.charAt(52 + (int) (Math.random() * 10))); // 数字

        // 填充剩余字符
        for (int i = 2; i < length; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }

        // 打乱顺序
        char[] array = password.toString().toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }

        return new String(array);
    }

    /**
     * 验证密码强度
     * @param password 密码
     * @return 验证结果，返回null表示通过
     */
    public static String validatePasswordStrength(String password) {
        if (!StringUtils.hasText(password)) {
            return "密码不能为空";
        }

        if (password.length() < 6) {
            return "密码长度不能少于6位";
        }

        if (password.length() > 20) {
            return "密码长度不能超过20位";
        }

        // 检查是否包含字母和数字
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");

        if (!hasLetter || !hasDigit) {
            return "密码必须包含字母和数字";
        }

        // 检查字符是否在允许范围内
        if (!password.matches("^[a-zA-Z0-9@$!%*?&.]+$")) {
            return "密码只能包含字母、数字及 @$!%*?&. 这些特殊字符";
        }

        return null; // 验证通过
    }
}

