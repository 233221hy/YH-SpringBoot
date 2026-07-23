package cn.xfywz.guozespring.exception;

import lombok.Getter;

/**
 * 数据库操作异常（包含异常类型）
 */
@Getter
public class DatabaseException extends RuntimeException {

  private final ErrorType errorType;

  public enum ErrorType {
    DATA_ACCESS,         // 数据访问异常
    CONNECTION,          // 连接异常
    EXECUTION,           // SQL执行异常
    UNKNOWN              // 未知异常
  }

  public DatabaseException(String message) {
    super(message);
    this.errorType = ErrorType.UNKNOWN;
  }

  public DatabaseException(String message, Throwable cause) {
    super(message, cause);
    this.errorType = ErrorType.UNKNOWN;
  }

  public DatabaseException(ErrorType errorType, String message) {
    super(message);
    this.errorType = errorType;
  }

  public DatabaseException(ErrorType errorType, String message, Throwable cause) {
    super(message, cause);
    this.errorType = errorType;
  }

    // 快速创建方法
  public static DatabaseException connection(String message) {
    return new DatabaseException(ErrorType.CONNECTION, message);
  }

  public static DatabaseException execution(String message) {
    return new DatabaseException(ErrorType.EXECUTION, message);
  }

  public static DatabaseException dataAccess(String message) {
    return new DatabaseException(ErrorType.DATA_ACCESS, message);
  }
}
