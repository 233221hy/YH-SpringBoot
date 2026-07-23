package cn.xfywz.guozespring.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.ResultSet;
import java.sql.SQLException;


@EqualsAndHashCode(callSuper = true)
@Data
public class StudentLoginStatsVO extends BaseLoginStatsVO {
    private Long id;              // 学生ID
    private String stuNumber;      // 学号
    private String stuName;        // 姓名
    private Long classId;          // 班级ID
    private String className;      // 班级名称

    public static StudentLoginStatsVO fromResultSet(ResultSet rs) {
        try {
            StudentLoginStatsVO vo = new StudentLoginStatsVO();
            vo.setId(rs.getLong("id"));
            vo.setStuNumber(rs.getString("stuNumber"));
            vo.setStuName(rs.getString("stuName"));
            vo.setClassId(rs.getLong("classId"));
            vo.setClassName(rs.getString("className"));
            vo.populateBaseFields(rs);
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
