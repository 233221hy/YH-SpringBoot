package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatisticsResultVo {
    private String schoolName;      // 学校名称
    private Long studentCount;      // 学生人数
    private Long teacherCount;      // 老师人数
    private Long classCount;        // 行政班级数
    private Long courseCount;       // 建课数
    private Long courseSelectionCount; // 选课人次
    private Long activeCourseCount; // 开课数(状态正常)
    private Long courseStudentCount; // 选课人数
    private Long requiredCourseCount; // 必修课数
    private Long electiveCourseCount; // 选修课数
    private Long teachingClassCount; // 教学班级数
    private Long topicDiscussionCount; // 主题讨论数
    private Long commentReplyCount; // 评论回复数
    private Long videoDiscussionCount; // 视频讨论数
    private Long paperCount;        // 试卷数
    private Long questionCount;     // 试题数
    private Long workCount;         // 作业数
    private Long examCount;         // 考试数
}