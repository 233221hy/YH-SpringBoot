package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpenNode {
  private long id;
  private String name;
  private String type;
  @TableField(value = "tabVideo")
  private long tabVideo;
  @TableField(value = "tabFile")
  private long tabFile;
  @TableField(value = "tabVote")
  private long tabVote;
  @TableField(value = "tabWork")
  private long tabWork;
  @TableField(value = "tabExam")
  private long tabExam;
  @TableField(value = "chapterId")
  private long chapterId;
  @TableField(value = "courseId")
  private long courseId;
  @TableField(value = "videoFile")
  private String videoFile;
  @TableField(value = "videoDuration")
  private long videoDuration;
  @TableField(value = "localFile")
  private String localFile;
  @TableField(value = "votingPath")
  private String votingPath;
  private long sort;
  @TableField(value = "videoMode")
  private long videoMode;
  @TableField(value = "schoolId")
  private long schoolId;
}
