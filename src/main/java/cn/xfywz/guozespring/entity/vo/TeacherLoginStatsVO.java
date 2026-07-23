package cn.xfywz.guozespring.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.ResultSet;
import java.sql.SQLException;

@EqualsAndHashCode(callSuper = true)
@Data
public class TeacherLoginStatsVO extends BaseLoginStatsVO {
    private Long id;              // 教师ID
    private String teaAccount;     // 账号
    private String teaName;        // 姓名

    public static TeacherLoginStatsVO fromResultSet(ResultSet rs) {
        try {
            TeacherLoginStatsVO vo = new TeacherLoginStatsVO();
            vo.setId(rs.getLong("id"));
            vo.setTeaAccount(rs.getString("teaAccount"));
            vo.setTeaName(rs.getString("teaName"));
            vo.populateBaseFields(rs);
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
