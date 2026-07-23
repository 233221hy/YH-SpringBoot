package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeNoticeVo {
    private long id;
    private long courseId;
    private String courseName;
    private long type;
    private List<Integer> classIds;;
    private String userNumber;
    private String title;
    private String summary;
    private String content;
    private long userId;
    private java.sql.Timestamp addTime;
    private long isPush;
    private java.sql.Timestamp pushTime;
    private long sysPush;
    private long schoolId;
    private String className;
}
