package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 视频基础信息统计（总时长、总数）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoInfoStatsVO {
    private Long totalVideoDurationSec;   // 总视频时长（秒）
    private BigDecimal totalVideoDurationMin;   // 总视频时长（分钟）
    private Long totalVideoCount;         // 总视频数量

    public static VideoInfoStatsVO fromResultSet(ResultSet rs) throws SQLException {
        long totalSec = rs.getLong("totalSec");
        return new VideoInfoStatsVO(
                totalSec,
                BigDecimal.valueOf(totalSec)
                        .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP),
                rs.getLong("totalCnt")
        );
    }
}

