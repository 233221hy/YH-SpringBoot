package cn.xfywz.guozespring.entity.vo;

import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;

@Data
public class DiscussRecordVO {
    private Long discussId;
    private String title;
    private Integer postQty;
    private Integer replyQty;
    private Integer likeQty;
    private Integer getScore;
    private Integer fullScore;

    public static DiscussRecordVO fromResultSet(ResultSet rs){
        try {
            DiscussRecordVO vo = new DiscussRecordVO();
            vo.setDiscussId(rs.getLong("discussId"));
            vo.setTitle(rs.getString("title"));
            vo.setPostQty(rs.getInt("postQty"));
            vo.setReplyQty(rs.getInt("replyQty"));
            vo.setLikeQty(rs.getInt("likeQty"));
            vo.setGetScore(rs.getObject("getScore") != null ? rs.getInt("getScore") : 0);
            vo.setFullScore(100); // 固定满分100分
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
