package cn.xfywz.guozespring.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeExamExportDTO {
    private Integer schoolId;
    private Integer courseId;
    private Integer examId;

    //关键字（学号or姓名）
    private String keyword;
    //课程班级
    private Integer classId;
    //交卷状态
    private Integer submitted;
    //批阅状态
    private Integer reviewState;
    //打分状态
    private Integer scored;
}
