package cn.xfywz.guozespring.entity.mhmain;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlDocument {
  private long id;
  private String title;
  @TableField(value = "`key`")
  private String key;
  private String content;
  private long allow;
  @TableField(value = "addTime")
  private java.sql.Timestamp addTime;
  @TableField(value = "createId")
  private long createId;
  @TableField(value = "docType")
  private long docType;
  private long sort;
}
