package cn.xfywz.guozespring.excel;

import cn.xfywz.guozespring.annotation.ExcelExportConfig;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.write.handler.WriteHandler;
import com.alibaba.excel.write.merge.OnceAbsoluteMergeStrategy;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.CollectionUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用Excel导出工具类
 */
public final class ExcelExportUtil {

    private static final Map<Class<?>, ExcelExportConfig> CONFIG_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> SORTED_FIELDS_CACHE = new ConcurrentHashMap<>();

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy年MM月dd日");
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 默认导出配置值
    private static final String DEFAULT_FILE_NAME = "导出数据";
    private static final String DEFAULT_SHEET_NAME = "Sheet1";
    private static final int DEFAULT_FREEZE_ROWS = 2;
    private static final int[] DEFAULT_COLUMN_WIDTHS = new int[0];

    private ExcelExportUtil() {
        throw new IllegalStateException("Utility class");
    }

    // ==================== 核心导出方法 ====================

    /**
     * 通用导出方法
     */
    public static <T> void export(List<T> data, OutputStream outputStream, Class<T> clazz) {
        if (CollectionUtils.isEmpty(data)) {
            return;
        }

        ExcelExportConfig config = getConfig(clazz);
        String title = buildTitle(config);
        List<List<String>> head = buildHead(clazz, title);
        List<List<Object>> rows = convertToRows(data, clazz);

        // 构建写入器
        var writerBuilder = EasyExcel.write(outputStream)
                .head(head)
                .registerWriteHandler(new OnceAbsoluteMergeStrategy(0, 0, 0, head.size() - 1));

        // 注册样式处理器
        for (WriteHandler handler : buildWriteHandlers(config, clazz, head.size())) {
            writerBuilder.registerWriteHandler(handler);
        }

        writerBuilder.sheet(getSheetName(config)).doWrite(rows);
    }

    // ==================== 便捷导出方法 ====================

    /**
     * 便捷导出方法（包含响应头设置）
     */
    public static <T> void exportToResponse(List<T> data,
                                            HttpServletResponse response,
                                            Class<T> clazz) throws IOException {
        String filename = generateFileName(clazz);
        setExcelResponseHeader(response, filename);
        export(data, response.getOutputStream(), clazz);
    }

    /**
     * 导出并预处理数据（原地修改）
     */
    public static <T> void exportWithPreprocess(List<T> data,
                                                HttpServletResponse response,
                                                Class<T> clazz) throws IOException {
        ExcelDataPreprocessor.batchPreprocess(data);
        exportToResponse(data, response, clazz);
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取配置（若无注解则返回null，后续使用默认值）
     */
    private static ExcelExportConfig getConfig(Class<?> clazz) {
        return CONFIG_CACHE.computeIfAbsent(clazz, k -> clazz.getAnnotation(ExcelExportConfig.class));
    }

    /**
     * 构建标题字符串
     */
    private static String buildTitle(ExcelExportConfig config) {
        String fileName = (config != null && !config.fileName().isEmpty()) ? config.fileName() : DEFAULT_FILE_NAME;
        String dateStr = LocalDateTime.now().format(DATE_FORMAT);
        return fileName + "（" + dateStr + " 导出）";
    }

    /**
     * 获取 sheet 名称
     */
    private static String getSheetName(ExcelExportConfig config) {
        return (config != null && !config.sheetName().isEmpty()) ? config.sheetName() : DEFAULT_SHEET_NAME;
    }

    /**
     * 获取冻结行数
     */
    private static int getFreezeRows(ExcelExportConfig config) {
        return config != null ? config.freezeRows() : DEFAULT_FREEZE_ROWS;
    }

    /**
     * 获取列宽数组
     */
    private static int[] getColumnWidths(ExcelExportConfig config) {
        return config != null ? config.columnWidths() : DEFAULT_COLUMN_WIDTHS;
    }

    /**
     * 构建表头（按字段顺序，基于 @ExcelProperty 的 value）
     */
    private static List<List<String>> buildHead(Class<?> clazz, String title) {
        List<List<String>> head = new ArrayList<>();
        for (Field field : getSortedFields(clazz)) {
            ExcelProperty property = field.getAnnotation(ExcelProperty.class);
            if (property != null && property.value().length > 0) {
                List<String> columnHead = new ArrayList<>();
                columnHead.add(title);           // 第一行：总标题
                columnHead.add(property.value()[0]); // 第二行：列名
                head.add(columnHead);
            }
        }
        return head;
    }

    /**
     * 将数据转换为行列表
     */
    private static <T> List<List<Object>> convertToRows(List<T> data, Class<T> clazz) {
        List<List<Object>> rows = new ArrayList<>(data.size());
        List<Field> fields = getSortedFields(clazz);
        for (T item : data) {
            rows.add(convertRow(item, fields));
        }
        return rows;
    }

    /**
     * 单行转换
     */
    private static <T> List<Object> convertRow(T item, List<Field> fields) {
        List<Object> row = new ArrayList<>(fields.size());
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                row.add(field.get(item));
            } catch (IllegalAccessException e) {
                row.add(null);
            }
        }
        return row;
    }

    /**
     * 获取排序后的字段列表（按 @ExcelProperty.index() 升序，若无 index 则按声明顺序）
     */
    private static List<Field> getSortedFields(Class<?> clazz) {
        return SORTED_FIELDS_CACHE.computeIfAbsent(clazz, k -> {
            Field[] fields = clazz.getDeclaredFields();
            List<Field> list = new ArrayList<>();
            for (Field f : fields) {
                if (f.isAnnotationPresent(ExcelProperty.class)) {
                    list.add(f);
                }
            }
            // 按 index 排序（默认值 Integer.MAX_VALUE 表示无 index，放在最后）
            list.sort((f1, f2) -> {
                int idx1 = f1.getAnnotation(ExcelProperty.class).index();
                int idx2 = f2.getAnnotation(ExcelProperty.class).index();
                return Integer.compare(idx1, idx2);
            });
            return Collections.unmodifiableList(list);
        });
    }

    /**
     * 构建处理器列表
     */
    private static List<WriteHandler> buildWriteHandlers(ExcelExportConfig config,
                                                         Class<?> clazz,
                                                         int columnCount) {
        List<WriteHandler> handlers = new ArrayList<>();
        handlers.add(ExcelExportStyles.defaultTitleRow(columnCount));
        handlers.add(ExcelExportStyles.freezeAndWidth(getColumnWidths(config), getFreezeRows(config)));

        Set<Integer> textColumns = ExcelDataPreprocessor.identifyTextColumns(clazz);
        if (!textColumns.isEmpty()) {
            handlers.add(ExcelExportStyles.textColumns(
                    textColumns.stream().mapToInt(Integer::intValue).toArray()
            ));
        }
        handlers.add(ExcelExportStyles.defaultStyleStrategy());
        return handlers;
    }

    // ==================== 公共方法 ====================

    /**
     * 设置 Excel 响应头
     */
    private static void setExcelResponseHeader(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        try {
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-disposition",
                    "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");
        } catch (Exception e) {
            // 退化处理
            response.setHeader("Content-disposition",
                    "attachment;filename=" + fileName + ".xlsx");
        }
    }

    /**
     * 生成导出文件名（不含扩展名）
     */
    public static String generateFileName(Class<?> clazz) {
        ExcelExportConfig config = getConfig(clazz);
        String fileName = (config != null && !config.fileName().isEmpty()) ? config.fileName() : DEFAULT_FILE_NAME;
        String dateStr = LocalDateTime.now().format(FILE_DATE_FORMAT);
        return fileName + "_" + dateStr;
    }

    /**
     * 导出为字节数组
     */
    public static <T> byte[] exportToBytes(List<T> data, Class<T> clazz) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            export(data, bos, clazz);
            return bos.toByteArray();
        }
    }

    /**
     * 导出到文件
     */
    public static <T> void exportToFile(List<T> data, String filePath, Class<T> clazz) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            export(data, fos, clazz);
        }
    }
}