package cn.xfywz.guozespring.entity.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("yee_discuss_reply")
public class DiscussReplyVo {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer discussId;
    private Integer userId;
    private String content;
    private LocalDateTime addTime;
    private String images;
    private String files;
    private Integer pid;
    private Integer replyId;
    private Integer reUserId;
    private Integer classId;
    private String platform;
    private Integer schoolId;

    // 树形结构字段
    private List<DiscussReplyVo> children = new ArrayList<>();
}
