package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourseStudent {
  private long id;
  private long classId;
  private long courseId;
  private long studentId;
  private long videoLearned;//已学视频数量
  private long videoCount;//需学视频数量
  private long lastNodeId;//上次学习节点
  private long workLearned;//已学作业数量
  private long workCount;//作业数量
  private long examLearned;//已学考试数量
  private long examCount;//考试数量
  private long discussJoin;//参与讨论数
  private long discussCount;//讨论数量
  private long schoolId;
  private long studyTime;//总学习时长
  private long change;//有记录更新
  private long calculate;//可以计算成绩
  private java.sql.Timestamp addTime;
  private java.sql.Date addDate;
}
