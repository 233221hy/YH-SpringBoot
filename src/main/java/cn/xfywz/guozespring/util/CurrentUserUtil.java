package cn.xfywz.guozespring.util;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从 AOP 鉴权存入的 request attribute 中读取当前用户信息，
 * 避免业务层重复解析 JWT token。
 */
public class CurrentUserUtil {

    public static final String CURRENT_USER_ID_ATTR = "currentUserId";
    public static final String CURRENT_USER_SUBJECT_ATTR = "currentUserSubject";

    /** 获取当前请求的 userId（由 AuthAspect 在鉴权时存入） */
    public static Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object userId = request.getAttribute(CURRENT_USER_ID_ATTR);
        if (userId instanceof Long) {
            return (Long) userId;
        }
        if (userId instanceof Number) {
            return ((Number) userId).longValue();
        }
        return null;
    }

    /** 获取当前用户的 JWT subject JSON（由 AuthAspect 在鉴权时存入） */
    public static JSONObject getCurrentUserSubject() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        Object subject = request.getAttribute(CURRENT_USER_SUBJECT_ATTR);
        if (subject instanceof String) {
            return com.alibaba.fastjson2.JSON.parseObject((String) subject);
        }
        return null;
    }
}