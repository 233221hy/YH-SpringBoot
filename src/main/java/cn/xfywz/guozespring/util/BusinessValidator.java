package cn.xfywz.guozespring.util;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.exception.BusinessException;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import java.util.Collection;

import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

/**
 * 业务验证工具类
 */
@Component
@RequiredArgsConstructor
public class BusinessValidator {

  private final SlSchoolMapper slSchoolMapper;

  // ================ 业务验证 ================

  /**
   * 验证学校有效性
   */
  public SlSchool validateSchool(int schoolId) {
    // 1. 验证学校
    SlSchool slSchool = slSchoolMapper.selectById(schoolId);
    // 2. 验证学校有效性
    if (slSchool == null) {
      throw BusinessException.schoolNotFound(schoolId);
    }
    // 3. 验证学校状态
    if (slSchool.getAllow() == 0) {
      throw BusinessException.schoolNotApproved(schoolId);
    }
    return slSchool;
  }

  /**
   * 验证学号唯一性
   */
  public void validateStuNumberUnique(DatabaseUtil databaseUtil, int schoolId, String number, Long excludeId) {
    if (StringUtils.isEmpty(number)) {
      return;
    }

    QueryBuilder queryBuilder = databaseUtil.query(schoolId)
        .sql("SELECT id FROM yee_student")
        .eq("schoolId", schoolId)
        .eq("number", number);

    if (excludeId != null) {
      queryBuilder.where("id != ?", excludeId);
    }

    boolean exists = queryBuilder.exists();
    if (exists) {
      throw BusinessException.duplicateStudentNumber(number);
    }
  }

  /**
   * 验证学生身份证唯一性
   */
  public void validateStuIdCardUnique(DatabaseUtil databaseUtil, int schoolId, String idCard, Long excludeId) {
    if (StringUtils.isEmpty(idCard)) {
      return;
    }
    QueryBuilder queryBuilder = databaseUtil.query(schoolId)
            .sql("SELECT id FROM yee_student")
            .eq("schoolId", schoolId)
            .eq("idCard", idCard);

    if (excludeId != null) {
      queryBuilder.where("id != ?", excludeId);
    }
    boolean exists = queryBuilder.exists();
    if (exists) {
      throw BusinessException.duplicateStudentIdCard(idCard);
    }

  }

  /**
   * 验证教师账号唯一性
   */
  public void validateTeacherAccountUnique(DatabaseUtil databaseUtil, int schoolId, String account, Long excludeId) {
    if (StringUtils.isEmpty(account)) {
      return;
    }

    QueryBuilder queryBuilder = databaseUtil.query(schoolId)
            .sql("SELECT id FROM yee_manage")
            .eq("schoolId", schoolId)
            .eq("account", account);

    if (excludeId != null) {
      queryBuilder.where("id != ?", excludeId);
    }

    boolean exists = queryBuilder.exists();
    if (exists) {
      throw BusinessException.duplicateTeacherAccount(account);
    }

  }


  // ================ 文件验证 ================

  /**
   * 验证上传文件
   */
  public void validateFile(MultipartFile file) {
    validateFile(file, null, null);
  }

  /**
   * 验证上传文件（带类型和大小限制）
   */
  public void validateFile(MultipartFile file, String[] allowedExtensions, Long maxSize) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException("文件不能为空");
    }

    // 验证文件大小
    if (maxSize != null && file.getSize() > maxSize) {
      throw new BusinessException(
          String.format("文件大小不能超过 %.2fMB", maxSize / (1024.0 * 1024.0))
      );
    }

    // 验证文件类型
    if (allowedExtensions != null && allowedExtensions.length > 0) {
      String originalFilename = file.getOriginalFilename();
      if (originalFilename == null) {
        throw new BusinessException("文件名不能为空");
      }

      String fileExtension = getFileExtension(originalFilename).toLowerCase();
      if (!"xlsx".equals(fileExtension) && !"xls".equals(fileExtension)) {
        throw new BusinessException("仅支持.xlsx或.xls格式的Excel文件");
      }
    }
  }

  // ================ 参数验证 ================

  /**
   * 验证ID有效性
   */
  public void validateId(Long id, String fieldName) {
    Objects.requireNonNull(id, fieldName + "不能为空");
    if (id <= 0) {
      throw new BusinessException(fieldName + "必须大于0");
    }
  }

  /**
   * 验证字符串非空
   */
  public void validateNotBlank(String value, String fieldName) {
    if (value == null || value.trim().isEmpty()) {
      throw new BusinessException(fieldName + "不能为空");
    }
  }

  /**
   * 验证列表非空
   */
  public <T> void validateNotEmpty(Collection<T> collection, String fieldName) {
    if (collection == null || collection.isEmpty()) {
      throw new BusinessException(fieldName + "不能为空");
    }
  }

  // ================ 辅助方法 ================

  /**
   * 获取文件扩展名
   */
  private String getFileExtension(String filename) {
    int lastDotIndex = filename.lastIndexOf('.');
    if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
      return "";
    }
    return filename.substring(lastDotIndex + 1);
  }
}
