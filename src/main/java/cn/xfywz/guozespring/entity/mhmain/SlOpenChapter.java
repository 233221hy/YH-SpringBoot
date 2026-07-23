package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpenChapter {
  private long id;
  private String name;
  @TableField(value = "courseId")
  private long courseId;
  @TableField(value = "sort")
  private long sort;
  @TableField(value = "schoolId")
  private long schoolId;
}
