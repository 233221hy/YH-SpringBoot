package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 学习人数统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearnerStatsVO {
    private Long learnerNum;
    private Long pc;
    private Long mobile;

    public static LearnerStatsVO fromResultSet(ResultSet rs) throws SQLException {
        return new LearnerStatsVO(
                rs.getLong("learnerNum"),
                rs.getLong("pc"),
                rs.getLong("mobile")
        );
    }
}