package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeMuted {
    private long id;
    @TableField("userId")
    private long userId;
    @TableField("unlockTime")
    private long unlockTime;
    private String forum;
    @TableField("teacherId")
    private long teacherId;
    @TableField("addTime")
    private java.sql.Timestamp addTime;
    private String content;
    @TableField("schoolId")
    private long schoolId;
    @TableField("replyId")
    private long replyId;
}
