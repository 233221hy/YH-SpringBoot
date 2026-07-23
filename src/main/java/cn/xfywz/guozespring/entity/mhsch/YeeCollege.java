package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCollege {
  private long id;
  private String name;
  private int allow;
  @TableField("schoolId")
  private long schoolId;
}
