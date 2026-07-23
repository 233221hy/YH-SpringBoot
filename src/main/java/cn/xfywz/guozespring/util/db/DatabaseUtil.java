package cn.xfywz.guozespring.util.db;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.exception.BusinessException;
import cn.xfywz.guozespring.exception.DatabaseException;
import cn.xfywz.guozespring.util.BusinessValidator;
import cn.xfywz.guozespring.util.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 数据库操作工具类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseUtil {

  private final BusinessValidator businessValidator;

  // SQL语句验证正则表达式 - 只允许基本的查询、更新、删除操作
  private static final Pattern SQL_PATTERN = Pattern.compile(
          "^(?i)(SELECT|INSERT|UPDATE|DELETE|WITH)\\s+.*$",
          Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  );

  // 危险SQL关键字黑名单
  private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
          "DROP", "CREATE", "ALTER", "TRUNCATE", "EXEC", "CALL",
          "DECLARE", "PROCEDURE", "FUNCTION"
  );

  @FunctionalInterface
  public interface SqlFunction<T, R> {
    R apply(T t) throws SQLException;
  }

  // ================ 链式调用支持 ================

  /**
   * 创建查询构建器
   */
  public QueryBuilder query(int schoolId) {
    return new QueryBuilder(schoolId, this);
  }

  /**
   * 创建更新构建器
   */
  public DmlBuilder update(int schoolId) {
    return new DmlBuilder(schoolId, this);
  }

  // ================ 核心执行方法 ================

  /**
   * 执行查询
   */
  public <T> T executeQuery(int schoolId, BuiltSql builtSql, Function<ResultSet, T> resultHandler) {
    return executeWithConnection(schoolId, connection -> {
      try (PreparedStatement ps = connection.prepareStatement(builtSql.sql())) {
        JdbcParamsUtil.setParams(ps, builtSql.params());
        try (ResultSet rs = ps.executeQuery()) {
          return resultHandler.apply(rs);
        }
      } catch (SQLException e) {
        throw new DatabaseException("查询执行失败: " + builtSql.sql(), e);
      }
    });
  }

  /**
   * 执行查询（简单参数）
   */
  public <T> T executeQuery(int schoolId, String sql, Function<ResultSet, T> resultHandler, Object... params) {
    return executeQuery(schoolId, BuiltSql.of(sql, params), resultHandler);
  }

  /**
   * 在已有连接上执行查询（用于事务内）
   * @param conn 数据库连接（由调用方管理，不会自动关闭）
   * @param builtSql 封装的SQL与参数
   * @param resultHandler 结果集处理函数
   * @return 处理后的结果
   */
  public <T> T executeQuery(Connection conn, BuiltSql builtSql, SqlFunction<ResultSet, T> resultHandler) {
    try (PreparedStatement ps = conn.prepareStatement(builtSql.sql())) {
      // 添加调试日志
      log.debug("执行 SQL: {}", builtSql.sql());
      log.debug("参数数量：{}, 参数列表：{}", builtSql.params().size(), builtSql.params());
      JdbcParamsUtil.setParams(ps, builtSql.params());
      try (ResultSet rs = ps.executeQuery()) {
        return resultHandler.apply(rs);
      }
    } catch (SQLException e) {
      throw new DatabaseException("查询执行失败: " + builtSql.sql(), e);
    }
  }
//  public <T> T executeQuery(Connection conn, BuiltSql builtSql, Function<ResultSet, T> resultHandler) {
//    try (PreparedStatement ps = conn.prepareStatement(builtSql.sql())) {
//      JdbcParamsUtil.setParams(ps, builtSql.params());
//      try (ResultSet rs = ps.executeQuery()) {
//        return resultHandler.apply(rs);
//      }
//    } catch (SQLException e) {
//      throw new DatabaseException("查询执行失败: " + builtSql.sql(), e);
//    }
//  }

  /**
   * 执行更新
   */
  public int executeUpdate(int schoolId, BuiltSql builtSql) {
    return executeWithConnection(schoolId, connection -> {
      try (PreparedStatement ps = connection.prepareStatement(builtSql.sql())) {
        JdbcParamsUtil.setParams(ps, builtSql.params());
        return ps.executeUpdate();
      } catch (SQLException e) {
        throw new DatabaseException("更新执行失败: " + builtSql.sql(), e);
      }
    });
  }

  /**
   * 执行更新（简单参数）
   */
  public int executeUpdate(int schoolId, String sql, Object... params) {
    return executeUpdate(schoolId, BuiltSql.of(sql, params));
  }

  /**
   * 在已有连接上执行更新操作（用于事务内）
   * @param conn 数据库连接（需由调用方管理，不会自动关闭）
   * @param builtSql 封装的SQL与参数
   * @return 影响行数
   */
  public int executeUpdate(Connection conn, BuiltSql builtSql) {
    try (PreparedStatement ps = conn.prepareStatement(builtSql.sql())) {
      JdbcParamsUtil.setParams(ps, builtSql.params());
      return ps.executeUpdate();
    } catch (SQLException e) {
      throw new DatabaseException("更新执行失败: " + builtSql.sql(), e);
    }
  }


  /**
   * 执行插入并返回生成的主键
   */
  public Long executeInsertWithGeneratedKey(int schoolId, BuiltSql builtSql) {
    return executeWithConnection(schoolId, connection -> {
      try (PreparedStatement ps = connection.prepareStatement(builtSql.sql(), Statement.RETURN_GENERATED_KEYS)) {
        JdbcParamsUtil.setParams(ps, builtSql.params());
        int rowsAffected = ps.executeUpdate();
        if (rowsAffected > 0) {
          try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
            if (generatedKeys.next()) {
              return generatedKeys.getLong(1);
            }
          }
        }
        return null;
      } catch (SQLException e) {
        throw new DatabaseException("插入执行失败: " + builtSql.sql(), e);
      }
    });
  }

  /**
   * 在已有连接上执行插入并返回主键（用于事务内）
   * @param conn 数据库连接
   * @param builtSql 封装的SQL与参数
   * @return 生成的主键，若无则返回null
   */
  public Long executeInsertWithGeneratedKey(Connection conn, BuiltSql builtSql) {
    try (PreparedStatement ps = conn.prepareStatement(builtSql.sql(), Statement.RETURN_GENERATED_KEYS)) {
      JdbcParamsUtil.setParams(ps, builtSql.params());
      int rowsAffected = ps.executeUpdate();
      if (rowsAffected > 0) {
        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
          if (generatedKeys.next()) {
            return generatedKeys.getLong(1);
          }
        }
      }
      return null;
    } catch (SQLException e) {
      throw new DatabaseException("插入执行失败: " + builtSql.sql(), e);
    }
  }

  /**
   * 执行批量操作
   */
  public int executeBatch(int schoolId, BatchBuilder batchBuilder) {
    return executeWithConnection(schoolId, connection -> {
      try (PreparedStatement ps = connection.prepareStatement(batchBuilder.getSql())) {
        for (List<Object> params : batchBuilder.getBatchParams()) {
          JdbcParamsUtil.setParams(ps, params);
          ps.addBatch();
        }
        int[] results = ps.executeBatch();
        int affected = 0;
        for (int result : results) {
          if (result > 0) {
            affected += result;
          } else if (result == Statement.SUCCESS_NO_INFO) {
            affected += 1;
          }
        }
        return affected;
      } catch (SQLException e) {
        throw new DatabaseException("批量插入失败: " + batchBuilder.getSql(), e);
      }
    });
  }

  /**
   * 在已有连接上执行批量操作（用于事务内）
   * @param conn 数据库连接（由事务提供）
   * @param batchBuilder 批量构建器
   * @return 影响行数总和
   */
  public int executeBatch(Connection conn, BatchBuilder batchBuilder) {
    if (batchBuilder.isEmpty()) {
      return 0;
    }
    try (PreparedStatement ps = conn.prepareStatement(batchBuilder.getSql())) {
      for (List<Object> params : batchBuilder.getBatchParams()) {
        JdbcParamsUtil.setParams(ps, params);
        ps.addBatch();
      }
      int[] results = ps.executeBatch();
      int affected = 0;
      for (int result : results) {
        if (result > 0) {
          affected += result;
        } else if (result == Statement.SUCCESS_NO_INFO) {
          affected += 1;
        }
      }
      return affected;
    } catch (SQLException e) {
      throw new DatabaseException("批量执行失败: " + batchBuilder.getSql(), e);
    }
  }



  // ================ 事务管理 ================

  /**
   * 在事务中执行操作
   */
  public <T> T executeInTransaction(int schoolId, Function<Connection, T> operation) {
    return executeWithConnection(schoolId, connection -> {
      boolean originalAutoCommit = true;
      try {
        originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        T result = operation.apply(connection);
        connection.commit();
        return result;
      } catch (Exception e) {
        try {
          connection.rollback();
        } catch (SQLException rollbackEx) {
          log.warn("事务回滚失败: {}", rollbackEx.getMessage());
        }
        throw new DatabaseException("事务执行失败: schoolId=" + schoolId, e);
      } finally {
        try {
          connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException e) {
          log.warn("恢复自动提交失败: {}", e.getMessage());
        }
      }
    });
  }

  /**
   * 在事务中执行操作（无返回值）
   */
  public void executeInTransaction(int schoolId, Consumer<Connection> operation) {
    executeInTransaction(schoolId, connection -> {
      operation.accept(connection);
      return null;
    });
  }


  /**
   * 批量执行（便捷方法）
   */
  public int executeBatch(int schoolId, String sql, Consumer<BatchBuilder> batchConfigurer) {
    BatchBuilder batchBuilder = new BatchBuilder(sql);
    batchConfigurer.accept(batchBuilder);
    return executeBatch(schoolId, batchBuilder);
  }

  // ================ 分页查询 ================

  /**
   * 分页查询
   */
  public <T> PageResult<T> queryPage(int schoolId, BuiltSql dataSql, BuiltSql countSql,
                                     Function<ResultSet, T> rowMapper) {
    // 查询数据
    List<T> rows = executeQuery(schoolId, dataSql, rs -> {
      List<T> resultList = new ArrayList<>();
      try {
        while (rs.next()) {
          resultList.add(rowMapper.apply(rs));
        }
      } catch (SQLException e) {
        throw new DatabaseException("结果集处理失败", e);
      }
      return resultList;
    });

    // 查询总数
    long total = executeQuery(schoolId, countSql, rs -> {
      try {
        return rs.next() ? rs.getLong(1) : 0L;
      } catch (SQLException e) {
        throw new DatabaseException("计数查询失败", e);
      }
    });

    return PageResult.of(total, rows);
  }

  /**
   * 分页查询（简单参数）
   */
  public <T> PageResult<T> queryPage(int schoolId, String dataSql, String countSql,
                                     Function<ResultSet, T> rowMapper,
                                     Object... params) {
    return queryPage(schoolId,
            BuiltSql.of(dataSql, params),
            BuiltSql.of(countSql, params),
            rowMapper
    );
  }



  // ================ DML操作 ================

  /**
   * 插入记录并返回主键
   */
  public Long insert(int schoolId, String tableName, Consumer<DmlBuilder> builderConsumer) {
    DmlBuilder builder = new DmlBuilder(schoolId, this);
    builder.table(tableName);
    builderConsumer.accept(builder);
    return builder.insert();
  }

  /**
   * 更新记录
   */
  public int update(int schoolId, String tableName, Consumer<DmlBuilder> builderConsumer) {
    DmlBuilder builder = new DmlBuilder(schoolId, this);
    builder.table(tableName);
    builderConsumer.accept(builder);
    return builder.update();
  }

  /**
   * 删除记录
   */
  public int delete(int schoolId, String tableName, Consumer<DmlBuilder> builderConsumer) {
    DmlBuilder builder = new DmlBuilder(schoolId, this);
    builder.table(tableName);
    builderConsumer.accept(builder);
    return builder.delete();
  }

//  /**
//   * 批量插入
//   */
//  public int batchInsert(int schoolId, String tableName, List<Map<String, Object>> records) {
//    if (records == null || records.isEmpty()) {
//      return 0;
//    }
//
//    // 使用第一个记录来确定字段
//    Map<String, Object> firstRecord = records.get(0);
//    String columns = firstRecord.keySet().stream()
//        .map(k -> "`" + k + "`")
//        .collect(Collectors.joining(", "));
//
//    String placeholders = firstRecord.keySet().stream()
//        .map(k -> "?")
//        .collect(Collectors.joining(", "));
//
//    String sql = "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + placeholders + ")";
//
//    BatchBuilder batchBuilder = new BatchBuilder(sql);
//    for (Map<String, Object> record : records) {
//      List<Object> params = new ArrayList<>(firstRecord.keySet().size());
//      for (String key : firstRecord.keySet()) {
//        params.add(record.get(key));
//      }
//      batchBuilder.addBatch(params);
//    }
//
//    return executeBatch(schoolId, batchBuilder);
//  }

  // ================ 实用方法 ================

  /**
   * 加载缓存数据到Map
   */
  public <K, V> Map<K, V> loadCache(int schoolId, String sql,
                                    Function<ResultSet, K> keyMapper,
                                    Function<ResultSet, V> valueMapper,
                                    Object... params) {
    return executeQuery(schoolId, sql, rs -> {
      Map<K, V> map = new HashMap<>();
      try {
        while (rs.next()) {
          K key = keyMapper.apply(rs);
          V value = valueMapper.apply(rs);
          if (key != null) {
            map.put(key, value);
          }
        }
      } catch (SQLException e) {
        throw new DatabaseException("结果集处理失败", e);
      }
      return map;
    }, params);
  }

  /**
   * 加载数据到Set
   */
  public <T> Set<T> loadSet(int schoolId, String sql,
                            Function<ResultSet, T> valueMapper,
                            Object... params) {
    return executeQuery(schoolId, sql, rs -> {
      Set<T> set = new HashSet<>();
      try {
        while (rs.next()) {
          T value = valueMapper.apply(rs);
          if (value != null) {
            set.add(value);
          }
        }
      } catch (SQLException e) {
        throw new DatabaseException("结果集处理失败", e);
      }
      return set;
    }, params);
  }

  /**
   * 检查记录是否存在
   */
  public boolean exists(int schoolId, String tableName, String condition, Object... params) {
    String sql = "SELECT COUNT(1) FROM " + tableName + " WHERE " + condition;
    Integer count = executeQuery(schoolId, sql, rs -> {
      try {
        return rs.next() ? rs.getInt(1) : 0;
      } catch (SQLException e) {
        throw new DatabaseException("存在性检查失败", e);
      }
    }, params);
    return count > 0;
  }

  /**
   * 查询单条记录
   */
  public <T> Optional<T> querySingle(int schoolId, String sql,
                                     Function<ResultSet, T> rowMapper,
                                     Object... params) {
    return executeQuery(schoolId, sql, rs -> {
      try {
        if (rs.next()) {
          return Optional.ofNullable(rowMapper.apply(rs));
        }
        return Optional.empty();
      } catch (SQLException e) {
        throw new DatabaseException("查询单条记录失败", e);
      }
    }, params);
  }

  /**
   * 执行标量查询，返回单行单列的 long 值（通常用于 COUNT、SUM 等）
   * @param conn 数据库连接（由调用方管理）
   * @param sql  SQL 语句
   * @param params 参数列表
   * @return 查询结果，若无结果则返回 0L
   */
  public long executeScalar(Connection conn, String sql, Object... params) {
    return executeScalar(conn, BuiltSql.of(sql, params));
  }

  /**
   * 执行标量查询，返回单行单列的 long 值（通常用于 COUNT、SUM 等）
   * @param conn 数据库连接（由调用方管理）
   * @param builtSql 封装的 SQL 与参数
   * @return 查询结果，若无结果则返回 0L
   */
  public long executeScalar(Connection conn, BuiltSql builtSql) {
    return executeQuery(conn, builtSql, rs -> {
      try {
        return rs.next() ? rs.getLong(1) : 0L;
      } catch (SQLException e) {
        throw new DatabaseException("标量查询失败", e);
      }
    });
  }

  /**
   * 获取学校数据库连接
   */
  public Connection getConnection(int schoolId) throws SQLException {
    SlSchool school = businessValidator.validateSchool(schoolId);
    return SlaveMysqlConnectionUtil.getConnection(school);
  }

  // ================ 私有方法 ================

  /**
   * 验证SQL语句的安全性
   */
  private void validateSql(String sql) {
    if (sql == null || sql.trim().isEmpty()) {
      throw new DatabaseException("SQL语句不能为空");
    }

    // 检查是否符合基本的SQL语句格式
    if (!SQL_PATTERN.matcher(sql).matches()) {
      log.warn("非法SQL格式: {}", sql);
      throw new DatabaseException("非法的SQL语句格式");
    }

    // 检查是否包含危险关键字
    String upperSql = sql.toUpperCase();
    for (String keyword : DANGEROUS_KEYWORDS) {
      if (upperSql.contains(keyword)) {
        log.warn("检测到危险SQL关键字: {}, SQL: {}", keyword, sql);
        throw new DatabaseException("SQL语句包含危险关键字: " + keyword);
      }
    }

    // 记录SQL执行日志
    log.debug("执行SQL: {}", sql);
  }

  /**
   * 通用的连接执行方法
   */
  private <T> T executeWithConnection(int schoolId, Function<Connection, T> operation) {
    // 1. 验证学校
    SlSchool school = businessValidator.validateSchool(schoolId);

    try (Connection connection = SlaveMysqlConnectionUtil.getConnection(school)) {
      // 2. 执行数据库操作
      return operation.apply(connection);

    } catch (BusinessException | DatabaseException e) {
      // 业务异常和数据库异常直接抛出
      throw e;

    } catch (SQLException e) {
      // 3. 处理SQL技术异常
      log.error("数据库SQL异常: schoolId={}, errorCode={}, state={}",
              schoolId, e.getErrorCode(), e.getSQLState(), e);
      throw new DatabaseException("数据库操作失败: " + e.getMessage(), e);

    } catch (Exception e) {
      // 4. 处理其他异常
      log.error("数据库操作未知异常: schoolId={}", schoolId, e);
      throw new DatabaseException("数据库操作失败: schoolId=" + schoolId, e);
    }
  }

  /**
   * 处理SQL技术异常
   */
  private void handleSQLException(SQLException e, int schoolId) {
    String state = e.getSQLState();
    String errorMessage = e.getMessage();

    log.error("SQL技术异常: state={}, message={}, schoolId={}", state, errorMessage, schoolId, e);

    if (errorMessage.contains("connection") ||
            errorMessage.contains("Communications link failure") ||
            errorMessage.contains("Connection refused")) {
      // 连接异常
      throw DatabaseException.connection("数据库连接失败: schoolId=" + schoolId);

    } else if (errorMessage.contains("doesn't exist") ||
            errorMessage.contains("unknown database") ||
            errorMessage.contains("table") && errorMessage.contains("not exist")) {
      // 表或数据库不存在
      throw DatabaseException.dataAccess("数据表不存在: schoolId=" + schoolId);

    } else if (errorMessage.contains("syntax") ||
            errorMessage.contains("SQL syntax")) {
      // SQL语法错误
      throw DatabaseException.execution("SQL语法错误: schoolId=" + schoolId);

    } else {
      // 其他技术异常
      throw DatabaseException.execution("数据库执行失败: schoolId=" + schoolId);
    }
  }

}