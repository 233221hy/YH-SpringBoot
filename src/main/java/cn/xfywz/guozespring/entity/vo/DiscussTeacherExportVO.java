package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.annotation.ExcelExportConfig;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

@Data
@ExcelExportConfig(
        fileName = "老师讨论统计表",
        sheetName = "讨论统计",
        columnWidths = {20, 15, 20, 15, 15, 15, 15}
)
@NoArgsConstructor
public class DiscussTeacherExportVO {

    @ExcelProperty("工号")
    private String account;

    @ExcelProperty("老师姓名")
    private String name;

    @ExcelProperty("班级名称")
    private String className;

    @ExcelProperty("参与总量")
    private Integer allQty;

    @ExcelProperty("主贴数量")
    private Integer postQty;

    @ExcelProperty("回复数量")
    private Integer replyQty;

    @ExcelProperty("获赞数量")
    private Integer likeQty;

    public static DiscussTeacherExportVO fromResultSet(ResultSet rs) {
        try {
            DiscussTeacherExportVO vo = new DiscussTeacherExportVO();
            vo.setAccount(rs.getString("account"));
            vo.setName(rs.getString("name"));
            vo.setClassName(rs.getString("className"));
            vo.setAllQty(rs.getInt("allQty"));
            vo.setPostQty(rs.getInt("postQty"));
            vo.setReplyQty(rs.getInt("replyQty"));
            vo.setLikeQty(rs.getInt("likeQty"));
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException("构建老师讨论统计导出VO失败", e);
        }
    }
}
