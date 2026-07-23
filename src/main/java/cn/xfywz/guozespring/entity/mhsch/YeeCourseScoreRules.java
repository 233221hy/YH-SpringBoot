package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourseScoreRules implements Serializable {
    private static final long serialVersionUID = 1L;
  private long id;
  private long courseId;
  private long classId;
  private long useVideo;
  private long videoRatio;
  private long useDiscuss;
  private long discussRatio;
  private String discussItems;
  private long useWork;
  private long workRatio;
  private String workItems;
  private long useExam;
  private long examRatio;
  private String examItems;
  private long useExtra;
  private long extraRatio;
  private long useReport;
  private long reportRatio;
  private Timestamp addTime;
  private long calcNumber;
  private Timestamp updateTime;
  private String videoItems;
  private long realTime;
  private long videoMode;
  private String description;
  private Integer schoolId;
  private long announce;
}
