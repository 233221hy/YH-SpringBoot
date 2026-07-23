package cn.xfywz.guozespring.excel;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 原始导入错误信息 - 最小化数据结构
 */
@Data
@AllArgsConstructor
public class RawImportError {
    /**
     * Excel行号（从1开始）
     */
    private final int row;

    /**
     * 字段名/错误类型标识
     */
    private final String fieldOrType;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 错误类型
     */
    private final ErrorType errorType;

    /**
     * 错误类型枚举
     */
    public enum ErrorType {
        // JSR-303验证错误
        VALIDATION("字段验证错误"),
        // 业务规则错误
        BUSINESS("业务规则错误"),
        // 数据转换错误
        DATA_CONVERT("数据转换错误"),
        // 数据预处理错误
        PRE_PROCESS("数据预处理错误"),
        // 批量处理错误
        BATCH_PROCESS("批量处理错误"),
        // 系统错误
        SYSTEM("系统错误");

        private final String displayName;

        ErrorType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 创建JSR-303验证错误
     */
    public static RawImportError validationError(int row, String field, String message) {
        return new RawImportError(row, field, message, ErrorType.VALIDATION);
    }

    /**
     * 创建业务错误
     */
    public static RawImportError businessError(int row, String message) {
        return new RawImportError(row, "BUSINESS", message, ErrorType.BUSINESS);
    }

    /**
     * 创建数据转换错误
     */
    public static RawImportError dataConvertError(int row, String column, String message) {
        return new RawImportError(row, column, message, ErrorType.DATA_CONVERT);
    }

    @Override
    public String toString() {
        return String.format("第%d行[%s]: %s", row, fieldOrType, message);
    }
}
