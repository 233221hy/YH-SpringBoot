package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlTplDiscuss {
  private long id;
  private String title;
  @TableField("teacherId")
  private long teacherId;
  @TableField("addTime")
  private java.sql.Timestamp addTime;
  private String content;
  private String images;
  private String files;
  @TableField("courseId")
  private long courseId;
  private long top;
  @TableField("changeTime")
  private long changeTime;
}
