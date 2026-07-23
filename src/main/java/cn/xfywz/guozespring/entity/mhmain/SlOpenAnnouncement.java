package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpenAnnouncement {
  private long id;
  private String title;
  private String content;
  @TableField(value = "addTime")
  private java.sql.Timestamp addTime;
  @TableField(value = "courseId")
  private long courseId;
  @TableField(value = "userId")
  private long userId;
  @TableField(value = "schoolId")
  private long schoolId;
}
