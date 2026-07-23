package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeSignInRecord {
  private long id;
  private long signId;
  private long userId;
  private Timestamp signTime;
  private long courseId;
  private long classId;
  private long schoolId;
  private long state;
}
