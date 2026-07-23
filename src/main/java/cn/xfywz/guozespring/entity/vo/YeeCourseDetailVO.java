package cn.xfywz.guozespring.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
public class YeeCourseDetailVO {


    // 原始课程字段（按需添加，避免过度传输）
    private Long id;
    private String name;
    private Long mode;
    private Long collegeId;
    private String categoryId;
    private String lecturers;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    private String cover;
    private String content;
    private Double credit;
    private Long allow;
    private String intro;
    private String teacherIntro;
    private String code;
    private Long stuCount;
    private String proclamation;
    private Long clusterId;
    private String periodName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp addTime;

    private Long createId;
    private Long schoolId;
    private Long cateBid;
    private Long cateMid;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp signStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Timestamp signEndTime;

    private Long signScope;
    private String signClass;  // 原始JSON字符串
    private String lecturerName;
    private Long offline;
    private Long mission;
    private Long signLimit;
    private Long lineLock;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date addDate;

    private Long tplId;

    // 扩展字段（新增）
    private String bigCategoryName;   // 大分类名称
    private String midCategoryName;   // 中分类名称
    private List<String> classNames;  // 班级名称列表
    private List<String> responsibleTeacher;  //责任教师

    public void setAddTime(Timestamp addTime) {
        this.addTime = addTime;
    }

    public Timestamp getAddTime() {
        return addTime;
    }
    public void setSignStartTime(Timestamp addTime) {
        this.addTime = addTime;
    }

    public Timestamp getSignStartTime() {
        return addTime;
    }
    public void setSignEndTime(Timestamp addTime) {
        this.addTime = addTime;
    }

    public Timestamp getSignEndTime() {
        return addTime;
    }

    public YeeCourseDetailVO() {
        this.classNames = new ArrayList<>();
        this.responsibleTeacher = new ArrayList<>();
    }


}
