package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求参数 ：用于视频播放排行榜查询
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoRank {
    // 基础必填
    private Integer schoolId;   // 学校ID
    private long courseId;      // 课程ID

    // 可选筛选
    private Long classId;       // 班级ID（全部可不传或传0）
    private Long startTime;     // 开始时间（毫秒时间戳，可空）
    private Long endTime;       // 结束时间（毫秒时间戳，可空）
    private Integer terminalType; // 端类型：0全部 1PC 2移动（默认0）


    // 分页
    private Integer pageNum;    // 页码（默认1）
    private Integer pageSize;   // 页大小（默认10）
}
