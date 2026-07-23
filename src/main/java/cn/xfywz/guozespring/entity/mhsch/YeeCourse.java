package cn.xfywz.guozespring.entity.mhsch;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.sql.Date;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourse {
  private long id;
  private String name;
  private long mode;
  private long collegeId;
  private String categoryId;
  private String lecturers;

  @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private Date startDate;

  @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private Date endDate;

  private String cover;
  private String content;
  private double credit;
  private long allow;
  private String intro;
  private String teacherIntro;
  private String code;
  private long stuCount;
  private String proclamation;
  private long clusterId;
  private String periodName;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Timestamp addTime;

  private long createId;
  private long schoolId;
  private long cateBid;
  private long cateMid;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Timestamp signStartTime;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
  @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Timestamp signEndTime;
  
  private long signScope;
  private String signClass;
  private String lecturerName;
  private long offline;
  private long mission;
  private long signLimit;
  private long lineLock;
  
  @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
  @DateTimeFormat(pattern = "yyyy-MM-dd")
  private Date addDate;
  
  private long tplId;
  private long templateId;
  private long isPractice;

}
