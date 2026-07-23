package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlAuthNode {
  private long id;
  private long pid;
  private String name;
  private String controller;
  private String action;
  private String args;
  private long sort;
  @TableField(value = "mainBar")
  private long mainBar;
  private String app;
}
