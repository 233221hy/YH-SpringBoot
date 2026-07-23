package cn.xfywz.guozespring.entity.mhsch;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

/**
 * 考试
 * @TableName yee_exam
 */
@TableName(value ="yee_exam")
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class YeeExam {
    /**
     * 
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 用户Id
     */
    private Integer userId;

    /**
     * 测验标题
     */
    private String title;

    /**
     * 题目数量
     */
    private Integer topicNumber;

    /**
     * 总分数
     */
    private Integer score;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private Date addTime;

    /**
     * 所在节点
     */
    private Integer nodeId;

    /**
     * 所在课程
     */
    private Integer courseId;

    /**
     * 限时
     */
    private Integer limitedTime;

    /**
     * 试题顺序
     */
    private Integer sequence;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 选择试卷
     */
    private Integer paperId;

    /**
     * 开始时间
     */
    private Integer startTime;

    /**
     * 结束时间
     */
    private Integer endTime;

    /**
     * 创建人
     */
    private Integer createUserId;

    /**
     * 选择班级
     */
    private Object classList;

    /**
     * 考试班级
     */
    private Integer isPrivate;

    /**
     * 老师类型
     */
    private Integer teacherType;

    /**
     * 是否启用
     */
    private Integer allow;

    /**
     * 次数
     */
    private Integer frequency;

    /**
     * 已收卷
     */
    private Integer hasCollect;

    /**
     * 学校Id
     */
    private Integer schoolId;

    /**
     * 显示解析
     */
    private Integer parsing;

    /**
     * 
     */
    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    private Date addDate;

    /**
     * 是否随机抽题
     */
    private Integer random;

    /**
     * 随机抽题设置
     */
    private Map<String, Integer> randData;

    /**
     * 实际抽题总数量
     */
    private Integer randNumber;
}