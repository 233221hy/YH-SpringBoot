package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlSettings {
  private long id;
  private String webname;
  @TableField(value = "switch")
  private long Switch;
  @TableField(value = "closeInfo")
  private String closeInfo;
  @TableField(value = "headCode")
  private String headCode;
  @TableField(value = "footCode")
  private String footCode;
  private String icp;
  private String license;
  @TableField(value = "wxIcon")
  private String wxIcon;
  private String contact;
  private String copyright;
}
