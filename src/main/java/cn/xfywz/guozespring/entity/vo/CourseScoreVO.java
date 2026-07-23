package cn.xfywz.guozespring.entity.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 成绩已公布
 */
@Data
@NoArgsConstructor
public class CourseScoreVO {
    private Long studentId;
    private String studentName;
    private String studentNumber;
    private Long classId;
    private String className;
    private Long courseId;
    private Double score;
    private Integer announce = 1; // 固定为 1

    //fromResultSet
    public static CourseScoreVO fromResultSet(ResultSet rs) {
        CourseScoreVO vo = new CourseScoreVO();
        try {
            vo.setStudentId(rs.getLong("studentId"));
            vo.setStudentName(rs.getString("studentName"));
            vo.setStudentNumber(rs.getString("studentNumber"));
            vo.setClassId(rs.getLong("classId"));
            vo.setClassName(rs.getString("className"));
            vo.setCourseId(rs.getLong("courseId"));
            vo.setScore(rs.getDouble("score"));
            vo.setAnnounce(rs.getInt("announce"));
        } catch (SQLException e) {
            throw new RuntimeException("映射学生成绩列表失败", e);
        }
        return vo;
    }
}