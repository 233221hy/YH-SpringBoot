package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.dto.DiscussStatsDTO;
import cn.xfywz.guozespring.entity.vo.DiscussStudentExportVO;
import cn.xfywz.guozespring.entity.vo.DiscussTeacherExportVO;
import cn.xfywz.guozespring.entity.vo.YeeDiscussReplyVo;
import cn.xfywz.guozespring.entity.vo.YeeDiscussVo;
import cn.xfywz.guozespring.excel.ExcelExportUtil;
import cn.xfywz.guozespring.service.teacher.DiscussStatsService;
import cn.xfywz.guozespring.util.CurrentUserUtil;
import cn.xfywz.guozespring.util.PageResult;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.BuiltSql;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DiscussStatsServiceImpl implements DiscussStatsService {

    @Autowired
    private DatabaseUtil databaseUtil;


    @Override
    public Result list(DiscussStatsDTO param) {
        try {
            int pageNum = param.getPageNum() == null || param.getPageNum() < 1 ? 1 : param.getPageNum();
            int pageSize = param.getPageSize() == null || param.getPageSize() < 1 ? 10 : param.getPageSize();
            int offset = (pageNum - 1) * pageSize;

            int statsType = param.getStatsType() == null ? 1 : param.getStatsType();
            if (statsType == 2) {
                return getTeacherStatsList(param, pageSize, offset);
            }
            return getStudentStatsList(param, pageSize, offset);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取老师统计数据列表
     */
    private Result getTeacherStatsList(DiscussStatsDTO param, int pageSize, int offset) {
        int schoolId = (int) param.getSchoolId();

        // 数据 SQL（含分页）
        String dataSql = buildTeacherDataSqlGeneric(param, true, false);
        List<Object> dataParams = buildTeacherDataParamsGeneric(param, true, pageSize, offset);

        List<Map<String, Object>> list = databaseUtil.executeQuery(schoolId,
                BuiltSql.of(dataSql, dataParams),
                rs -> {
                    try {
                        List<Map<String, Object>> result = new ArrayList<>();
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            row.put("teacherId", rs.getLong("teacherId"));
                            row.put("account", rs.getString("account"));
                            row.put("number", rs.getString("account"));
                            row.put("name", rs.getString("name"));
                            row.put("className", rs.getString("className"));
                            row.put("allQty", rs.getInt("allQty"));
                            row.put("postQty", rs.getInt("postQty"));
                            row.put("replyQty", rs.getInt("replyQty"));
                            row.put("likeQty", rs.getInt("likeQty"));
                            result.add(row);
                        }
                        return result;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

        // count SQL：将无分页的通用 SQL 包装为子查询计数
        String baseSql = buildTeacherDataSqlGeneric(param, false, false);
        List<Object> baseParams = buildTeacherDataParamsGeneric(param, false, 0, 0);
        String countSql = "SELECT COUNT(*) FROM (" + baseSql + ") AS _cnt";

        long total = databaseUtil.executeQuery(schoolId,
                BuiltSql.of(countSql, baseParams),
                rs -> {
                    try {
                        return rs.next() ? rs.getLong(1) : 0L;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

        return Result.success(list, total);
    }

    /**
     * 构建老师统计数据SQL（通用）
     * @param withPagination 是否包含 LIMIT/OFFSET
     * @param exportView 是否导出视图（导出不需要 teacherId）
     */
    private String buildTeacherDataSqlGeneric(DiscussStatsDTO param, boolean withPagination, boolean exportView) {
        String likeSubT = buildTeacherLikeSubSql(param);
        String statsSubT = buildTeacherStatsSubSql(param, likeSubT);
        String teacherBase = buildTeacherBaseSql(param);

        StringBuilder sql = new StringBuilder("""
                SELECT
            """);
        if (exportView) {
            sql.append("""
                    ym.account AS account,
                    ym.name AS name,
                    '' AS className,
            """);
        } else {
            sql.append("""
                    ym.id AS teacherId,
                    ym.account AS account,
                    ym.name AS name,
                    '' AS className,
            """);
        }
        sql.append("""
                    IFNULL(s.allQty,0) AS allQty,
                    IFNULL(s.postQty,0) AS postQty,
                    IFNULL(s.replyQty,0) AS replyQty,
                    IFNULL(s.likeQty,0) AS likeQty
                FROM (
            """);
        sql.append(teacherBase);
        sql.append("""
                ) t
                JOIN yee_manage ym ON ym.id = t.teacherId AND ym.schoolId = t.schoolId
                LEFT JOIN (
            """);
        sql.append(statsSubT);
        sql.append("""
                ) s ON s.userId = ym.id AND s.schoolId = ym.schoolId
                WHERE 1=1
                """);

        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            sql.append(" AND (ym.name LIKE ? OR ym.account LIKE ?)");
        }

        sql.append(" ORDER BY s.allQty DESC, ym.id DESC");

        if (withPagination) {
            sql.append(" LIMIT ? OFFSET ?");
        }
        log.debug("Teacher SQL (withPagination={}, exportView={}): {}", withPagination, exportView, sql);
        return sql.toString();
    }

    /**
     * 构建老师基础SQL
     */
    private String buildTeacherBaseSql(DiscussStatsDTO param) {
        return """
            SELECT
                DISTINCT teacherId,
                         schoolId
            FROM yee_course_class
            WHERE schoolId = ? AND courseId = ?
            """ + (param.getClassId() > 0 ? "  AND classId = ?" : "");
    }

    /**
     * 构建老师点赞子查询SQL
     */
    private String buildTeacherLikeSubSql(DiscussStatsDTO param) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                rr.userId,
                rr.schoolId,
                COUNT(*) AS likeQty
            FROM yee_reply_like rl
            INNER JOIN yee_discuss_reply rr ON rr.id = rl.replyId AND rr.isDelete = 0
            WHERE rl.schoolId = ? AND rr.courseId = ?
            """);

        if (param.getClassId() > 0) {
            sql.append(" AND rr.classId = ?");
        }
        if (param.getDiscussId() > 0) {
            sql.append(" AND rr.discussId = ?");
        }
        if (param.getStartTime() != null && param.getEndTime() != null) {
            sql.append(" AND rr.addTime BETWEEN ? AND ?");
        } else if (param.getStartTime() != null) {
            sql.append(" AND rr.addTime >= ?");
        } else if (param.getEndTime() != null) {
            sql.append(" AND rr.addTime <= ?");
        }
        sql.append(" GROUP BY rr.userId, rr.schoolId");

        return sql.toString();
    }

    /**
     * 构建老师统计子查询SQL（导出 + list 通用）
     */
    private String buildTeacherStatsSubSql(DiscussStatsDTO param, String likeSubT) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                r.userId,
                r.schoolId,
                COUNT(*) AS allQty,
                SUM(CASE WHEN IFNULL(r.replyId,0) = 0 THEN 1 ELSE 0 END) AS postQty,
                SUM(CASE WHEN IFNULL(r.replyId,0) <> 0 THEN 1 ELSE 0 END) AS replyQty,
                COALESCE(MAX(l.likeQty), 0) AS likeQty
            FROM yee_discuss_reply r
            LEFT JOIN (""");
        sql.append(likeSubT);
        sql.append("""
            ) l ON l.userId = r.userId AND l.schoolId = r.schoolId
            WHERE r.schoolId = ? AND r.courseId = ? AND r.isDelete = 0
            """);

        if (param.getClassId() > 0) {
            sql.append(" AND r.classId = ?");
        }
        if (param.getDiscussId() > 0) {
            sql.append(" AND r.discussId = ?");
        }
        if (param.getStartTime() != null && param.getEndTime() != null) {
            sql.append(" AND r.addTime BETWEEN ? AND ?");
        } else if (param.getStartTime() != null) {
            sql.append(" AND r.addTime >= ?");
        } else if (param.getEndTime() != null) {
            sql.append(" AND r.addTime <= ?");
        }

        // 保留原来的 IN 结构，导出才不会报错！
        sql.append(" AND r.userId IN (SELECT DISTINCT teacherId FROM yee_course_class WHERE schoolId = ? AND courseId = ?");
        if (param.getClassId() > 0) {
            sql.append(" AND classId = ?");
        }
        sql.append(") GROUP BY r.userId, r.schoolId");

        return sql.toString();
    }

    /**
     * 构建老师统计数据参数（通用，list + export 共用）
     */
    private List<Object> buildTeacherDataParamsGeneric(DiscussStatsDTO param, boolean withPagination, int pageSize, int offset) {
        List<Object> params = new ArrayList<>();
        // 1. teacherBase 参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());
        // 2. likeSubT 参数
        addTeacherLikeSubParams(param, params);
        // 3. statsSubT 参数
        addTeacherStatsSubParams(param, params);
        // 4. 关键词
        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            String kw = "%" + param.getKeyword().trim() + "%";
            params.add(kw);
            params.add(kw);
        }
        // 5. 分页
        if (withPagination) {
            params.add(pageSize);
            params.add(offset);
        }
        return params;
    }

    /**
     * 添加老师点赞子查询参数
     */
    private void addTeacherLikeSubParams(DiscussStatsDTO param, List<Object> params) {
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());
        if (param.getDiscussId() > 0) params.add(param.getDiscussId());
        if (param.getStartTime() != null && param.getEndTime() != null) {
            params.add(new Timestamp(param.getStartTime()));
            params.add(new Timestamp(param.getEndTime()));
        } else if (param.getStartTime() != null) {
            params.add(new Timestamp(param.getStartTime()));
        } else if (param.getEndTime() != null) {
            params.add(new Timestamp(param.getEndTime()));
        }
    }

    /**
     * 添加老师统计子查询参数
     */
    private void addTeacherStatsSubParams(DiscussStatsDTO param, List<Object> params) {
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());
        if (param.getDiscussId() > 0) params.add(param.getDiscussId());
        if (param.getStartTime() != null && param.getEndTime() != null) {
            params.add(new Timestamp(param.getStartTime()));
            params.add(new Timestamp(param.getEndTime()));
        } else if (param.getStartTime() != null) {
            params.add(new Timestamp(param.getStartTime()));
        } else if (param.getEndTime() != null) {
            params.add(new Timestamp(param.getEndTime()));
        }
        // 限定为本课程的任课教师参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());
    }

    /**
     * 获取学生统计数据列表
     */
    private Result getStudentStatsList(DiscussStatsDTO param, int pageSize, int offset) {
        int schoolId = (int) param.getSchoolId();

        // count：独立统计选课学生数（比包装数据 SQL 更高效）
        String countSql = buildStudentCountSql(param);
        List<Object> countParams = buildStudentCountParams(param);
        long total = databaseUtil.executeQuery(schoolId,
                BuiltSql.of(countSql, countParams),
                rs -> {
                    try {
                        return rs.next() ? rs.getLong(1) : 0L;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

        // data
        String dataSql = buildStudentDataSqlGeneric(param, true, false);
        List<Object> dataParams = buildStudentDataParamsGeneric(param, true, pageSize, offset);
        List<Map<String, Object>> list = databaseUtil.executeQuery(schoolId,
                BuiltSql.of(dataSql, dataParams),
                rs -> {
                    try {
                        List<Map<String, Object>> result = new ArrayList<>();
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            row.put("studentId", rs.getLong("studentId"));
                            row.put("number", rs.getString("number"));
                            row.put("name", rs.getString("name"));
                            row.put("className", rs.getString("className"));
                            row.put("allQty", rs.getInt("allQty"));
                            row.put("postQty", rs.getInt("postQty"));
                            row.put("replyQty", rs.getInt("replyQty"));
                            row.put("likeQty", rs.getInt("likeQty"));
                            result.add(row);
                        }
                        return result;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

        return Result.success(list, total);
    }

    /**
     * 构建学生统计数量SQL
     */
    private String buildStudentCountSql(DiscussStatsDTO param) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    COUNT(DISTINCT ycs.studentId)
                FROM yee_course_student ycs
                LEFT JOIN yee_student ys ON ys.id = ycs.studentId AND ys.schoolId = ycs.schoolId
                LEFT JOIN yee_course_class ycc ON ycc.id = ycs.classId AND ycc.schoolId = ycs.schoolId
                WHERE ycs.schoolId = ? AND ycs.courseId = ?
                """);

        if (param.getClassId() > 0) {
            sql.append(" AND ycs.classId = ?");
        }

        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            sql.append(" AND (ys.name LIKE ? OR ys.number LIKE ?)");
        }

        return sql.toString();
    }

    /**
     * 构建学生统计数量参数
     */
    private List<Object> buildStudentCountParams(DiscussStatsDTO param) {
        List<Object> params = new ArrayList<>();
        params.add(param.getSchoolId());
        params.add(param.getCourseId());

        if (param.getClassId() > 0) {
            params.add(param.getClassId());
        }

        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            String kw = "%" + param.getKeyword().trim() + "%";
            params.add(kw);
            params.add(kw);
        }

        return params;
    }

    /**
     * 构建学生统计数据SQL（list 视图）
     */
    private String buildStudentDataSql(DiscussStatsDTO param) {
        return buildStudentDataSqlGeneric(param, true, false);
    }

    /**
     * 构建学生统计数据SQL（通用）
     * @param withPagination 是否包含 LIMIT/OFFSET
     * @param exportView 是否导出视图（导出不需要 studentId）
     */
    private String buildStudentDataSqlGeneric(DiscussStatsDTO param, boolean withPagination, boolean exportView) {
        String likeSub = buildStudentLikeSubSql(param);
        String statsSub = buildStudentStatsSubSql(param, likeSub);

        StringBuilder sql = new StringBuilder("""
                SELECT
            """);
        if (exportView) {
            sql.append("""
                    ys.number AS number,
                    ys.name AS name,
                    IFNULL(ycc.name,'') AS className,
            """);
        } else {
            sql.append("""
                    ys.id AS studentId,
                    ys.number AS number,
                    ys.name AS name,
                    IFNULL(ycc.name,'') AS className,
            """);
        }
        sql.append("""
                    IFNULL(s.allQty,0) AS allQty,
                    IFNULL(s.postQty,0) AS postQty,
                    IFNULL(s.replyQty,0) AS replyQty,
                    IFNULL(s.likeQty,0) AS likeQty
                FROM yee_course_student ycs
                LEFT JOIN yee_student ys ON ys.id = ycs.studentId AND ys.schoolId = ycs.schoolId
                LEFT JOIN yee_course_class ycc ON ycc.id = ycs.classId AND ycc.schoolId = ycs.schoolId
                LEFT JOIN (
                """);
        sql.append(statsSub);
        sql.append("""
                ) s ON s.userId = ys.id AND s.schoolId = ys.schoolId
                WHERE ycs.schoolId = ? AND ycs.courseId = ?
                """);

        if (param.getClassId() > 0) {
            sql.append(" AND ycs.classId = ?");
        }
        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            sql.append(" AND (ys.name LIKE ? OR ys.number LIKE ?)");
        }
        sql.append(" ORDER BY ys.id DESC");
        if (withPagination) {
            sql.append(" LIMIT ? OFFSET ?");
        }
        log.debug("Student SQL (withPagination={}, exportView={}): {}", withPagination, exportView, sql);
        return sql.toString();
    }

    /**
     * 构建学生点赞子查询SQL
     */
    private String buildStudentLikeSubSql(DiscussStatsDTO param) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    rr.userId,
                    rr.schoolId,
                    COUNT(*) AS likeQty
                FROM yee_reply_like rl
                INNER JOIN yee_discuss_reply rr ON rr.id = rl.replyId AND rr.isDelete = 0
                WHERE rl.schoolId = ? AND rr.courseId = ?
                """);

        if (param.getClassId() > 0) {
            sql.append(" AND rr.classId = ?");
        }
        if (param.getDiscussId() > 0) {
            sql.append(" AND rr.discussId = ?");
        }
        // 时间范围过滤（基于被点赞的回复的 addTime）
        if (param.getStartTime() != null && param.getEndTime() != null) {
            sql.append(" AND rr.addTime BETWEEN ? AND ?");
        } else if (param.getStartTime() != null) {
            sql.append(" AND rr.addTime >= ?");
        } else if (param.getEndTime() != null) {
            sql.append(" AND rr.addTime <= ?");
        }
        sql.append(" GROUP BY rr.userId, rr.schoolId");

        return sql.toString();
    }

    /**
     * 构建学生统计子查询SQL（导出 + list 通用）
     */
    private String buildStudentStatsSubSql(DiscussStatsDTO param, String likeSub) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                r.userId,
                r.schoolId,
                COUNT(*) AS allQty,
                SUM(CASE WHEN IFNULL(r.replyId,0) = 0 THEN 1 ELSE 0 END) AS postQty,
                SUM(CASE WHEN IFNULL(r.replyId,0) <> 0 THEN 1 ELSE 0 END) AS replyQty,
                COALESCE(MAX(l.likeQty), 0) AS likeQty
            FROM yee_discuss_reply r
            LEFT JOIN (
            """);
        sql.append(likeSub);
        sql.append("""
            ) l ON l.userId = r.userId AND l.schoolId = r.schoolId
            WHERE r.schoolId = ? AND r.courseId = ? AND r.isDelete = 0
            """);

        if (param.getClassId() > 0) {
            sql.append(" AND r.classId = ?");
        }
        if (param.getDiscussId() > 0) {
            sql.append(" AND r.discussId = ?");
        }
        if (param.getStartTime() != null && param.getEndTime() != null) {
            sql.append(" AND r.addTime BETWEEN ? AND ?");
        } else if (param.getStartTime() != null) {
            sql.append(" AND r.addTime >= ?");
        } else if (param.getEndTime() != null) {
            sql.append(" AND r.addTime <= ?");
        }
        sql.append(" GROUP BY r.userId, r.schoolId");

        return sql.toString();
    }

    /**
     * 构建学生统计数据参数（通用，list + export 共用）
     */
    private List<Object> buildStudentDataParamsGeneric(DiscussStatsDTO param, boolean withPagination, int pageSize, int offset) {
        List<Object> params = new ArrayList<>();
        // likeSub 参数
        addStudentLikeSubParams(param, params);
        // statsSub 参数
        addStudentStatsSubParams(param, params);
        // 外层 where 参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());
        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            String kw = "%" + param.getKeyword().trim() + "%";
            params.add(kw);
            params.add(kw);
        }
        // 分页参数
        if (withPagination) {
            params.add(pageSize);
            params.add(offset);
        }
        return params;
    }

    /**
     * 添加学生点赞子查询参数
     */
    private void addStudentLikeSubParams(DiscussStatsDTO param, List<Object> params) {
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());
        if (param.getDiscussId() > 0) params.add(param.getDiscussId());
        if (param.getStartTime() != null && param.getEndTime() != null) {
            params.add(new Timestamp(param.getStartTime()));
            params.add(new Timestamp(param.getEndTime()));
        } else if (param.getStartTime() != null) {
            params.add(new Timestamp(param.getStartTime()));
        } else if (param.getEndTime() != null) {
            params.add(new Timestamp(param.getEndTime()));
        }
    }

    /**
     * 添加学生统计子查询参数
     */
    private void addStudentStatsSubParams(DiscussStatsDTO param, List<Object> params) {
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());
        if (param.getDiscussId() > 0) params.add(param.getDiscussId());
        if (param.getStartTime() != null && param.getEndTime() != null) {
            params.add(new Timestamp(param.getStartTime()));
            params.add(new Timestamp(param.getEndTime()));
        } else if (param.getStartTime() != null) {
            params.add(new Timestamp(param.getStartTime()));
        } else if (param.getEndTime() != null) {
            params.add(new Timestamp(param.getEndTime()));
        }
    }

    // 讨论详情列表（按用户维度展示其发表或回复）
    @Override
    public Result detailList(DiscussStatsDTO param) {
        try {
            if (param.getUserId() <= 0) {
                return Result.error("缺少目标用户ID");
            }
            int pageNum = (param.getPageNum() == null || param.getPageNum() < 1) ? 1 : param.getPageNum();
            int pageSize = (param.getPageSize() == null || param.getPageSize() < 1) ? 10 : param.getPageSize();
            int offset = (pageNum - 1) * pageSize;
            int listType = (param.getListType() == null || param.getListType() < 1 || param.getListType() > 2) ? 1 : param.getListType();

            return getUserDiscussDetailList(param, listType, pageSize, offset);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户讨论详情列表
     */
    private Result getUserDiscussDetailList(DiscussStatsDTO param, int listType, int pageSize, int offset) {
        int schoolId = (int) param.getSchoolId();

        // count
        String countSql = buildUserDiscussDetailCountSql(param, listType);
        List<Object> countParams = buildUserDiscussDetailCountParams(param, listType);
        long total = databaseUtil.executeQuery(schoolId,
                BuiltSql.of(countSql, countParams),
                rs -> {
                    try {
                        return rs.next() ? rs.getLong(1) : 0L;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

        // data
        String dataSql = buildUserDiscussDetailDataSql(param, listType);
        List<Object> dataParams = buildUserDiscussDetailDataParams(param, listType, pageSize, offset);
        List<Map<String, Object>> list = databaseUtil.executeQuery(schoolId,
                BuiltSql.of(dataSql, dataParams),
                rs -> {
                    try {
                        List<Map<String, Object>> result = new ArrayList<>();
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            row.put("replyId", rs.getLong("replyId"));
                            row.put("discussId", rs.getLong("discussId"));
                            row.put("discussTitle", rs.getString("discussTitle"));
                            row.put("content", rs.getString("content"));
                            row.put("addTime", rs.getTimestamp("addTime"));
                            row.put("images", rs.getString("images"));
                            row.put("files", rs.getString("files"));
                            row.put("likeQty", rs.getInt("likeQty"));
                            row.put("replyQty", rs.getInt("replyQty"));
                            result.add(row);
                        }
                        return result;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });

        return Result.success(list, total);
    }

    /**
     * 构建用户讨论详情统计数量SQL
     */
    private String buildUserDiscussDetailCountSql(DiscussStatsDTO param, int listType) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                COUNT(*)
            FROM yee_discuss_reply r
            JOIN yee_discuss d ON d.id = r.discussId AND d.schoolId = r.schoolId
            WHERE r.schoolId = ? AND r.courseId = ? AND r.userId = ? AND r.isDelete = 0
            """);

        // 仅保留 discussId 作为可选条件
        if (param.getDiscussId() > 0) {
            sql.append(" AND r.discussId = ?");
        }

        if (listType == 1) {
            sql.append(" AND IFNULL(r.replyId,0) = 0"); // 发表（顶层）
        } else {
            sql.append(" AND IFNULL(r.replyId,0) <> 0"); // 回复
        }

        return sql.toString();
    }

    /**
     * 构建用户讨论详情统计数量参数
     */
    private List<Object> buildUserDiscussDetailCountParams(DiscussStatsDTO param, int listType) {
        List<Object> params = new ArrayList<>();
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        params.add(param.getUserId());

        // 仅添加 discussId（如果有效）
        if (param.getDiscussId() > 0) {
            params.add(param.getDiscussId());
        }

        return params;
    }

    /**
     * 构建用户讨论详情数据SQL
     */
    private String buildUserDiscussDetailDataSql(DiscussStatsDTO param, int listType) {
        String likeSub = buildLikeSubQuery();
        String replyCountSub = buildReplyCountSubQuery();

        StringBuilder sql = new StringBuilder("""
        SELECT
            r.id AS replyId,
            r.discussId,
            d.title AS discussTitle,
            r.content,
            r.addTime,
            r.images,
            r.files,
            IFNULL(l.likeQty,0) AS likeQty,
            IFNULL(rc.replyQty,0) AS replyQty
        FROM yee_discuss_reply r
        JOIN yee_discuss d ON d.id = r.discussId AND d.schoolId = r.schoolId
        LEFT JOIN (
        """);
        sql.append(likeSub);
        sql.append("""
        ) l ON l.replyId = r.id AND l.schoolId = r.schoolId
        LEFT JOIN (
        """);
        sql.append(replyCountSub);
        sql.append("""
        ) rc ON rc.replyId = r.id AND rc.schoolId = r.schoolId
        WHERE r.schoolId = ? AND r.courseId = ? AND r.userId = ? AND r.isDelete = 0""");

        // 加上 discussId 条件
        if (param.getDiscussId() > 0) {
            sql.append(" AND r.discussId = ?");
        }

        if (listType == 1) {
            sql.append(" AND IFNULL(r.replyId,0) = 0");
        } else {
            sql.append(" AND IFNULL(r.replyId,0) <> 0");
        }

        sql.append(" ORDER BY r.addTime DESC, r.id DESC LIMIT ? OFFSET ?");

        return sql.toString();
    }

    /**
     * 构建用户讨论详情数据参数
     */
    private List<Object> buildUserDiscussDetailDataParams(DiscussStatsDTO param, int listType, int pageSize, int offset) {
        List<Object> params = new ArrayList<>();

        // likeSub 参数（schoolId）
        params.add(param.getSchoolId());

        // 主查询基础参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        params.add(param.getUserId());

        // 可选：discussId
        if (param.getDiscussId() > 0) {
            params.add(param.getDiscussId());
        }

        // 分页
        params.add(pageSize);
        params.add(offset);

        return params;
    }

    /**
     * 构建点赞子查询SQL（可复用）
     */
    private String buildLikeSubQuery() {
        return """
            SELECT
                rl.replyId,
                rl.schoolId,
                COUNT(*) AS likeQty
            FROM yee_reply_like rl
            WHERE rl.schoolId = ?
            GROUP BY rl.replyId, rl.schoolId
            """;
    }

    /**
     * 构建回复数子查询SQL（可复用）
     */
    private String buildReplyCountSubQuery() {
        return """
            SELECT
                rr.replyId,
                rr.schoolId,
                COUNT(*) AS replyQty
            FROM yee_discuss_reply rr
            WHERE rr.isDelete = 0
            GROUP BY rr.replyId, rr.schoolId
            """;
    }

    @Override
    public Result discussDetail(int schoolId, long discussId, int pageNum, int pageSize) {
        try {

            // ==========  分页参数处理 ==========
            pageNum = Math.max(pageNum, 1);
            pageSize = Math.min(Math.max(pageSize, 10), 20);

            // ========== 3. 从请求上下文获取当前用户信息（由 AuthAspect 鉴权时存入） ==========
            JSONObject subjectJson = CurrentUserUtil.getCurrentUserSubject();
            Long currentUserId = CurrentUserUtil.getCurrentUserId();
            if (currentUserId == null || currentUserId <= 0) {
                return Result.error("无法识别当前用户身份");
            }

            // ========== 4. 使用 QueryBuilder 查询讨论主体 ==========
            YeeDiscussVo discuss = databaseUtil.query(schoolId)
                    .sql("""
            SELECT
                d.id, d.courseId, d.title, d.content, d.addTime, d.teacherId,
                d.images, d.files,
                c.name AS courseName,
                m.name AS teacherName, m.avatar AS teacherAvatar
            FROM yee_discuss d
            LEFT JOIN yee_course c ON d.courseId = c.id
            LEFT JOIN yee_manage m ON d.teacherId = m.id AND d.schoolId = m.schoolId
            WHERE d.id = ? AND d.isDelete = 0
            """)
                    .params(discussId)
                    .single(rs -> {
                        YeeDiscussVo vo = new YeeDiscussVo();
                        vo.setId(rs.getLong("id"));
                        vo.setCourseId(rs.getInt("courseId"));
                        vo.setTitle(rs.getString("title"));
                        vo.setContent(rs.getString("content"));
                        vo.setAddTime(rs.getTimestamp("addTime"));
                        vo.setTeacherId(rs.getInt("teacherId"));
                        vo.setImages(rs.getString("images"));
                        vo.setFiles(rs.getString("files"));
                        vo.setCourseName(rs.getString("courseName"));
                        vo.setTeacherName(rs.getString("teacherName"));
                        vo.setTeacherAvatar(rs.getString("teacherAvatar"));
                        return vo;
                    })
                    .orElse(null);

            if (discuss == null) {
                return Result.error("讨论主题不存在或已被删除");
            }
            if (discuss.getCourseId() == 0) {
                discuss.setCourseName("未分类课程");
            }

            // ========== 5. 填充当前用户信息到讨论对象 ==========
            fillCurrentUserInfo(discuss, subjectJson, schoolId);

            // ======================= 核心优化：强制锁定索引 + JOIN聚合替换行内COUNT =======================
            QueryBuilder commentQuery = databaseUtil.query(schoolId)
                    .sql("""
            SELECT
                r.id, r.discussId, r.userId, r.reUserId, r.content, r.pid,
                r.platform, r.addTime, r.replyId, r.images, r.files, r.courseId,
                IFNULL(l.like_count,0) AS like_count,
                IFNULL(rp.reply_count,0) AS reply_count
            FROM yee_discuss_reply r USE INDEX (idx_school_discuss_pid_del_sort)
            LEFT JOIN (
                SELECT schoolId,replyId,COUNT(*) AS like_count
                FROM yee_reply_like
                GROUP BY schoolId,replyId
            ) l ON l.schoolId = r.schoolId AND l.replyId = r.id
            LEFT JOIN (
                SELECT schoolId,pid,COUNT(*) AS reply_count
                FROM yee_discuss_reply
                WHERE isDelete = 0
                GROUP BY schoolId,pid
            ) rp ON rp.schoolId = r.schoolId AND rp.pid = r.id
            WHERE r.schoolId = ? AND r.discussId = ? AND r.isDelete = 0 AND (r.pid IS NULL OR r.pid = 0)
            """)
                    // 参数顺序：schoolId 在前，discussId在后
                    .params(schoolId, discussId)
                    .orderBy("r.addTime DESC, r.id DESC");
            // ==========================================================================================

            // 分页查询总数+列表
            PageResult<YeeDiscussReplyVo> pageResult = commentQuery
                    .page(YeeDiscussReplyVo::mapDiscussReplyVo, pageNum, pageSize);

            List<YeeDiscussReplyVo> comments = pageResult.getRows();
            long total = pageResult.getTotal();

            // ========== 7. 收集用户ID、回复ID批量查询 ==========
            Set<Long> allUserIds = new HashSet<>();
            Set<Long> allReplyIds = new HashSet<>();
            for (YeeDiscussReplyVo c : comments) {
                allUserIds.add((long) c.getUserId());
                allReplyIds.add(c.getId());
            }

            // ========== 8. 批量查询二级子回复 ==========
            Map<Long, List<YeeDiscussReplyVo>> repliesMap = new HashMap<>();
            if (!comments.isEmpty()) {
                List<Long> parentIds = comments.stream().map(YeeDiscussReplyVo::getId).toList();
                String placeholders = parentIds.stream().map(id -> "?").collect(Collectors.joining(","));

                List<YeeDiscussReplyVo> allSubReplies = databaseUtil.query(schoolId)
                        .sql(String.format("""
                SELECT
                    r.id, r.discussId, r.userId, r.reUserId, r.content, r.pid,
                    r.platform, r.addTime, r.replyId, r.images, r.files, r.courseId,
                    IFNULL(l.like_count,0) AS like_count,
                    0 AS reply_count
                FROM yee_discuss_reply r
                LEFT JOIN (
                    SELECT schoolId,replyId,COUNT(*) like_count
                    FROM yee_reply_like
                    GROUP BY schoolId,replyId
                ) l ON l.schoolId=r.schoolId AND l.replyId=r.id
                WHERE r.schoolId = ? AND r.pid IN (%s) AND r.isDelete = 0
                ORDER BY r.addTime ASC
                LIMIT 5
                """, placeholders))
                        .params(schoolId)
                        .params(parentIds.toArray())
                        .list(YeeDiscussReplyVo::mapDiscussReplyVo);

                // 子回复按父ID分组
                for (YeeDiscussReplyVo reply : allSubReplies) {
                    repliesMap.computeIfAbsent((long) reply.getPid(), k -> new ArrayList<>()).add(reply);
                    allUserIds.add((long) reply.getUserId());
                    allReplyIds.add(reply.getId());
                }
            }

            // ========== 9. 批量查询学生、教师名称头像 ==========
            Map<Long, String> userNameMap = new HashMap<>();
            Map<Long, String> userAvatarMap = new HashMap<>();
            if (!allUserIds.isEmpty()) {
                String idsPlaceholder = allUserIds.stream().map(id -> "?").collect(Collectors.joining(","));

                // 查学生
                databaseUtil.query(schoolId)
                        .sql("SELECT id, name, avatar FROM yee_student WHERE id IN (" + idsPlaceholder + ")")
                        .params(allUserIds.toArray())
                        .forEach(rs -> {
                            long id = rs.getLong("id");
                            userNameMap.put(id, rs.getString("name"));
                            userAvatarMap.put(id, rs.getString("avatar"));
                        });

                // 查教师
                databaseUtil.query(schoolId)
                        .sql("SELECT id, name, avatar FROM yee_manage WHERE id IN (" + idsPlaceholder + ")")
                        .params(allUserIds.toArray())
                        .forEach(rs -> {
                            long id = rs.getLong("id");
                            userNameMap.putIfAbsent(id, rs.getString("name"));
                            userAvatarMap.putIfAbsent(id, rs.getString("avatar"));
                        });
            }

            // ========== 10. 查询当前登录用户点赞标记 ==========
            Set<Long> likedReplyIds = new HashSet<>();
            if (!allReplyIds.isEmpty()) {
                String replyIdsPlaceholder = allReplyIds.stream().map(id -> "?").collect(Collectors.joining(","));
                databaseUtil.query(schoolId)
                        .sql("SELECT replyId FROM yee_reply_like WHERE userId = ? AND replyId IN (" + replyIdsPlaceholder + ")")
                        .params(currentUserId)
                        .params(allReplyIds.toArray())
                        .forEach(rs -> likedReplyIds.add(rs.getLong("replyId")));
            }

            // ========== 11. 填充VO展示数据 ==========
            for (YeeDiscussReplyVo comment : comments) {
                Long uid = (long) comment.getUserId();
                comment.setUserName(userNameMap.getOrDefault(uid, "用户已注销"));
                comment.setUserAvatar(userAvatarMap.getOrDefault(uid, ""));
                comment.setLiked(likedReplyIds.contains(comment.getId()));

                List<YeeDiscussReplyVo> subReplies = repliesMap.getOrDefault(comment.getId(), Collections.emptyList());
                for (YeeDiscussReplyVo reply : subReplies) {
                    Long ruid = (long) reply.getUserId();
                    reply.setUserName(userNameMap.getOrDefault(ruid, "用户已注销"));
                    reply.setUserAvatar(userAvatarMap.getOrDefault(ruid, ""));
                    reply.setLiked(likedReplyIds.contains(reply.getId()));
                }
                comment.setReplies(subReplies);
            }

            // ========== 12. 组装返回结果 ==========
            Map<String, Object> result = new HashMap<>();
            result.put("discuss", discuss);
            result.put("comments", comments);
            return Result.success(result, total);

        } catch (Exception e) {
            log.error("查询讨论详情失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

// ========== 辅助方法 ==========

    /**
     * 从 JWT subject 中提取用户ID
     */
    private Long extractUserId(JSONObject subjectJson) {
        if (subjectJson.containsKey("classId")) {
            return subjectJson.getLong("id");
        } else {
            JSONObject manage = subjectJson.getJSONObject("yeeManage");
            return Objects.requireNonNullElse(manage, subjectJson).getLong("id");
        }
    }

    /**
     * 填充当前用户信息（原逻辑，可根据实际需求调整）
     */
    private void fillCurrentUserInfo(YeeDiscussVo discuss, JSONObject subjectJson, int schoolId) {
        String userName = "未知用户";
        String userAvatar = "";
        Long currentUserId = extractUserId(subjectJson);

        if (subjectJson.containsKey("classId")) {
            // 学生身份，需要查库
            Optional<Map<String, String>> userInfo = databaseUtil.query(schoolId)
                    .sql("SELECT IFNULL(name, '') AS name, IFNULL(avatar, '') AS avatar FROM yee_student WHERE id = ?")
                    .params(currentUserId)
                    .single(rs -> Map.of("name", rs.getString("name"), "avatar", rs.getString("avatar")));
            if (userInfo.isPresent()) {
                userName = userInfo.get().get("name");
                userAvatar = userInfo.get().get("avatar");
            }
        } else {
            userName = subjectJson.getString("name");
            userAvatar = subjectJson.getString("avatar");
            if (userName == null) userName = "未知用户";
            if (userAvatar == null) userAvatar = "";
        }
        discuss.setUserName(userName);
        discuss.setUserAvatar(userAvatar);
    }


    @Override
    public void exportData(DiscussStatsDTO param, HttpServletResponse response) {
        try {
            int schoolId = (int) param.getSchoolId();
            int statsType = param.getStatsType() == null ? 1 : param.getStatsType();

            if (statsType == 2) {
                String sql = buildTeacherDataSqlGeneric(param, false, true);
                List<Object> params = buildTeacherDataParamsGeneric(param, false, 0, 0);
                List<DiscussTeacherExportVO> exportList = databaseUtil.executeQuery(schoolId,
                        BuiltSql.of(sql, params),
                        rs -> {
                            List<DiscussTeacherExportVO> result = new ArrayList<>();
                            while (true) {
                                try {
                                    if (!rs.next()) break;
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                                result.add(DiscussTeacherExportVO.fromResultSet(rs));
                            }
                            return result;
                        });
                ExcelExportUtil.exportWithPreprocess(exportList, response, DiscussTeacherExportVO.class);
            } else {
                String sql = buildStudentDataSqlGeneric(param, false, true);
                List<Object> params = buildStudentDataParamsGeneric(param, false, 0, 0);
                List<DiscussStudentExportVO> exportList = databaseUtil.executeQuery(schoolId,
                        BuiltSql.of(sql, params),
                        rs -> {
                            List<DiscussStudentExportVO> result = new ArrayList<>();
                            while (true) {
                                try {
                                    if (!rs.next()) break;
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                                result.add(DiscussStudentExportVO.fromResultSet(rs));
                            }
                            return result;
                        });
                ExcelExportUtil.exportWithPreprocess(exportList, response, DiscussStudentExportVO.class);
            }
        } catch (Exception e) {
            log.error("导出讨论统计失败", e);
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("导出失败:" + e.getMessage());
            } catch (Exception ignored) {}
        }
    }
}
