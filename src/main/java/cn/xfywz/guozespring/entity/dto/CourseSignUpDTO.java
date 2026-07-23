package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseSignUpDTO {

    //分页
    private Integer pageNum;
    private Integer pageSize;

    //必填
    private Integer schoolId;
    private Integer courseId;
    private Integer studentId;
    private Integer collegeId;
    private Integer classId;
}
