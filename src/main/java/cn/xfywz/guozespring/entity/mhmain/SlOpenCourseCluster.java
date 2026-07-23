package cn.xfywz.guozespring.entity.mhmain;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpenCourseCluster {
  private long id;
  private String name;
  private String enName;
  private String code;
  @TableField(value = "schoolId")
  private long schoolId;
  @TableField(value = "collegeId")
  private long collegeId;
  @TableField(value = "cateBid")
  private long cateBid;
  @TableField(value = "cateMid")
  private long cateMid;
  private String cover;
  @TableField(value = "categoryId")
  private String categoryId;
  @TableField(value = "categoryItem")
  private String categoryItem;
  private long mark;
  private long weight;
  @TableField(value = "createId")
  private long createId;
  @TableField(value = "addTime")
  private java.sql.Timestamp addTime;
}
