package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeChapter {
  private long id;
  private String name;
  private long courseId;
  private long sort;
  private long schoolId;

  private List<Object> children;
}
