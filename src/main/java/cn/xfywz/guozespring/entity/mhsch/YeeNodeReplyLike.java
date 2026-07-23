package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 章节评论点赞
 * @TableName yee_node_reply_like
 */
@TableName(value ="yee_node_reply_like")
@Data
public class YeeNodeReplyLike {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 回复Id
     */
    private Integer replyId;

    /**
     * 用户Id
     */
    private Integer userId;

    /**
     * 学校Id
     */
    private Integer schoolId;
}