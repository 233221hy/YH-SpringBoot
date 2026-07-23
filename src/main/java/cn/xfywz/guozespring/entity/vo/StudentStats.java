package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 学生统计数据实体类
 * 用于封装学生在某个学校中的各项行为统计数据
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentStats implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;           // 学生ID
    private String number;       // 学号
    private String name;       // 学生姓名
    private String signature;       // 个性签名
    private Long courseCount;  // 我的课程数量
    private Long evaluationCount; // 我的互评数量
    private Long circleCount;  // 乐学圈发帖数量
    private Long discussionCount; // 讨论主题回复数量
    private Integer totalStudyDuration; // 总学习时长

}