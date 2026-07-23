package cn.xfywz.guozespring.util.db;

import java.sql.Connection;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * DML操作构建器（增、删、改）
 */
@RequiredArgsConstructor
public class DmlBuilder {
  private final int schoolId;
  private final DatabaseUtil databaseUtil;

  // SQL构建相关
  private String tableName;
  private final Map<String, Object> setValues = new LinkedHashMap<>();
  // 存储原始 SET 表达式（如 "isLock = 1 - isLock"）
  private final List<String> rawSetClauses = new ArrayList<>();
  private final StringBuilder whereClause = new StringBuilder();
  private final List<Object> whereParams = new ArrayList<>();
  private int conditionCount = 0;
  // 用于存储带参数的原始 SET 子句
  private final List<Map.Entry<String, List<Object>>> rawSetClausesWithParams = new ArrayList<>();


  // ================ 表设置方法 ================

  /**
   * 设置表名
   */
  public DmlBuilder table(String tableName) {
    this.tableName = tableName;
    return this;
  }

  // ================ SET子句构建方法 ================

  /**
   * 设置字段值
   */
  public DmlBuilder set(String field, Object value) {
    if (value != null) {
      setValues.put(field, value);
    }
    return this;
  }

  /**
   * 设置字段值（如果字符串不为null且不为空）
   */
  public DmlBuilder setIfNotEmpty(String field, String value) {
    if (StringUtils.hasText(value)) {
      setValues.put(field, value);
    }
    return this;
  }

  /**
   * 设置字段值（如果值为正数）
   */
  public DmlBuilder setIfPositive(String field, Number value) {
    if (value != null && value.doubleValue() > 0) {
      setValues.put(field, value);
    }
    return this;
  }

  /**
   * 设置字段值（如果值不为null）
   */
  public DmlBuilder setIfNotNull(String field, Object value) {
    if (value != null) {
      setValues.put(field, value);
    }
    return this;
  }

  /**
   * 设置字段值(如果值不为负数)
   */
  public DmlBuilder setIfNotNegative(String field, Number value) {
    if (value != null && value.doubleValue() >= 0) {
      setValues.put(field, value);
    }
    return this;
  }

  /**
   * 设置字段值（如果值为true）
   */
  public DmlBuilder setIfTrue(String field, Boolean value) {
    if (value != null && value) {
      setValues.put(field, value);
    }
    return this;
  }

  /**
   * 批量设置字段值
   */
  public DmlBuilder setAll(Map<String, Object> values) {
    if (values != null) {
      values.forEach(this::set);
    }
    return this;
  }

  /**
   * 添加原始SET表达式（如 "isLock = 1 - isLock"）
   */
  public DmlBuilder setRaw(String rawClause) {
    if (StringUtils.hasText(rawClause)) {
      rawSetClauses.add(rawClause);
    }
    return this;
  }

  /**
   * 添加带参数的原始 SET 表达式（例如 "stuCount = CASE WHEN stuCount >= ? THEN stuCount - ? ELSE 0 END"）
   * @param rawClause SQL 片段，可包含占位符 ?
   * @param params 与占位符对应的参数
   */
  public DmlBuilder setRaw(String rawClause, Object... params) {
    if (StringUtils.hasText(rawClause)) {
      rawSetClausesWithParams.add(new AbstractMap.SimpleEntry<>(
              rawClause,
              params == null ? Collections.emptyList() : Arrays.asList(params)
      ));
    }
    return this;
  }

  // ================ WHERE子句构建方法 ================

  /**
   * 添加WHERE条件
   */
  public DmlBuilder where(String condition, Object... conditionParams) {
    if (StringUtils.hasText(condition)) {
      addCondition(condition);
      if (conditionParams != null) {
        whereParams.addAll(Arrays.asList(conditionParams));
      }
    }
    return this;
  }

  /**
   * 相等条件
   */
  public DmlBuilder eq(String field, Object value) {
    if (value != null) {
      addCondition(field + " = ?");
      whereParams.add(value);
    }
    return this;
  }

  /**
   * IN条件
   */
  public DmlBuilder in(String field, List<?> values) {
    if (values != null && !values.isEmpty()) {
      String placeholders = String.join(",", Collections.nCopies(values.size(), "?"));
      addCondition(field + " IN (" + placeholders + ")");
      whereParams.addAll(values);
    }
    return this;
  }


  // ================ 构建方法 ================

  /**
   * 构建INSERT SQL
   */
  private BuiltSql buildInsert() {
    if (tableName == null) {
      throw new IllegalStateException("表名未设置");
    }
    if (setValues.isEmpty()) {
      throw new IllegalStateException("至少设置一个字段值");
    }

    String columns = setValues.keySet().stream()
        .map(k -> "`" + k + "`")
        .collect(Collectors.joining(", "));

    String placeholders = setValues.keySet().stream()
        .map(k -> "?")
        .collect(Collectors.joining(", "));

    String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
    List<Object> params = new ArrayList<>(setValues.values());

    return BuiltSql.of(sql, params);
  }

  /**
   * 构建UPDATE SQL
   */
  private BuiltSql buildUpdate() {
    if (tableName == null) {
      throw new IllegalStateException("表名未设置");
    }
    if (setValues.isEmpty() && rawSetClauses.isEmpty() && rawSetClausesWithParams.isEmpty()) {
      throw new IllegalStateException("至少设置一个更新字段或原始表达式");
    }

    StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
    List<Object> params = new ArrayList<>();

    // 1. 普通字段赋值
    boolean hasSetClause = false;
    for (Map.Entry<String, Object> entry : setValues.entrySet()) {
      if (hasSetClause) sql.append(", ");
      sql.append("`").append(entry.getKey()).append("` = ?");
      params.add(entry.getValue());
      hasSetClause = true;
    }

    // 2. 带参数的原始表达式
    for (Map.Entry<String, List<Object>> entry : rawSetClausesWithParams) {
      if (hasSetClause) sql.append(", ");
      sql.append(entry.getKey());
      params.addAll(entry.getValue());
      hasSetClause = true;
    }

    // 3. 无参原始表达式
    for (String rawClause : rawSetClauses) {
      if (hasSetClause) sql.append(", ");
      sql.append(rawClause);
      hasSetClause = true;
    }

    // 4. WHERE 条件
    if (!whereClause.isEmpty()) {
      sql.append(" WHERE ").append(whereClause);
      params.addAll(whereParams);
    }

    return BuiltSql.of(sql.toString(), params);
  }
//  private BuiltSql buildUpdate() {
//    if (tableName == null) {
//      throw new IllegalStateException("表名未设置");
//    }
//    if (setValues.isEmpty()) {
//      throw new IllegalStateException("至少设置一个更新字段");
//    }
//
//    StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
//
// 1. 先处理普通字段赋值: `field = ?`
//    boolean hasSetClause = false;
//    for (Map.Entry<String, Object> entry : setValues.entrySet()) {
//      if (hasSetClause) {
//        sql.append(", ");
//      }
//      sql.append("`").append(entry.getKey()).append("` = ?");
//      params.add(entry.getValue());
//      hasSetClause = true;
//    }
//
//    // 2. 再处理原始表达式: 如 `isLock = 1 - isLock`
//    for (String rawClause : rawSetClauses) {
//      if (hasSetClause) {
//        sql.append(", ");
//      }
//      sql.append(rawClause); // 直接拼接，不加参数
//      hasSetClause = true;
//    }
//
//    // 3. 添加 WHERE 条件
//    if (!whereClause.isEmpty()) {
//      sql.append(" WHERE ").append(whereClause);
//      params.addAll(whereParams);
//    }
//
//    return BuiltSql.of(sql.toString(), params);
//  }

  /**
   * 构建DELETE SQL
   */
  private BuiltSql buildDelete() {
    if (tableName == null) {
      throw new IllegalStateException("表名未设置");
    }

    StringBuilder sql = new StringBuilder("DELETE FROM ").append(tableName);
    List<Object> params = new ArrayList<>();

    // 添加WHERE条件
    if (!whereClause.isEmpty()) {
      sql.append(" WHERE ").append(whereClause);
      params.addAll(whereParams);
    }

    return BuiltSql.of(sql.toString(), params);
  }

  // ================ 链式调用支持 ================

  /**
   * 应用自定义配置
   */
  public DmlBuilder apply(Consumer<DmlBuilder> consumer) {
    consumer.accept(this);
    return this;
  }

  // ================ 执行方法 ================

  /**
   * 执行INSERT并返回生成的主键
   */
  public Long insert() {
    BuiltSql builtSql = buildInsert();
    return databaseUtil.executeInsertWithGeneratedKey(schoolId, builtSql);
  }

  /**
   * 在指定连接上执行INSERT并返回生成的主键（用于事务内）
   * @param conn 数据库连接（由事务提供）
   */
  public Long insert(Connection conn) {
    BuiltSql builtSql = buildInsert();
    return databaseUtil.executeInsertWithGeneratedKey(conn, builtSql);
  }

  /**
   * 执行UPDATE
   */
  public int update() {
    BuiltSql builtSql = buildUpdate();
    return databaseUtil.executeUpdate(schoolId, builtSql);
  }

  /**
   * 在指定连接上执行UPDATE（用于事务内）
   */
  public int update(Connection conn) {
    BuiltSql builtSql = buildUpdate();
    return databaseUtil.executeUpdate(conn, builtSql);
  }

  /**
   * 执行DELETE
   */
  public int delete() {
    BuiltSql builtSql = buildDelete();
    return databaseUtil.executeUpdate(schoolId, builtSql);
  }

  /**
   * 在指定连接上执行DELETE（用于事务内）
   */
  public int delete(Connection conn) {
    BuiltSql builtSql = buildDelete();
    return databaseUtil.executeUpdate(conn, builtSql);
  }

  /**
   * 直接执行SQL（保持原有功能）
   */
  public int execute() {
    if (tableName == null || (!setValues.isEmpty() || !whereClause.isEmpty())) {
      throw new IllegalStateException("请使用insert()、update()或delete()方法");
    }
    // 这里保持原有的execute方法逻辑
    return 0;
  }

  /**
   * 执行插入并返回生成的主键（保持原有功能）
   */
  public Long executeWithGeneratedKey() {
    if (tableName == null || (!setValues.isEmpty() || !whereClause.isEmpty())) {
      throw new IllegalStateException("请使用insert()方法");
    }
    // 这里保持原有的executeWithGeneratedKey方法逻辑
    return null;
  }

  // ================ 私有方法 ================

  private void addCondition(String condition) {
    if (conditionCount > 0) {
      whereClause.append(" AND ");
    }
    whereClause.append(condition);
    conditionCount++;
  }

}