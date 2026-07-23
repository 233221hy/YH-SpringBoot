package cn.xfywz.guozespring.util.db;

import lombok.Getter;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 批量操作构建器
 * 用于构建批量插入、更新、删除等操作
 */
@Getter
public class BatchBuilder {
  private final String sql;
  private final List<List<Object>> batchParams = new ArrayList<>();

  // 批量操作配置
  private int batchSize = 1000; // 默认批量大小
  private boolean validateParameters = true; // 是否验证参数

  public BatchBuilder(String sql) {
    this.sql = sql;
  }

  /**
   * 设置批量大小
   */
  public BatchBuilder batchSize(int batchSize) {
    if (batchSize <= 0) {
      throw new IllegalArgumentException("批量大小必须大于0");
    }
    this.batchSize = batchSize;
    return this;
  }

  /**
   * 设置是否验证参数
   */
  public BatchBuilder validateParameters(boolean validate) {
    this.validateParameters = validate;
    return this;
  }

  /**
   * 添加一组参数（List形式）
   */
  public BatchBuilder addParams(List<Object> params) {
    batchParams.add(params == null ? new ArrayList<>() : new ArrayList<>(params));
    return this;
  }

  /**
   * 添加一组参数（可变参数形式）
   */
  public BatchBuilder addParams(Object... params) {
    batchParams.add(params == null ? new ArrayList<>() : Arrays.asList(params));
    return this;
  }

  /**
   * 添加多组参数
   */
  public BatchBuilder addAllParams(List<List<Object>> allParams) {
    if (allParams != null) {
      for (List<Object> params : allParams) {
        addParams(params);
      }
    }
    return this;
  }

  /**
   * 批量添加参数（使用构建器模式）
   */
  public BatchBuilder addBatch(Consumer<BatchParamsBuilder> consumer) {
    BatchParamsBuilder builder = new BatchParamsBuilder();
    consumer.accept(builder);
    return addParams(builder.build());
  }

  /**
   * 从集合批量添加参数
   */
  public <T> BatchBuilder addFromCollection(Collection<T> collection,
                                            Function<T, List<Object>> mapper) {
    if (!CollectionUtils.isEmpty(collection)) {
      for (T item : collection) {
        List<Object> params = mapper.apply(item);
        if (params != null) {
          addParams(params);
        }
      }
    }
    return this;
  }

  /**
   * 从数组批量添加参数
   */
  public BatchBuilder addFromArray(Object[][] paramArray) {
    if (paramArray != null) {
      for (Object[] params : paramArray) {
        addParams(params);
      }
    }
    return this;
  }

  /**
   * 添加批量参数组
   */
  public BatchBuilder addBatchParams(List<Object> params) {
    return addParams(params);
  }

  /**
   * 获取参数组数
   */
  public int size() {
    return batchParams.size();
  }

  /**
   * 是否为空
   */
  public boolean isEmpty() {
    return batchParams.isEmpty();
  }

  /**
   * 清空参数
   */
  public void clear() {
    batchParams.clear();
  }

  /**
   * 获取所有参数组
   */
  public List<List<Object>> getAllParams() {
    return new ArrayList<>(batchParams);
  }

  /**
   * 获取分批的参数列表
   */
  public List<List<List<Object>>> getBatchedParams() {
    if (isEmpty()) {
      return Collections.emptyList();
    }

    List<List<List<Object>>> batches = new ArrayList<>();
    int total = batchParams.size();

    for (int i = 0; i < total; i += batchSize) {
      int end = Math.min(i + batchSize, total);
      batches.add(new ArrayList<>(batchParams.subList(i, end)));
    }

    return batches;
  }

  /**
   * 构建BuiltSql（用于单批执行）
   */
  public BuiltSql build() {
    if (isEmpty()) {
      throw new IllegalStateException("没有需要执行的参数");
    }

    if (validateParameters) {
      validateParameters();
    }

    // 创建包含所有参数的单个BuiltSql
    List<Object> flattenedParams = new ArrayList<>();
    for (List<Object> params : batchParams) {
      flattenedParams.addAll(params);
    }

    // 构建批量SQL：INSERT INTO table (col1, col2) VALUES (?, ?), (?, ?), ...
    String batchSql = buildBatchSql();

    return BuiltSql.of(batchSql, flattenedParams);
  }

  /**
   * 构建多批次BuiltSql列表
   */
  public List<BuiltSql> buildBatches() {
    if (isEmpty()) {
      return Collections.emptyList();
    }

    if (validateParameters) {
      validateParameters();
    }

    List<List<List<Object>>> batches = getBatchedParams();
    List<BuiltSql> builtSqls = new ArrayList<>();

    for (List<List<Object>> batch : batches) {
      List<Object> flattenedParams = new ArrayList<>();
      for (List<Object> params : batch) {
        flattenedParams.addAll(params);
      }

      String batchSql = buildBatchSqlForBatch(batch.size());
      builtSqls.add(BuiltSql.of(batchSql, flattenedParams));
    }

    return builtSqls;
  }

  /**
   * 验证参数数量是否与SQL中的占位符匹配
   */
  public void validateParameters() {
    if (isEmpty()) {
      return;
    }

    int paramCount = batchParams.get(0).size();
    int placeholderCount = countPlaceholders(sql);

    if (placeholderCount != paramCount) {
      throw new IllegalArgumentException(
              String.format("参数数量不匹配。SQL需要%d个参数，但提供了%d个参数",
                      placeholderCount, paramCount));
    }

    // 验证所有参数组的数量是否一致
    for (int i = 1; i < batchParams.size(); i++) {
      if (batchParams.get(i).size() != paramCount) {
        throw new IllegalArgumentException(
                String.format("第%d组参数数量不一致。期望%d个，实际%d个",
                        i + 1, paramCount, batchParams.get(i).size()));
      }
    }
  }

  /**
   * 应用自定义配置
   */
  public BatchBuilder apply(Consumer<BatchBuilder> consumer) {
    consumer.accept(this);
    return this;
  }


  // ================ 私有方法 ================

  private String buildBatchSql() {
    if (isEmpty()) {
      return sql;
    }

    // 如果是INSERT语句，构建批量插入SQL
    String trimmedSql = sql.trim().toUpperCase();
    if (trimmedSql.startsWith("INSERT")) {
      return buildBatchInsertSql();
    }

    // 其他类型的SQL不支持批量参数化，返回原SQL
    // 实际执行时会通过addBatch方式执行
    return sql;
  }

  private String buildBatchSqlForBatch(int batchCount) {
    if (batchCount <= 0) {
      return sql;
    }

    String trimmedSql = sql.trim().toUpperCase();
    if (trimmedSql.startsWith("INSERT")) {
      return buildBatchInsertSqlForCount(batchCount);
    }

    return sql;
  }

  private String buildBatchInsertSql() {
    // 提取INSERT语句的基本部分
    String baseSql = sql.trim();

    // 如果是VALUES (...)，则扩展为多个VALUES
    int valuesIndex = baseSql.toUpperCase().indexOf("VALUES");
    if (valuesIndex > 0) {
      String beforeValues = baseSql.substring(0, valuesIndex).trim();
      String valuesTemplate = baseSql.substring(valuesIndex).trim();

      // 提取VALUES部分模板，如 "(?, ?, ?)"
      int parenStart = valuesTemplate.indexOf('(');
      int parenEnd = valuesTemplate.lastIndexOf(')');
      if (parenStart > 0 && parenEnd > parenStart) {
        String singleValues = valuesTemplate.substring(parenStart, parenEnd + 1);

        // 构建多个VALUES
        StringBuilder batchValues = new StringBuilder();
        for (int i = 0; i < batchParams.size(); i++) {
          if (i > 0) {
            batchValues.append(", ");
          }
          batchValues.append(singleValues);
        }

        return beforeValues + " VALUES " + batchValues;
      }
    }

    return baseSql;
  }

  private String buildBatchInsertSqlForCount(int count) {
    String baseSql = sql.trim();
    int valuesIndex = baseSql.toUpperCase().indexOf("VALUES");

    if (valuesIndex > 0) {
      String beforeValues = baseSql.substring(0, valuesIndex).trim();
      String valuesTemplate = baseSql.substring(valuesIndex).trim();

      int parenStart = valuesTemplate.indexOf('(');
      int parenEnd = valuesTemplate.lastIndexOf(')');
      if (parenStart > 0 && parenEnd > parenStart) {
        String singleValues = valuesTemplate.substring(parenStart, parenEnd + 1);

        StringBuilder batchValues = new StringBuilder();
        for (int i = 0; i < count; i++) {
          if (i > 0) {
            batchValues.append(", ");
          }
          batchValues.append(singleValues);
        }

        return beforeValues + " VALUES " + batchValues;
      }
    }

    return baseSql;
  }

  private int countPlaceholders(String sql) {
    int count = 0;
    boolean inString = false;
    boolean escaped = false;

    for (int i = 0; i < sql.length(); i++) {
      char c = sql.charAt(i);

      if (escaped) {
        escaped = false;
        continue;
      }

      if (c == '\\') {
        escaped = true;
        continue;
      }

      if (c == '\'' || c == '"') {
        inString = !inString;
        continue;
      }

      if (!inString && c == '?' && (i == 0 || sql.charAt(i - 1) != '?')) {
        count++;
      }
    }

    return count;
  }

  // ================ 内部类 ================

  /**
   * 批量参数构建器
   */
  public static class BatchParamsBuilder {
    private final List<Object> params = new ArrayList<>();

    public BatchParamsBuilder param(Object value) {
      params.add(value);
      return this;
    }

    public BatchParamsBuilder params(Object... values) {
      if (values != null) {
        params.addAll(Arrays.asList(values));
      }
      return this;
    }

    public BatchParamsBuilder params(List<Object> values) {
      if (values != null) {
        params.addAll(values);
      }
      return this;
    }

    public List<Object> build() {
      return new ArrayList<>(params);
    }
  }


    private int getBatchSizeFromSql(String sql) {
      // 简单估算批次大小，通过计算VALUES数量
      if (sql.toUpperCase().contains("VALUES")) {
        int count = 0;
        int index = sql.indexOf("VALUES");
        if (index > 0) {
          String valuesPart = sql.substring(index);
          // 计算括号数量来估算批次
          int parenCount = 0;
          for (char c : valuesPart.toCharArray()) {
            if (c == '(') parenCount++;
          }
          return parenCount;
        }
      }
      return 1;
    }
}