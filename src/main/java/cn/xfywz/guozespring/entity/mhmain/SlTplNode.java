package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlTplNode {
  private long id;
  private String name;
  private String type;
  @TableField("tabVideo")
  private long tabVideo;
  @TableField("tabFile")
  private long tabFile;
  @TableField("tabVote")
  private long tabVote;
  @TableField("tabWork")
  private long tabWork;
  @TableField("tabExam")
  private long tabExam;
  @TableField("chapterId")
  private long chapterId;
  @TableField("courseId")
  private long courseId;
  @TableField("videoFile")
  private String videoFile;
  @TableField("videoDuration")
  private long videoDuration;
  @TableField("localFile")
  private String localFile;
  @TableField("votingPath")
  private String votingPath;
  @TableField("sort")
  private long sort;
  @TableField("videoMode")
  private long videoMode;
  @TableField("schoolId")
  private long schoolId;
}
