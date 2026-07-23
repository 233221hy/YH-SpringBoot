package cn.xfywz.guozespring.entity.vo;

import com.alibaba.excel.annotation.format.NumberFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

@Data
public class ExamRecordVO {
    private Long examId;
    private String title;
    private String nodeName;
    private Integer topicNumber;
    private Double totalScore;
    private Long recordId;
    private Integer startTime;
    private Integer finishTime;
    private Double getScore;

//    private Integer recordState; // 0:未考, 1:已考
    private Integer state;
    private Integer frequency;

    /**
     * 静态工厂方法：从 ResultSet 构建 VO
     */
    public static ExamRecordVO fromResultSet(ResultSet rs){
        try {
            ExamRecordVO vo = new ExamRecordVO();
            vo.setExamId(rs.getLong("examId"));
            vo.setTitle(rs.getString("title"));
            vo.setNodeName(rs.getString("nodeName"));
            vo.setTopicNumber(rs.getInt("topicNumber"));
            vo.setTotalScore(rs.getDouble("totalScore"));
            vo.setRecordId(rs.getLong("recordId"));
            // 数据库存储的是 LONG/INTEGER 类型的 Unix 时间戳
            vo.setStartTime(rs.getInt("startTime"));
            vo.setFinishTime(rs.getInt("finishTime"));
            vo.setGetScore(rs.getDouble("getScore"));
//            Integer state = rs.getObject("state") != null ? 1 : null;
            Integer stateVal = rs.getObject("state", Integer.class);
            vo.setState(stateVal);
//            vo.setState(state);
            vo.setFrequency(rs.getInt("frequency"));
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
