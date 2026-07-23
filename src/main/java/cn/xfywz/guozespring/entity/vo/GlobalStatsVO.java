package cn.xfywz.guozespring.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

/**
 * 全局统计数据 VO
 */
@Data
@NoArgsConstructor
public class GlobalStatsVO {
    private Long teachingClassCount;   // 教学班级数 git
    private Long questionCount;        // 试题数量
    private Long courseCount;          // 课程总数
    private Long teacherCount;         // 教师人数
    private Long studentCount;         // 学生人数
    private Long assessmentCount;      // 课程测评总数(考试+作业)
    private Long examPersonCount;      // 考试人数汇总
    private Map<String, Long> schoolCourseSelectCountMap; // 各学校选课人数
}