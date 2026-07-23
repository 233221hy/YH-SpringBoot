package cn.xfywz.guozespring.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 发帖统计
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostStatsVO {
    private PostQuantityStatsVO qty;
    private PostStuNumStatsVO stuNum;

    public static PostStatsVO fromResultSet(ResultSet rs) throws SQLException {
        PostQuantityStatsVO qty = PostQuantityStatsVO.fromResultSet(rs);
        PostStuNumStatsVO stuNum = PostStuNumStatsVO.fromResultSet(rs);
        return new PostStatsVO(qty, stuNum);
    }
}

