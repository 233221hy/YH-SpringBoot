package cn.xfywz.guozespring.entity.mhsch;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 试题题库实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YeeQuestion {

    private Integer id;

    /**
     * 题干
     */
    private String topic;

    /**
     * 试题类型
     */
    private Integer type;

    /**
     * 难度等级
     */
    private Integer level;

    /**
     * 默认分值
     */
    private Integer score;

    /**
     * 漏选分值 (JSON 格式)
     * 示例：[70,50,30,10,0]
     */
    private List<Integer> missScore;

    /**
     * 题目解析
     */
    private String analysis;

    /**
     * 父Id (用于组合题等)
     */
    private Integer pid;

    /**
     * 标识
     */
    private String title;

    /**
     * 排序ID / 顺序
     */
    private Integer oid;

    /**
     * 上传附件路径
     */
    private String upload;

    /**
     * 题目选项 (JSON 格式)
     * 示例：[{"key": "A", "value": "选项A内容"}, {"key": "B", "value": "选项B内容"}]
     */
    private List<Map<String, Object>> option;

    /**
     * 计分模式
     */
    private Integer scoreMode;

    /**
     * 学校Id
     */
    private Integer schoolId;

    /**
     * 学科分类 (JSON 数组，存储分类ID)
     * 示例：[1, 2, 3]
     */
    private List<Integer> categoryId;

    /**
     * 学科分类 (大类)
     */
    private Integer cateBid;

    /**
     * 学科分类 (中类)
     */
    private Integer cateMid;

    /**
     * 创建人
     */
    private Integer createId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime addTime;

    /**
     * 创建日期 (虚拟列，由 addTime 转换而来)
     * 注意：该字段通常不用于插入/更新，仅用于查询
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private java.sql.Date addDate;
    
    /**
     * 逻辑删除标志：0-未删除，1-已删除
     */
//    private Integer deleted = 0;

    // 注意：addDate 是数据库中的生成列（GENERATED ALWAYS AS），通常在 Java 中作为只读字段处理
    // 在插入或更新时，不应设置此字段
}