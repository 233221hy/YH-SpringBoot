package cn.xfywz.guozespring.service.student.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.student.StudyDurationService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@Service
public class StudyDurationServiceImpl implements StudyDurationService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result stats(int schoolId, long studentId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            String sql = """
                SELECT
                    SUM(CASE WHEN addDate = CURDATE() THEN duration ELSE 0 END) AS todaySec,
                    SUM(CASE WHEN addDate >= DATE_SUB(CURDATE(), INTERVAL 6 DAY) THEN duration ELSE 0 END) AS d7Sec,
                    SUM(CASE WHEN addDate >= DATE_SUB(CURDATE(), INTERVAL 29 DAY) THEN duration ELSE 0 END) AS d30Sec,
                    SUM(duration) AS totalSec
                FROM yee_study_time 
                WHERE schoolId = ? AND userId = ?
            """;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, schoolId);
                ps.setLong(2, studentId);
                try (ResultSet rs = ps.executeQuery()) {
                    long todaySec = 0, d7Sec = 0, d30Sec = 0, totalSec = 0;
                    if (rs.next()) {
                        todaySec = rs.getLong("todaySec");
                        d7Sec = rs.getLong("d7Sec");
                        d30Sec = rs.getLong("d30Sec");
                        totalSec = rs.getLong("totalSec");
                    }
                    Map<String, Object> data = new HashMap<>();
                    data.put("today", secToMin(todaySec));
                    data.put("last7Days", secToMin(d7Sec));
                    data.put("last30Days", secToMin(d30Sec));
                    data.put("total", secToMin(totalSec));
                    return Result.success(data);
                }
            }
        }
    }

    @Override
    public Result courseCompare(int schoolId, long studentId, Integer days) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT yt.courseId, c.name AS courseName, SUM(yt.duration) AS totalSec
            FROM yee_study_time yt
            LEFT JOIN yee_course c ON c.id = yt.courseId
            WHERE yt.schoolId = ? AND yt.userId = ?
        """);
        if (days != null && days > 0) {
            sql.append(" AND yt.addDate >= DATE_SUB(CURDATE(), INTERVAL ? DAY)");
        }
        sql.append(" GROUP BY yt.courseId, c.name ORDER BY totalSec DESC");

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            ps.setInt(idx++, schoolId);
            ps.setLong(idx++, studentId);
            if (days != null && days > 0) {
                ps.setInt(idx++, days - 1);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> list = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("courseId", rs.getLong("courseId"));
                    row.put("courseName", rs.getString("courseName"));
                    row.put("minutes", secToMin(rs.getLong("totalSec")));
                    list.add(row);
                }
                return Result.success(list);
            }
        }
    }

    @Override
    public Result overview(int schoolId, long studentId, Integer days) throws Exception {
        Map<String, Object> data = new HashMap<>();
        Result stats = stats(schoolId, studentId);
        Result compare = courseCompare(schoolId, studentId, days);
        data.put("stats", stats.getData());
        data.put("courseCompare", compare.getData());
        return Result.success(data);
    }

    private long secToMin(long sec) {
        if (sec <= 0) return 0;
        return Math.round((double) sec / 60.0);
    }
}
