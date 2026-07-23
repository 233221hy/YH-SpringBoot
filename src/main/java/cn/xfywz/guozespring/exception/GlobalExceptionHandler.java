package cn.xfywz.guozespring.exception;


import cn.xfywz.guozespring.util.ExceptionPackageUtil;
import cn.xfywz.guozespring.util.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import cn.dev33.satoken.exception.NotLoginException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TARGET_PACKAGE = "cn.xfywz.guozespring";

    // ================ 业务异常（HTTP 400） ================
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleBusinessException(BusinessException e) {
        // 只有来自本项目的 BusinessException 才记录日志
        if (ExceptionPackageUtil.isThrownFromPackage(e, TARGET_PACKAGE)) {
            BusinessException.ErrorCode errorCode = e.getErrorCode();
            if (errorCode != null) {
                log.warn("业务异常[{}]: {}", errorCode, e.getMessage());
            } else {
                log.warn("业务异常: {}", e.getMessage());
            }
        }

        // 无论是否记录日志，都返回错误响应（保证 API 行为一致）
        return Result.error(e.getMessage());
    }
//    // ================ 业务验证异常（HTTP 400） ================
//    @ExceptionHandler(BusinessException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    public Result handleBusinessValidationException(BusinessException e) {
//        return Result.error(e.getMessage());
//    }

    // ================ 学校验证异常（HTTP 400） ================
//    @ExceptionHandler(SchoolValidationException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    public Result handleSchoolValidationException(SchoolValidationException e) {
//        return Result.error(e.getMessage());
//    }

    // ================ 导入导出异常（HTTP 400） ================
    @ExceptionHandler(ImportExportException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleImportExportException(ImportExportException e) {
        log.error("导入导出异常: {}", e.getMessage(), e);
        return Result.error(e.getMessage());
    }

    // ================ 数据库技术异常 ================
    @ExceptionHandler(DatabaseException.class)
    public Result handleDatabaseException(DatabaseException e) {
        DatabaseException.ErrorType errorType = e.getErrorType();

        return switch (errorType) {
            case CONNECTION -> {
                // 连接异常返回503
                log.error("数据库连接异常: {}", e.getMessage(), e);
                yield Result.error("数据库连接失败，请稍后重试");
            }
            case DATA_ACCESS -> {
                // 数据访问异常返回500
                log.error("数据访问异常: {}", e.getMessage(), e);
                yield Result.error("数据访问异常，请联系管理员");
            }
            case EXECUTION -> {
                // SQL执行异常返回500
                log.error("SQL执行异常: {}", e.getMessage(), e);
                yield Result.error("数据库操作失败");
            }
            default -> {
                log.error("数据库异常: {}", e.getMessage(), e);
                yield Result.error("系统异常，请联系管理员");
            }
        };
    }


    // ================ 数据访问异常（HTTP 500） ================
    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleDataAccessException(DataAccessException e) {
        log.error("数据访问异常: {}", e.getMessage(), e);
        return Result.error("数据访问异常: " + e.getMessage());
    }

    // ================ 参数校验异常（HTTP 400） ================
    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleDuplicateKeyException(DuplicateKeyException e) {
        log.warn("数据重复: {}", e.getMessage());
        String msg = e.getMessage();
        String errMsg = msg.substring(msg.indexOf("Duplicate entry"));
        String [] arr = errMsg.split(" ");
        return Result.error(arr[2]+"已存在");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getAllErrors().stream()
            .map(ObjectError::getDefaultMessage)
            .collect(Collectors.joining(", "));

        log.error("参数校验失败: {}", errorMessage);
        return Result.error("参数校验失败: " + errorMessage);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleConstraintViolationException(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().stream()
            .map(ConstraintViolation::getMessage)
            .collect(Collectors.joining(", "));

        log.error("参数校验失败: {}", errorMessage);
        return Result.error("参数校验失败: " + errorMessage);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<String> handleMissingParams(MissingServletRequestParameterException ex) {
        String name = ex.getParameterName();
        return ResponseEntity.badRequest().body("缺少必要参数: " + name);
    }

    // ================ 文件上传异常（HTTP 400） ================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("文件大小超过限制: {}", e.getMessage());
        return Result.error("上传文件大小超过限制");
    }

    // ================ Sa-Token 未登录异常（HTTP 401） ================
    @ExceptionHandler(NotLoginException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result handleNotLoginException(NotLoginException e) {
        log.warn("Sa-Token未登录: {}", e.getMessage());
        return Result.error("登录已过期，请重新登录");
    }

    // ================ 系统异常（HTTP 500） ================
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result handleException(Exception e) {
        log.error("系统异常: {}", e.getMessage(), e);
        return Result.error("系统异常，请联系管理员");
    }
}
