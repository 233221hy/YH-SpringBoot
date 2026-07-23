package cn.xfywz.guozespring.entity.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StudyDuration {
    // 汇总（分钟）
    private long today;
    private long last7Days;
    private long last30Days;
    private long total;

    // 课程对比（课程名称 -> 学习时长（分钟））
    private List<Map<String, Object>> courseCompare; // 每项包含 courseId, courseName, minutes
}
