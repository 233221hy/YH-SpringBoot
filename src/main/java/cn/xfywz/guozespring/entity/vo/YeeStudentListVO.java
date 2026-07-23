package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.exception.DatabaseException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class YeeStudentListVO {
    private Integer id;
    private String number;
    private String name;
    private String idCard;
    private String gender;
    private String mobile;
    private Integer entryYear;
    private String className;
    private String collegeName;
    private Date addDate;

    /**
     * 从ResultSet的当前行映射单个VO对象
     */
    public static YeeStudentListVO fromResultSet(ResultSet rs) {
        try {
            YeeStudentListVO vo = new YeeStudentListVO();
            vo.setId(rs.getInt("id"));
            vo.setNumber(rs.getString("number"));
            vo.setName(rs.getString("name"));
            vo.setIdCard(rs.getString("idCard"));
            vo.setGender(rs.getString("gender"));
            vo.setMobile(rs.getString("mobile"));
            vo.setEntryYear(rs.getInt("entryYear"));
            vo.setClassName(rs.getString("className"));
            vo.setCollegeName(rs.getString("collegeName"));
            vo.setAddDate(rs.getDate("addDate"));
            return vo;
        } catch (SQLException e) {
            throw new DatabaseException("映射学生数据失败", e);
        }
    }

    /**
     * 将ResultSet映射为VO列表
     */
    public static List<YeeStudentListVO> mapStudents(ResultSet rs) {
        List<YeeStudentListVO> list = new ArrayList<>();
        try {
            while (rs.next()) {
                list.add(fromResultSet(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("映射学生列表失败", e);
        }
        return list;
    }

}