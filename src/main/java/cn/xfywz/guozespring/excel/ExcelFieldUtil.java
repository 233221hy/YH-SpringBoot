package cn.xfywz.guozespring.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel字段工具类 - 只负责字段名转换
 */
@Slf4j
public class ExcelFieldUtil {

    private static final Map<Class<?>, Map<String, String>> CLASS_FIELD_MAPPING_CACHE =
            new ConcurrentHashMap<>();

    private static final Map<Class<?>, Map<Integer, String>> CLASS_COLUMN_MAPPING_CACHE =
            new ConcurrentHashMap<>();

    private ExcelFieldUtil() {
        // 工具类，防止实例化
    }

    /**
     * 获取类的字段中英文映射
     */
    public static Map<String, String> getFieldChineseMapping(Class<?> clazz) {
        if (clazz == null) {
            return new HashMap<>();
        }

        return CLASS_FIELD_MAPPING_CACHE.computeIfAbsent(clazz, k -> {
            Map<String, String> mapping = new HashMap<>();
            ReflectionUtils.doWithFields(clazz, field -> {
                String fieldName = field.getName();
                String chineseName = getFieldChineseName(field);
                mapping.put(fieldName, chineseName);
                mapping.put("this." + fieldName, chineseName);
                log.debug("字段映射: {} -> {}", fieldName, chineseName);
            });
            return Map.copyOf(mapping);
        });
    }

    /**
     * 获取类的列索引到中文名映射
     */
    public static Map<Integer, String> getColumnChineseMapping(Class<?> clazz) {
        if (clazz == null) {
            return new HashMap<>();
        }

        return CLASS_COLUMN_MAPPING_CACHE.computeIfAbsent(clazz, k -> {
            Map<Integer, String> mapping = new HashMap<>();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                ExcelProperty excelProperty = AnnotationUtils.getAnnotation(field, ExcelProperty.class);
                if (excelProperty != null && excelProperty.index() >= 0) {
                    String chineseName = getFieldChineseName(field);
                    mapping.put(excelProperty.index(), chineseName);
                    log.debug("列映射: 第{}列 -> {}", excelProperty.index() + 1, chineseName);
                }
            }
            return Map.copyOf(mapping);
        });
    }

    /**
     * 获取字段的中文名称
     */
    private static String getFieldChineseName(Field field) {
        ExcelProperty excelProperty = AnnotationUtils.getAnnotation(field, ExcelProperty.class);
        if (excelProperty != null && excelProperty.value().length > 0) {
            String name = excelProperty.value()[0];
            // 清理可能的格式字符（如*号）
            if (name != null && !name.trim().isEmpty()) {
                return name.trim().replace("*", "");
            }
        }
        return field.getName();
    }

    /**
     * 根据属性路径获取中文名称
     */
    public static String getChineseName(Class<?> clazz, String propertyPath) {
        if (clazz == null || propertyPath == null || propertyPath.isEmpty()) {
            return "未知字段";
        }

        Map<String, String> mapping = getFieldChineseMapping(clazz);

        // 处理嵌套属性路径
        String fieldName;
        if (propertyPath.contains(".")) {
            String[] parts = propertyPath.split("\\.");
            fieldName = parts[parts.length - 1];
        } else {
            fieldName = propertyPath;
        }

        return mapping.getOrDefault(fieldName, fieldName);
    }

    /**
     * 根据列索引获取列的中文名称
     */
    public static String getColumnChineseName(Class<?> clazz, int columnIndex) {
        if (clazz == null) {
            return "第" + (columnIndex + 1) + "列";
        }

        Map<Integer, String> mapping = getColumnChineseMapping(clazz);
        String chineseName = mapping.get(columnIndex);

        return chineseName != null ? chineseName : "第" + (columnIndex + 1) + "列";
    }

    /**
     * 清空缓存（热部署时使用）
     */
    public static void clearCache() {
        CLASS_FIELD_MAPPING_CACHE.clear();
        CLASS_COLUMN_MAPPING_CACHE.clear();
    }
}
