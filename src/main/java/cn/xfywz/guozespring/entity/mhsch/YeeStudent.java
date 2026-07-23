package cn.xfywz.guozespring.entity.mhsch;


import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.TableField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.sql.Date;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YeeStudent {

  private long id;

  @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "学号只能包含字母和数字")
  @NotBlank(message = "学号不能为空")
  @Size(min = 1, max = 20, message = "学号长度必须在1-20位之间")
  private String number;

  private String name;

  @TableField("idCard")
  private String idCard;

  private String gender;

  @TableField("entryYear")
  private long entryYear;

  private String mobile;

  @TableField("weChat")
  private String weChat;

  private String email;

  private String intro;

  @TableField("classId")
  private long classId;

  @TableField("collegeId")
  private long collegeId;

  private String avatar;

  private String password;

  private long point;

  private String area;

  private long province;

  private long city;

  private long region;

  private String address;

  @TableField("addTime")
  private Timestamp addTime;

  @TableField("schoolId")
  private long schoolId;

  @TableField("tipPass")
  private long tipPass;

  private String signature;

  @TableField("studyDuration")
  private long studyDuration;

  @TableField("discJoin")
  private long discJoin;

  @TableField("discReply")
  private long discReply;

  @TableField("completeCourse")
  private long completeCourse;

  @TableField("studyCourse")
  private long studyCourse;

  @TableField("circleCount")
  private long circleCount;

  @TableField("errorCount")
  private long errorCount;

  @TableField("errorTime")
  private long errorTime;

  private String passport;
  @Getter
  @TableField("addDate")
  private Date addDate;
  /**
   * 班级名称（非数据库字段）
   */
  @ExcelProperty(value = "所属班级")
  @TableField(exist = false)
  private String className;

  /**
   * 学院名称（非数据库字段）
   */
  @ExcelProperty(value = "所属学院")
  @TableField(exist = false)
  private String collegeName;

}
