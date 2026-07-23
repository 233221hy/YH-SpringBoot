package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.vo.LoginLog;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.LoginLogService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result studentList(LoginLog param) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            int offset = (param.getPageNum() - 1) * param.getPageSize();

            // 基础 FROM 与可选 JOIN
            String baseFrom = """
                 FROM yee_course_student ycs
                LEFT JOIN yee_student st ON st.id = ycs.studentId AND st.schoolId = ycs.schoolId
                LEFT JOIN yee_course_class ycc ON ycc.id = ycs.classId AND ycc.schoolId = ycs.schoolId
                INNER JOIN yee_online ol ON ol.userId = ycs.studentId AND ol.schoolId = ycs.schoolId
                """;

            // where 条件
            StringBuilder where = new StringBuilder(" WHERE ycs.schoolId = ? AND ycs.courseId = ?");
            List<Object> params = new ArrayList<>();
            params.add(param.getSchoolId());
            params.add(param.getCourseId());
            if (param.getClassId() != null && param.getClassId() > 0) {
                where.append(" AND ycs.classId = ?");
                params.add(param.getClassId());
            }
            if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
                where.append(" AND (st.name LIKE ? OR st.number LIKE ?)");
                String like = "%" + param.getKeyword().trim() + "%";
                params.add(like); params.add(like);
            }
            // 时间范围对 ol.logintime2/ol.lastTime2 的约束（存在即过滤，不存在不影响学生的行）
            if (param.getStartTime() != null && param.getEndTime() != null) {
                where.append(" AND EXISTS (SELECT 1 FROM yee_online ol WHERE ol.userId = ycs.studentId AND ol.schoolId = ycs.schoolId AND ol.loginTime2 BETWEEN ? AND ?)");
                params.add(new Timestamp(param.getStartTime()));
                params.add(new Timestamp(param.getEndTime()));
            } else if (param.getStartTime() != null) {
                where.append(" AND EXISTS (SELECT 1 FROM yee_online ol WHERE ol.userId = ycs.studentId AND ol.schoolId = ycs.schoolId AND ol.loginTime2 >= ?)");
                params.add(new Timestamp(param.getStartTime()));
            } else if (param.getEndTime() != null) {
                where.append(" AND EXISTS (SELECT 1 FROM yee_online ol WHERE ol.userId = ycs.studentId AND ol.schoolId = ycs.schoolId AND ol.loginTime2 <= ?)");
                params.add(new Timestamp(param.getEndTime()));
            }


            String countSql = "SELECT COUNT(DISTINCT ycs.studentId)" + baseFrom + where;

            String dataSql = """
                SELECT
                    ycs.studentId AS id,
                    st.number AS stuNumber,
                    st.name AS stuName,
                    ycs.classId,
                    IFNULL(ycc.name,'') AS className,
                """ +
                // 登录次数
                "IFNULL((SELECT COUNT(*) FROM yee_online ol WHERE ol.userId = ycs.studentId AND ol.schoolId = ycs.schoolId" + timeFilterSql(param, params) + "),0) AS loginCount, " +
                // 总时长（秒）
                "IFNULL((SELECT SUM(TIMESTAMPDIFF(SECOND, ol.loginTime2, ol.lastTime2)) FROM yee_online ol WHERE ol.userId = ycs.studentId AND ol.schoolId = ycs.schoolId" + timeFilterSql(param, params) + "),0) AS totalDurationSeconds, " +
                // 总时长（格式化）
                "SEC_TO_TIME(IFNULL((SELECT SUM(TIMESTAMPDIFF(SECOND, ol.loginTime2, ol.lastTime2)) FROM yee_online ol WHERE ol.userId = ycs.studentId AND ol.schoolId = ycs.schoolId" + timeFilterSql(param, params) + "),0)) AS totalDurationFormatted, " +
                // 最近一次登录时间（按 loginTime2 最大）
                "(SELECT MAX(ol.loginTime2) FROM yee_online ol WHERE ol.userId = ycs.studentId AND ol.schoolId = ycs.schoolId" + timeFilterSql(param, params) + ") AS lastLoginTime, " +
                // 最近一次会话时长、IP、平台
                """
                (SELECT ol2.duration FROM yee_online ol2 WHERE ol2.userId = ycs.studentId AND ol2.schoolId = ycs.schoolId ORDER BY ol2.loginTime DESC LIMIT 1) AS lastSessionDuration,
                SEC_TO_TIME((SELECT ol2.duration FROM yee_online ol2 WHERE ol2.userId = ycs.studentId AND ol2.schoolId = ycs.schoolId ORDER BY ol2.loginTime DESC LIMIT 1)) AS lastSessionDurationFormatted,
                (SELECT ol2.ip2 FROM yee_online ol2 WHERE ol2.userId = ycs.studentId AND ol2.schoolId = ycs.schoolId ORDER BY ol2.loginTime DESC LIMIT 1) AS lastLoginIP,
                (SELECT ol2.platform FROM yee_online ol2 WHERE ol2.userId = ycs.studentId AND ol2.schoolId = ycs.schoolId ORDER BY ol2.loginTime DESC LIMIT 1) AS lastLoginPlatform, 
                """ +
                // 在线状态：当前时间-1小时小于ol.lastTime2时判断为在线，否则是离线状态
                "CASE WHEN MAX(ol.lastTime2) > DATE_SUB(NOW(), INTERVAL 1 HOUR) THEN 1 ELSE 0 END AS isOnline" +
                baseFrom + where +
                """
                 GROUP BY ycs.studentId, st.number, st.name, ycs.classId, ycc.name
                 ORDER BY ycs.studentId ASC LIMIT ? OFFSET ?
                """;

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement cps = conn.prepareStatement(countSql);
             PreparedStatement dps = conn.prepareStatement(dataSql)) {

            // 设置 countSql 参数
            int idx = 1;
            for (Object p : params) {
                cps.setObject(idx++, p);
            }

            // 执行 countSql 查询
            long total = 0;
            try (ResultSet crs = cps.executeQuery()) {
                if (crs.next()) {
                    total = crs.getLong(1);
                }
            }

            // 设置 dataSql 参数
            int didx = 1;
            for (Object p : params) {
                dps.setObject(didx++, p);
            }
            dps.setInt(didx++, param.getPageSize());
            dps.setInt(didx, offset);

            // 执行 dataSql 查询并处理结果
            List<Map<String, Object>> list = new ArrayList<>();
            try (ResultSet rs = dps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("stuNumber", rs.getString("stuNumber"));
                    row.put("stuName", rs.getString("stuName"));
                    row.put("classId", rs.getLong("classId"));
                    row.put("className", rs.getString("className"));
                    row.put("loginCount", rs.getInt("loginCount"));
                    row.put("totalDurationSeconds", rs.getLong("totalDurationSeconds"));
                    row.put("totalDurationFormatted", rs.getString("totalDurationFormatted"));
                    row.put("lastLoginTime", rs.getTimestamp("lastLoginTime"));
                    row.put("lastSessionDuration", rs.getObject("lastSessionDuration") == null ? 0 : rs.getLong("lastSessionDuration"));
                    row.put("lastSessionDurationFormatted", rs.getString("lastSessionDurationFormatted"));
                    row.put("lastLoginIP", rs.getString("lastLoginIP"));
                    row.put("lastLoginPlatform", rs.getString("lastLoginPlatform"));
                    row.put("isOnline", rs.getInt("isOnline"));
                    list.add(row);
                }
            }

            return Result.success(list, total);
        }
    } catch (Exception e) {
        return Result.error("查询失败：" + e.getMessage());
    }
    }

    @Override
    public Result teacherList(LoginLog param) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            int offset = (param.getPageNum() - 1) * param.getPageSize();

            String baseFrom = """
                 FROM yee_course_class ycc
                INNER JOIN yee_manage tm ON ycc.teacherId = tm.id AND tm.schoolId = ycc.schoolId
                INNER JOIN yee_online ol ON tm.id = ol.userId AND ycc.schoolId = ol.schoolId
                """;

        StringBuilder where = new StringBuilder(" WHERE ycc.schoolId = ? AND ycc.courseId = ?");
            List<Object> params = new ArrayList<>();
            params.add(param.getSchoolId());
            params.add(param.getCourseId());
            if (param.getClassId() != null && param.getClassId() > 0) {
                where.append(" AND ycc.classId = ?");
                params.add(param.getClassId());
            }
            if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
                where.append(" AND (tm.name LIKE ? OR tm.account LIKE ?)");
                String like = "%" + param.getKeyword().trim() + "%";
                params.add(like); params.add(like);
            }
            if (param.getStartTime() != null && param.getEndTime() != null) {
                where.append(" AND EXISTS (SELECT 1 FROM yee_online ol WHERE ol.userId = tm.id AND ol.schoolId = ycc.schoolId AND ol.loginTime2 BETWEEN ? AND ?)");
                params.add(new Timestamp(param.getStartTime()));
                params.add(new Timestamp(param.getEndTime()));
            } else if (param.getStartTime() != null) {
                where.append(" AND EXISTS (SELECT 1 FROM yee_online ol WHERE ol.userId = tm.id AND ol.schoolId = ycc.schoolId AND ol.loginTime2 >= ?)");
                params.add(new Timestamp(param.getStartTime()));
            } else if (param.getEndTime() != null) {
                where.append(" AND EXISTS (SELECT 1 FROM yee_online ol WHERE ol.userId = tm.id AND ol.schoolId = ycc.schoolId AND ol.loginTime2 <= ?)");
                params.add(new Timestamp(param.getEndTime()));
            }


            String countSql = "SELECT COUNT(DISTINCT tm.id)" + baseFrom + where;

            String dataSql = """
                SELECT
                    tm.id AS id, tm.account AS teaAccount, tm.name AS teaName,
                """ +
                // 登录次数
                "IFNULL((SELECT COUNT(*) FROM yee_online ol WHERE ol.userId = tm.id AND ol.schoolId = ycc.schoolId" + timeFilterSql(param, params) + "),0) AS loginCount, " +
                // 总时长
                "IFNULL((SELECT SUM(TIMESTAMPDIFF(SECOND, ol.loginTime2, ol.lastTime2)) FROM yee_online ol WHERE ol.userId = tm.id AND ol.schoolId = ycc.schoolId" + timeFilterSql(param, params) + "),0) AS totalDurationSeconds, " +
                "SEC_TO_TIME(IFNULL((SELECT SUM(TIMESTAMPDIFF(SECOND, ol.loginTime2, ol.lastTime2)) FROM yee_online ol WHERE ol.userId = tm.id AND ol.schoolId = ycc.schoolId" + timeFilterSql(param, params) + "),0)) AS totalDurationFormatted, " +
                // 最近一次登录时间
                "(SELECT MAX(ol.loginTime2) FROM yee_online ol WHERE ol.userId = tm.id AND ol.schoolId = ycc.schoolId" + timeFilterSql(param, params) + ") AS lastLoginTime, " +
                // 最近一次会话时长、IP、平台loginTime
                """
                (SELECT ol2.duration FROM yee_online ol2 WHERE ol2.userId = tm.id AND ol2.schoolId = ycc.schoolId ORDER BY ol2.loginTime DESC LIMIT 1) AS lastSessionDuration,
                SEC_TO_TIME((SELECT ol2.duration FROM yee_online ol2 WHERE ol2.userId = tm.id AND ol2.schoolId = ycc.schoolId ORDER BY ol2.loginTime DESC LIMIT 1)) AS lastSessionDurationFormatted,
                (SELECT ol2.ip2 FROM yee_online ol2 WHERE ol2.userId = tm.id AND ol2.schoolId = ycc.schoolId ORDER BY ol2.loginTime DESC LIMIT 1) AS lastLoginIP,
                (SELECT ol2.platform FROM yee_online ol2 WHERE ol2.userId = tm.id AND ol2.schoolId = ycc.schoolId ORDER BY ol2.loginTime DESC LIMIT 1) AS lastLoginPlatform,
                """ +
                // 在线状态：当前时间-1小时小于ol.lastTime2时判断为在线，否则是离线状态
                "CASE WHEN MAX(ol.lastTime2) > DATE_SUB(NOW(), INTERVAL 1 HOUR) THEN 1 ELSE 0 END AS isOnline" +
                baseFrom + where +
                """
                 GROUP BY tm.id, tm.account, tm.name
                 ORDER BY tm.id ASC LIMIT ? OFFSET ?
                """;

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement cps = conn.prepareStatement(countSql);
             PreparedStatement dps = conn.prepareStatement(dataSql)) {

            // 设置 countSql 参数
            int idx = 1;
            for (Object p : params) {
                cps.setObject(idx++, p);
            }

            // 执行 countSql 查询
            long total = 0;
            try (ResultSet crs = cps.executeQuery()) {
                if (crs.next()) {
                    total = crs.getLong(1);
                }
            }

            // 设置 dataSql 参数
            int didx = 1;
            for (Object p : params) {
                dps.setObject(didx++, p);
            }
            dps.setInt(didx++, param.getPageSize());
            dps.setInt(didx, offset);

            // 执行 dataSql 查询并处理结果
            List<Map<String, Object>> list = new ArrayList<>();
            try (ResultSet rs = dps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("teaAccount", rs.getString("teaAccount"));
                    row.put("teaName", rs.getString("teaName"));
                    row.put("loginCount", rs.getInt("loginCount"));
                    row.put("totalDurationSeconds", rs.getLong("totalDurationSeconds"));
                    row.put("totalDurationFormatted", rs.getString("totalDurationFormatted"));
                    row.put("lastLoginTime", rs.getTimestamp("lastLoginTime"));
                    row.put("lastSessionDuration", rs.getObject("lastSessionDuration") == null ? 0 : rs.getLong("lastSessionDuration"));
                    row.put("lastSessionDurationFormatted", rs.getString("lastSessionDurationFormatted"));
                    row.put("lastLoginIP", rs.getString("lastLoginIP"));
                    row.put("lastLoginPlatform", rs.getString("lastLoginPlatform"));
                    row.put("isOnline", rs.getInt("isOnline"));
                    list.add(row);
                }
            }

            return Result.success(list, total);
        }
    } catch (Exception e) {
        return Result.error("查询失败：" + e.getMessage());
    }
}

    // 组装对 yee_online 的时间过滤片段（用于子查询）
    private String timeFilterSql(LoginLog param, List<Object> params) {
        StringBuilder sb = new StringBuilder();
        if (param.getStartTime() != null && param.getEndTime() != null) {
            sb.append(" AND ol.loginTime2 BETWEEN ? AND ?");
            params.add(new Timestamp(param.getStartTime()));
            params.add(new Timestamp(param.getEndTime()));
        } else if (param.getStartTime() != null) {
            sb.append(" AND ol.loginTime2 >= ?");
            params.add(new Timestamp(param.getStartTime()));
        } else if (param.getEndTime() != null) {
            sb.append(" AND ol.loginTime2 <= ?");
            params.add(new Timestamp(param.getEndTime()));
        }
        return sb.toString();
    }


}
