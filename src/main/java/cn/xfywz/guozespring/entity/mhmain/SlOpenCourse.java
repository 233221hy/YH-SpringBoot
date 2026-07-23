package cn.xfywz.guozespring.entity.mhmain;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlOpenCourse {
  @TableId(value = "id", type = IdType.AUTO)
  private long id;
  private String name;
  private String code;
  @TableField(value = "categoryId")
  private String categoryId;
  @TableField(value = "cateBid")
  private long cateBid;
  @TableField(value = "cateMid")
  private long cateMid;
  private String cover;
  private String intro;
  private String content;
  private long allow;
  @TableField(value = "addTime")
  private java.sql.Timestamp addTime;
  private long mode;
  private long week;
  private long times;
  @TableField(value = "startTime")
  private java.sql.Timestamp startTime;
  @TableField(value = "categoryItem")
  private String categoryItem;
  @TableField(value = "clusterId")
  private long clusterId;
  @TableField(value = "endTime")
  private java.sql.Timestamp endTime;
  @TableField(value = "signStartTime")
  private java.sql.Timestamp signStartTime;
  @TableField(value = "signEndTime")
  private java.sql.Timestamp signEndTime;
  private long mark;
  private long weight;
  private String lecturer;
  private String organization;
  @TableField(value = "teacherIntro")
  private String teacherIntro;
  @TableField(value = "viewRate")
  private long viewRate;
  @TableField(value = "stuCount")
  private long stuCount;
  @TableField(value = "periodName")
  private String periodName;
  @TableField(value = "createId")
  private long createId;
  @TableField(value = "schoolId")
  private long schoolId;
  private long open;
  @TableField(value = "schoolAllow")
  private long schoolAllow;
  private long free;
  private double price;
  @TableField(value = "dbLock")
  private long dbLock;
  @TableField(value = "click")
  private long click;
}
