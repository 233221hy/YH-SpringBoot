package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlLiveVideo {
  private long id;
  private String name;
  @TableField(value = "categoryId")
  private String categoryId;
  @TableField(value = "cateBid")
  private long cateBid;
  @TableField(value = "cateMid")
  private long cateMid;
  private String cover;
  @TableField(value = "startTime")
  private java.sql.Timestamp startTime;
  @TableField(value = "endTime")
  private java.sql.Timestamp endTime;
  private long allow;
  private long sort;
  private String publisher;
  @TableField(value = "`desc`")
  private String desc;
  private String pwd;
  private long mode;
  private String link;
}
