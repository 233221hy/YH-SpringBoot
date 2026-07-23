package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 待批阅统计（考试/作业共用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsVO {
    private Long pendingReviewStuNum;
    private Long reviewCount;

    public static ReviewStatsVO fromResultSet(ResultSet rs) throws SQLException {
        return new ReviewStatsVO(
                rs.getLong("pendingReviewStuNum"),
                rs.getLong("reviewCount")
        );
    }
}
