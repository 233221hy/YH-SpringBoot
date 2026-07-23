package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 发帖人数统计（发帖/回复共用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostStuNumStatsVO {
    private Long postStuNum;
    private Long mobilePostStuNum;
    private Long pcPostStuNum;

    public static PostStuNumStatsVO fromResultSet(ResultSet rs) throws SQLException {
        return new PostStuNumStatsVO(
                rs.getLong("postStuNum"),
                rs.getLong("mobilePostStuNum"),
                rs.getLong("pcPostStuNum")
        );
    }
}
