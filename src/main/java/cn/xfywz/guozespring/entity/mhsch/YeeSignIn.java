package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeSignIn {
  private long id;
  private long courseId;
  private String name;
  private long teacherId;
  private String classList;
  private Long allow;
  private long finish;
  private long schoolId;
  private Timestamp signInTime;
  private Timestamp endTime;
  private long lateTime;
}
