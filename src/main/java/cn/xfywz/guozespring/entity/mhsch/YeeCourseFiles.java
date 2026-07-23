package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourseFiles {
  private long id;
  private long courseId;
  private String name;
  private String uploadPath;
  private long timeView;
  private long createUserId;
  private Timestamp addTime;
  private String fileName;
  private long schoolId;
}
