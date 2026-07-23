package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.vo.PracticeReportStatsVO;
import cn.xfywz.guozespring.entity.vo.PracticeReportVO;
import cn.xfywz.guozespring.service.teacher.YeePracticeReportService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

@Service
public class YeePracticeReportServiceImpl implements YeePracticeReportService {

    @Resource
    private DatabaseUtil databaseUtil;

    @Override
    public Result stats(int schoolId, int courseId) throws Exception {
        String sql = """
            SELECT
                COUNT(1) AS totalStudents,
                SUM(CASE WHEN pr.status IS NULL THEN 1 ELSE 0 END) AS notSubmitted,
                SUM(CASE WHEN pr.status IN (1, 2, 3) THEN 1 ELSE 0 END) AS submitted,
                SUM(CASE WHEN pr.status = 2 THEN 1 ELSE 0 END) AS passed,
                SUM(CASE WHEN pr.status = 3 THEN 1 ELSE 0 END) AS notPassed
            FROM yee_course_student cs
            LEFT JOIN yee_practice_report pr
                ON cs.courseId = pr.courseId AND cs.studentId = pr.studentId
            WHERE cs.courseId = ?
            """;

        PracticeReportStatsVO stats = databaseUtil.query(schoolId)
                .sql(sql)
                .param(courseId)
                .single(rs -> {
                    PracticeReportStatsVO vo = new PracticeReportStatsVO();
                    vo.setTotalStudents(rs.getInt("totalStudents"));
                    vo.setNotSubmitted(rs.getInt("notSubmitted"));
                    vo.setSubmitted(rs.getInt("submitted"));
                    vo.setPassed(rs.getInt("passed"));
                    vo.setNotPassed(rs.getInt("notPassed"));
                    return vo;
                })
                .orElse(null);

        return Result.success(stats);
    }

    @Override
    public Result list(int schoolId, int courseId, Integer classId,
                       String studentNumber, String studentName, Integer status,
                       int pageNum, int pageSize) throws Exception {

        StringBuilder sql = new StringBuilder("""
            SELECT pr.id, s.number, s.name, cc.name AS className,
                   pr.title, pr.submitTime, pr.status
            FROM yee_course_student cs
            JOIN yee_student s ON cs.studentId = s.id
            LEFT JOIN yee_course_class cc ON cs.classId = cc.id
            LEFT JOIN yee_practice_report pr
                ON pr.studentId = cs.studentId AND pr.courseId = cs.courseId
            WHERE cs.courseId = ?
            """);

        List<Object> params = new ArrayList<>();
        params.add(courseId);

        if (classId != null && classId > 0) {
            sql.append(" AND cs.classId = ?");
            params.add(classId);
        }
        if (studentNumber != null && !studentNumber.isBlank()) {
            sql.append(" AND s.number LIKE ?");
            params.add("%" + studentNumber + "%");
        }
        if (studentName != null && !studentName.isBlank()) {
            sql.append(" AND s.name LIKE ?");
            params.add("%" + studentName + "%");
        }
        if (status != null) {
            sql.append(" AND (pr.status = ? OR pr.id IS NULL)");
            params.add(status);
        }

        var pageResult = databaseUtil.query(schoolId)
                .sql(sql.toString())
                .params(params.toArray())
                .orderBy("pr.id IS NULL, pr.submitTime DESC")
                .page(rs -> {
                    try {
                        PracticeReportVO vo = new PracticeReportVO();
                        vo.setId(rs.getLong("id"));
                        vo.setStudentNumber(rs.getString("number"));
                        vo.setStudentName(rs.getString("name"));
                        vo.setClassName(rs.getString("className"));
                        vo.setTitle(rs.getString("title"));
                        vo.setSubmitTime(rs.getTimestamp("submitTime"));
                        vo.setStatus(rs.getInt("status"));
                        return vo;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }, pageNum, pageSize);

        return Result.success(pageResult.getRows(), pageResult.getTotal());
    }

    @Override
    public Result detail(int schoolId, long reportId) throws Exception {
        String sql = """
            SELECT pr.*, s.number, s.name, cc.name AS className
            FROM yee_practice_report pr
            LEFT JOIN yee_student s ON pr.studentId = s.id
            LEFT JOIN yee_course_class cc ON pr.classId = cc.id
            WHERE pr.id = ?
            """;

        PracticeReportVO vo = databaseUtil.query(schoolId)
                .sql(sql)
                .param(reportId)
                .single(rs -> {
                    PracticeReportVO v = new PracticeReportVO();
                    v.setId(rs.getLong("id"));
                    v.setStudentNumber(rs.getString("number"));
                    v.setStudentName(rs.getString("name"));
                    v.setClassName(rs.getString("className"));
                    v.setTitle(rs.getString("title"));
                    v.setSubmitTime(rs.getTimestamp("submitTime"));
                    v.setStatus(rs.getInt("status"));
                    v.setContent(rs.getString("content"));
                    v.setFiles(rs.getString("files"));
                    v.setRemark(rs.getString("remark"));
                    v.setReviewTime(rs.getTimestamp("reviewTime"));
                    return v;
                })
                .orElse(null);

        if (vo == null) {
            return Result.error("报告不存在");
        }
        return Result.success(vo);
    }

    @Override
    public Result review(int schoolId, long reportId, String result,
                         long reviewerId, String remark) throws Exception {
        String checkSql = "SELECT status FROM yee_practice_report WHERE id = ?";
        Integer status = databaseUtil.query(schoolId)
                .sql(checkSql)
                .param(reportId)
                .scalar(rs -> rs.getInt("status"))
                .orElse(null);

        if (status == null) {
            return Result.error("报告不存在");
        }
        if (status != 1) {
            return Result.error("该报告当前状态不允许审核");
        }

        int newStatus = "pass".equals(result) ? 2 : 3;

        String updateSql = """
            UPDATE yee_practice_report
            SET status = ?, reviewTime = ?, reviewerId = ?, remark = ?
            WHERE id = ?
            """;

        databaseUtil.executeUpdate(schoolId, updateSql,
                newStatus, new Timestamp(System.currentTimeMillis()), reviewerId, remark != null ? remark : "", reportId);

        return Result.success(newStatus == 2 ? "已通过" : "已驳回");
    }

    @Override
    public Result allReportsForExport(int schoolId, int courseId,
                                      Integer classId, Integer status) throws Exception {
        StringBuilder sql = new StringBuilder("""
            SELECT pr.*, s.number, s.name, cc.name AS className
            FROM yee_practice_report pr
            LEFT JOIN yee_student s ON pr.studentId = s.id
            LEFT JOIN yee_course_class cc ON pr.classId = cc.id
            WHERE pr.courseId = ?
            """);

        List<Object> params = new ArrayList<>();
        params.add(courseId);

        if (classId != null && classId > 0) {
            sql.append(" AND pr.classId = ?");
            params.add(classId);
        }
        if (status != null) {
            sql.append(" AND pr.status = ?");
            params.add(status);
        }

        List<Map<String, Object>> list = databaseUtil.query(schoolId)
                .sql(sql.toString())
                .params(params.toArray())
                .list(rs -> {
                    try {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", rs.getLong("id"));
                        map.put("courseId", rs.getLong("courseId"));
                        map.put("studentId", rs.getLong("studentId"));
                        map.put("title", rs.getString("title"));
                        map.put("content", rs.getString("content"));
                        map.put("files", rs.getString("files"));
                        map.put("status", rs.getInt("status"));
                        map.put("submitTime", rs.getTimestamp("submitTime"));
                        map.put("number", rs.getString("number"));
                        map.put("name", rs.getString("name"));
                        map.put("className", rs.getString("className"));
                        return map;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

        return Result.success(list);
    }
}