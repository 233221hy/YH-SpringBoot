package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.annotation.ExcelExportConfig;
import cn.xfywz.guozespring.excel.ExcelDataPreprocessor;
import cn.xfywz.guozespring.exception.DatabaseException;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * 学生信息导出VO
 */
@Data
@ExcelExportConfig(
        fileName = "导出学生账号",
        sheetName = "学生信息",
        columnWidths = {14, 14, 10, 22, 24, 16, 24, 20}
)
public class YeeStudentExportVO {

    @ExcelProperty(value = "学号", index = 0)
    private String number;

    @ExcelProperty(value = "姓名", index = 1)
    private String name;

    @ExcelProperty(value = "性别", index = 2)
    private String gender;

    @ExcelProperty(value = "身份证号", index = 3)
    private String idCard;

    @ExcelProperty(value = "邮箱", index = 4)
    private String email;

    @ExcelProperty(value = "手机", index = 5)
    private String mobile;

    @ExcelProperty(value = "所属班级", index = 6)
    private String className;

    @ExcelProperty(value = "所属学院", index = 7)
    private String collegeName;


    /**
     * 从ResultSet的当前行映射单个VO对象
     */
    public static YeeStudentExportVO fromResultSet(ResultSet rs) {
        try {
            YeeStudentExportVO vo = new YeeStudentExportVO();
            vo.setNumber(rs.getString("number"));
            vo.setName(rs.getString("name"));
            vo.setGender(rs.getString("gender"));
            vo.setIdCard(rs.getString("idCard"));
            vo.setEmail(rs.getString("email"));
            vo.setMobile(rs.getString("mobile"));
            vo.setClassName(rs.getString("className"));
            vo.setCollegeName(rs.getString("collegeName"));
            return vo;
        } catch (SQLException e) {
            throw new DatabaseException("映射导出学生数据失败", e);
        }
    }

    /**
     * 数据预处理方法
     */
    public void preprocess() {
        ExcelDataPreprocessor.autoPreprocess(this);
    }

    /**
     * 批量预处理静态方法
     */
    public static List<YeeStudentExportVO> preprocessList(List<YeeStudentExportVO> list) {
        if (list == null || list.isEmpty()) {
            return list;
        }
        ExcelDataPreprocessor.batchPreprocess(list);
        return list;
    }

}
