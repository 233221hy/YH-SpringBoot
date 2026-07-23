package cn.xfywz.guozespring.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;

/**
 * 课程成绩列表回显 VO
 * 用于列表展示课程成绩相关字段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class YeeCourseResultsVO {
    private long id;
    private long courseId;
    private long userId;

    private BigDecimal score;
    private BigDecimal videoScore;
    private BigDecimal examScore;
    private BigDecimal workScore;
    private BigDecimal discussScore;
    private BigDecimal extraScore;
    private BigDecimal reportScore;

    private String stuName;
    private String stuNumber;

    private long classId;
    private String courseClassName;
    private long ranking;
    private BigDecimal videoResult;
    private BigDecimal examResult;
    private BigDecimal workResult;
    private BigDecimal discussResult;
    private BigDecimal reportResult;

    private long schoolId;
    private Date calcDate;

    /**
     * 从 ResultSet 映射为 YeeCourseResultsVO
     */
    public static YeeCourseResultsVO fromResultSet(ResultSet rs){
        try {
            YeeCourseResultsVO vo = new YeeCourseResultsVO();
            vo.setId(rs.getLong("id"));
            vo.setCourseId(rs.getLong("courseId"));
            vo.setUserId(rs.getLong("userId"));
            vo.setScore(rs.getBigDecimal("score"));
            vo.setVideoScore(rs.getBigDecimal("videoScore"));
            vo.setExamScore(rs.getBigDecimal("examScore"));
            vo.setWorkScore(rs.getBigDecimal("workScore"));
            vo.setDiscussScore(rs.getBigDecimal("discussScore"));
            vo.setExtraScore(rs.getBigDecimal("extraScore"));
            vo.setReportScore(rs.getBigDecimal("reportScore"));
            vo.setStuName(rs.getString("stuName"));
            vo.setStuNumber(rs.getString("stuNumber"));
            vo.setClassId(rs.getLong("classId"));
            vo.setCourseClassName(rs.getString("courseClassName"));
            vo.setRanking(rs.getLong("ranking"));
            vo.setVideoResult(rs.getBigDecimal("videoResult"));
            vo.setExamResult(rs.getBigDecimal("examResult"));
            vo.setWorkResult(rs.getBigDecimal("workResult"));
            vo.setDiscussResult(rs.getBigDecimal("discussResult"));
            vo.setReportResult(rs.getBigDecimal("reportResult"));
            vo.setSchoolId(rs.getLong("schoolId"));
            vo.setCalcDate(rs.getDate("calcDate"));
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException("映射学生课程成绩失败", e);
        }

    }
}