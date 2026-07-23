package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpenNodeFiles {
  @TableField(value = "nodeId")
  private long nodeId;
  @TableField(value = "id")
  private long id;
  @TableField(value = "courseId")
  private long courseId;
  @TableField(value = "name")
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
  @TableField(value = "schoolId")
  private long schoolId;
}
