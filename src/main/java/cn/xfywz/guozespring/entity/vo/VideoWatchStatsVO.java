package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 视频观看统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoWatchStatsVO {
    private Long viewCount;
    private Long pc;
    private Long mobile;

    public static VideoWatchStatsVO fromResultSet(ResultSet rs) throws SQLException {
        return new VideoWatchStatsVO(
                rs.getLong("viewCount"),
                rs.getLong("pc"),
                rs.getLong("mobile")
        );
    }
}
