package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpinion {
  private long id;
  @TableField(value = "userId")
  private long userId;
  private long type;
  private String content;
  @TableField(value = "addTime")
  private java.sql.Timestamp addTime;
  private String mobile;
  private String email;
  @TableField(value = "schoolId")
  private long schoolId;
  private String platform;
  private String data;
  private String files;
  @TableField(value = "openUid")
  private String openUid;
}
