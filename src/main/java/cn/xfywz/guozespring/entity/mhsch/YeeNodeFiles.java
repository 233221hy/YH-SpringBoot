package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeNodeFiles {
  private long id;
  private long nodeId;
  private long courseId;
  private String name;
  private String uploadPath;
  private long timeView;
  private long createUserId;
  private java.sql.Timestamp addTime;
  private String fileName;
  private long schoolId;
}
