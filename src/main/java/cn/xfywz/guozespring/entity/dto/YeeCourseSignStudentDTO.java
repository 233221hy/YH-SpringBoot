package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourseSignStudentDTO {
    //分页
    private Integer pageNum;
    private Integer pageSize;

    //必填
    private Integer schoolId;
    private Integer courseId;

    //可选
    private String keyword;
    private String idCard;
    private Integer collegeId;
    private Integer classId;
    private String gender;
    private String states;

}
