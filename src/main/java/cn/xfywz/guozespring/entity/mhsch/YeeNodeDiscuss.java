package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 章节讨论
 * @TableName yee_node_discuss
 */
@TableName(value ="yee_node_discuss")
@Data
public class YeeNodeDiscuss {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 创建时间
     */
    private Date addTime;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 上传图片
     */
    private Object images;

    /**
     * 节点Id
     */
    private Integer nodeId;

    /**
     * 所属课程iD
     */
    private Integer courseId;

    /**
     * 用户Id
     */
    private Integer userId;

    /**
     * 第一层回复Id
     */
    private Integer replyId;

    /**
     * 被回复用户Id
     */
    private Integer reUserId;

    /**
     * 上传附件
     */
    private Object files;

    /**
     * 删除
     */
    private Integer isDelete;

    /**
     * 平台
     */
    private String platform;

    /**
     * 学校Id
     */
    private Integer schoolId;

    /**
     * 
     */
    private Date addDate;



    private String userName;
    private String avatar;
}