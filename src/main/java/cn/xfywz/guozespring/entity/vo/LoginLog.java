package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginLog {
    // 分页
    private Integer pageNum;     // 页码，从1开始
    private Integer pageSize;    // 每页数量

    // 必填
    private long schoolId;       // 学校ID
    private long courseId;       // 课程ID

    // 可选过滤
    private Long classId;        // 课程班级ID（可选）
    private String keyword;      // 关键字（学生：姓名/学号/班级名；老师：姓名/账号/班级名）
    private Long startTime;      // 开始时间（毫秒）
    private Long endTime;        // 结束时间（毫秒）

}
