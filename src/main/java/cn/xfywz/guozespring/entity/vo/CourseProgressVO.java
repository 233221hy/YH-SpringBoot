package cn.xfywz.guozespring.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 成绩未公布的学生进度
 */
@Data
@NoArgsConstructor
public class CourseProgressVO {
    private Long studentId;
    private Long classId;
    private Long courseId;
    private String studentName;
    private String studentNumber;
    private String className;
    private String videoProgress;
    private String workProgress;
    private String examProgress;
    private String discussProgress;
    private Long studyTime;
    private Integer announce = 0; // 固定为 0

    public static CourseProgressVO fromResultSet(ResultSet rs) {
        CourseProgressVO vo = new CourseProgressVO();
        try {
            vo.setStudentId(rs.getLong("studentId"));
            vo.setClassId(rs.getLong("classId"));
            vo.setCourseId(rs.getLong("courseId"));
            vo.setStudentName(rs.getString("studentName"));
            vo.setStudentNumber(rs.getString("studentNumber"));
            vo.setClassName(rs.getString("className"));
            vo.setVideoProgress(rs.getLong("videoLearned") + "/" + rs.getLong("videoCount"));
            vo.setWorkProgress(rs.getLong("workLearned") + "/" + rs.getLong("workCount"));
            vo.setExamProgress(rs.getLong("examLearned") + "/" + rs.getLong("examCount"));
            vo.setDiscussProgress(rs.getLong("discussJoin") + "/" + rs.getLong("discussCount"));
            vo.setStudyTime(rs.getLong("studyTime"));
            vo.setAnnounce(0);
        } catch (SQLException e) {
            throw new RuntimeException("映射学生学习进度列表失败", e);
        }
        return vo;
    }
}