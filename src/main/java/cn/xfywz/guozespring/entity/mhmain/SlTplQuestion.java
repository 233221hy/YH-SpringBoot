package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlTplQuestion {
  private long id;
  @TableField("title")
  private String title;
  @TableField("topic")
  private String topic;
  @TableField("type")
  private long type;
  @TableField("level")
  private long level;
  @TableField("score")
  private long score;
  @TableField("missScore")
  private String missScore;
  @TableField("analysis")
  private String analysis;
  private long pid;
  @TableField("upload")
  private String upload;
  @TableField("`option`")
  private String option;
  @TableField("scoreMode")
  private long scoreMode;
  @TableField("categoryId")
  private String categoryId;
  @TableField("cateBid")
  private long cateBid;
  @TableField("cateMid")
  private long cateMid;
  @TableField("addTime")
  private java.sql.Date addTime;
}
