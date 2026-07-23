package cn.xfywz.guozespring.entity.mhmain;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlRole {
  private long id;
  private String name;
  private long sort;
  private String nodes;
  @TableField(value = "schoolId")
  private long schoolId;
  private long fixed;
  private String admNodes;
  @TableField(value = "dataAuth")
  private Integer dataAuth;
}
