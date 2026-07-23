package cn.xfywz.guozespring.excel;

import cn.xfywz.guozespring.excel.RawImportError;
import cn.xfywz.guozespring.excel.ExcelFieldUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 导入错误格式化器 - 负责错误信息格式化和分组
 */
@Slf4j
@Component
public class ImportErrorFormatter {

    // 最大显示错误数
    private static final int MAX_ERROR_DISPLAY = 10;

    /**
     * 格式化原始错误
     */
    public List<String> formatErrors(List<RawImportError> rawErrors, Class<?> clazz) {
        if (rawErrors == null || rawErrors.isEmpty()) {
            return Collections.emptyList();
        }

        return rawErrors.stream()
                .map(error -> formatSingleError(error, clazz))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 格式化单个错误
     */
    private String formatSingleError(RawImportError error, Class<?> clazz) {
        try {
            String fieldDisplayName = getFieldDisplayName(error, clazz);

            // 根据错误类型格式化
            switch (error.getErrorType()) {
                case VALIDATION:
                    return String.format("第%d行[%s]: %s",
                            error.getRow(), fieldDisplayName, error.getMessage());

                case BUSINESS:
                case PRE_PROCESS:
                case BATCH_PROCESS:
                case SYSTEM:
                    return String.format("第%d行[%s]: %s",
                            error.getRow(), error.getErrorType().getDisplayName(), error.getMessage());

                case DATA_CONVERT:
                    // 处理列转换错误
                    try {
                        int columnIndex = Integer.parseInt(error.getFieldOrType());
                        String columnName = ExcelFieldUtil.getColumnChineseName(clazz, columnIndex);
                        return String.format("第%d行[%s]: %s",
                                error.getRow(), columnName, error.getMessage());
                    } catch (NumberFormatException e) {
                        return String.format("第%d行[%s]: %s",
                                error.getRow(), error.getFieldOrType(), error.getMessage());
                    }

                default:
                    return String.format("第%d行: %s", error.getRow(), error.getMessage());
            }
        } catch (Exception e) {
            log.error("格式化错误信息失败: {}", error, e);
            return null;
        }
    }

    /**
     * 获取字段显示名称
     */
    private String getFieldDisplayName(RawImportError error, Class<?> clazz) {
        if (error.getErrorType() == RawImportError.ErrorType.VALIDATION) {
            return ExcelFieldUtil.getChineseName(clazz, error.getFieldOrType());
        }
        return error.getFieldOrType();
    }

    /**
     * 按行分组错误
     */
    public Map<Integer, List<String>> groupErrorsByRow(List<String> formattedErrors) {
        if (formattedErrors == null || formattedErrors.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, List<String>> errorsByRow = new TreeMap<>();

        for (String error : formattedErrors) {
            Integer row = extractRowFromError(error);
            if (row != null) {
                errorsByRow.computeIfAbsent(row, k -> new ArrayList<>())
                        .add(error);
            }
        }

        return errorsByRow;
    }

    /**
     * 生成错误摘要
     */
    public String generateErrorSummary(List<String> formattedErrors, int totalErrors) {
        if (formattedErrors == null || formattedErrors.isEmpty()) {
            return "无错误";
        }

        Map<Integer, List<String>> errorsByRow = groupErrorsByRow(formattedErrors);
        StringBuilder sb = new StringBuilder();

        // 只显示前10行错误
        int rowCount = 0;
        for (Map.Entry<Integer, List<String>> entry : errorsByRow.entrySet()) {
            if (rowCount >= MAX_ERROR_DISPLAY) break;

            sb.append(String.format("第%d行：", entry.getKey()));
            if (entry.getValue().size() > 1) {
                sb.append(String.format("（%d个问题）", entry.getValue().size()));
            }
            sb.append(entry.getValue().get(0)).append("\n");
            rowCount++;
        }

        // 如果还有更多错误未显示
        if (totalErrors > formattedErrors.size() || rowCount < errorsByRow.size()) {
            int remaining = Math.max(totalErrors - formattedErrors.size(),
                    errorsByRow.size() - rowCount);
            sb.append(String.format("... 还有%d个错误未显示\n", remaining));
        }

        return sb.toString();
    }

    /**
     * 生成错误统计
     */
    public ErrorStatistics generateStatistics(List<RawImportError> rawErrors) {
        if (rawErrors == null || rawErrors.isEmpty()) {
            return new ErrorStatistics();
        }

        ErrorStatistics stats = new ErrorStatistics();
        stats.setTotalErrors(rawErrors.size());

        // 统计失败行数（去重）
        Set<Integer> failedRows = rawErrors.stream()
                .map(RawImportError::getRow)
                .filter(row -> row > 0) // 过滤掉行号为0的错误
                .collect(Collectors.toSet());
        stats.setFailedRows(failedRows.size());

        // 按错误类型统计
        Map<RawImportError.ErrorType, Long> errorTypeCount = rawErrors.stream()
                .collect(Collectors.groupingBy(
                        RawImportError::getErrorType,
                        Collectors.counting()
                ));
        stats.setErrorTypeCount(errorTypeCount);

        return stats;
    }

    /**
     * 从错误信息中提取行号
     */
    public Integer extractRowFromError(String error) {
        if (error == null) return null;

        try {
            if (error.startsWith("第")) {
                int endIndex = error.indexOf("行");
                if (endIndex > 1) {
                    String rowStr = error.substring(1, endIndex);
                    return Integer.parseInt(rowStr.trim());
                }
            }
        } catch (Exception e) {
            log.debug("提取行号失败: {}", error);
        }
        return null;
    }

    /**
     * 获取截断后的错误列表
     */
    public List<String> getTruncatedErrors(List<String> formattedErrors) {
        if (formattedErrors == null || formattedErrors.isEmpty()) {
            return Collections.emptyList();
        }

        if (formattedErrors.size() <= MAX_ERROR_DISPLAY) {
            return formattedErrors;
        }

        return formattedErrors.subList(0, MAX_ERROR_DISPLAY);
    }

    /**
     * 错误统计信息
     */
    @lombok.Data
    public static class ErrorStatistics {
        private int totalErrors;
        private int failedRows;
        private Map<RawImportError.ErrorType, Long> errorTypeCount = new HashMap<>();

        /**
         * 获取主要错误类型
         */
        public String getMainErrorType() {
            if (errorTypeCount.isEmpty()) {
                return "无错误";
            }

            return errorTypeCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(entry -> entry.getKey().getDisplayName())
                    .orElse("未知错误");
        }
    }
}