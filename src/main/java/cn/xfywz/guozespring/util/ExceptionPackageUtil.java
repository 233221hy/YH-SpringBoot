package cn.xfywz.guozespring.util;

/**
 * 异常包名过滤工具类
 */
public final class ExceptionPackageUtil {

    private ExceptionPackageUtil() {
        // 工具类禁止实例化
    }

    /**
     * 判断异常是否由指定基础包（或其子包）中的类直接抛出
     *
     * @param throwable   异常对象
     * @param basePackage 基础包名，例如 "cn.xfywz.guozespring"
     * @return true 表示该异常来自目标包
     */
    public static boolean isThrownFromPackage(Throwable throwable, String basePackage) {
        if (throwable == null || basePackage == null) {
            return false;
        }

        StackTraceElement[] stackTrace = throwable.getStackTrace();
        if (stackTrace.length == 0) {
            return false;
        }

        // 获取异常最初被 throw 的位置（栈顶）
        String className = stackTrace[0].getClassName();

        // 精确匹配包前缀（防止 "cn.xfywz.guozespringx" 被误匹配）
        return className.startsWith(basePackage + ".");
    }
}