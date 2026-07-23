package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlTplNodeFiles {
  private long id;
  @TableField("nodeId")
  private long nodeId;
  @TableField("courseId")
  private long courseId;
  private String name;
  @TableField("uploadPath")
  private String uploadPath;
  @TableField("timeView")
  private long timeView;
  @TableField("createUserId")
  private long createUserId;
  @TableField("addTime")
  private java.sql.Timestamp addTime;
  @TableField("fileName")
  private String fileName;
  @TableField("schoolId")
  private long schoolId;
}
