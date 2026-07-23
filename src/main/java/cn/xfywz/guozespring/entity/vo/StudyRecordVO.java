package cn.xfywz.guozespring.entity.vo;

import cn.xfywz.guozespring.util.TimeFormatUtil;
import lombok.Data;
import java.sql.ResultSet;
import java.sql.SQLException;

@Data
public class StudyRecordVO {
    private Long id;
    private Long classId;
    private String className;
    private Long studentId;
    private String studentName;
    private String studentNumber;

    // 原始数据
    private Long videoLearned;
    private Long videoCount;
    private Long workLearned;
    private Long workCount;
    private Long examLearned;
    private Long examCount;
    private Long discussJoin;
    private Long discussCount;
    private Long studyTime;

    // 格式化后的字段 (非数据库字段，用于前端展示)
    private String videoProgress;
    private String workProgress;
    private String examProgress;
    private String discussProgress;
    private String studyTimeFormatted;

    /**
     * 静态工厂方法：从 ResultSet 构建 VO
     * 这是 DatabaseUtil 调用的核心方法
     */
    public static StudyRecordVO fromResultSet(ResultSet rs){
        try {
            StudyRecordVO vo = new StudyRecordVO();
            vo.setId(rs.getLong("id"));
            vo.setClassId(rs.getLong("classId"));
            vo.setClassName(rs.getString("className"));
            vo.setStudentId(rs.getLong("studentId"));
            vo.setStudentName(rs.getString("studentName"));
            vo.setStudentNumber(rs.getString("studentNumber"));

            vo.setVideoLearned(rs.getLong("videoLearned"));
            vo.setVideoCount(rs.getLong("videoCount"));
            vo.setWorkLearned(rs.getLong("workLearned"));
            vo.setWorkCount(rs.getLong("workCount"));
            vo.setExamLearned(rs.getLong("examLearned"));
            vo.setExamCount(rs.getLong("examCount"));
            vo.setDiscussJoin(rs.getLong("discussJoin"));
            vo.setDiscussCount(rs.getLong("discussCount"));
            vo.setStudyTime(rs.getLong("studyTime"));

            // 在构建时直接计算格式化字段，避免 Service 层二次处理
            vo.setVideoProgress(vo.getVideoLearned() + "/" + vo.getVideoCount());
            vo.setWorkProgress(vo.getWorkLearned() + "/" + vo.getWorkCount());
            vo.setExamProgress(vo.getExamLearned() + "/" + vo.getExamCount());
            vo.setDiscussProgress(vo.getDiscussJoin() + "/" + vo.getDiscussCount());
            vo.setStudyTimeFormatted(TimeFormatUtil.formatDuration(vo.getStudyTime()));

            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
