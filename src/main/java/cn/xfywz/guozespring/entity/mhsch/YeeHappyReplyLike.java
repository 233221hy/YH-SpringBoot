package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeHappyReplyLike {
    private long id;
    @TableField("replyId")
    private long replyId;
    @TableField("userId")
    private long userId;
    @TableField("schoolId")
    private long schoolId;
}
