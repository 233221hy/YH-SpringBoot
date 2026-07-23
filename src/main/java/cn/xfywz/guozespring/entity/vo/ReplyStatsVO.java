package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 回复统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReplyStatsVO {
    private PostQuantityStatsVO qty;
    private PostStuNumStatsVO stuNum;

    public static ReplyStatsVO fromResultSet(ResultSet rs) throws SQLException {
        // 使用回复统计实际的列名
        PostQuantityStatsVO qty = new PostQuantityStatsVO(
                rs.getLong("replyQty"),
                rs.getLong("mobileReplyQty"),
                rs.getLong("pcReplyQty")
        );
        PostStuNumStatsVO stuNum = new PostStuNumStatsVO(
                rs.getLong("replyStuNum"),
                rs.getLong("mobileReplyStuNum"),
                rs.getLong("pcReplyStuNum")
        );
        return new ReplyStatsVO(qty, stuNum);
    }
}
