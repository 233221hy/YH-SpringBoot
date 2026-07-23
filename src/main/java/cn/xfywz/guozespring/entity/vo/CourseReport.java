package cn.xfywz.guozespring.entity.vo;

import lombok.Data;

/**
 * 课程报表查询参数
 */
@Data
public class CourseReport {
    // 必填
    private Integer schoolId;
    private Integer courseId;

    // 可选过滤
    private Integer classId; // 过滤指定班级，null 或 <=0 表示全部
}
