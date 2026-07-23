package cn.xfywz.guozespring.entity.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ColumnWidth(25) // 默认列宽
public class CourseStudentEnrollmentExportDto {

    @ExcelProperty(value = "课程名称", index = 0)
    private String courseName;

    @ExcelProperty(value = "课程模式", index = 1)
    private String courseMode;

    @ExcelProperty(value = "课程代码", index = 2)
    private String courseCode;

    @ExcelProperty(value = "主讲教师", index = 3)
    private String lecturerName;

    @ExcelProperty(value = "开课时间", index = 4)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDateTime startDate;

    @ExcelProperty(value = "结束时间", index = 5)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDateTime endDate;

    @ExcelProperty(value = "选课时间", index = 6)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime enrollmentTime;

    @ExcelProperty(value = "学号", index = 7)
    private String studentId;

    @ExcelProperty(value = "学生姓名", index = 8)
    private String studentName;

//    @ExcelProperty(value = "性别", index = 9)
//    private String gender;
//
//    @ExcelProperty(value = "身份证", index = 10)
//    private String idCard;

    @ExcelProperty(value = "所属学院", index = 9)
    private String collegeName;

    @ExcelProperty(value = "所属班级", index = 10)
    private String className;

    @ExcelProperty(value = "年级", index = 11)
    private Integer grade;
}