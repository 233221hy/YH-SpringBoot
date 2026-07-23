package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 学习时长统计（天）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DurationStatsVO {
    private Long totalDurationDays;
    private Long pcDays;
    private Long mobileDays;

    public static DurationStatsVO fromResultSet(ResultSet rs) throws SQLException {
        return new DurationStatsVO(
                rs.getLong("totalDurationDays"),
                rs.getLong("pcDays"),
                rs.getLong("mobileDays")
        );
    }
}
