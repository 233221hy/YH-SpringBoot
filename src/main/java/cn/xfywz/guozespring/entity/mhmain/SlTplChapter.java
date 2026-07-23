package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlTplChapter {
  private long id;
  private String name;
  @TableField("courseId")
  private long courseId;
  private long sort;
  @TableField("schoolId")
  private long schoolId;
}
