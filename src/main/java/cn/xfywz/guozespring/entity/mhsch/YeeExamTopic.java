package cn.xfywz.guozespring.entity.mhsch;


import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeExamTopic {

    private long id;
    private String topic;
    private long type;
    private long level;
    private long score;
    @TableField("missScore")
    /**
     * 漏选得分规则（JSON格式，如：[70,50,30,20,10]）
     * 用于多选题评分策略
     */
    private List<Integer> missScore;
    private String option1;
    private String option2;
    private String option3;
    private String analysis;
    private long pid;
    @TableField("examId")
    private long examId;
    private String title;
    private long oid;
    private long number;
    private List<Map<String, Object>> option;
    private String upload;
    @TableField("scoreMode")
    private long scoreMode;
    @TableField("schoolId")
    private long schoolId;
    @TableField("categoryId")
    /**
     * 学科分类（JSON数组，如：[1,2,3]）
     */
    private List<Integer> categoryId;
    @TableField("cateBid")
    private long cateBid;
    @TableField("cateMid")
    private long cateMid;

}
