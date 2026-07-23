package cn.xfywz.guozespring.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class CourseTreeVo{
    private Long courseId;
    private String courseName;
    private Integer mode;
    private Integer collegeId;
    private String categoryId;
    private String lecturers;
    private Date startDate;
    private Date endDate;
    private String cover;
    private String content;
    private Long totalVideoDuration;
    private String totalVideoDurationText;
    @JsonFormat(
            shape = JsonFormat.Shape.NUMBER,
            pattern = "0.00"
    )
    private BigDecimal credit;
    private Integer allow;
    private String intro;
    private String teacherIntro;
    private String code;
    private Integer stuCount;
    private String proclamation;
    private Integer clusterId;
    private String periodName;
    private Date addTime;
    private Integer createId;
    private Integer schoolId;
    private Integer cateBid;
    private Integer cateMid;
    private Date signStartTime;
    private Date signEndTime;
    private Integer signScope;
    private String signClass;
    private String lecturerName;
    private Integer offline;
    private Integer mission;
    private Integer signLimit;
    private Integer lineLock;
    private Integer tplId;
    private String createName;
    private Integer isPractice;

    private List<ChapterTreeNodeVo> chapterList;
}
