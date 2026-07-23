package cn.xfywz.guozespring.entity.mhsch;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YeePracticeReport {
    private Long id;
    private Long courseId;
    private Long classId;
    private Long studentId;
    private String title;
    private String content;
    private String files;
    private Integer status;
    private Date submitTime;
    private Date reviewTime;
    private Long reviewerId;
    private String remark;
    private String pdfPath;
    private Long schoolId;
    private Date addTime;
}