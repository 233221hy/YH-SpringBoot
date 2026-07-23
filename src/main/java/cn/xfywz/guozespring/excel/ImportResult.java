package cn.xfywz.guozespring.excel;

import cn.xfywz.guozespring.excel.ImportErrorFormatter;
import cn.xfywz.guozespring.excel.listener.ExcelReadListener;
import cn.xfywz.guozespring.excel.RawImportError;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一导入结果 - 负责最终结果封装
 */
@Slf4j
@Data
@Builder
public class ImportResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 错误格式化器
    private static final ImportErrorFormatter ERROR_FORMATTER = new ImportErrorFormatter();

    // 常量：最大显示错误数
    private static final int MAX_ERROR_DISPLAY = 10;

    //Excel总读取行数
    private int totalRead;

    //解析阶段校验通过行数（监听器统计）
    private int validCount;

    //实际数据库插入成功行数（Service统计）
    private int dbInserted;

    //失败的行数（去重后的）
    private int failedRows;

    //总耗时（毫秒）
    private long durationMs;

    //错误信息（截断后）
    private List<String> errors;

    //错误总数（包括被截断的）
    private int totalErrors;

    //是否包含被截断的错误
    private boolean hasTruncatedErrors;

    //主要错误类型
    private String mainErrorType;

    /**
     * 构建导入结果
     */
    public static <T> ImportResult build(
            int totalRead,
            int validCount,
            int dbInserted,
            int failedRows,
            int totalErrors,
            List<String> errors,
            boolean hasTruncatedErrors,
            String mainErrorType,
            long durationMs) {

        return ImportResult.builder()
                .totalRead(totalRead)
                .validCount(validCount)
                .dbInserted(dbInserted)
                .failedRows(failedRows)
                .durationMs(durationMs)
                .errors(errors)
                .totalErrors(totalErrors)
                .hasTruncatedErrors(hasTruncatedErrors)
                .mainErrorType(mainErrorType)
                .build();
    }

    /**
     * 从监听器构建结果
     */
    public static <T> ImportResult fromListener(
            ExcelReadListener<T> listener,
            int dbInserted,
            Map<Integer, String> businessErrors,
            long durationMs) {

        if (listener == null) {
            return systemError("监听器为null", durationMs);
        }

        int totalRead = listener.getTotalCount();
        int validCount = listener.getSuccessCount();

        // 处理监听器原始错误
        List<RawImportError> allRawErrors = new ArrayList<>(listener.getRawErrors());

        // 添加业务错误
        if (businessErrors != null && !businessErrors.isEmpty()) {
            businessErrors.forEach((row, message) ->
                    allRawErrors.add(RawImportError.businessError(row, message))
            );
        }

        // 格式化错误
        List<String> formattedErrors = ERROR_FORMATTER.formatErrors(allRawErrors, listener.getClazz());

        // 生成统计
        ImportErrorFormatter.ErrorStatistics statistics =
                ERROR_FORMATTER.generateStatistics(allRawErrors);

        // 获取截断后的错误
        List<String> truncatedErrors = ERROR_FORMATTER.getTruncatedErrors(formattedErrors);

        return build(
                totalRead,
                validCount,
                dbInserted,
                statistics.getFailedRows(),
                statistics.getTotalErrors(),
                truncatedErrors,
                formattedErrors.size() > MAX_ERROR_DISPLAY,
                statistics.getMainErrorType(),
                durationMs
        );
    }

    /**
     * 从监听器构建结果（只有监听器错误）
     */
    public static <T> ImportResult fromListenerOnly(
            ExcelReadListener<T> listener,
            long durationMs) {

        return fromListener(listener, 0, null, durationMs);
    }

    /**
     * 从监听器构建结果（监听器错误 + 数据库插入统计）
     */
    public static <T> ImportResult fromListenerWithDb(
            ExcelReadListener<T> listener,
            int dbInserted,
            long durationMs) {

        return fromListener(listener, dbInserted, null, durationMs);
    }

    /**
     * 从业务错误构建结果
     */
    public static <T> ImportResult fromBusinessErrors(
            ExcelReadListener<T> listener,
            int dbInserted,
            Map<Integer, String> businessErrors,
            long durationMs) {

        return fromListener(listener, dbInserted, businessErrors, durationMs);
    }

    /**
     * 系统错误结果
     */
    public static ImportResult systemError(String errorMessage, long durationMs) {
        RawImportError error = RawImportError.dataConvertError(0, "SYSTEM", errorMessage);
        List<RawImportError> rawErrors = Collections.singletonList(error);
        List<String> formattedErrors = ERROR_FORMATTER.formatErrors(rawErrors, null);
        ImportErrorFormatter.ErrorStatistics statistics = ERROR_FORMATTER.generateStatistics(rawErrors);

        return build(
                0, 0, 0,
                statistics.getFailedRows(),
                statistics.getTotalErrors(),
                formattedErrors,
                false,
                statistics.getMainErrorType(),
                durationMs
        );
    }

    /**
     * 判断导入是否成功
     */
    public boolean isSuccess() {
        return failedRows == 0 && dbInserted == validCount;
    }

    /**
     * 判断是否有部分成功
     */
    public boolean isPartialSuccess() {
        return dbInserted > 0 && failedRows > 0;
    }

    /**
     * 判断是否完全失败
     */
    public boolean isCompleteFail() {
        return dbInserted == 0 && failedRows > 0;
    }

    /**
     * 获取统计信息
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("导入统计：读取%d行，校验通过%d行，数据库插入%d行，失败%d行",
                totalRead, validCount, dbInserted, failedRows));

        if (totalErrors > 0) {
            sb.append(String.format("，发现%d个错误", totalErrors));
            if (hasTruncatedErrors) {
                sb.append(String.format("（显示前%d个）", MAX_ERROR_DISPLAY));
            }
            if (mainErrorType != null) {
                sb.append(String.format("，主要错误：%s", mainErrorType));
            }
        }

        return sb.toString();
    }

    /**
     * 获取详细错误摘要
     */
    public String getDetailedErrorSummary() {
        if (errors == null || errors.isEmpty()) {
            return "无错误";
        }

        return ERROR_FORMATTER.generateErrorSummary(errors, totalErrors);
    }

    /**
     * 获取失败消息
     */
    public String getFailMessage(String baseMessage) {
        if (errors == null || errors.isEmpty()) {
            return baseMessage;
        }

        String message = baseMessage;
        if (hasTruncatedErrors) {
            message += String.format("（共%d个错误，显示前%d个）", totalErrors, MAX_ERROR_DISPLAY);
        }
        if (mainErrorType != null && !mainErrorType.isEmpty()) {
            message += "，" + mainErrorType;
        }
        message += "，例如：" + errors.get(0);

        return message;
    }

    /**
     * 转换为Map（用于JSON序列化）
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalRead", totalRead);
        map.put("validCount", validCount);
        map.put("dbInserted", dbInserted);
        map.put("failedRows", failedRows);
        map.put("durationMs", durationMs);
        map.put("errors", errors);
        map.put("totalErrors", totalErrors);
        map.put("hasTruncatedErrors", hasTruncatedErrors);
        map.put("mainErrorType", mainErrorType);
        map.put("success", isSuccess());
        map.put("partialSuccess", isPartialSuccess());
        map.put("completeFail", isCompleteFail());
        map.put("statistics", getStatistics());

        return map;
    }
}