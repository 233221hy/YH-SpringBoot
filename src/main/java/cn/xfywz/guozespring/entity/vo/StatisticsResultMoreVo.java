package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResultMoreVo {
    private String schoolName;           // 学校名称
    private Long studentCount;           // 学生人数
    private Long teacherCount;           // 教师人数
    private Long classCount;             // 行政班级数
    private Long courseCount;            // 建课数
    private Long courseSelectionCount;   // 选课人次
    private Long paperCount;             // 试卷数
    private Long questionCount;          // 试题数
    private Long workCount;              // 作业数
    private Long workCountRecord;        // 作业答题数
    private Long examCount;              // 考试数
    private Long examCountRecord;        // 考试答题数
    private Long topicDiscussionCount;   // 主题讨论数
    private Long videoDiscussionCount;   // 视频回帖数
    private Long happyCircleCount;       // 乐学圈


    private Long activeCourseCount;      // 开课数
    private Long notActiveCourseCount;   // 开课数

    private Long requiredCourseCount;    // 必修课数
    private Long electiveCourseCount;    // 选修课数

    private Long allowPaperCount;        // 试卷数 已启用
    private Long notAllowPaperCount;     // 试卷数 未启用

    private Long allowWorkCount;         // 作业数 已启用
    private Long notAllowWorkCount;      // 作业数 未启用

    private Long allowExamCount;         // 考试数 已启用
    private Long notAllowExamCount;      // 考试数 未启用



}