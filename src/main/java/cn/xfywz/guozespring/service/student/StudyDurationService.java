package cn.xfywz.guozespring.service.student;

import cn.xfywz.guozespring.util.Result;

public interface StudyDurationService {
    // 学习时长汇总（今日/7天/30天/总计，单位：分钟）
    Result stats(int schoolId, long studentId) throws Exception;

    // 按课程分组统计学习时长对比（单位：分钟），可选天数范围
    Result courseCompare(int schoolId, long studentId, Integer days) throws Exception;

    // 总览：返回包含汇总和课程对比的数据结构
    Result overview(int schoolId, long studentId, Integer days) throws Exception;
}
