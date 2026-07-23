package cn.xfywz.guozespring.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ContentRowHeight(20)
@HeadRowHeight(25)
@ColumnWidth(20)
public class YeeCourseExportVo {
    
    @ExcelProperty("课程ID")
    private Long id;
    
    @ExcelProperty("课程名称")
    private String name;
    
    @ExcelProperty("课程代码")
    private String code;
    
    @ExcelProperty("课程模式")
    private String mode;
    
    @ExcelProperty("是否自建")
    private String tplId;
    
    @ExcelProperty("开课时间")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDateTime startDate;
    
    @ExcelProperty("结束时间")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDateTime endDate;
    
    @ExcelProperty("学分")
    @NumberFormat("#.##")
    private Double credit;
    
    @ExcelProperty("是否发布")
    private String allow;
    
    @ExcelProperty("选课人数")
    private Long stuCount;
    
    @ExcelProperty("创建者")
    private String createName;
    
    @ExcelProperty("学校ID")
    private String schoolId;
    
    @ExcelProperty("学院名称")
    private String collegeName;
    
    @ExcelProperty("创建日期")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDateTime addDate;
}