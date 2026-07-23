package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.TableField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

@Data
public class YeeManage {
  @Min(value = 0, message = "ID不能小于0")
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
  private Timestamp thisTime;
  @TableField("lastTime")
  private Timestamp lastTime;
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
  @Min(value = 0, message = "学校ID不能小于0")
  @TableField("schoolId")
  private Integer schoolId;
  @TableField("super")
  private long sl_super;
  @TableField("collegeId")
  private long collegeId;
  private long general;
  @TableField("loginCode")
  private String loginCode;
  @NotNull
  @Min(0)
  private long recommend = 0L;
  private long active;
  private String colleges;
  @TableField("addTime")
  private Timestamp addTime;
  @NotNull
  @Min(0)
  @TableField("`force`")
  private long force = 0L;
  private String passport;
  @TableField("bindId")
  private long bindId;
  @TableField("addDate")
  private Date addDate;
}
