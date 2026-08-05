package cn.xfywz.guozespring.util;

import java.util.concurrent.ThreadLocalRandom;

public class RandomPwdUtil {
    //随机生成大小写字母和数字组合的密码
    public static String generateRandomCode(int length) {
        // 字符池：大写A-Z，小写a-z，数字0-9
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        // 调用循环生成
        for (int i = 0; i < length; i++) {
            int index = ThreadLocalRandom.current().nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }
}
