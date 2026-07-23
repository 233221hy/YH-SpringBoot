package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 作业统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkStatsVO {
    private Long pcStuNum;
    private Long mobileStuNum;
    private SubmittedStatsVO submitted;
    private ReviewStatsVO review;

    public static WorkStatsVO fromResultSet(ResultSet rs) throws SQLException {
        SubmittedStatsVO submitted = SubmittedStatsVO.fromResultSet(rs);
        ReviewStatsVO review = ReviewStatsVO.fromResultSet(rs);
        return new WorkStatsVO(
                rs.getLong("pcStuNum"),
                rs.getLong("mobileStuNum"),
                submitted,
                review
        );
    }
}
