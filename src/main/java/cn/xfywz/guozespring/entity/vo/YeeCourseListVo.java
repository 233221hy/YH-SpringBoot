package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeCourseListVo {
    private long id;
    private String name;
    private String code;
    private String mode;
    private String tplId;
    private Date startDate;
    private Date endDate;
    private double credit;
    private String allow;
    private long stuCount;
    private String schoolId;
    private Date addDate;
    private String isPractice;

    //    private String schoolName;
    private String collegeName;
    private String createName;

}