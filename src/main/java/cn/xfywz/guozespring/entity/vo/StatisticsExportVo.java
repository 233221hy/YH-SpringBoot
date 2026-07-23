package cn.xfywz.guozespring.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ContentRowHeight(20)
@HeadRowHeight(25)
@ColumnWidth(20)
public class StatisticsExportVo {
    @ExcelProperty("学校名称")
    private String schoolName;
    
    @ExcelProperty("学生人数")
    private Long studentCount;
    
    @ExcelProperty("老师人数")
    private Long teacherCount;
    
    @ExcelProperty("行政班级")
    private Long classCount;
    
    @ExcelProperty("建课数")
    private Long courseCount;
    
    @ExcelProperty("选课人次")
    private Long courseSelectionCount;
    
    @ExcelProperty("开课数(状态)")
    private Long activeCourseCount;
    
    @ExcelProperty("选课人数")
    private Long courseStudentCount;
    
    @ExcelProperty("必修课")
    private Long requiredCourseCount;
    
    @ExcelProperty("选修课")
    private Long electiveCourseCount;
    
    @ExcelProperty("教学班级")
    private Long teachingClassCount;
    
    @ExcelProperty("主题讨论")
    private Long topicDiscussionCount;
    
    @ExcelProperty("评论回复")
    private Long commentReplyCount;
    
    @ExcelProperty("视频讨论")
    private Long videoDiscussionCount;
    
    @ExcelProperty("试卷")
    private Long paperCount;
    
    @ExcelProperty("试题")
    private Long questionCount;
    
    @ExcelProperty("作业")
    private Long workCount;
    
    @ExcelProperty("考试")
    private Long examCount;
}