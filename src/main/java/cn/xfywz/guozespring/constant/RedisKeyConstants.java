package cn.xfywz.guozespring.constant;

public class RedisKeyConstants {
    // 精确key：online:{userType}:{schoolId}:{userId}
    public static final String ONLINE_USER_LAST_ACTIVE = "online:%s:%s:%s";
    // 模糊pattern：online:{userType}:{schoolId}:*
    public static final String ONLINE_USER_PATTERN = "online:%s:%s:*";

    public static final String USER_TYPE_STUDENT = "student";
    public static final String USER_TYPE_MANAGE = "manage";

    // 【原有方法】构建精确key（用于存储用户活跃时间）
    public static String buildOnlineUserKey(String userType, Long schoolId, String userId) {
        return String.format(ONLINE_USER_LAST_ACTIVE, userType, schoolId, userId);
    }

    // 【新增方法】构建模糊匹配pattern（用于统计时按学校+类型查询）
    public static String buildOnlineUserPattern(String userType, Long schoolId) {
        return String.format(ONLINE_USER_PATTERN, userType, schoolId);
    }
}