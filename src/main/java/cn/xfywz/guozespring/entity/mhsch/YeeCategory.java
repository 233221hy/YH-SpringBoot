package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCategory {
  private long id;
  private String name;
  private long allow;
  @TableField("schoolId")
  private long schoolId;
  private long pid;
  private String code;
}
