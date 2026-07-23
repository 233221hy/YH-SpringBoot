package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeNotice {
  private long id;
  private long courseId;
  private long type;
  private List<Integer> classIds;;
  private String userNumber;
  private String title;
  private String summary;
  private String content;
  private long userId;
  private java.sql.Timestamp addTime;
  private long isPush;
  private java.sql.Timestamp pushTime;
  private long sysPush;
  private long schoolId;
}
