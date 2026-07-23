package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeAnnouncement {
  private long id;
  private String title;
  private String content;
  private java.sql.Timestamp addTime;
  private long courseId;
  private long userId;
  private long schoolId;
}
