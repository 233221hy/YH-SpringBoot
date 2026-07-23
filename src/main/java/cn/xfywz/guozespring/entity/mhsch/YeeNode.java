package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeNode {
  private long id;
  private String name; //节名称
  private String type; //节类型
  private long chapterId; //章节id
  private long courseId; //课程id
  private String videoFile; //视频文件
  private long videoDuration; //视频时长
  private String votingPath; //投票路径
  private long tabVideo; //视频
  private long tabFile; //文件
  private long tabVote; //投票
  private long tabWork; //作业
  private long tabExam; //考试
  private long sort; //排序
  private long videoMode; //视频模式
  private String localFile; //本地文件
  private long schoolId; //学校id
  private long lock; //时间锁
  private long unlockTime; //解锁时间
}
