package cn.xfywz.guozespring.entity.mhsch;

/**
 * @Author: ChengLin
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 试卷题目实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeePaperTopic {

    /**
     * 主键ID
     */
    private Integer id;

    /**
     * 原题ID（关联原始题库）
     */
    private Integer oid;

    /**
     * 题干（支持HTML或富文本）
     */
    private String topic;

    /**
     * 试题类型
     * 1: 单选题
     * 2: 多选题
     * 3: 判断题
     * 4: 填空题
     * 5: 简答题等
     */
    private Integer type;

    /**
     * 难度等级（1-5）
     */
    private Integer level;

    /**
     * 默认分值
     */
    private Integer score;

    /**
     * 漏选得分规则（JSON格式，如：[70,50,30,20,10]）
     * 用于多选题评分策略
     */
    private List<Integer> missScore;

    /**
     * 单选题选项（JSON）
     * 示例: [{"key":"A","value":"选项A"},{"key":"B","value":"选项B"}]
     */
    private List<Map<String, String>> option1;

    /**
     * 多选题选项（JSON）
     * 结构同 option1
     */
    private List<Map<String, String>> option2;

    /**
     * 判断题选项（JSON）
     * 示例: [{"key":"true","value":"正确"},{"key":"false","value":"错误"}]
     */
    private List<Map<String, String>> option3;

    /**
     * 题目解析（支持富文本）
     */
    private String analysis;

    /**
     * 父题ID（用于组合题/大题下的子题）
     */
    private Integer pid;

    /**
     * 所属试卷ID
     */
    private Integer paperId;

    /**
     * 标识（如：T1, Q2 等用于排序或标记）
     */
    private String title;

    /**
     * 上传附件（文件路径或URL）
     */
    private String upload;

    /**
     * 题目选项 (JSON 格式)
     * 示例：[{"key": "A", "value": "选项A内容"}, {"key": "B", "value": "选项B内容"}]
     */
    private List<Map<String, Object>> option;

    /**
     * 计分模式
     * 1: 全对得分
     * 2: 按比例得分
     * 3: 漏选部分得分（配合 missScore）
     */
    private Integer scoreMode;

    /**
     * 学校ID
     */
    private Integer schoolId;

    /**
     * 学科分类（JSON数组，如：[1,2,3]）
     */
    private List<Integer> categoryId;

    /**
     * 大类学科ID（如：1-语文，2-数学）
     */
    private Integer cateBid;

    /**
     * 中类学科ID（如：数学 -> 代数、几何）
     */
    private Integer cateMid;

    /**
     * 题目在试卷中的序号
     */
    private Integer number;

}
