package cn.xfywz.guozespring.entity.mhmain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlArea {
  private long id;
  private String name;
  private long code;
  private long pid;
  private long allow;
  private long sort;
}
