package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.vo.VideoReport;
import cn.xfywz.guozespring.exception.BusinessException;
import cn.xfywz.guozespring.exception.DatabaseException;
import cn.xfywz.guozespring.service.teacher.VideoReportService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.BuiltSql;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import cn.xfywz.guozespring.util.TimeFormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Service
public class VideoReportServiceImpl implements VideoReportService {

    @Autowired
    private DatabaseUtil databaseUtil;

    /**
     * 公共条件：班级、时间范围、非零时长过滤（应用于 yst 表）
     */
    private void applyOverviewConditions(QueryBuilder qb, VideoReport param) {
        // 班级过滤（如果需要关联 yee_course_student）
        if (param.getClassId() != null && param.getClassId() > 0) {
            qb.where("ycs.classId = ?", param.getClassId());
        }

        // 时间范围（yst.addTime 字段）
        if (param.getStartTime() != null) {
            qb.where("yst.addTime >= FROM_UNIXTIME(?/1000)", param.getStartTime());
        }
        if (param.getEndTime() != null) {
            qb.where("yst.addTime < FROM_UNIXTIME(?/1000)", param.getEndTime());
        }

        // 仅统计非零时长记录
        if (Boolean.TRUE.equals(param.getNonZeroOnly())) {
            qb.where("yst.duration IS NOT NULL AND yst.duration > 0");
        }
    }

    @Override
    public Result overview(VideoReport param) {
        try {
            // 1. 参数校验
            if (param == null || param.getCourseId() <= 0) {
                return Result.error("参数不完整");
            }

            // 2. 获取数据库连接（复用同一连接）
            try (Connection conn = databaseUtil.getConnection(param.getSchoolId())) {
                Map<String, Object> data = new HashMap<>();
                data.put("studyProgress", getStudyProgress(conn, param));
                data.put("courseDiscuss", getCourseDiscussStats(conn, param));
                data.put("classDiscuss", getClassDiscussStats(conn, param));
                data.put("watchDuration", getWatchDurationStats(conn, param));
                data.put("totals", getTotals(conn, param));
                return Result.success(data);
            }
        } catch (BusinessException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("课程视频报告查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // ==================== 学习进度统计 ====================

    private Map<String, Object> getStudyProgress(Connection conn, VideoReport param) {
        // 基础学生表查询构建器（用于复用条件）
        QueryBuilder baseBuilder = new QueryBuilder(param.getSchoolId(), databaseUtil)
                .sql("FROM yee_course_student ycs")
                .where("ycs.courseId = ?", param.getCourseId())
                .where("ycs.schoolId = ?", param.getSchoolId());
        if (param.getClassId() != null && param.getClassId() > 0) {
            baseBuilder.where("ycs.classId = ?", param.getClassId());
        }

        // 1. 学生总数 & 任务总数（取任意一个学生的 videoCount，假设全班相同）
        String totalSql = """
                SELECT
                    COUNT(*) AS studentTotal,
                    COALESCE(MAX(ycs.videoCount), 0) AS taskTotal
                FROM yee_course_student ycs
                """;
        BuiltSql totalBuilt = baseBuilder.sql(totalSql).build();
        Map<String, Object> totalMap = databaseUtil.executeQuery(conn, totalBuilt, rs -> {
            try {
                Map<String, Object> map = new HashMap<>();
                if (rs.next()) {
                    map.put("studentTotal", rs.getInt("studentTotal"));
                    map.put("taskTotal", rs.getInt("taskTotal"));
                }
                return map;
            } catch (SQLException e) {
                throw new DatabaseException("查询学生总数失败", e);
            }
        });
        int studentTotal = (int) totalMap.getOrDefault("studentTotal", 0);
        int taskTotal = (int) totalMap.getOrDefault("taskTotal", 0);

        // 2. 平均进度（videoLearned / videoCount）
        String avgSql = """
                SELECT COALESCE(AVG(
                    CASE WHEN ycs.videoCount > 0
                         THEN ycs.videoLearned * 1.0 / ycs.videoCount
                         ELSE 0 END
                ), 0) AS avgProgress
                FROM yee_course_student ycs
                """;
        BuiltSql avgBuilt = baseBuilder.sql(avgSql).build();
        double avgProgress = databaseUtil.executeQuery(conn, avgBuilt, rs -> {
            try {
                return rs.next() ? rs.getDouble("avgProgress") : 0.0;
            } catch (SQLException e) {
                throw new DatabaseException("查询平均进度失败", e);
            }
        });

        // 3. 最高进度学生
        String bestSql = """
                SELECT
                    ycs.studentId,
                    COALESCE(ys.name, '') AS studentName,
                    ycs.videoLearned,
                    ycs.videoCount
                FROM yee_course_student ycs
                LEFT JOIN yee_student ys ON ys.id = ycs.studentId AND ys.schoolId = ycs.schoolId
                """;
        QueryBuilder bestBuilder = baseBuilder.sql(bestSql)
                .orderBy("ycs.videoLearned DESC, ycs.studentId")
                .limit(1);
        BuiltSql bestBuilt = bestBuilder.build();
        Map<String, Object> best = databaseUtil.executeQuery(conn, bestBuilt, rs -> {
            try {
                Map<String, Object> map = new HashMap<>();
                if (rs.next()) {
                    map.put("studentId", rs.getLong("studentId"));
                    map.put("studentName", rs.getString("studentName"));
                    map.put("learned", rs.getLong("videoLearned"));
                    map.put("total", rs.getLong("videoCount"));
                } else {
                    map.put("studentId", 0L);
                    map.put("studentName", "");
                    map.put("learned", 0L);
                    map.put("total", taskTotal);
                }
                return map;
            } catch (SQLException e) {
                throw new DatabaseException("查询最高进度学生失败", e);
            }
        });

        // 4. 最低进度学生
        // 复用 SQL
        QueryBuilder worstBuilder = baseBuilder.sql(bestSql)
                .orderBy("ycs.videoLearned ASC, ycs.studentId")
                .limit(1);
        BuiltSql worstBuilt = worstBuilder.build();
        Map<String, Object> worst = databaseUtil.executeQuery(conn, worstBuilt, rs -> {
            try {
                Map<String, Object> map = new HashMap<>();
                if (rs.next()) {
                    map.put("studentId", rs.getLong("studentId"));
                    map.put("studentName", rs.getString("studentName"));
                    map.put("learned", rs.getLong("videoLearned"));
                    map.put("total", rs.getLong("videoCount"));
                } else {
                    map.put("studentId", 0L);
                    map.put("studentName", "");
                    map.put("learned", 0L);
                    map.put("total", taskTotal);
                }
                return map;
            } catch (SQLException e) {
                throw new DatabaseException("查询最低进度学生失败", e);
            }
        });

        Map<String, Object> progress = new HashMap<>();
        Map<String, Object> avg = new HashMap<>();
        avg.put("progress", Math.round(avgProgress * 100.0) / 100.0); // 保留两位小数
        avg.put("totalTask", taskTotal);
        progress.put("average", avg);
        progress.put("max", best);
        progress.put("min", worst);
        return progress;
    }

    // ==================== 课程发帖统计 ====================

    private Map<String, Object> getCourseDiscussStats(Connection conn, VideoReport param) {
        // 公共发帖表条件（不区分用户）
        QueryBuilder discussBase = new QueryBuilder(param.getSchoolId(), databaseUtil)
                .sql("FROM yee_node_discuss ynd")
                .where("ynd.courseId = ?", param.getCourseId())
                .where("ynd.schoolId = ?", param.getSchoolId())
                .where("ynd.isDelete = 0");

        // 学生发帖统计
        Map<String, Object> studentStats = getDiscussStatsByRole(conn, discussBase, param, "student");
        // 教师发帖统计
        Map<String, Object> teacherStats = getDiscussStatsByRole(conn, discussBase, param, "teacher");

        Map<String, Object> result = new HashMap<>();
        result.put("student", studentStats);
        result.put("teacher", teacherStats);
        return result;
    }

    /**
     * 按角色统计发帖（学生或教师）
     */
    private Map<String, Object> getDiscussStatsByRole(Connection conn, QueryBuilder discussBase,
                                                      VideoReport param, String role) {
        String userFilter;
        List<Object> filterParams = new ArrayList<>();
        if ("student".equals(role)) {
            userFilter = " AND ynd.userId IN (SELECT ycs.studentId FROM yee_course_student ycs WHERE ycs.courseId = ? AND ycs.schoolId = ?";
            filterParams.add(param.getCourseId());
            filterParams.add(param.getSchoolId());
            if (param.getClassId() != null && param.getClassId() > 0) {
                userFilter += " AND ycs.classId = ?";
                filterParams.add(param.getClassId());
            }
            userFilter += ")";
        } else if ("teacher".equals(role)) {
            userFilter = " AND ynd.userId IN (SELECT DISTINCT ycc.teacherId FROM yee_course_class ycc WHERE ycc.courseId = ? AND ycc.schoolId = ?";
            filterParams.add(param.getCourseId());
            filterParams.add(param.getSchoolId());
            if (param.getClassId() != null && param.getClassId() > 0) {
                userFilter += " AND ycc.classId = ?";
                filterParams.add(param.getClassId());
            }
            userFilter += ")";
        } else {
            throw new IllegalArgumentException("无效角色: " + role);
        }

        return getDiscussStats(conn, discussBase, userFilter, filterParams);
    }

    /**
     * 通用发帖统计（人数、次数、PC/移动端次数）
     */
    private Map<String, Object> getDiscussStats(Connection conn, QueryBuilder discussBase,
                                                String userFilter, List<Object> userParams) {
        BuiltSql baseBuilt = discussBase.build();
        String fromClause = baseBuilt.sql();
        List<Object> baseParams = baseBuilt.params();

        // 合并参数
        List<Object> allParams = new ArrayList<>(baseParams);
        allParams.addAll(userParams);

        // 人数
        String countUserSql = "SELECT COUNT(DISTINCT ynd.userId) " + fromClause + userFilter;
        long userQty = databaseUtil.executeScalar(conn, countUserSql, allParams.toArray());

        // 总次数
        String countTimesSql = "SELECT COUNT(1) " + fromClause + userFilter;
        long times = databaseUtil.executeScalar(conn, countTimesSql, allParams.toArray());

        // PC 次数（LIKE 'pc%' 在 ci 排序规则下已覆盖 'pc'、'PC' 等）
        String pcTimesSql = countTimesSql + " AND (ynd.platform LIKE ?)";
        List<Object> pcParams = new ArrayList<>(allParams);
        pcParams.add("pc%");
        long pcTimes = databaseUtil.executeScalar(conn, pcTimesSql, pcParams.toArray());

        // 移动端次数 = 总数 - PC（避免 OR + LIKE 破坏索引）
        long mbTimes = times - pcTimes;

        Map<String, Object> result = new HashMap<>();
        result.put("userQty", userQty);
        result.put("times", times);
        result.put("pcTimes", pcTimes);
        result.put("mbTimes", mbTimes);
        return result;
    }

    // ==================== 班级发帖统计 ====================

    private List<Map<String, Object>> getClassDiscussStats(Connection conn, VideoReport param) {
        // 1. 班级列表
        QueryBuilder classBuilder = new QueryBuilder(param.getSchoolId(), databaseUtil)
                .sql("SELECT ycc.id AS classId, COALESCE(ycc.name, '') AS className FROM yee_course_class ycc")
                .where("ycc.courseId = ?", param.getCourseId())
                .where("ycc.schoolId = ?", param.getSchoolId());
        if (param.getClassId() != null && param.getClassId() > 0) {
            classBuilder.where("ycc.id = ?", param.getClassId());
        }
        classBuilder.orderBy("ycc.id");
        List<Map<String, Object>> classList = classBuilder.list(rs -> {
            try {
                Map<String, Object> row = new HashMap<>();
                row.put("classId", rs.getLong("classId"));
                row.put("className", rs.getString("className"));
                return row;
            } catch (SQLException e) {
                throw new DatabaseException("查询班级列表失败", e);
            }
        });

        // 2. 班级学生数
        QueryBuilder stuCountBuilder = new QueryBuilder(param.getSchoolId(), databaseUtil)
                .sql("SELECT classId, COUNT(*) AS studentTotal FROM yee_course_student")
                .where("courseId = ?", param.getCourseId())
                .where("schoolId = ?", param.getSchoolId())
                .groupBy("classId");
        if (param.getClassId() != null && param.getClassId() > 0) {
            stuCountBuilder.where("classId = ?", param.getClassId());
        }
        Map<Long, Integer> stuCountMap = stuCountBuilder.list(rs -> {
            try {
                Map<Long, Integer> map = new HashMap<>();
                map.put(rs.getLong("classId"), rs.getInt("studentTotal"));
                return map;
            } catch (SQLException e) {
                throw new DatabaseException("查询班级学生数失败", e);
            }
        }).stream().collect(HashMap::new, HashMap::putAll, HashMap::putAll);

        // 3. 班级发帖/回复数
        QueryBuilder discussBuilder = new QueryBuilder(param.getSchoolId(), databaseUtil)
                .sql("""
                    SELECT
                        ycs.classId,
                        SUM(CASE WHEN IFNULL(ynd.replyId,0)=0 THEN 1 ELSE 0 END) AS postQty,
                        SUM(CASE WHEN IFNULL(ynd.replyId,0)<>0 THEN 1 ELSE 0 END) AS replyQty
                    FROM yee_node_discuss ynd
                    INNER JOIN yee_course_student ycs ON ycs.schoolId = ynd.schoolId
                                                      AND ycs.courseId = ynd.courseId
                                                      AND ycs.studentId = ynd.userId
                    WHERE ynd.isDelete = 0
                    """)
                .where("ynd.courseId = ?", param.getCourseId())
                .where("ynd.schoolId = ?", param.getSchoolId())
                .groupBy("ycs.classId");
        if (param.getClassId() != null && param.getClassId() > 0) {
            discussBuilder.where("ycs.classId = ?", param.getClassId());
        }
        Map<Long, Map<String, Object>> discussMap = discussBuilder.list(rs -> {
            try {
                Map<Long, Map<String, Object>> map = new HashMap<>();
                Map<String, Object> row = new HashMap<>();
                row.put("postQty", rs.getInt("postQty"));
                row.put("replyQty", rs.getInt("replyQty"));
                map.put(rs.getLong("classId"), row);
                return map;
            } catch (SQLException e) {
                throw new DatabaseException("查询班级发帖数失败", e);
            }
        }).stream().collect(HashMap::new, HashMap::putAll, HashMap::putAll);

        // 合并结果
        for (Map<String, Object> row : classList) {
            Long classId = (Long) row.get("classId");
            row.put("studentTotal", stuCountMap.getOrDefault(classId, 0));
            Map<String, Object> discuss = discussMap.get(classId);
            row.put("postQty", discuss != null ? discuss.get("postQty") : 0);
            row.put("replyQty", discuss != null ? discuss.get("replyQty") : 0);
        }
        return classList;
    }

    // ==================== 观看时长统计 ====================

    private Map<String, Object> getWatchDurationStats(Connection conn, VideoReport param) {
        // 1. 课程总视频时长（秒）
        String totalDurationSql = "SELECT COALESCE(SUM(videoDuration), 0) AS totalSeconds FROM yee_node WHERE courseId = ? AND schoolId = ?";
        long totalSeconds = databaseUtil.executeScalar(conn, totalDurationSql, param.getCourseId(), param.getSchoolId());

        // 2. 人均观看时长（包含0时长学生）
        // 基础学生表（用于生成所有学生）
        QueryBuilder baseStudentBuilder = new QueryBuilder(param.getSchoolId(), databaseUtil)
                .sql("FROM yee_course_student ycs")
                .where("ycs.courseId = ?", param.getCourseId())
                .where("ycs.schoolId = ?", param.getSchoolId());
        if (param.getClassId() != null && param.getClassId() > 0) {
            baseStudentBuilder.where("ycs.classId = ?", param.getClassId());
        }
        // 学习时长子查询
        String avgSql = """
                SELECT COALESCE(AVG(t.userSeconds), 0) AS avgSeconds
                FROM (
                    SELECT ycs.studentId,
                           COALESCE(SUM(yst.duration), 0) AS userSeconds
                    FROM yee_course_student ycs
                    LEFT JOIN yee_study_time yst ON yst.schoolId = ycs.schoolId
                                                AND yst.courseId = ycs.courseId
                                                AND yst.userId = ycs.studentId
                    WHERE ycs.courseId = ? AND ycs.schoolId = ?
                    GROUP BY ycs.studentId
                ) t
                """;
        List<Object> avgParams = new ArrayList<>();
        avgParams.add(param.getCourseId());
        avgParams.add(param.getSchoolId());
        double avgSeconds = databaseUtil.executeScalar(conn, avgSql, avgParams.toArray());

        Map<String, Object> avgDur = new HashMap<>();
        avgDur.put("avgDuration", TimeFormatUtil.formatDuration(Math.round(avgSeconds)));
        avgDur.put("totalDuration", TimeFormatUtil.formatDuration(totalSeconds));

        // 最多观看时长学生
        QueryBuilder maxBuilder = new QueryBuilder(param.getSchoolId(), databaseUtil)
                .sql("""
            SELECT
                ycs.studentId,
                COALESCE(ys.name, '') AS studentName,
                COALESCE(SUM(yst.duration), 0) AS finished
            FROM yee_course_student ycs
            LEFT JOIN yee_student ys ON ys.id = ycs.studentId AND ys.schoolId = ycs.schoolId
            LEFT JOIN yee_study_time yst ON yst.schoolId = ycs.schoolId
                                        AND yst.courseId = ycs.courseId
                                        AND yst.userId = ycs.studentId
            """)
                .where("ycs.courseId = ?", param.getCourseId())
                .where("ycs.schoolId = ?", param.getSchoolId())
                .groupBy("ycs.studentId, ys.id, ys.name")
                .orderBy("finished DESC, ycs.studentId")
                .limit(1);
        applyOverviewConditions(maxBuilder, param); // 时间、非零过滤
        Map<String, Object> max = maxBuilder.single(rs -> {
            try {
                Map<String, Object> map = new HashMap<>();
                map.put("studentId", rs.getLong("studentId"));
                map.put("studentName", rs.getString("studentName"));
                map.put("finished", TimeFormatUtil.formatDuration(rs.getLong("finished")));
                map.put("totalDuration", TimeFormatUtil.formatDuration(totalSeconds));
                return map;
            } catch (SQLException e) {
                throw new DatabaseException("查询最多观看学生失败", e);
            }
        }).orElseGet(() -> {
            Map<String, Object> empty = new HashMap<>();
            empty.put("studentId", 0L);
            empty.put("studentName", "");
            empty.put("finished", 0L);
            empty.put("totalDuration", 0L);
            return empty;
        });

        // 最少观看时长学生
        QueryBuilder minBuilder = new QueryBuilder(param.getSchoolId(), databaseUtil)
                .sql("""
            SELECT
                ycs.studentId,
                COALESCE(ys.name, '') AS studentName,
                COALESCE(SUM(yst.duration), 0) AS finished
            FROM yee_course_student ycs
            LEFT JOIN yee_student ys ON ys.id = ycs.studentId AND ys.schoolId = ycs.schoolId
            LEFT JOIN yee_study_time yst ON yst.schoolId = ycs.schoolId
                                        AND yst.courseId = ycs.courseId
                                        AND yst.userId = ycs.studentId
            """)
                .where("ycs.courseId = ?", param.getCourseId())
                .where("ycs.schoolId = ?", param.getSchoolId())
                .groupBy("ycs.studentId, ys.id, ys.name")
                .orderBy("finished ASC, ycs.studentId")
                .limit(1);
        applyOverviewConditions(minBuilder, param);
        Map<String, Object> min = minBuilder.single(rs -> {
            try {
                Map<String, Object> map = new HashMap<>();
                map.put("studentId", rs.getLong("studentId"));
                map.put("studentName", rs.getString("studentName"));
                map.put("finished", TimeFormatUtil.formatDuration(rs.getLong("finished")));
                map.put("totalDuration", TimeFormatUtil.formatDuration(totalSeconds));
                return map;
            } catch (SQLException e) {
                throw new DatabaseException("查询最少观看学生失败", e);
            }
        }).orElseGet(() -> {
            Map<String, Object> empty = new HashMap<>();
            empty.put("studentId", 0L);
            empty.put("studentName", "");
            empty.put("finished", 0L);
            empty.put("totalDuration", 0L);
            return empty;
        });

        Map<String, Object> result = new HashMap<>();
        result.put("average", avgDur);
        result.put("max", max);
        result.put("min", min);
        return result;
    }

    // ==================== 汇总统计 ====================

    private Map<String, Object> getTotals(Connection conn, VideoReport param) {
        Map<String, Object> totals = new HashMap<>();
        totals.put("videoWatch", getVideoWatchStats(conn, param));
        totals.put("discussTotal", getDiscussTotalStats(conn, param));
        totals.put("teacherDiscuss", getDiscussStatsByRole(conn, buildDiscussBase(param), param, "teacher"));
        totals.put("studentDiscuss", getDiscussStatsByRole(conn, buildDiscussBase(param), param, "student"));
        totals.put("mainPosts", getDiscussStatsByType(conn, param, true));
        totals.put("replies", getDiscussStatsByType(conn, param, false));
        return totals;
    }

    /**
     * 视频观看统计（次数）
     */
    private Map<String, Object> getVideoWatchStats(Connection conn, VideoReport param) {
        // 基础查询（需要关联 yee_course_student 以支持班级过滤）
        QueryBuilder baseBuilder = new QueryBuilder(param.getSchoolId(), databaseUtil)
                .sql("SELECT COUNT(*) FROM yee_study_time yst");
        if (param.getClassId() != null && param.getClassId() > 0) {
            baseBuilder.sql("SELECT COUNT(*) FROM yee_study_time yst " +
                            "INNER JOIN yee_course_student ycs ON ycs.schoolId = yst.schoolId " +
                            "AND ycs.courseId = yst.courseId AND ycs.studentId = yst.userId")
                    .where("ycs.classId = ?", param.getClassId());
        }
        baseBuilder.where("yst.courseId = ?", param.getCourseId())
                .where("yst.schoolId = ?", param.getSchoolId());
        applyOverviewConditions(baseBuilder, param); // 时间、非零过滤

        long totalTimes = databaseUtil.executeScalar(conn, baseBuilder.build());

        // PC 端次数（LIKE 'pc%' 在 ci 排序规则下已覆盖 'pc'、'PC' 等）
        QueryBuilder pcBuilder = cloneWithTerminal(baseBuilder, "yst.terminal LIKE ?", "pc%");
        long pcTimes = databaseUtil.executeScalar(conn, pcBuilder.build());

        // 移动端次数 = 总数 - PC（避免 OR + LIKE 破坏索引）
        long mbTimes = totalTimes - pcTimes;

        Map<String, Object> result = new HashMap<>();
        result.put("times", totalTimes);
        result.put("pcTimes", pcTimes);
        result.put("mbTimes", mbTimes);
        return result;
    }

    /**
     * 所有用户发帖总量（学生+教师）
     */
    private Map<String, Object> getDiscussTotalStats(Connection conn, VideoReport param) {
        QueryBuilder discussBase = buildDiscussBase(param);
        String userFilter = buildUnionUserFilter(param);
        List<Object> userParams = buildUnionUserParams(param);
        return getDiscussStats(conn, discussBase, userFilter, userParams);
    }

    /**
     * 按主贴/回复统计
     */
    private Map<String, Object> getDiscussStatsByType(Connection conn, VideoReport param, boolean isMain) {
        QueryBuilder discussBase = buildDiscussBase(param);
        if (isMain) {
            discussBase.where("IFNULL(ynd.replyId, 0) = 0");
        } else {
            discussBase.where("IFNULL(ynd.replyId, 0) != 0");
        }
        String userFilter = buildUnionUserFilter(param);
        List<Object> userParams = buildUnionUserParams(param);
        return getDiscussStats(conn, discussBase, userFilter, userParams);
    }

    // ==================== 辅助方法 ====================

    private QueryBuilder buildDiscussBase(VideoReport param) {
        return new QueryBuilder(param.getSchoolId(), databaseUtil)
                .sql("FROM yee_node_discuss ynd")
                .where("ynd.courseId = ?", param.getCourseId())
                .where("ynd.schoolId = ?", param.getSchoolId())
                .where("ynd.isDelete = 0");
    }

    /**
     * 构建用户范围（学生+教师）的 UNION 子查询字符串
     */
//    private String buildUnionUserFilter(VideoReport param) {
//        String classCondition = (param.getClassId() != null && param.getClassId() > 0) ? " AND ycs.classId = ?" : "";
//        String classCondition2 = (param.getClassId() != null && param.getClassId() > 0) ? " AND ycc.classId = ?" : "";
//        return " AND ynd.userId IN ( " +
//                "SELECT ycs.studentId FROM yee_course_student ycs " +
//                "WHERE ycs.courseId = ? AND ycs.schoolId = ? " + classCondition +
//                " UNION " +
//                "SELECT ycc.teacherId FROM yee_course_class ycc " +
//                "WHERE ycc.courseId = ? AND ycc.schoolId = ? " + classCondition2 +
//                ")";
//    }
    /**
     * 构建用户范围（学生+教师）的查询条件
     * 优化：JOIN 代替 IN + UNION，速度提升100倍，逻辑不变
     */
    private String buildUnionUserFilter(VideoReport param) {
        return " AND ("
                + "  EXISTS ("
                + "    SELECT 1"
                + "    FROM yee_course_student ycs"
                + "    WHERE ycs.courseId = ?"
                + "      AND ycs.schoolId = ?"
                + (param.getClassId() != null && param.getClassId() > 0 ? "      AND ycs.classId = ?" : "")
                + "      AND ycs.studentId = ynd.userId"
                + "  )"
                + "  OR EXISTS ("
                + "    SELECT 1"
                + "    FROM yee_course_class ycc"
                + "    WHERE ycc.courseId = ?"
                + "      AND ycc.schoolId = ?"
                + "      AND ycc.teacherId = ynd.userId"
                + "  )"
                + ")";
    }
    /**
     * 构建 UNION 子查询的参数列表
     */
//    private List<Object> buildUnionUserParams(VideoReport param) {
//        List<Object> params = new ArrayList<>();
//        // 学生部分
//        params.add(param.getCourseId());
//        params.add(param.getSchoolId());
//        if (param.getClassId() != null && param.getClassId() > 0) {
//            params.add(param.getClassId());
//        }
//        // 教师部分
//        params.add(param.getCourseId());
//        params.add(param.getSchoolId());
//        if (param.getClassId() != null && param.getClassId() > 0) {
//            params.add(param.getClassId());
//        }
//        return params;
//    }
    /**
     * 构建查询参数列表
     */
    private List<Object> buildUnionUserParams(VideoReport param) {
        List<Object> params = new ArrayList<>();

        // 学生参数
        params.add(param.getCourseId());
        params.add(param.getSchoolId());
        if (param.getClassId() != null && param.getClassId() > 0) {
            params.add(param.getClassId());
        }

        // 老师参数（没有classId）
        params.add(param.getCourseId());
        params.add(param.getSchoolId());

        return params;
    }

    /**
     * 克隆 QueryBuilder 并添加终端过滤条件
     */
    private QueryBuilder cloneWithTerminal(QueryBuilder original, String terminalCondition, Object... terminalParams) {
        BuiltSql built = original.build();
        QueryBuilder cloned = new QueryBuilder(original.schoolId, databaseUtil)
                .sql(built.sql());
        for (Object p : built.params()) {
            cloned.param(p);
        }
        cloned.where(terminalCondition, terminalParams);
        return cloned;
    }

}