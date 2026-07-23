package cn.xfywz.guozespring.entity.mhmain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

@Data
public class SlManage {
  private long id;
  private String account;
  @JsonIgnore
  private String password;
  private String name;
  @TableField("errorCount")
  private long errorCount;
  @TableField("errorTime")
  private long errorTime;
  @TableField("thisTime")
  private java.sql.Timestamp thisTime;
  @TableField("lastTime")
  private java.sql.Timestamp lastTime;
  @TableField("thisIp")
  private String thisIp;
  @TableField("lastIp")
  private String lastIp;
  @TableField("isLock")
  private long isLock;
  @TableField("email")
  private String email;
  private long role;
  private String avatar;
  private String mobile;
  private String gender;
  @TableField("wechat")
  private String weChat;
  private String intro;
  @TableField("schoolId")
  private long schoolId;
  @TableField("super")
  private long sl_super;
  @TableField("collegeId")
  private long collegeId;
  private long general;
  @TableField("loginCode")
  private String loginCode;
  private long recommend;
  private long active;
  private String colleges;
  @TableField("addTime")
  private java.sql.Timestamp addTime;
  @TableField("`force`")
  private long force;
  private String passport;
  @TableField("bindId")
  private long bindId;



}
