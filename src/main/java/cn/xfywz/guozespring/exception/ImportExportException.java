package cn.xfywz.guozespring.exception;

/**
 * 导入导出异常
 */
public class ImportExportException extends RuntimeException {
  public ImportExportException(String message) {
    super(message);
  }

  public ImportExportException(String message, Throwable cause) {
    super(message, cause);
  }
}