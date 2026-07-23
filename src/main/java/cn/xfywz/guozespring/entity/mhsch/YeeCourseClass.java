package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourseClass {
  private long id;
  private String name;
  private long courseId;
  private long teacherId;
  private long schoolId;
  private long allow;
  private java.sql.Timestamp addTime;
  private long createId;
  private long change;
  private long calculate;
  private java.sql.Date addDate;
}
