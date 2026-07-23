package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.annotation.ExcelExportConfig;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.NumberFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 课程额外得分导出VO
 */
@Data
@ExcelExportConfig(
        fileName = "导出课程额外得分",
        sheetName = "课程额外得分",
        columnWidths = {22, 18, 18}
)
@NoArgsConstructor
public class YeeCourseExtraScoreExportVO {
    @ColumnWidth(30)
    @ExcelProperty("学号")
    private String stuNumber;
    @ExcelProperty("姓名")
    private String stuName;
    @ExcelProperty("额外得分")
    @NumberFormat("#.##")
    private BigDecimal extraScore;

    /**
     * 从ResultSet的当前行映射单个VO对象
     */
    public static YeeCourseExtraScoreExportVO fromResultSet(ResultSet rs) {
        try {
            YeeCourseExtraScoreExportVO vo = new YeeCourseExtraScoreExportVO();
            vo.setStuNumber(rs.getString("stuNumber"));
            vo.setStuName(rs.getString("stuName"));
            vo.setExtraScore(rs.getBigDecimal("extraScore"));
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}