package cn.xfywz.guozespring.entity.vo;


import lombok.Data;

@Data
public class SelectedCoursesStudents {
    private Long id;
    private String name;
    private String number;
    private String idCard;
    private String className;
    private String gender;
    private String point;
    private String collegeName;
    private Integer type;//是否已选该课程（0-未选该课程，1-已选该课程本班级，2-已选该课程其他班级）
}
