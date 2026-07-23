package cn.xfywz.guozespring.entity.dto;

import lombok.Data;

@Data
public class SlSchoolThematicWithCourseNames {

    private Integer id;
    private String name;
    private String background;
    private String tags;
    private Integer allow;
    private Integer sort;
    private Integer schoolId;

    // 只保留课程名称
    private String courseName1;
    private String courseName2;
    private String courseName3;
    private String courseName4;
    private String courseName5;
    private String courseName6;
}
