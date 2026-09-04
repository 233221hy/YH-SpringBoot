package cn.xfywz.guozespring.util;

import org.apache.commons.lang3.StringUtils;
import java.util.HashSet;
import java.util.Set;

//邮箱格式验证
public class EmailUtil {
    private static final Set<String> ALLOW_DOMAIN = new HashSet<>();
    static {
        ALLOW_DOMAIN.add("qq.com");
        ALLOW_DOMAIN.add("vip.qq.com");
        ALLOW_DOMAIN.add("foxmail.com");
        ALLOW_DOMAIN.add("163.com");
        ALLOW_DOMAIN.add("126.com");
        ALLOW_DOMAIN.add("yeah.net");
        ALLOW_DOMAIN.add("188.com");
        ALLOW_DOMAIN.add("139.com");
        ALLOW_DOMAIN.add("189.cn");
        ALLOW_DOMAIN.add("wo.cn");
        ALLOW_DOMAIN.add("sina.com");
        ALLOW_DOMAIN.add("sina.cn");
        ALLOW_DOMAIN.add("aliyun.com");
        ALLOW_DOMAIN.add("sohu.com");
        ALLOW_DOMAIN.add("tom.com");
        ALLOW_DOMAIN.add("21cn.com");
    }

    public static boolean isAllowEmail(String email) {
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            return false;
        }
        String domain = email.substring(email.lastIndexOf("@") + 1).toLowerCase();
        return ALLOW_DOMAIN.contains(domain);
    }
}