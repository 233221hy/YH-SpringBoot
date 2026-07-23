package cn.xfywz.guozespring.excel;

import com.alibaba.excel.annotation.ExcelProperty;

import java.lang.reflect.Field;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Excel数据预处理工具类
 */
public final class ExcelDataPreprocessor {
  private ExcelDataPreprocessor() {
    throw new IllegalStateException("Utility class");
  }

  // 预编译的正则表达式，提高性能
  private static final Pattern COMMA_PATTERN = Pattern.compile(",");
  private static final Pattern CONTROL_CHAR_PATTERN = Pattern.compile("\\p{Cntrl}");
  private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
  private static final Pattern NON_DIGIT_PATTERN = Pattern.compile("\\D");

  // 缓存识别结果，提高性能
  private static final Map<Class<?>, Set<Integer>> TEXT_COLUMNS_CACHE = new ConcurrentHashMap<>();

  // ==================== 基础清理方法 ====================

  /**
   * 基础清理：去除控制字符、规范化空白字符
   */
  public static String basicClean(String value) {
    if (value == null) {
      return null;
    }

    // 1. 规范化Unicode字符
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);

    // 2. 去除控制字符（包括不可见字符）
    normalized = CONTROL_CHAR_PATTERN.matcher(normalized).replaceAll("");

    // 3. 去除首尾空白
    normalized = normalized.strip();

    return normalized.isEmpty() ? null : normalized;
  }

  /**
   * 清理字符串中的逗号，替换为空格
   */
  public static String safeReplaceComma(String value) {
    return value == null ? "" : value.replace(',', ' ');
  }

  /**
   * Excel安全处理（综合处理）
   * 包含：基础清理、换行符转空格、压缩多空格、逗号替换
   */
  public static String safeForExcel(String value) {
    if (value == null) {
      return "";
    }

    // 基础清理
    String cleaned = basicClean(value);
    if (cleaned == null) {
      return "";
    }

    // 处理可能的换行符（Excel单元格中的换行）
    cleaned = cleaned.replace("\r\n", " ").replace("\n", " ");

    // 压缩多个空格为单个空格
    cleaned = MULTI_SPACE_PATTERN.matcher(cleaned).replaceAll(" ");

    // 处理逗号（防止CSV导出问题）
    cleaned = COMMA_PATTERN.matcher(cleaned).replaceAll(" ");

    return cleaned;
  }

  /**
   * 清理数字字符串（移除所有非数字字符）
   */
  public static String cleanNumericString(String value) {
    if (value == null) {
      return "";
    }

    String cleaned = basicClean(value);
    if (cleaned == null) {
      return "";
    }

    // 只保留数字
    return NON_DIGIT_PATTERN.matcher(cleaned).replaceAll("");
  }

  // ==================== 自动预处理方法 ====================

  /**
   * 自动预处理单个对象（根据字段名和注解智能处理）
   */
  public static <T> void autoPreprocess(T object) {
    if (object == null) {
      return;
    }

    Class<?> clazz = object.getClass();
    Field[] fields = clazz.getDeclaredFields();

    for (Field field : fields) {
      try {
        field.setAccessible(true);
        Object value = field.get(object);

        if (value instanceof String strValue) {
            String processedValue = processField(field, strValue);
          if (processedValue != null && !processedValue.equals(strValue)) {
            field.set(object, processedValue);
          }
        }
      } catch (IllegalAccessException e) {
        // 忽略无法访问的字段
      }
    }
  }

  /**
   * 批量自动预处理
   */
  public static <T> void batchPreprocess(List<T> objects) {
    if (objects == null || objects.isEmpty()) {
      return;
    }

    for (T object : objects) {
      autoPreprocess(object);
    }
  }

  /**
   * 根据字段信息智能处理
   */
  private static String processField(Field field, String value) {
    if (value == null) {
      return null;
    }

    if (isNumericField(field)) {
      // 对于数值型字段，进行清理但不添加前缀
      return cleanNumericString(value);
    }

    // 对于普通文本字段，进行基本清理
    return safeForExcel(value);
  }

  /**
   * 判断是否为数值型字段
   */
  private static boolean isNumericField(Field field) {
    // 只有String类型才考虑
    if (field.getType() != String.class) {
      return false;
    }

    String fieldName = field.getName().toLowerCase();
    String fieldComment = getFieldComment(field);

    // 根据字段名判断
    // 特殊处理：如果是学号字段，不当作纯数值型字段
    if (fieldName.contains("number") || fieldName.contains("studentid") ||
            fieldName.contains("studentnumber")) {
      return false; // 学号可以包含字母，不作为纯数字处理
    }
    // 身份证
    if (fieldName.contains("idcard")) {
      return true;
    }
    // 电话
    if (fieldName.contains("mobile") || fieldName.contains("phone")) {
      return true;
    }

    // 根据注解值判断
    if (fieldComment != null) {
      String commentLower = fieldComment.toLowerCase();
      // 排除学号
      if (commentLower.contains("学号")) {
        return false;
      }
      return commentLower.contains("身份证") || commentLower.contains("手机") ||
              commentLower.contains("电话") || commentLower.contains("号码") ||
              commentLower.contains("编号") || commentLower.contains("账号") ||
              commentLower.contains("证件号") || commentLower.contains("序列号");
    }

    return false;
  }

  /**
   * 获取字段的注释（从ExcelProperty注解）
   */
  private static String getFieldComment(Field field) {
    ExcelProperty excelProperty = field.getAnnotation(ExcelProperty.class);
    if (excelProperty != null && excelProperty.value().length > 0) {
      return excelProperty.value()[0];
    }
    return null;
  }

  // ==================== 文本列识别方法 ====================

  /**
   * 识别需要文本格式的列索引
   */
  public static Set<Integer> identifyTextColumns(Class<?> clazz) {
    return TEXT_COLUMNS_CACHE.computeIfAbsent(clazz, k -> {
      Set<Integer> textColumns = new HashSet<>();
      Field[] fields = clazz.getDeclaredFields();

      for (int i = 0; i < fields.length; i++) {
        Field field = fields[i];
        if (isNumericField(field)) {
          textColumns.add(i);
        }
      }
      return Collections.unmodifiableSet(textColumns);
    });
  }

  /**
   * 获取需要文本格式的列索引数组
   */
  public static int[] getTextColumnIndexes(Class<?> clazz) {
    Set<Integer> columns = identifyTextColumns(clazz);
    return columns.stream().mapToInt(Integer::intValue).toArray();
  }

  // ==================== 字段处理器 ====================

  /**
   * 批量处理对象字段
   */
  public static <T> void processObjectFields(T object, FieldProcessor<T> processor) {
    if (object == null || processor == null) {
      return;
    }
    processor.process(object);
  }

  /**
   * 批量处理对象列表
   */
  public static <T> void processObjectList(List<T> objects, FieldProcessor<T> processor) {
    if (objects == null || processor == null) {
      return;
    }
    objects.forEach(processor::process);
  }

  /**
   * 字段处理器接口（用于自定义处理逻辑）
   */
  @FunctionalInterface
  public interface FieldProcessor<T> {
    void process(T object);
  }

  // ==================== 辅助方法 ====================

  /**
   * 复制对象属性（简化版）
   */
  private static <T> void copyProperties(T source, T target) throws IllegalAccessException {
    if (source == null || target == null) {
      return;
    }

    Class<?> clazz = source.getClass();
    Field[] fields = clazz.getDeclaredFields();

    for (Field field : fields) {
      field.setAccessible(true);
      Object value = field.get(source);
      field.set(target, value);
    }
  }

  /**
   * 获取对象中所有String字段的值
   */
  public static Map<String, String> getAllStringFields(Object object) {
    if (object == null) {
      return Collections.emptyMap();
    }

    Map<String, String> fieldValues = new HashMap<>();
    Class<?> clazz = object.getClass();
    Field[] fields = clazz.getDeclaredFields();

    for (Field field : fields) {
      if (field.getType() == String.class) {
        try {
          field.setAccessible(true);
          String value = (String) field.get(object);
          fieldValues.put(field.getName(), value);
        } catch (IllegalAccessException e) {
          // 忽略无法访问的字段
        }
      }
    }

    return fieldValues;
  }

  /**
   * 检查对象中是否有需要预处理的字段
   */
  public static boolean hasProcessableFields(Class<?> clazz) {
    Field[] fields = clazz.getDeclaredFields();
    for (Field field : fields) {
      if (field.getType() == String.class) {
        return true;
      }
    }
    return false;
  }

  /**
   * 获取需要预处理的字段列表
   */
  public static List<String> getProcessableFieldNames(Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
            .filter(field -> field.getType() == String.class)
            .map(Field::getName)
            .collect(Collectors.toList());
  }

  // ==================== 缓存管理 ====================

  /**
   * 清除文本列识别缓存（热部署时使用）
   */
  public static void clearCache() {
    TEXT_COLUMNS_CACHE.clear();
  }

  /**
   * 清除指定类的缓存
   */
  public static void clearCache(Class<?> clazz) {
    TEXT_COLUMNS_CACHE.remove(clazz);
  }

  /**
   * 获取缓存大小
   */
  public static int getCacheSize() {
    return TEXT_COLUMNS_CACHE.size();
  }

  /**
   * 获取所有缓存类名
   */
  public static Set<String> getCachedClassNames() {
    return TEXT_COLUMNS_CACHE.keySet().stream()
            .map(Class::getSimpleName)
            .collect(Collectors.toSet());
  }
}