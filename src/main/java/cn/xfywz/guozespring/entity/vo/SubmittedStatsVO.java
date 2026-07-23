package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 提交统计（考试/作业共用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmittedStatsVO {
    private Long submittedStuNum;
    private Long submittedCount;

    public static SubmittedStatsVO fromResultSet(ResultSet rs) throws SQLException {
        return new SubmittedStatsVO(
                rs.getLong("submittedStuNum"),
                rs.getLong("submittedCount")
        );
    }
}
