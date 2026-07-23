package cn.xfywz.guozespring.util;

/**
 * 时间格式化工具类
 */
public final class TimeFormatUtil {
    private TimeFormatUtil() {}

    /**
     * 将秒数格式化为可读的时间字符串
     * @param seconds 秒数
     * @return 格式化后的时间字符串，例如："1小时30分15秒"
     */
    public static String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("小时");
        if (m > 0) sb.append(m).append("分");
        if (s > 0 || sb.isEmpty()) sb.append(s).append("秒");
        return sb.toString();
    }
}