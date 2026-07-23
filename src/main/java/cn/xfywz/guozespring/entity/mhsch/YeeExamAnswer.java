package cn.xfywz.guozespring.entity.mhsch;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 作业答题记录实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeeExamAnswer {

    /**
     * 主键ID
     */
    private Integer id;


    /**
     * 记录ID（关联 yee_work_record.id）
     */
    private Integer recordId;

    /**
     * 作业ID
     */
    private Integer examId;

    /**
     * 题目ID
     */
    private Integer topicId;

    /**
     * 是否已答题（0:未答, 1:已答）
     */
    private Integer answered;

    /**
     * 得分
     */
    private BigDecimal score;

    /**
     * 答案内容（支持富文本/JSON格式）
     */
    private String answer;

    /**
     * 上传图片（JSON数组存储路径）
     */
    private String images;

    /**
     * 上传文件（JSON数组存储路径）
     */
    private String files;

    /**
     * 是否已批阅（支持多种状态，如 '0', '1', 'teacher', 'auto' 等）
     */
    private String marked;

    /**
     * 老师评语
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String remark;

    /**
     * 答题结果命中状态
     * 0: 未处理
     * 1: 全部正确
     * 2: 部分正确
     * 3: 全部错误
     */
    private Integer hit;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 课程ID
     */
    private Integer courseId;


    /**
     * 学校ID（用于分库分表）
     */
    private Integer schoolId;
}