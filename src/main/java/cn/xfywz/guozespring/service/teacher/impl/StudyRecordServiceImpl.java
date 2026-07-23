package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.dto.StudyRecordQuery;
import cn.xfywz.guozespring.entity.vo.*;
import cn.xfywz.guozespring.excel.ExcelExportUtil;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.StudyRecordService;
import cn.xfywz.guozespring.util.AuthDataPermissionUtil;
import cn.xfywz.guozespring.util.PageResult;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.BuiltSql;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import cn.xfywz.guozespring.util.TimeFormatUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;


import org.springframework.util.StringUtils;

@Slf4j
@Service
public class StudyRecordServiceImpl implements StudyRecordService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private DatabaseUtil databaseUtil;

    // ================ 辅助方法 ================
    /**
     * 构建学习记录基础查询 SQL
     * @param isExport 是否是导出
     *                 true: 导出 (只查展示字段)
     *                 false: 列表 (查主键及完整字段)
     * @return SELECT ... FROM ... 子句
     */
    private String buildStudyQuerySql(boolean isExport) {
        String selectCols = isExport ?
                """
                ys.number AS studentNumber, ys.name AS studentName, IFNULL(ycc.name,'') AS className,
                ycs.videoLearned, ycs.videoCount, ycs.workLearned, ycs.workCount,
                ycs.examLearned, ycs.examCount, ycs.discussJoin, ycs.discussCount,
                ycs.studyTime
                """ :
                """
                ycs.id, ycs.classId, ycs.courseId, ycs.studentId,
                ycs.videoLearned, ycs.videoCount, ycs.workLearned, ycs.workCount,
                ycs.examLearned, ycs.examCount, ycs.discussJoin, ycs.discussCount,
                ycs.studyTime,
                ys.name AS studentName, ys.number AS studentNumber, ycc.name AS className
                """;

        return "SELECT " + selectCols +
                """
                 FROM yee_course_student ycs
                LEFT JOIN yee_student ys ON ys.id = ycs.studentId AND ys.schoolId = ycs.schoolId
                LEFT JOIN yee_course_class ycc ON ycc.id = ycs.classId AND ycc.schoolId = ycs.schoolId
                """;
    }

    /**
     * 应用学习记录查询条件
     * @param queryBuilder 查询构建器
     * @param param 查询参数
     */
    private void applyStudyQueryConditions(QueryBuilder queryBuilder, StudyRecordQuery param) {
        log.debug("开始应用学习记录查询条件: {}", param);

        // 基础条件
        queryBuilder.eq("ycs.schoolId", param.getSchoolId());
        queryBuilder.eq("ycs.courseId", param.getCourseId());

        // 班级筛选
        if (param.getClassId() > 0) {
            queryBuilder.eq("ycs.classId", param.getClassId());
        }

        // 关键字查询（姓名、学号或班级名）
        if (StringUtils.hasText(param.getKeyword())) {
            String keyword = "%" + param.getKeyword().trim() + "%";
            queryBuilder.where("(ys.name LIKE ? OR ys.number LIKE ? OR ycc.name LIKE ?)", keyword, keyword, keyword);
        }

        log.debug("学习记录查询条件应用完成");
    }

    // ================= 学习记录模块 =================
    @Override
    public Result list(StudyRecordQuery param) {
        // 基础SQL构建
        StringBuilder baseSqlBuilder = new StringBuilder();
        baseSqlBuilder.append(buildStudyQuerySql(false)).append(" WHERE 1=1 ");
        List<Object> baseParams = new ArrayList<>();

        // 1. 固定条件
        baseSqlBuilder.append(" AND ycs.schoolId = ? ");
        baseParams.add(param.getSchoolId());
        baseSqlBuilder.append(" AND ycs.courseId = ? ");
        baseParams.add(param.getCourseId());

        // 2. 可选条件
        if (param.getClassId() != 0) {
            baseSqlBuilder.append(" AND ycs.classId = ? ");
            baseParams.add(param.getClassId());
        }
        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            String keyword = "%" + param.getKeyword().trim() + "%";
            baseSqlBuilder.append(" AND (ys.name LIKE ? OR ys.number LIKE ?) ");
            baseParams.add(keyword);
            baseParams.add(keyword);
        }

        // 3. 拼接通用权限（这一行搞定）
        AuthDataPermissionUtil.buildDataPermission(
                baseSqlBuilder,
                baseParams,
                "ycs.courseId",
                "ycs.classId"
        );

        // 4. 分页参数
        int pageNum = param.getPageNum() == null ? 1 : param.getPageNum();
        int pageSize = param.getPageSize() == null ? 10 : param.getPageSize();
        int start = (pageNum - 1) * pageSize;

        // 5. 组装SQL
        String dataSql = baseSqlBuilder + " ORDER BY ycs.addTime DESC LIMIT ?, ?";
        // 注意：这里需要把 baseParams 复制一份，因为 LIMIT 的两个参数是新增的
        List<Object> dataParams = new ArrayList<>(baseParams);
        dataParams.add(start);
        dataParams.add(pageSize);

        String countSql = "SELECT COUNT(1) FROM (" + baseSqlBuilder + ") AS temp";
        List<Object> countParams = new ArrayList<>(baseParams);

        // 6. 执行查询 -> 修复点：使用 4个参数 的方法
        BuiltSql dataBuiltSql = BuiltSql.of(dataSql, dataParams.toArray());
        BuiltSql countBuiltSql = BuiltSql.of(countSql, countParams.toArray());

        PageResult<StudyRecordVO> pageResult = databaseUtil.queryPage(
                param.getSchoolId(),
                dataBuiltSql,
                countBuiltSql,
                StudyRecordVO::fromResultSet
        );

        return Result.success(pageResult.getRows(), pageResult.getTotal());
    }

    @Override
    public void exportData(StudyRecordQuery param, HttpServletResponse response) throws IOException {

        // 1. 构建 SQL (true 表示导出模式)
        String sql = buildStudyQuerySql(true);

        // 2. 执行全量查询
        List<StudyRecordExportVO> exportList = databaseUtil.query(param.getSchoolId())
                .sql(sql + " WHERE 1=1")
                .apply(qb -> applyStudyQueryConditions(qb, param))
                .orderBy("ycs.addTime DESC")
                .list(StudyRecordExportVO::fromResultSet);

        // 3. 导出
        ExcelExportUtil.exportWithPreprocess(exportList, response, StudyRecordExportVO.class);
    }

    // ================= 考试记录模块 =================

//    private String buildExamQuerySql(boolean hasStudentId) {
//        String joinBase = "yer.examId = ye.id";
//        String leftJoin;
//        if (hasStudentId) {
//            leftJoin = String.format("""
//                    LEFT JOIN (
//                    SELECT t1.* FROM yee_exam_record t1
//                    INNER JOIN (
//                        SELECT examId,userid,MAX(id) AS maxId
//                        FROM yee_exam_record GROUP BY examId,userid
//                    ) t2 ON t1.examId = t2.examId AND t1.userid = t2.userid AND t1.id = t2.maxId
//                    ) yer ON %s AND yer.userid = ?
//                    """, joinBase);
//        } else {
//            leftJoin = "LEFT JOIN yee_exam_record yer ON " + joinBase;
//        }
//        return String.format("""
//    SELECT ye.id AS examId, ye.title, ye.topicNumber, ye.score AS totalScore, yn.name AS nodeName,
//           yer.id AS recordId, yer.startTime, yer.finishTime, yer.score AS getScore,
//           yer.state AS state, yer.frequency
//    FROM yee_exam ye
//    %s
//    INNER JOIN yee_node yn ON yn.id = ye.nodeId
//    """, leftJoin);
//    }
private String buildExamQuerySql(boolean hasStudentId) {
    // 修复1：修正joinBase 避免双等号语法错误
    String joinBase = "ye.id";
    String leftJoin;
    if (hasStudentId) {
        leftJoin = String.format("""
                LEFT JOIN yee_exam_record yer
                    ON yer.examId = %s
                    AND yer.userid = ?
                    AND yer.id = (SELECT MAX(id) FROM yee_exam_record sub WHERE sub.examId = ye.id AND sub.userid = ?)
                """, joinBase);
    } else {
        leftJoin = "LEFT JOIN yee_exam_record yer ON yer.examId = ye.id";
    }
    // 修复2：带出count过滤需要的字段，解决 t.xxx 字段不存在
    return String.format("""
SELECT ye.id AS examId, ye.title, ye.topicNumber, ye.score AS totalScore, yn.name AS nodeName,
       yer.id AS recordId, yer.startTime, yer.finishTime, yer.score AS getScore,
       yer.state AS state, yer.frequency,
       ye.courseId, ye.allow, ye.classList, ye.target_class
FROM yee_exam ye
%s
INNER JOIN yee_node yn ON yn.id = ye.nodeId
""", leftJoin);
}

    @Override
    public Result examList(StudyRecordQuery param) {
        boolean hasStudentId = param.getStudentId() != 0;
        Integer schoolId = param.getSchoolId();
        Long courseId = param.getCourseId();
        Long studentId = param.getStudentId();
        String keyword = param.getKeyword();
        Integer stateFilter = param.getState();
        int pageNum = param.getPageNum();
        int pageSize = param.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        // 预先查出学生 classId，避免每行 yee_exam 执行一次关联子查询
        Long classId = null;
        if (hasStudentId) {
            classId = databaseUtil.query(schoolId)
                    .sql("SELECT classId FROM yee_course_student WHERE courseId = ? AND studentId = ?")
                    .params(courseId, studentId)
                    .single(rs -> {
                        long id = rs.getLong("classId");
                        return rs.wasNull() ? null : id;
                    })
                    .orElse(null);
        }
        boolean hasClass = classId != null && classId > 0;

        StringBuilder baseSqlSb = new StringBuilder(buildExamQuerySql(hasStudentId));
        List<Object> baseParams = new ArrayList<>();
        if (hasStudentId) {
            // 两处 userid = ?，连续添加两次学生ID
            baseParams.add(studentId);
            baseParams.add(studentId);
        }
        baseSqlSb.append(" WHERE 1=1 ");
        baseSqlSb.append(" AND ye.courseId = ? ");
        baseParams.add(courseId);
        baseSqlSb.append(" AND ye.allow = 1 ");

        if (hasStudentId) {
            if (hasClass) {
                baseSqlSb.append(" AND (JSON_LENGTH(ye.classList) = 0 OR ye.target_class = ? OR JSON_CONTAINS(ye.classList, CAST(? AS JSON))) ");
                baseParams.add(classId);
                baseParams.add(classId);
            } else {
                baseSqlSb.append(" AND JSON_LENGTH(ye.classList) = 0 ");
            }
        }

        if (Objects.nonNull(keyword) && !keyword.isBlank()) {
            String likeVal = "%" + keyword.trim() + "%";
            baseSqlSb.append(" AND (ye.title LIKE ? OR yn.name LIKE ?) ");
            baseParams.add(likeVal);
            baseParams.add(likeVal);
        }

        // 修复3：状态条件兼容未作答NULL，不会丢失试卷数据
        if (hasStudentId && Objects.nonNull(stateFilter)) {
            baseSqlSb.append(" AND (yer.state IS NULL OR yer.state = ?) ");
            baseParams.add(stateFilter);
        }

        String orderSql = " ORDER BY CASE WHEN yer.startTime IS NULL THEN 1 ELSE 0 END, yer.startTime DESC, ye.addTime DESC ";
        baseSqlSb.append(orderSql);

        // 分页明细SQL
        String dataSql = baseSqlSb + " LIMIT ?, ?";
        List<Object> dataParams = new ArrayList<>(baseParams);
        dataParams.add(offset);
        dataParams.add(pageSize);

        // Count SQL：统一子查询口径，修复 COUNT(DISTINCT ye.id) 别名错误 → t.examId
        String examBaseSql = buildExamQuerySql(hasStudentId);
        StringBuilder countSqlSb = new StringBuilder("SELECT COUNT(DISTINCT t.examId) FROM (" + examBaseSql + ") t WHERE 1=1 ");
        List<Object> countParams = new ArrayList<>();
        if (hasStudentId) {
            countParams.add(studentId);
            countParams.add(studentId);
        }
        countSqlSb.append(" AND t.courseId = ? ");
        countParams.add(courseId);
        countSqlSb.append(" AND t.allow = 1 ");
        if (hasStudentId) {
            if (hasClass) {
                countSqlSb.append(" AND (JSON_LENGTH(t.classList) = 0 OR t.target_class = ? OR JSON_CONTAINS(t.classList, CAST(? AS JSON))) ");
                countParams.add(classId);
                countParams.add(classId);
            } else {
                countSqlSb.append(" AND JSON_LENGTH(t.classList) = 0 ");
            }
        }
        if (Objects.nonNull(keyword) && !keyword.isBlank()) {
            String likeVal = "%" + keyword.trim() + "%";
            countSqlSb.append(" AND (t.title LIKE ? OR t.nodeName LIKE ?) ");
            countParams.add(likeVal);
            countParams.add(likeVal);
        }
        if (hasStudentId && Objects.nonNull(stateFilter)) {
            countSqlSb.append(" AND (t.state IS NULL OR t.state = ?) ");
            countParams.add(stateFilter);
        }

        BuiltSql dataBuiltSql = BuiltSql.of(dataSql, dataParams.toArray());
        BuiltSql countBuiltSql = BuiltSql.of(countSqlSb.toString(), countParams.toArray());

        PageResult<ExamRecordVO> pageResult = databaseUtil.queryPage(
                schoolId,
                dataBuiltSql,
                countBuiltSql,
                ExamRecordVO::fromResultSet
        );

        // 返回结构完全原样不动
        return Result.success(pageResult.getRows(), pageResult.getTotal());
    }

    /**
     * 构建作业查询的基础 SQL（包含 SELECT、FROM、JOIN）
     * @param hasStudentId 是否包含学生ID
     * @return 基础 SQL 字符串（不含 WHERE 条件）
     */
    private String buildWorkQuerySql(boolean hasStudentId) {
        // 修复1：修正joinBase，不再拼接出 ywr.workId = ywr.workId = yw.id 语法错误
        String joinBase = "yw.id";
        String leftJoin;
        if (hasStudentId) {
            leftJoin = String.format("""
            LEFT JOIN yee_work_record ywr
                ON ywr.workId = %s
                AND ywr.userid = ?
                AND ywr.id = (SELECT MAX(id) FROM yee_work_record sub WHERE sub.workId = yw.id AND sub.userid = ?)
            """, joinBase);
        } else {
            leftJoin = "LEFT JOIN yee_work_record ywr ON ywr.workId = yw.id";
        }
        // 修复2：内层查询带出count过滤需要的字段 courseId / allow / classList / target_class，解决 t.xxx 不存在
        return String.format("""
SELECT yw.id AS workId, yw.title, yw.topicNumber, yw.score AS totalScore, yn.name AS nodeName,
       ywr.id AS recordId, ywr.startTime, ywr.finishTime, ywr.score AS getScore,
       ywr.state AS state, ywr.frequency,
       yw.courseId, yw.allow, yw.classList, yw.target_class
FROM yee_work yw
%s
INNER JOIN yee_node yn ON yn.id = yw.nodeId
""", leftJoin);
    }

    @Override
    public Result workList(StudyRecordQuery param) {
        boolean hasStudentId = param.getStudentId() != 0;
        Integer schoolId = param.getSchoolId();
        Long courseId = param.getCourseId();
        Long studentId = param.getStudentId();
        String keyword = param.getKeyword();
        Integer stateFilter = param.getState();
        int pageNum = param.getPageNum();
        int pageSize = param.getPageSize();
        int offset = (pageNum - 1) * pageSize;
        Long classId = null;
        if (hasStudentId) {
            classId = databaseUtil.query(schoolId)
                    .sql("SELECT classId FROM yee_course_student WHERE courseId = ? AND studentId = ?")
                    .params(courseId, studentId)
                    .single(rs -> {
                        long id = rs.getLong("classId");
                        return rs.wasNull() ? null : id;
                    })
                    .orElse(null);
        }

        boolean hasClass = classId != null && classId > 0;
        StringBuilder baseSqlSb = new StringBuilder(buildWorkQuerySql(hasStudentId));
        List<Object> baseParams = new ArrayList<>();
        if (hasStudentId) {
            // 两处 userid = ?，连续添加两次学生ID
            baseParams.add(studentId);
            baseParams.add(studentId);
        }
        baseSqlSb.append(" WHERE 1=1 ");
        baseSqlSb.append(" AND yw.courseId = ? ");
        baseParams.add(courseId);
        baseSqlSb.append(" AND yw.allow = 1 ");

        if (hasStudentId) {
            if (hasClass) {
                baseSqlSb.append(" AND (JSON_LENGTH(yw.classList) = 0 OR yw.target_class = ? OR JSON_CONTAINS(yw.classList, CAST(? AS JSON))) ");
                baseParams.add(classId);
                baseParams.add(classId);
            } else {
                baseSqlSb.append(" AND JSON_LENGTH(yw.classList) = 0 ");
            }
        }

        if (keyword != null && !keyword.isBlank()) {
            String likeVal = "%" + keyword.trim() + "%";
            baseSqlSb.append(" AND (yw.title LIKE ? OR yn.name LIKE ?) ");
            baseParams.add(likeVal);
            baseParams.add(likeVal);
        }

        // 修复3：状态筛选不会把LEFT JOIN转为内连接，未作答作业不消失
        if (hasStudentId && stateFilter != null) {
            baseSqlSb.append(" AND (ywr.state IS NULL OR ywr.state = ?) ");
            baseParams.add(stateFilter);
        }

        String orderSql = " ORDER BY CASE WHEN ywr.startTime IS NULL THEN 1 ELSE 0 END, ywr.startTime DESC, yw.addTime DESC ";
        baseSqlSb.append(orderSql);
        String dataSql = baseSqlSb + " LIMIT ?, ?";
        List<Object> dataParams = new ArrayList<>(baseParams);
        dataParams.add(offset);
        dataParams.add(pageSize);

        // Count SQL 保留子查询统一口径，内层已带出所需过滤字段
        String workBaseSql = buildWorkQuerySql(hasStudentId);
        StringBuilder countSqlSb = new StringBuilder("SELECT COUNT(DISTINCT t.workId) FROM (" + workBaseSql + ") t WHERE 1=1 ");
        List<Object> countParams = new ArrayList<>();
        if (hasStudentId) {
            countParams.add(studentId);
            countParams.add(studentId);
        }
        countSqlSb.append(" AND t.courseId = ? ");
        countParams.add(courseId);
        countSqlSb.append(" AND t.allow = 1 ");
        if (hasStudentId) {
            if (hasClass) {
                countSqlSb.append(" AND (JSON_LENGTH(t.classList) = 0 OR t.target_class = ? OR JSON_CONTAINS(t.classList, CAST(? AS JSON))) ");
                countParams.add(classId);
                countParams.add(classId);
            } else {
                countSqlSb.append(" AND JSON_LENGTH(t.classList) = 0 ");
            }
        }
        if (keyword != null && !keyword.isBlank()) {
            String likeVal = "%" + keyword.trim() + "%";
            countSqlSb.append(" AND (t.title LIKE ? OR t.nodeName LIKE ?) ");
            countParams.add(likeVal);
            countParams.add(likeVal);
        }
        if (hasStudentId && stateFilter != null) {
            countSqlSb.append(" AND (t.state IS NULL OR t.state = ?) ");
            countParams.add(stateFilter);
        }

        BuiltSql dataBuiltSql = BuiltSql.of(dataSql, dataParams.toArray());
        BuiltSql countBuiltSql = BuiltSql.of(countSqlSb.toString(), countParams.toArray());
        PageResult<WorkRecordVO> pageResult = databaseUtil.queryPage(
                schoolId,
                dataBuiltSql,
                countBuiltSql,
                WorkRecordVO::fromResultSet
        );

        // 返回格式完全不变
        return Result.success(pageResult.getRows(), pageResult.getTotal());
    }


    /**
     * 构建讨论查询的基础 SQL（包含 SELECT、FROM、JOIN）
     * @param hasStudentId 是否包含学生ID
     * @return 基础 SQL 字符串（不含 WHERE 条件）
     */
    private String buildDiscussQuerySql(boolean hasStudentId) {
        String joinCondition = "yds.discussId = yd.id AND yds.schoolId = yd.schoolId" +
                (hasStudentId ? " AND yds.userId = ?" : "");
        return String.format("""
        SELECT
            yd.id AS discussId,
            yd.title,
            IFNULL(yds.postQty,0) AS postQty,
            IFNULL(yds.replyQty,0) AS replyQty,
            IFNULL(yds.likeQty,0) AS likeQty,
            IFNULL(yds.score,0) AS getScore,
            100 AS fullScore
        FROM yee_discuss yd
        LEFT JOIN yee_discuss_score yds ON %s
        """, joinCondition);
    }

    /**
     * 讨论记录列表
     */
    @Override
    public Result discussList(StudyRecordQuery param) {
        boolean hasStudentId = param.getStudentId() != 0;
        String baseSql = buildDiscussQuerySql(hasStudentId);

        // 预绑定 JOIN 中的参数（如果有学生ID）
        List<Object> baseParams = new ArrayList<>();
        if (hasStudentId) {
            baseParams.add(param.getStudentId());
        }

        PageResult<DiscussRecordVO> pageResult = databaseUtil.query(param.getSchoolId())
                .sql(baseSql + " WHERE 1=1")
                .params(baseParams.toArray())
                .apply(qb -> {
                    // 固定 WHERE 条件
                    qb.eq("yd.schoolId", param.getSchoolId())
                            .eq("yd.courseId", param.getCourseId())
                            .eq("yd.isDelete", 0);

                    // 关键词搜索
                    if (StringUtils.hasText(param.getKeyword())) {
                        qb.like("yd.title", param.getKeyword().trim());
                    }

                    // 状态筛选
                    if (param.getState() != null) {
                        if (param.getState() == 0) {
                            qb.where("(yds.allQty = 0 OR yds.allQty IS NULL)");
                        } else {
                            qb.where("yds.allQty > 0");
                        }
                    }
                })
                .orderBy("yd.top DESC, yd.addTime DESC, yd.id DESC")
                .pageDistinct("discussId", DiscussRecordVO::fromResultSet,
                        param.getPageNum(), param.getPageSize());

        return Result.success(pageResult.getRows(), pageResult.getTotal());
    }
    @Override
    public Result videoList(StudyRecordQuery param) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            int offset = (param.getPageNum() - 1) * param.getPageSize();

            // 构建动态 WHERE 条件（仅针对 yee_node）
            StringBuilder whereBuilder = new StringBuilder();
            List<Object> params = new ArrayList<>();

            whereBuilder.append(" AND yn.schoolId = ? AND yn.courseId = ? AND yn.tabVideo = 1 ");
            params.add(param.getSchoolId());
            params.add(param.getCourseId());

            if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
                whereBuilder.append(" AND yn.name LIKE ? ");
                params.add("%" + param.getKeyword().trim() + "%");
            }

            // 基础查询：左联yee_study_total权威汇总表，仅用state判断完成，移除时长兜底
            String baseSql = """
SELECT
    yn.id AS nodeId,
    yn.name AS nodeName,
    c.name AS chapterName,
    st.startTime AS startTime,
    st.lastTime AS finishTime,
    IFNULL(st.viewCount, 0) AS viewCount,
    -- 优先使用汇总表权威总时长
    IFNULL(yst.duration, IFNULL(st.totalDuration, 0)) AS totalDuration,
    yn.videoDuration AS videoDuration,
    CAST(
    CASE
        WHEN st.viewCount IS NULL THEN '0'
        -- 仅汇总表标记完成才判定已完成，删除时长兜底判断
        WHEN yst.state = 1 THEN '1'
        ELSE '2'
    END AS CHAR(1)) AS stateCode
FROM yee_node yn
LEFT JOIN yee_chapter c ON yn.chapterId = c.id
-- 关联用户该课程节点的学习汇总（权威状态源）
LEFT JOIN yee_study_total yst
    ON yst.nodeId = yn.id
    AND yst.userId = ?
    AND yst.courseId = ?
    AND yst.schoolId = ?
LEFT JOIN (
    SELECT
        nodeId,
        MIN(beginTime) AS startTime,
        MAX(lastTime) AS lastTime,
        COUNT(*) AS viewCount,
        SUM(duration) AS totalDuration
    FROM yee_study_time
    WHERE schoolId = ? AND courseId = ? AND userId = ?
    GROUP BY nodeId
) st ON st.nodeId = yn.id
WHERE 1=1
""" + whereBuilder.toString() + """
ORDER BY
    IFNULL(c.sort, 999999) ASC,
    c.id ASC,
    IFNULL(yn.sort, 999999) ASC,
    yn.id ASC
""";

            // 根据 state 参数决定外层过滤
            String finalSql;
            if (param.getState() != null) {
                if (param.getState() == 0) {
                    finalSql = "SELECT * FROM (" + baseSql + ") AS t WHERE t.stateCode IN (0, 2) LIMIT ? OFFSET ?";
                } else {
                    finalSql = "SELECT * FROM (" + baseSql + ") AS t WHERE t.stateCode = 1 LIMIT ? OFFSET ?";
                }
            } else {
                finalSql = "SELECT * FROM (" + baseSql + ") AS t LIMIT ? OFFSET ?";
            }

            // 参数组装：前3个yee_study_total关联条件
            List<Object> allParams = new ArrayList<>();
            allParams.add(param.getStudentId());  // yst.userId
            allParams.add(param.getCourseId());    // yst.courseId
            allParams.add(param.getSchoolId());    // yst.schoolId
            // 子查询st参数
            allParams.add(param.getSchoolId());
            allParams.add(param.getCourseId());
            allParams.add(param.getStudentId());
            // 节点筛选条件
            allParams.addAll(params);
            // 分页参数
            allParams.add(param.getPageSize());
            allParams.add(offset);

            try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
                 PreparedStatement ps = connection.prepareStatement(finalSql)) {

                for (int i = 0; i < allParams.size(); i++) {
                    ps.setObject(i + 1, allParams.get(i));
                }

                // ========== 统计SQL同步修改，同样只依靠yst.state判断 ==========
                String countBaseSql = """
SELECT
    CAST(
    CASE
        WHEN st.viewCount IS NULL THEN '0'
        WHEN yst.state = 1 THEN '1'
        ELSE '2'
    END AS CHAR(1)) AS stateCode
FROM yee_node yn
LEFT JOIN yee_chapter c ON yn.chapterId = c.id
LEFT JOIN yee_study_total yst
    ON yst.nodeId = yn.id
    AND yst.userId = ?
    AND yst.courseId = ?
    AND yst.schoolId = ?
LEFT JOIN (
    SELECT nodeId, SUM(duration) AS totalDuration, COUNT(*) AS viewCount
    FROM yee_study_time
    WHERE schoolId = ? AND courseId = ? AND userId = ?
    GROUP BY nodeId
) st ON st.nodeId = yn.id
WHERE 1=1
""" + whereBuilder.toString();

                String countSql;
                if (param.getState() != null) {
                    if (param.getState() == 0) {
                        countSql = "SELECT COUNT(*) FROM (" + countBaseSql + ") AS t WHERE t.stateCode IN (0, 2)";
                    } else {
                        countSql = "SELECT COUNT(*) FROM (" + countBaseSql + ") AS t WHERE t.stateCode = 1";
                    }
                } else {
                    countSql = "SELECT COUNT(*) FROM (" + countBaseSql + ") AS t";
                }

                // 统计参数顺序和主查询保持一致
                List<Object> countParams = new ArrayList<>();
                countParams.add(param.getStudentId());
                countParams.add(param.getCourseId());
                countParams.add(param.getSchoolId());
                countParams.add(param.getSchoolId());
                countParams.add(param.getCourseId());
                countParams.add(param.getStudentId());
                countParams.addAll(params);

                long total;
                try (PreparedStatement cp = connection.prepareStatement(countSql)) {
                    for (int i = 0; i < countParams.size(); i++) {
                        cp.setObject(i + 1, countParams.get(i));
                    }
                    try (ResultSet rs = cp.executeQuery()) {
                        total = rs.next() ? rs.getLong(1) : 0;
                    }
                }

                // 结果封装逻辑无改动，时长展示已取汇总表值
                List<Map<String, Object>> list = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("nodeId", rs.getLong("nodeId"));
                        row.put("nodeName", rs.getString("nodeName"));
                        row.put("chapterName", rs.getString("chapterName"));

                        Object startTimeObj = rs.getObject("startTime");
                        Object finishTimeObj = rs.getObject("finishTime");

                        Timestamp startTime = null;
                        Timestamp finishTime = null;

                        if (startTimeObj != null) {
                            startTime = new Timestamp(((Number) startTimeObj).longValue() * 1000L);
                        }
                        if (finishTimeObj != null) {
                            finishTime = new Timestamp(((Number) finishTimeObj).longValue() * 1000L);
                        }
                        row.put("startTime", startTime);
                        row.put("finishTime", finishTime);
                        row.put("viewCount", rs.getLong("viewCount"));

                        long totalDuration = rs.getLong("totalDuration");
                        long videoDuration = rs.getLong("videoDuration");
                        int stateCode = Integer.parseInt(rs.getString("stateCode"));
                        long showDuration = totalDuration;

                        if (stateCode == 1) {
                            showDuration = Math.max(totalDuration, videoDuration);
                        }

                        row.put("totalDuration", TimeFormatUtil.formatDuration(showDuration));
                        row.put("videoDuration", TimeFormatUtil.formatDuration(videoDuration));

                        String stateStr = switch (stateCode) {
                            case 0 -> "未开始";
                            case 1 -> "已完成";
                            default -> "学习中";
                        };
                        row.put("state", stateStr);
                        list.add(row);
                    }
                }

                return Result.success(list, total);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result resetStudyRecord(StudyRecordQuery param) {
        try {
            if (param.getSchoolId() <= 0 || param.getCourseId() <= 0 || param.getStudentId() <= 0) {
                return Result.error("参数不完整：需要schoolId、courseId、studentId");
            }
            SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            Connection connection = null;
            try {
                connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
                connection.setAutoCommit(false);

                // 删除学习明细
                try (PreparedStatement pst = connection.prepareStatement(
                        "DELETE FROM yee_study_time WHERE schoolId = ? AND courseId = ? AND userId = ?")) {
                    pst.setLong(1, param.getSchoolId());
                    pst.setLong(2, param.getCourseId());
                    pst.setLong(3, param.getStudentId());
                    pst.executeUpdate();
                }
                // 删除学习汇总
                try (PreparedStatement pst = connection.prepareStatement(
                        "DELETE FROM yee_study_total WHERE schoolId = ? AND courseId = ? AND userId = ?")) {
                    pst.setLong(1, param.getSchoolId());
                    pst.setLong(2, param.getCourseId());
                    pst.setLong(3, param.getStudentId());
                    pst.executeUpdate();
                }
                // 重置课程-学生学习统计：清零视频完成数、学习时长、上次节点，标记有更新
                try (PreparedStatement pst = connection.prepareStatement(
                        "UPDATE yee_course_student SET videoLearned = 0, studyTime = 0, lastNodeId = 0, `change` = 1 WHERE schoolId = ? AND courseId = ? AND studentId = ?")) {
                    pst.setLong(1, param.getSchoolId());
                    pst.setLong(2, param.getCourseId());
                    pst.setLong(3, param.getStudentId());
                    pst.executeUpdate();
                }

                connection.commit();
                return Result.success("已退回重学");
            } catch (Exception ex) {
                if (connection != null) try { connection.rollback(); } catch (Exception ignore) {}
                return Result.error("退回重学失败：" + ex.getMessage());
            } finally {
                if (connection != null) try { connection.close(); } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            return Result.error("退回重学失败：" + e.getMessage());
        }
    }
}
