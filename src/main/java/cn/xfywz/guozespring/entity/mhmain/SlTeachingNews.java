package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlTeachingNews {
  private long id;
  private String title;
  private String cover;
  private String source;
  private String intro;
  private String content;
  private long allow;
  private long sort;
  @TableField(value = "addTime")
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private Date addTime;
  @TableField(value = "createId")
  private long createId;
  @TableField(value = "schoolId")
  private long schoolId;
  private long open;
  @TableField(value = "schoolAllow")
  private long schoolAllow;
}
