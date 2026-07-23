package cn.xfywz.guozespring.entity.vo;

import lombok.Data;

/**
 * 视频报表查询参数 VO
 * - 在某课程内，可按班级筛选；可选时间范围过滤发帖数据
 */
@Data
public class VideoReport {
    // 必填
    private Integer schoolId;   // 学校ID
    private long courseId;      // 课程ID

    // 可选筛选
    private Long classId;       // 班级ID（不传或<=0 表示全部）
    private Boolean nonZeroOnly;    // 是否仅显示非0数据（true表示只显示有学习记录的学生）

    private Long startTime;     // 开始时间（毫秒时间戳，可空，用于发帖统计时间过滤）
    private Long endTime;       // 结束时间（毫秒时间戳，可空，用于发帖统计时间过滤）



}