package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 学生查询自己课程成绩
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseResultsQueryDTO {
    private Integer schoolId;
    private long courseId;
    private long classId;
    private long studentId;

}
