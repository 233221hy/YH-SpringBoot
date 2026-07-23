package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 讨论得分
 * @TableName yee_discuss_score
 */
@TableName(value ="yee_discuss_score")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeDiscussScore {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 课程
     */
    private Integer courseId;

    /**
     * 讨论
     */
    private Integer discussId;

    /**
     * 用户Id
     */
    private Integer userId;

    /**
     * 得分
     */
    private BigDecimal score;

    /**
     * 班级
     */
    private Integer classId;

    /**
     * 主贴数量
     */
    private Integer postQty;

    /**
     * 回复数量
     */
    private Integer replyQty;

    /**
     * 点赞数量
     */
    private Integer likeQty;

    /**
     * 已打分
     */
    private Integer scored;

    /**
     * 排名
     */
    private Integer rank;

    /**
     * 用户类型
     */
    private Integer userType;

    /**
     * 总发帖数
     */
    private Integer allQty;

    /**
     * 学校Id
     */
    private Integer schoolId;
}