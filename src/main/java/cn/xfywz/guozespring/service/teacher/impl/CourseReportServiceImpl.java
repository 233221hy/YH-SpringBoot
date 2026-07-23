package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.vo.*;
import cn.xfywz.guozespring.service.teacher.CourseReportService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
public class CourseReportServiceImpl implements CourseReportService {

    @Autowired
    private DatabaseUtil databaseUtil;

    public Result overview(CourseReport param) {
        try {
            //  参数校验
            if (param == null || param.getSchoolId() <= 0 || param.getCourseId() <= 0) {
                return Result.error("参数不完整");
            }

            //  分别获取各模块数据
            Map<String, Object> data = new HashMap<>();
            data.put("videoWatch", getVideoWatchStats(param));
            data.put("learners", getLearnerStats(param));
            data.put("totalDuration", getDurationStats(param));
            data.put("exam", getExamStats(param));
            data.put("post", getPostStats(param));
            data.put("reply", getReplyStats(param));
            data.put("work", getWorkStats(param));

            return Result.success(data);
        } catch (Exception e) {
            log.error("课程概览查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    public Result activity(CourseReport param) {
        try {
            // 参数校验
            if (param == null || param.getSchoolId() <= 0 || param.getCourseId() <= 0) {
                return Result.error("参数不完整");
            }

            // 组装数据
            Map<String, Object> data = new HashMap<>();
            data.put("videoDistribution", getVideoDistributionStats(param));
            data.put("onlineRealtime", getOnlineRealtimeStats(param));

            return Result.success(data);
        } catch (Exception e) {
            log.error("活动统计查询失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 构建学习时间相关的查询构建器（支持 classId 过滤）
     * @param param 课程报告参数
     * @param selectSql SELECT 子句（不包含 FROM）
     * @return 配置好的 QueryBuilder
     */
    private QueryBuilder buildStudyTimeQuery(CourseReport param, String selectSql) {
        int courseId = param.getCourseId();
        Integer classId = param.getClassId();

        String baseSql = selectSql + " FROM yee_study_time yst ";
        QueryBuilder qb = databaseUtil.query(param.getSchoolId()).sql(baseSql);

        if (classId != null && classId > 0) {
            // 有班级过滤时，需要关联 yee_course_student 表
            qb.sql(baseSql + " INNER JOIN yee_course_student ycs ON yst.schoolId = ycs.schoolId AND yst.courseId = ycs.courseId AND yst.userId = ycs.studentId");
            qb.where("yst.courseId = ?", courseId)
              .where("ycs.classId = ?", classId);
        } else {
            qb.where("yst.courseId = ?", courseId);
        }
        return qb;
    }

    /**
     * 构建讨论相关查询构建器（支持 classId 过滤）
     * @param param 课程报告参数
     * @param selectSql SELECT 子句（不包含 FROM）
     * @return 配置好的 QueryBuilder
     */
    private QueryBuilder buildDiscussQuery(CourseReport param, String selectSql) {
        int courseId = param.getCourseId();
        Integer classId = param.getClassId();

        String baseSql = selectSql + " FROM yee_node_discuss ynd ";
        QueryBuilder qb = databaseUtil.query(param.getSchoolId()).sql(baseSql);

        if (classId != null && classId > 0) {
            // 有班级过滤时，需要关联 yee_course_student 表
            qb.sql(baseSql + " INNER JOIN yee_course_student ycs ON ycs.schoolId = ynd.schoolId AND ycs.courseId = ynd.courseId AND ycs.studentId = ynd.userId");
            qb.where("ynd.courseId = ?", courseId)
                    .where("ycs.classId = ?", classId);
        } else {
            qb.where("ynd.courseId = ?", courseId);
        }
        return qb;
    }

    /**
     * 视频观看统计（次数）
     */
    private VideoWatchStatsVO getVideoWatchStats(CourseReport param) {
        String selectSql = """
                SELECT
                    COUNT(*) AS viewCount,
                    SUM(CASE WHEN yst.terminal LIKE 'pc%' THEN 1 ELSE 0 END) AS pc,
                    SUM(CASE WHEN yst.terminal NOT LIKE 'pc%' THEN 1 ELSE 0 END) AS mobile
                """;
        return buildStudyTimeQuery(param, selectSql)
                .single(VideoWatchStatsVO::fromResultSet)
                .orElseGet(() -> new VideoWatchStatsVO(0L, 0L, 0L));
    }

    /**
     * 学习人数统计（去重用户）
     */
    private LearnerStatsVO getLearnerStats(CourseReport param) {
        String selectSql = """
            SELECT
                COUNT(DISTINCT yst.userId) AS learnerNum,
                COUNT(DISTINCT CASE WHEN yst.terminal LIKE 'pc%' THEN yst.userId END) AS pc,
                COUNT(DISTINCT CASE WHEN yst.terminal NOT LIKE 'pc%' THEN yst.userId END) AS mobile
            """;
        return buildStudyTimeQuery(param, selectSql)
                .single(LearnerStatsVO::fromResultSet)
                .orElseGet(() -> new LearnerStatsVO(0L, 0L, 0L));
    }

    /**
     * 学习总时长（天）
     */
    private DurationStatsVO getDurationStats(CourseReport param) {
        String selectSql = """
            SELECT
                ROUND(SUM(yst.duration) / 86400) AS totalDurationDays,
                ROUND(SUM(CASE WHEN yst.terminal LIKE 'pc%' THEN yst.duration ELSE 0 END) / 86400) AS pcDays,
                ROUND(SUM(CASE WHEN yst.terminal NOT LIKE 'pc%' THEN yst.duration ELSE 0 END) / 86400) AS mobileDays
            """;
        return buildStudyTimeQuery(param, selectSql)
                .single(DurationStatsVO::fromResultSet)
                .orElseGet(() -> new DurationStatsVO(0L, 0L, 0L));
    }

    /**
     * 考试提交统计
     */
    private ExamStatsVO getExamStats(CourseReport param) {
        int schoolId = param.getSchoolId();
        int courseId = param.getCourseId();
        Integer classId = param.getClassId();

        String baseSql = """
                SELECT
                    COUNT(DISTINCT CASE WHEN yer.state IN (2,3) THEN yer.userId END) AS submittedStuNum,
                    COUNT(CASE WHEN yer.state IN (2,3) THEN 1 END) AS submittedCount,
                    COUNT(DISTINCT CASE WHEN yer.state = 2 THEN yer.userId END) AS pendingReviewStuNum,
                    COUNT(CASE WHEN yer.state = 2 THEN 1 END) AS reviewCount,
                    COUNT(DISTINCT CASE WHEN yer.platform LIKE 'pc%' THEN yer.userId END) AS pcStuNum,
                    COUNT(DISTINCT CASE WHEN yer.platform NOT LIKE 'pc%' THEN yer.userId END) AS mobileStuNum
                FROM yee_exam_record yer
                """;
        QueryBuilder qb = databaseUtil.query(schoolId).sql(baseSql);

        if (classId != null && classId > 0) {
            qb.sql(baseSql + " INNER JOIN yee_course_student ycs ON yer.userId = ycs.studentId AND yer.schoolId = ycs.schoolId");
            qb.where("yer.courseId = ?", courseId)
              .where("ycs.classId = ?", classId);
        } else {
            qb.where("yer.courseId = ?", courseId);
        }

        return qb.single(ExamStatsVO::fromResultSet)
                .orElseGet(() -> new ExamStatsVO(0L, 0L,
                        new SubmittedStatsVO(0L, 0L),
                        new ReviewStatsVO(0L, 0L)));
    }


    /**
     * 讨论发帖统计
     */
    private PostStatsVO getPostStats(CourseReport param) {
        String selectSql = """
            SELECT
                COUNT(DISTINCT CASE WHEN ynd.replyId IS NULL OR ynd.replyId = 0 THEN ynd.userId END) AS postStuNum,
                SUM(CASE WHEN ynd.replyId IS NULL OR ynd.replyId = 0 THEN 1 END) AS postQty,
                COUNT(DISTINCT CASE WHEN (ynd.replyId IS NULL OR ynd.replyId = 0) AND ynd.platform NOT LIKE 'pc%' THEN ynd.userId END) AS mobilePostStuNum,
                COUNT(DISTINCT CASE WHEN (ynd.replyId IS NULL OR ynd.replyId = 0) AND ynd.platform LIKE 'pc%' THEN ynd.userId END) AS pcPostStuNum,
                SUM(CASE WHEN (ynd.replyId IS NULL OR ynd.replyId = 0) AND ynd.platform NOT LIKE 'pc%' THEN 1 END) AS mobilePostQty,
                SUM(CASE WHEN (ynd.replyId IS NULL OR ynd.replyId = 0) AND ynd.platform LIKE 'pc%' THEN 1 END) AS pcPostQty
            """;
        return buildDiscussQuery(param, selectSql)
                .single(PostStatsVO::fromResultSet)
                .orElseGet(() -> new PostStatsVO(
                        new PostQuantityStatsVO(0L, 0L, 0L),
                        new PostStuNumStatsVO(0L, 0L, 0L)
                ));
    }

    /**
     * 回复统计
     */
    private ReplyStatsVO getReplyStats(CourseReport param) {
        String selectSql = """
            SELECT
                COUNT(DISTINCT CASE WHEN ynd.replyId IS NOT NULL AND ynd.replyId != 0 THEN ynd.userId END) AS replyStuNum,
                SUM(CASE WHEN ynd.replyId IS NOT NULL AND ynd.replyId != 0 THEN 1 END) AS replyQty,
                COUNT(DISTINCT CASE WHEN (ynd.replyId IS NOT NULL AND ynd.replyId != 0) AND ynd.platform NOT LIKE 'pc%' THEN ynd.userId END) AS mobileReplyStuNum,
                COUNT(DISTINCT CASE WHEN (ynd.replyId IS NOT NULL AND ynd.replyId != 0) AND ynd.platform LIKE 'pc%' THEN ynd.userId END) AS pcReplyStuNum,
                SUM(CASE WHEN (ynd.replyId IS NOT NULL AND ynd.replyId != 0) AND ynd.platform NOT LIKE 'pc%' THEN 1 END) AS mobileReplyQty,
                SUM(CASE WHEN (ynd.replyId IS NOT NULL AND ynd.replyId != 0) AND ynd.platform LIKE 'pc%' THEN 1 END) AS pcReplyQty
            """;
        return buildDiscussQuery(param, selectSql)
                .single(ReplyStatsVO::fromResultSet)
                .orElseGet(() -> new ReplyStatsVO(
                        new PostQuantityStatsVO(0L, 0L, 0L),
                        new PostStuNumStatsVO(0L, 0L, 0L)
                ));
    }

    /**
     * 作业提交统计
     */
    private WorkStatsVO getWorkStats(CourseReport param) {
        int schoolId = param.getSchoolId();
        int courseId = param.getCourseId();
        Integer classId = param.getClassId();

        String baseSql = """
                SELECT
                    COUNT(DISTINCT CASE WHEN ywr.state IN (2,3) THEN ywr.userId END) AS submittedStuNum,
                    COUNT(CASE WHEN ywr.state IN (2,3) THEN 1 END) AS submittedCount,
                    COUNT(DISTINCT CASE WHEN ywr.state = 2 THEN ywr.userId END) AS pendingReviewStuNum,
                    COUNT(CASE WHEN ywr.state = 2 THEN 1 END) AS reviewCount,
                    COUNT(DISTINCT CASE WHEN ywr.platform LIKE 'pc%' THEN ywr.userId END) AS pcStuNum,
                    COUNT(DISTINCT CASE WHEN ywr.platform NOT LIKE 'pc%' THEN ywr.userId END) AS mobileStuNum
                FROM yee_work_record ywr
                """;
        QueryBuilder qb = databaseUtil.query(schoolId).sql(baseSql);

        if (classId != null && classId > 0) {
            qb.sql(baseSql + " INNER JOIN yee_course_student ycs ON ywr.userId = ycs.studentId AND ywr.schoolId = ycs.schoolId");
            qb.where("ywr.courseId = ?", courseId)
              .where("ycs.classId = ?", classId);
        } else {
            qb.where("ywr.courseId = ?", courseId);
        }

        return qb.single(WorkStatsVO::fromResultSet)
                .orElseGet(() -> new WorkStatsVO(0L, 0L,
                        new SubmittedStatsVO(0L, 0L),
                        new ReviewStatsVO(0L, 0L)));
    }

    // ==================== 视频分布统计 ====================

    /**
     * 获取视频完成分布统计（包含基础信息）
     */
    private VideoCompletionStatsVO getVideoDistributionStats(CourseReport param) {
        // 1. 获取视频基础信息
        VideoInfoStatsVO videoInfo = getVideoInfo(param);

        // 2. 获取按数量的完成比例分布
        Map<String, Long> countDistribution = getCountDistribution(param, videoInfo.getTotalVideoCount());

        // 3. 获取按时长的完成比例分布
        Map<String, Long> durationDistribution = getDurationDistribution(param, videoInfo.getTotalVideoDurationMin());

        // 4. 组装结果
        return new VideoCompletionStatsVO(countDistribution, durationDistribution);
    }

    /**
     * 获取视频基础信息（总时长、总数）
     */
    private VideoInfoStatsVO getVideoInfo(CourseReport param) {
        int schoolId = param.getSchoolId();
        int courseId = param.getCourseId();

        String sql = """
                SELECT
                    IFNULL(SUM(yn.videoDuration), 0) AS totalSec,
                    COUNT(*) AS totalCnt
                FROM yee_node yn
                WHERE yn.courseId = ? AND yn.tabVideo = 1
                """;
        return databaseUtil.query(schoolId)
                .sql(sql)
                .param(courseId)
                .single(VideoInfoStatsVO::fromResultSet)
                .orElseGet(() -> new VideoInfoStatsVO(0L, BigDecimal.ZERO, 0L));
    }

    /**
     * 执行分布统计查询（按数量或按时间），返回各区间计数
     * @param qb QueryBuilder 实例（已设置好基础 SQL 和参数）
     * @param baseSql 基础 SQL（不含最后的子查询结束括号）
     * @param classId 班级 ID（可能为 null）
     * @param additionalParams 除 classId 外的其他参数（已在 qb 中设置好）
     * @return 各区间统计 Map
     */
    private Map<String, Long> executeDistributionQuery(QueryBuilder qb, String baseSql, Integer classId, Object... additionalParams) {
        // 动态拼接 classId 条件
        if (classId != null && classId > 0) {
            qb.sql(baseSql + " AND ycs.classId = ?) t");
            qb.param(classId);
        } else {
            qb.sql(baseSql + ") t");
        }

        // 执行查询并映射结果
        return qb.single(rs -> {
            Map<String, Long> map = new LinkedHashMap<>();
            map.put("0-10%", rs.getLong("p0"));
            map.put("10-20%", rs.getLong("p10"));
            map.put("20-30%", rs.getLong("p20"));
            map.put("30-40%", rs.getLong("p30"));
            map.put("40-50%", rs.getLong("p40"));
            map.put("50-60%", rs.getLong("p50"));
            map.put("60-70%", rs.getLong("p60"));
            map.put("70-80%", rs.getLong("p70"));
            map.put("80-90%", rs.getLong("p80"));
            map.put("90-100%", rs.getLong("p90"));
            map.put("100%", rs.getLong("p100"));
            return map;
        }).orElseGet(HashMap::new);
    }

    /**
     * 按数量完成比例分布（学生完成视频数 / 总视频数）
     */
    private Map<String, Long> getCountDistribution(CourseReport param, long totalVideoCount) {
        int courseId = param.getCourseId();
        Integer classId = param.getClassId();

        String baseSql = """
            SELECT
                SUM(CASE WHEN prog >= 0 AND prog < 0.1 THEN 1 ELSE 0 END) AS p0,
                SUM(CASE WHEN prog >= 0.1 AND prog < 0.2 THEN 1 ELSE 0 END) AS p10,
                SUM(CASE WHEN prog >= 0.2 AND prog < 0.3 THEN 1 ELSE 0 END) AS p20,
                SUM(CASE WHEN prog >= 0.3 AND prog < 0.4 THEN 1 ELSE 0 END) AS p30,
                SUM(CASE WHEN prog >= 0.4 AND prog < 0.5 THEN 1 ELSE 0 END) AS p40,
                SUM(CASE WHEN prog >= 0.5 AND prog < 0.6 THEN 1 ELSE 0 END) AS p50,
                SUM(CASE WHEN prog >= 0.6 AND prog < 0.7 THEN 1 ELSE 0 END) AS p60,
                SUM(CASE WHEN prog >= 0.7 AND prog < 0.8 THEN 1 ELSE 0 END) AS p70,
                SUM(CASE WHEN prog >= 0.8 AND prog < 0.9 THEN 1 ELSE 0 END) AS p80,
                SUM(CASE WHEN prog >= 0.9 AND prog < 1 THEN 1 ELSE 0 END) AS p90,
                SUM(CASE WHEN prog >= 1 THEN 1 ELSE 0 END) AS p100
            FROM (
                SELECT
                    CASE WHEN ? > 0 THEN LEAST(IFNULL(ycs.videoLearned, 0) * 1.0 / ?, 1) ELSE 0 END AS prog
                FROM yee_course_student ycs
                WHERE ycs.courseId = ?
            """;

        QueryBuilder qb = databaseUtil.query(param.getSchoolId())
                .sql(baseSql)
                .param(totalVideoCount)
                .param(totalVideoCount)
                .param(courseId);

        return executeDistributionQuery(qb, baseSql, classId);
    }

    /**
     * 按时长完成比例分布（学生总学习分钟 / 课程总视频分钟）
     */
    private Map<String, Long> getDurationDistribution(CourseReport param, BigDecimal totalVideoDurationMin) {
        int courseId = param.getCourseId();
        Integer classId = param.getClassId();

        String baseSql = """
            SELECT
                SUM(CASE WHEN prog >= 0 AND prog < 0.1 THEN 1 ELSE 0 END) AS p0,
                SUM(CASE WHEN prog >= 0.1 AND prog < 0.2 THEN 1 ELSE 0 END) AS p10,
                SUM(CASE WHEN prog >= 0.2 AND prog < 0.3 THEN 1 ELSE 0 END) AS p20,
                SUM(CASE WHEN prog >= 0.3 AND prog < 0.4 THEN 1 ELSE 0 END) AS p30,
                SUM(CASE WHEN prog >= 0.4 AND prog < 0.5 THEN 1 ELSE 0 END) AS p40,
                SUM(CASE WHEN prog >= 0.5 AND prog < 0.6 THEN 1 ELSE 0 END) AS p50,
                SUM(CASE WHEN prog >= 0.6 AND prog < 0.7 THEN 1 ELSE 0 END) AS p60,
                SUM(CASE WHEN prog >= 0.7 AND prog < 0.8 THEN 1 ELSE 0 END) AS p70,
                SUM(CASE WHEN prog >= 0.8 AND prog < 0.9 THEN 1 ELSE 0 END) AS p80,
                SUM(CASE WHEN prog >= 0.9 AND prog < 1 THEN 1 ELSE 0 END) AS p90,
                SUM(CASE WHEN prog >= 1 THEN 1 ELSE 0 END) AS p100
            FROM (
                SELECT
                    CASE WHEN ? > 0 THEN LEAST(FLOOR(IFNULL(sd.totalDur, 0) / 60) * 1.0 / ?, 1) ELSE 0 END AS prog
                FROM yee_course_student ycs
                LEFT JOIN (
                    SELECT userId, SUM(duration) AS totalDur
                    FROM yee_study_total
                    WHERE courseId = ?
                    GROUP BY userId
                ) sd ON sd.userId = ycs.studentId
                WHERE ycs.courseId = ?
            """;

        QueryBuilder qb = databaseUtil.query(param.getSchoolId())
                .sql(baseSql)
                .param(totalVideoDurationMin)
                .param(totalVideoDurationMin)
                .param(courseId)
                .param(courseId);

        return executeDistributionQuery(qb, baseSql, classId);
    }

    // ==================== 实时在线统计 ====================

    /**
     * 获取实时在线统计（最近60分钟，每4分钟一个点）
     */
    private OnlineRealtimeStatsVO getOnlineRealtimeStats(CourseReport param) {
        int schoolId = param.getSchoolId();
        int courseId = param.getCourseId();
        Integer classId = param.getClassId();

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusMinutes(60);

        // 构建查询：获取时间窗口内的在线会话，并过滤课程学生
        String sql = """
                SELECT
                    FROM_UNIXTIME(t.loginTime) AS loginTime,
                    FROM_UNIXTIME(t.lastTime) AS lastTime
                FROM yee_online t
                INNER JOIN yee_course_student ycs ON ycs.schoolId = t.schoolId AND ycs.courseId = ? AND ycs.studentId = t.userId
                WHERE t.loginTime <= UNIX_TIMESTAMP(?)
                  AND t.lastTime >= UNIX_TIMESTAMP(?)
                """;
        QueryBuilder qb = databaseUtil.query(schoolId).sql(sql);
        qb.param(courseId).param(end).param(start);
        if (classId != null && classId > 0) {
            qb.sql(sql + " AND ycs.classId = ?");
            qb.param(classId);
        }

        // 查询所有会话（只取时间戳，无需平台信息）
        List<OnlineSession> sessions = qb.list(rs -> {
            OnlineSession s = new OnlineSession();
            try {
                s.setLoginTime(rs.getTimestamp("loginTime"));
                s.setLastTime(rs.getTimestamp("lastTime"));
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return s;
        });

        // 按4分钟一个桶计算在线人数
        List<Map<String, Object>> series = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        int binSizeMinutes = 4;
        int totalMinutes = 60;
        int bins = totalMinutes / binSizeMinutes;

        for (int i = 0; i < bins; i++) {
            LocalDateTime binStart = start.plusMinutes((long) i * binSizeMinutes);
            LocalDateTime binEnd = binStart.plusMinutes(binSizeMinutes);
            int cnt = 0;
            for (OnlineSession s : sessions) {
                if (s.getLoginTime() == null || s.getLastTime() == null) continue;
                long st = Math.max(s.getLoginTime().getTime(), Timestamp.valueOf(binStart).getTime());
                long et = Math.min(s.getLastTime().getTime(), Timestamp.valueOf(binEnd).getTime());
                if (st < et) cnt++;
            }
            Map<String, Object> point = new HashMap<>();
            point.put("time", fmt.format(binStart));
            point.put("count", cnt);
            series.add(point);
        }

        return new OnlineRealtimeStatsVO(series);
    }

    /**
     * 内部类：在线会话（仅用于内存计算）
     */
    @Data
    private static class OnlineSession {
        private Timestamp loginTime;
        private Timestamp lastTime;
    }
}

