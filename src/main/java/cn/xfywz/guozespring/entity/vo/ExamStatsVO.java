package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 考试统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamStatsVO {
    private Long pcStuNum;
    private Long mobileStuNum;
    private SubmittedStatsVO submitted;
    private ReviewStatsVO review;

    public static ExamStatsVO fromResultSet(ResultSet rs) throws SQLException {
        SubmittedStatsVO submitted = SubmittedStatsVO.fromResultSet(rs);
        ReviewStatsVO review = ReviewStatsVO.fromResultSet(rs);
        return new ExamStatsVO(
                rs.getLong("pcStuNum"),
                rs.getLong("mobileStuNum"),
                submitted,
                review
        );
    }
}