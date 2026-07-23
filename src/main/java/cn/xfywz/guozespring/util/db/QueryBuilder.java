package cn.xfywz.guozespring.util.db;

import cn.xfywz.guozespring.exception.DatabaseException;
import cn.xfywz.guozespring.util.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 查询构建器
 */
@RequiredArgsConstructor
public class QueryBuilder {
  public final int schoolId;
  private final DatabaseUtil databaseUtil;

  // SQL构建相关
  private String sql;
  private final StringBuilder whereClause = new StringBuilder();
  private final List<Object> params = new ArrayList<>();
  private int conditionCount = 0;
  private String orderBy;
  private String groupBy;
  private Integer limit;
  private Integer offset;

  // ================ SQL设置方法 ================

  /**
   * 设置SQL语句
   */
  public QueryBuilder sql(String sql) {
    this.sql = sql;
    return this;
  }

  /**
   * 添加单个参数
   */
  public QueryBuilder param(Object param) {
    this.params.add(param);
    return this;
  }

  /**
   * 添加多个参数
   */
  public QueryBuilder params(Object... params) {
    if (params != null) {
      this.params.addAll(Arrays.asList(params));
    }
    return this;
  }

  // ================ 条件构建方法 ================

  /**
   * 添加WHERE条件
   */
  public QueryBuilder where(String condition, Object... conditionParams) {
    if (StringUtils.hasText(condition)) {
      addCondition(condition);
      if (conditionParams != null) {
        this.params.addAll(Arrays.asList(conditionParams));
      }
    }
    return this;
  }

  /**
   * 相等条件
   */
  public QueryBuilder eq(String field, Object value) {
    if (value != null) {
      addCondition(field + " = ?");
      params.add(value);
    }
    return this;
  }

  /**
   * LIKE条件
   */
  public QueryBuilder like(String field, String value) {
    if (StringUtils.hasText(value)) {
      addCondition(field + " LIKE ?");
      params.add("%" + value + "%");
    }
    return this;
  }

  /**
   * IN条件
   */
  public QueryBuilder in(String field, List<?> values) {
    if (values != null && !values.isEmpty()) {
      String placeholders = String.join(",",
              Collections.nCopies(values.size(), "?"));
      addCondition(field + " IN (" + placeholders + ")");
      params.addAll(values);
    }
    return this;
  }

  /**
   * 大于条件
   */
  public QueryBuilder gt(String field, Comparable<?> value) {
    if (value != null) {
      addCondition(field + " > ?");
      params.add(value);
    }
    return this;
  }

  /**
   * 小于条件
   */
  public QueryBuilder lt(String field, Comparable<?> value) {
    if (value != null) {
      addCondition(field + " < ?");
      params.add(value);
    }
    return this;
  }

  /**
   * BETWEEN条件
   */
  public QueryBuilder between(String field, Object from, Object to) {
    if (from != null && to != null) {
      addCondition(field + " BETWEEN ? AND ?");
      params.add(from);
      params.add(to);
    }
    return this;
  }

  /**
   * IS NULL条件
   */
  public QueryBuilder isNull(String field) {
    addCondition(field + " IS NULL");
    return this;
  }

  /**
   * IS NOT NULL条件
   */
  public QueryBuilder isNotNull(String field) {
    addCondition(field + " IS NOT NULL");
    return this;
  }

  // ================ 排序和分页 ================

  /**
   * 设置排序
   */
  public QueryBuilder orderBy(String orderBy) {
    this.orderBy = orderBy;
    return this;
  }

  /**
   * 设置分组
   */
  public QueryBuilder groupBy(String groupBy) {
    this.groupBy = groupBy;
    return this;
  }

  /**
   * 设置分页
   */
  public QueryBuilder page(int pageNum, int pageSize) {
    this.limit = pageSize;
    this.offset = (pageNum - 1) * pageSize;
    return this;
  }

  /**
   * 设置限制
   */
  public QueryBuilder limit(int limit) {
    this.limit = limit;
    return this;
  }

  /**
   * 设置偏移量
   */
  public QueryBuilder offset(int offset) {
    this.offset = offset;
    return this;
  }

  // ================ 执行方法 ================

  /**
   * 构建BuiltSql
   */
  public BuiltSql build() {
    if (sql == null) {
      throw new IllegalStateException("SQL语句未设置");
    }

    StringBuilder finalSql = new StringBuilder(sql);

    // 添加WHERE条件
    if (!whereClause.isEmpty()) {
      if (!sql.toUpperCase().contains("WHERE")) {
        finalSql.append(" WHERE ");
      } else {
        finalSql.append(" AND ");
      }
      finalSql.append(whereClause);
    }
    // 添加分组
    if (StringUtils.hasText(groupBy)) {
      String normalizedGroupBy = groupBy.replaceAll("\\s+", " ").trim();
      finalSql.append(" GROUP BY ").append(normalizedGroupBy);
    }

    // 添加排序
    if (StringUtils.hasText(orderBy)) {
      String normalizedOrderBy = orderBy.replaceAll("\\s+", " ").trim();
      finalSql.append(" ORDER BY ").append(normalizedOrderBy);
    }

    // 添加分页
    if (limit != null && offset != null) {
      finalSql.append(" LIMIT ? OFFSET ?");
      List<Object> finalParams = new ArrayList<>(params);
      finalParams.add(limit);
      finalParams.add(offset);
      return BuiltSql.of(finalSql.toString(), finalParams);
    } else if (limit != null) {
      finalSql.append(" LIMIT ?");
      List<Object> finalParams = new ArrayList<>(params);
      finalParams.add(limit);
      return BuiltSql.of(finalSql.toString(), finalParams);
    }

    return BuiltSql.of(finalSql.toString(), params);
  }

  /**
   * 构建计数SQL(包含子查询)
   */
  public BuiltSql buildCount() {
    if (sql == null) {
      throw new IllegalStateException("SQL语句未设置");
    }

    // 备份排序分页相关字段
    String originalOrderBy = this.orderBy;
    Integer originalLimit = this.limit;
    Integer originalOffset = this.offset;

    try {
      // 临时清空，使得 build() 不会追加 ORDER BY 和 LIMIT
      this.orderBy = null;
      this.limit = null;
      this.offset = null;

      // 构建基础查询 SQL（包含 WHERE 和 GROUP BY，不含 ORDER BY / LIMIT）
      BuiltSql baseSql = build();  // 注意：build() 返回的 params 不包含 limit/offset 参数

      // 包装为 COUNT 子查询
      String countSql = "SELECT COUNT(*) FROM (" + baseSql.sql() + ") AS _cnt";
      return BuiltSql.of(countSql, baseSql.params());
    } finally {
      // 恢复原值
      this.orderBy = originalOrderBy;
      this.limit = originalLimit;
      this.offset = originalOffset;
    }
  }
//  public BuiltSql buildCount() {
//    if (sql == null) {
//      throw new IllegalStateException("SQL语句未设置");
//    }
//
//    // 从原SQL中提取FROM部分
//    String fromClause = extractFromClause(sql);
//    StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM ").append(fromClause);
//
//    // 添加WHERE条件
//    if (!whereClause.isEmpty()) {
//      countSql.append(" WHERE ").append(whereClause);
//    }
//
//    return BuiltSql.of(countSql.toString(), params);
//  }

  /**
   * 构建 COUNT(DISTINCT) SQL（基于子查询包装）
   *
   * @param countColumn 内层查询结果中的列名（ SELECT 列表中出现的别名或列名）
   */
  public BuiltSql buildCountDistinct(String countColumn) {
    if (sql == null) {
      throw new IllegalStateException("SQL语句未设置");
    }
    String originalOrderBy = this.orderBy;
    Integer originalLimit = this.limit;
    Integer originalOffset = this.offset;

    try {
      this.orderBy = null;
      this.limit = null;
      this.offset = null;
      BuiltSql baseSql = build(); // 含 WHERE / GROUP BY，无 ORDER BY 和 LIMIT

      // 使用内层查询作为子查询，外层对指定列做 DISTINCT 计数
      String countSql = "SELECT COUNT(DISTINCT " + countColumn + ") FROM (" + baseSql.sql() + ") AS _cnt";
      return BuiltSql.of(countSql, baseSql.params());
    } finally {
      this.orderBy = originalOrderBy;
      this.limit = originalLimit;
      this.offset = originalOffset;
    }
  }

//  /**
//   * 构建COUNT DISTINCT SQL(包含子查询)
//   */
//  public BuiltSql buildCountDistinct(String countColumn) {
//    if (sql == null) {
//      throw new IllegalStateException("SQL语句未设置");
//    }
//
//    // 临时清空排序分页，调用 build() 获取基础 SQL，然后包装
//    String originalOrderBy = this.orderBy;
//    Integer originalLimit = this.limit;
//    Integer originalOffset = this.offset;
//    try {
//      this.orderBy = null;
//      this.limit = null;
//      this.offset = null;
//      BuiltSql baseSql = build(); // 得到含 WHERE / GROUP BY 的基础查询
//
//      String countSql = "SELECT COUNT(DISTINCT " + countColumn + ") FROM (" + baseSql.sql() + ") AS _cnt";
//      return BuiltSql.of(countSql, baseSql.params());
//    } finally {
//      this.orderBy = originalOrderBy;
//      this.limit = originalLimit;
//      this.offset = originalOffset;
//    }
//  }
//  public BuiltSql buildCountDistinct(String countColumn) {
//    if (sql == null) {
//      throw new IllegalStateException("SQL语句未设置");
//    }
//
//    String fromClause = extractFromClause(sql); // 使用已有的私有方法
//    // 构建 COUNT(DISTINCT 列) FROM ...
//    StringBuilder countSql = new StringBuilder("SELECT COUNT(DISTINCT ")
//            .append(countColumn)
//            .append(") FROM ")
//            .append(fromClause);
//
//    // 添加WHERE条件
//    if (!whereClause.isEmpty()) {
//      countSql.append(" WHERE ").append(whereClause);
//    }
//
//    return BuiltSql.of(countSql.toString(), params);
//  }
//
//  /**
//   * 构建简单COUNT DISTINCT SQL
//   */
//  public BuiltSql buildCountDistinctSimple(String countColumn, String fromTable) {
//    StringBuilder countSql = new StringBuilder("SELECT COUNT(DISTINCT ")
//            .append(countColumn)
//            .append(") FROM ")
//            .append(fromTable);
//    if (!whereClause.isEmpty()) {
//      countSql.append(" WHERE ").append(whereClause);
//    }
//    return BuiltSql.of(countSql.toString(), params);
//  }


  /**
   * 查询列表
   */
  public <T> List<T> list(Function<ResultSet, T> rowMapper) {
    BuiltSql builtSql = build();
    return databaseUtil.executeQuery(schoolId, builtSql, rs -> {
      List<T> result = new ArrayList<>();
      try {
        while (rs.next()) {
          result.add(rowMapper.apply(rs));
        }
      } catch (SQLException e) {
        throw new DatabaseException("结果集处理失败", e);
      }
      return result;
    });
  }

  /**
   * 执行单条（一行）查询
  **/
  private <T> Optional<T> doQuerySingle(SqlFunction<ResultSet, T> mapper, String errorMsg) {
    this.limit = 1;
    BuiltSql builtSql = build();
    return databaseUtil.executeQuery(schoolId, builtSql, rs -> {
      try {
        if (rs.next()) {
          T value = mapper.apply(rs); // ← 可能抛 SQLException
          return Optional.ofNullable(value);
        }
        return Optional.empty();
      } catch (SQLException e) {
        throw new DatabaseException(errorMsg, e);
      }
    });
  }

  /**
   * 查询单个对象
   */
  public <T> Optional<T> single(SqlFunction<ResultSet, T> rowMapper) {
    return doQuerySingle(rowMapper, "查询单条记录失败");
  }

  /**
   * 查询单个值
   */
  public <T> Optional<T> scalar(SqlFunction<ResultSet, T> valueMapper) {
    return doQuerySingle(valueMapper, "查询单个值失败");
  }

  /**
   * 分页查询
   */
  public <T> PageResult<T> page(Function<ResultSet, T> rowMapper, int pageNum, int pageSize) {
    // 构建数据查询SQL
    this.limit = pageSize;
    this.offset = (pageNum - 1) * pageSize;
    BuiltSql dataSql = build();

    // 构建计数SQL
    BuiltSql countSql = buildCount();

    return databaseUtil.queryPage(schoolId, dataSql, countSql, rowMapper);
  }

  /**
   * 分页查询（支持 COUNT DISTINCT 去重计数）
   * @param countColumn 用于去重计数的列名，如 "ye.id"
   * @param rowMapper 结果映射函数
   * @param pageNum 页码
   * @param pageSize 每页大小
   */
  public <T> PageResult<T> pageDistinct(String countColumn,
                                        Function<ResultSet, T> rowMapper,
                                        int pageNum,
                                        int pageSize) {
    // 1. 构建分页数据 SQL（设置 limit/offset）
    this.limit = pageSize;
    this.offset = (pageNum - 1) * pageSize;
    BuiltSql dataSql = build();  // 此时 build() 会包含 LIMIT ? OFFSET ?

    // 2. 构建 COUNT DISTINCT SQL
    BuiltSql countSql = buildCountDistinct(countColumn);

    // 3. 调用 DatabaseUtil 的通用分页方法执行
    return databaseUtil.queryPage(schoolId, dataSql, countSql, rowMapper);
  }

  /**
   * 查询是否存在
   */
  public boolean exists() {
    BuiltSql builtSql = buildCount();
    Integer count = databaseUtil.executeQuery(schoolId, builtSql, rs -> {
      try {
        return rs.next() ? rs.getInt(1) : 0;
      } catch (SQLException e) {
        throw new DatabaseException("存在性检查失败", e);
      }
    });
    return count > 0;
  }

  /**
   * 查询数量
   */
  public long count() {
    BuiltSql builtSql = buildCount();
    Long count = databaseUtil.executeQuery(schoolId, builtSql, rs -> {
      try {
        return rs.next() ? rs.getLong(1) : 0L;
      } catch (SQLException e) {
        throw new DatabaseException("计数查询失败", e);
      }
    });
    return count != null ? count : 0L;
  }

  /**
   * 遍历结果集
   */
  public void forEach(ResultSetConsumer consumer) {
    BuiltSql builtSql = build();
    databaseUtil.executeQuery(schoolId, builtSql, rs -> {
      try {
        while (rs.next()) {
          consumer.accept(rs);
        }
      } catch (SQLException e) {
        throw new DatabaseException("遍历结果集失败", e);
      }
      return null;
    });
  }

  /**
   * 结果集消费者接口
   */
  @FunctionalInterface
  public interface ResultSetConsumer {
    void accept(ResultSet rs) throws SQLException;
  }


  /**
   * 应用构建者
   */
  public QueryBuilder apply(Consumer<QueryBuilder> consumer) {
    consumer.accept(this);
    return this;
  }

  // ================ 私有方法 ================

  //抛异常的Function接口
  @FunctionalInterface
  public interface SqlFunction<T, R> {
    R apply(T t) throws SQLException;
  }

  private void addCondition(String condition) {
    if (conditionCount > 0) {
      whereClause.append(" AND ");
    }
    whereClause.append(condition);
    conditionCount++;
  }

  private String extractFromClause(String sql) {
    // 移除换行符和多余空格后再处理
    String normalizedSql = sql.replaceAll("\\s+", " ").trim();
    // 提取FROM之后到WHERE/ORDER BY/LIMIT之前的部分
    String upperSql = normalizedSql.toUpperCase();
    int fromIndex = upperSql.indexOf(" FROM ");
    if (fromIndex == -1) {
      throw new IllegalArgumentException("SQL语句必须包含FROM子句: " + sql);
    }

    String fromPart = normalizedSql.substring(fromIndex + 6);

    // 移除后续子句
    int whereIndex = upperSql.indexOf(" WHERE ", fromIndex);
    int orderByIndex = upperSql.indexOf(" ORDER BY ", fromIndex);
    int limitIndex = upperSql.indexOf(" LIMIT ", fromIndex);
    int groupByIndex = upperSql.indexOf(" GROUP BY ", fromIndex);

    int endIndex = normalizedSql.length();
    if (whereIndex > 0) endIndex = Math.min(endIndex, whereIndex);
    if (orderByIndex > 0) endIndex = Math.min(endIndex, orderByIndex);
    if (limitIndex > 0) endIndex = Math.min(endIndex, limitIndex);
    if (groupByIndex > 0) endIndex = Math.min(endIndex, groupByIndex);

    return normalizedSql.substring(fromIndex + 6, endIndex).trim();
  }
}