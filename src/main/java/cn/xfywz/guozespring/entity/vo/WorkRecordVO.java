package cn.xfywz.guozespring.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

@Data
public class WorkRecordVO {
    private Long workId;
    private String title;
    private String nodeName;
    private Integer topicNumber;
    private BigDecimal totalScore;
    private Long recordId;
    private Integer startTime;
    private Integer finishTime;
    private BigDecimal getScore;
    private Integer state;
    private Integer frequency;

    public static WorkRecordVO fromResultSet(ResultSet rs){
        try {
            WorkRecordVO vo = new WorkRecordVO();
            vo.setWorkId(rs.getLong("workId"));
            vo.setTitle(rs.getString("title"));
            vo.setNodeName(rs.getString("nodeName"));
            vo.setTopicNumber(rs.getInt("topicNumber"));
            BigDecimal totalScore = rs.getBigDecimal("totalScore");
            vo.setTotalScore(totalScore != null ? totalScore : rs.getBigDecimal("totalScore"));
            vo.setRecordId(rs.getLong("recordId"));
            vo.setStartTime(rs.getInt("startTime"));
            vo.setFinishTime(rs.getInt("finishTime"));
            vo.setGetScore(rs.getBigDecimal("getScore"));
//            vo.setState(rs.getObject("state") != null ? rs.getInt("state") : 0);
            vo.setState(rs.getObject("state") != null ? rs.getInt("state") : null);
            vo.setFrequency(rs.getObject("frequency") != null ? rs.getInt("frequency") : 0);
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
