package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpenCourseFiles {
  private long id;
  @TableField(value = "courseId")
  private long courseId;
  private String name;
  @TableField(value = "uploadPath")
  private String uploadPath;
  @TableField(value = "timeView")
  private long timeView;
  @TableField(value = "createUserId")
  private long createUserId;
  @TableField(value = "addTime")
  private java.sql.Timestamp addTime;
  @TableField(value = "fileName")
  private String fileName;
}
