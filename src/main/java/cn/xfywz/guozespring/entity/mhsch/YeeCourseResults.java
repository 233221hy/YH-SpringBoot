package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourseResults {
  private long id;
  private long courseId;
  private long userId;
  private double score;
  private double videoScore;
  private double examScore;
  private double workScore;
  private double discussScore;
  private double extraScore;
  private double reportScore;
  private String stuName;
  private String stuNumber;
  private long classId;
  private long ranking;
  private double videoResult;
  private double examResult;
  private double workResult;
  private double discussResult;
  private double reportResult;
  private long schoolId;
  private java.sql.Date calcDate;
}
