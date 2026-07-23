package cn.xfywz.guozespring.util.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * 事务操作接口
 */
@FunctionalInterface
public interface TransactionOperation<T> {

  /**
   * 在事务中执行操作
   */
  T apply(Connection connection, PreparedStatement statement) throws SQLException;

  /**
   * 创建简单事务操作（不使用PreparedStatement）
   */
  static <T> TransactionOperation<T> simple(Function<Connection, T> operation) {
    return (connection, statement) -> operation.apply(connection);
  }
}