package cn.xfywz.guozespring.entity.mhmain;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlCategory {
  private long id;
  private String name;
  private long allow;
  @TableField(value = "schoolId")
  private long schoolId;
  private long pid;
  private String code;
  private long sort;
  private long bsort;
}
