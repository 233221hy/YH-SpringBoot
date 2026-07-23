package cn.xfywz.guozespring.entity.vo;

import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class YeeDiscussReplyVo {
    private Long id;
    private String content;
    private String images;
    private String files;
    private Date addTime;
    private Integer courseId;
    private String courseName;
    private Long discussId;
    private String discussName;
    private Long likeCount;
    private Long replyCount;
    private Integer reUserId;
    private Integer replyId;
    private Integer pid;
    private String platform;
    private Integer userId;
    private boolean liked = false;

    private  String userName;
    private  String userAvatar;
    private List<YeeDiscussReplyVo> replies = new ArrayList<>();

    /**
     * 将 ResultSet 映射为 YeeDiscussReplyVo
     */
    public static YeeDiscussReplyVo mapDiscussReplyVo(ResultSet rs){
        try {
            YeeDiscussReplyVo vo = new YeeDiscussReplyVo();
            vo.setId(rs.getLong("id"));
            vo.setDiscussId(rs.getLong("discussId"));
            vo.setUserId(rs.getInt("userId"));
            vo.setReUserId(rs.getInt("reUserId"));
            vo.setContent(rs.getString("content"));
            vo.setReplyId(rs.getInt("replyId"));
            vo.setPid(rs.getInt("pid"));
            vo.setImages(rs.getString("images"));
            vo.setFiles(rs.getString("files"));
            vo.setCourseId(rs.getInt("courseId"));
            vo.setPlatform(rs.getString("platform"));
            vo.setAddTime(rs.getTimestamp("addTime"));
            // 安全读取
            vo.setLikeCount(getLongSafe(rs, "like_count"));
            vo.setReplyCount(getLongSafe(rs, "reply_count"));
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static long getLongSafe(ResultSet rs, String label) throws SQLException {
        try {
            return rs.getLong(label);
        } catch (SQLException e) {
            if (e.getMessage().contains("Column") && e.getMessage().contains("not found")) {
                return 0L;
            }
            throw e;
        }
    }


}