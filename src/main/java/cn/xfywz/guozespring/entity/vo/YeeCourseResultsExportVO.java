package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.annotation.ExcelExportConfig;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 课程平时成绩导出类
 */
@Data
@ExcelExportConfig(
        fileName = "导出课程成绩",
        sheetName = "课程成绩",
        columnWidths = {20, 14, 20, 14, 14, 14, 14, 14, 14, 14}
)
@NoArgsConstructor
public class YeeCourseResultsExportVO {

    @ExcelProperty("学号")
    private String stuNumber;

    @ExcelProperty("姓名")
    private String stuName;

    @ExcelProperty("课程班级")
    private String className;

    @ExcelProperty("视频得分")
    @NumberFormat("#.##")
    private BigDecimal videoScore;

    @ExcelProperty("作业得分")
    @NumberFormat("#.##")
    private BigDecimal workScore;

    @ExcelProperty("考试得分")
    @NumberFormat("#.##")
    private BigDecimal examScore;

    @ExcelProperty("讨论得分")
    @NumberFormat("#.##")
    private BigDecimal discussScore;

    @ExcelProperty("额外得分")
    @NumberFormat("#.##")
    private BigDecimal extraScore;

    @ExcelProperty("报告得分")
    @NumberFormat("#.##")
    private BigDecimal reportScore;

    @ExcelProperty("总得分")
    @NumberFormat("#.##")
    private BigDecimal score;


    public static YeeCourseResultsExportVO fromResultSet(ResultSet rs){
        try {
            YeeCourseResultsExportVO vo = new YeeCourseResultsExportVO();
            vo.setStuNumber(rs.getString("stuNumber"));
            vo.setStuName(rs.getString("stuName"));
            vo.setClassName(rs.getString("className"));
            vo.setVideoScore(rs.getBigDecimal("videoScore"));
            vo.setWorkScore(rs.getBigDecimal("workScore"));
            vo.setExamScore(rs.getBigDecimal("examScore"));
            vo.setDiscussScore(rs.getBigDecimal("discussScore"));
            vo.setExtraScore(rs.getBigDecimal("extraScore"));
            vo.setReportScore(rs.getBigDecimal("reportScore"));
            vo.setScore(rs.getBigDecimal("score"));
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
