package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeHappyCircle {
    private long id;
    @TableField("addTime")
    private java.sql.Timestamp addTime;
    private String content;
    private String images;
    private String files;
    @TableField("userId")
    private long userId;
    @TableField("replyId")
    private long replyId;
    @TableField("reUserId")
    private long reUserId;
    @TableField("isDelete")
    private Integer isDelete;
    @TableField("schoolId")
    private long schoolId;
    @TableField("addDate")
    private java.sql.Date addDate;

    // 用户名和头像
    private String userName;
    private String userAvatar;

}
