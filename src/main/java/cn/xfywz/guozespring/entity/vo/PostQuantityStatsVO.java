package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 帖子数量及人数统计（发帖/回复共用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostQuantityStatsVO {
    private Long postQty;
    private Long mobilePostQty;
    private Long pcPostQty;

    public static PostQuantityStatsVO fromResultSet(ResultSet rs) throws SQLException {
        return new PostQuantityStatsVO(
                rs.getLong("postQty"),
                rs.getLong("mobilePostQty"),
                rs.getLong("pcPostQty")
        );
    }
}
