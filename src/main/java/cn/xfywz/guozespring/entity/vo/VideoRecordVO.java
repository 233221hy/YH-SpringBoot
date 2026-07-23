package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.util.TimeFormatUtil;
import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;

@Data
public class VideoRecordVO {
    private Long nodeId;
    private String nodeName;
    private Integer startTime;
    private Integer finishTime;
    private Long viewCount;
    private String totalDuration;   // 格式化后的展示时长
    private String videoDuration;   // 格式化后的视频总时长
    private String state;            // 中文状态

    public static VideoRecordVO fromResultSet(ResultSet rs){
        try {
            VideoRecordVO vo = new VideoRecordVO();
            vo.setNodeId(rs.getLong("nodeId"));
            vo.setNodeName(rs.getString("nodeName"));
            vo.setStartTime(rs.getInt("startTime"));
            vo.setFinishTime(rs.getInt("finishTime"));
            vo.setViewCount(rs.getLong("viewCount"));

            long totalDuration = rs.getLong("totalDuration");
            long videoDuration = rs.getLong("videoDuration");
            int stateCode = rs.getInt("stateCode");

            // 根据状态决定展示时长
            long showDuration = totalDuration;
            if (stateCode == 1 && totalDuration < videoDuration) {
                showDuration = videoDuration;
            }
            vo.setTotalDuration(TimeFormatUtil.formatDuration(showDuration));
            vo.setVideoDuration(TimeFormatUtil.formatDuration(videoDuration));

            // 状态转换
            String stateStr = switch (stateCode) {
                case 0 -> "未开始";
                case 1 -> "已完成";
                default -> "学习中";
            };
            vo.setState(stateStr);
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
