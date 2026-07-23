package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeDefaultScoreRule {
  private long id;
  private long courseId;
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
  private java.sql.Timestamp addTime;
  private long calcNumber;
  private String name;
  private String videoItems;
  private java.sql.Timestamp updateTime;
  private long videoMode;
  private String description;
  private long schoolId;
}
