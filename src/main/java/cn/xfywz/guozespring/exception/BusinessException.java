package cn.xfywz.guozespring.exception;

import lombok.Getter;

/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;



  public enum ErrorCode {
    // 学校相关
    SCHOOL_NOT_FOUND,        // 学校不存在
    SCHOOL_NOT_APPROVED,     // 学校未审核

    // 数据重复
    DUPLICATE_STUDENT_NUMBER, // 学号重复
    DUPLICATE_TEACHER_ACCOUNT, // 教师账号重复
    DUPLICATE_STUDENT_ID_CARD, // 学生身份证重复

    // 参数错误

    // 业务规则
    CLASS_FULL,              // 班级已满
    INVALID_STATUS,          // 状态不允许
    PERMISSION_DENIED,       // 权限不足

    // 数据验证
    INVALID_ID_CARD,         // 身份证无效
    INVALID_MOBILE,          // 手机号无效
    INVALID_EMAIL,           // 邮箱无效

    // 通用
    DATA_NOT_FOUND,          // 数据不存在
    PARAMETER_INVALID        // 参数无效
  }

  public BusinessException(String message) {
    super(message);
    this.errorCode = null;
  }

  public BusinessException(ErrorCode errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public BusinessException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = null;
  }

  public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
  }

    // 快速创建方法
  public static BusinessException schoolNotFound(int schoolId) {
    return new BusinessException(
            ErrorCode.SCHOOL_NOT_FOUND,
            "学校不存在: " + schoolId
    );
  }

  public static BusinessException schoolNotApproved(int schoolId) {
    return new BusinessException(
            ErrorCode.SCHOOL_NOT_APPROVED,
            "学校未审核: " + schoolId
    );
  }

  public static BusinessException duplicateStudentNumber(String number) {
    return new BusinessException(
            ErrorCode.DUPLICATE_STUDENT_NUMBER,
            "学号 " + number + " 已存在"
    );
  }

  //教师账号已存在
  public static BusinessException duplicateTeacherAccount(String account) {
    return new BusinessException(
            ErrorCode.DUPLICATE_TEACHER_ACCOUNT,
            "教师账号 " + account + " 已存在"
    );
  }

  //学生身份证已存在
  public static BusinessException duplicateStudentIdCard(String idCard) {
    return new BusinessException(
            ErrorCode.DUPLICATE_STUDENT_ID_CARD,
            "学生身份证 " + idCard + " 已存在"
    );
  }

  public static BusinessException classFull(String className) {
    return new BusinessException(
            ErrorCode.CLASS_FULL,
            "班级 " + className + " 人数已满"
    );
  }
}
