package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.dto.ExtraScoreExcelData;
import cn.xfywz.guozespring.entity.dto.YeeCourseResultsQueryDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeCourseResults;
import cn.xfywz.guozespring.excel.ExcelExportUtil;
import cn.xfywz.guozespring.excel.ExcelImportBuilder;
import cn.xfywz.guozespring.excel.ImportResult;
import cn.xfywz.guozespring.excel.validation.ExtraScoreImportValidationFactory;
import cn.xfywz.guozespring.exception.BusinessException;
import cn.xfywz.guozespring.exception.ImportExportException;
import cn.xfywz.guozespring.service.teacher.YeeCourseResultsService;
import cn.xfywz.guozespring.util.AuthDataPermissionUtil;
import cn.xfywz.guozespring.util.PageResult;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import cn.xfywz.guozespring.entity.vo.YeeCourseResultsVO;
import cn.xfywz.guozespring.entity.vo.YeeCourseResultsExportVO;
import cn.xfywz.guozespring.entity.vo.YeeCourseExtraScoreExportVO;

import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import com.alibaba.fastjson2.JSONObject;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 课程成绩管理
 * 成绩查询: list方法用于分页查询课程成绩结果
 * 成绩维护: 提供add、delete、update方法进行成绩的增删改操作
 * 成绩计算:
 * calculateScore方法用于计算指定班级所有学生的综合成绩
 * 支持视频、作业、考试、讨论、额外加分等多种成绩类型的计算
 * 排名计算: recalculateRankingInternal方法重新计算学生在班级中的排名
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class YeeCourseResultsServiceImpl implements YeeCourseResultsService {

    private final DatabaseUtil databaseUtil;

    // 内部类
    private static class SqlBuilder {

        static BuiltSql buildUpdate(YeeCourseResults param) {
            if (param == null || param.getId() <= 0) {
                return null;
            }

            LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
            if (param.getCourseId() > 0) fields.put("courseId", param.getCourseId());
            if (param.getUserId() > 0) fields.put("userId", param.getUserId());
            if (param.getScore() >= 0) fields.put("score", param.getScore());
            if (param.getVideoScore() >= 0) fields.put("videoScore", param.getVideoScore());
            if (param.getExamScore() >= 0) fields.put("examScore", param.getExamScore());
            if (param.getWorkScore() >= 0) fields.put("workScore", param.getWorkScore());
            if (param.getDiscussScore() >= 0) fields.put("discussScore", param.getDiscussScore());
            if (param.getExtraScore() >= 0) fields.put("extraScore", param.getExtraScore());
            if (param.getReportScore() >= 0) fields.put("reportScore", param.getReportScore());
            if (param.getStuName() != null && !param.getStuName().trim().isEmpty()) fields.put("stuName", param.getStuName());
            if (param.getStuNumber() != null && !param.getStuNumber().trim().isEmpty()) fields.put("stuNumber", param.getStuNumber());
            if (param.getClassId() > 0) fields.put("classId", param.getClassId());
            if (param.getRanking() > 0) fields.put("ranking", param.getRanking());
            if (param.getVideoResult() >= 0) fields.put("videoResult", param.getVideoResult());
            if (param.getExamResult() >= 0) fields.put("examResult", param.getExamResult());
            if (param.getWorkResult() >= 0) fields.put("workResult", param.getWorkResult());
            if (param.getDiscussResult() >= 0) fields.put("discussResult", param.getDiscussResult());
            if (param.getReportResult() >= 0) fields.put("reportResult", param.getReportResult());
            if (param.getCalcDate() != null) fields.put("calcDate", param.getCalcDate());

            if (fields.isEmpty()) return null;

            String setClause = fields.keySet().stream()
                    .map(k -> "`" + k + "` = ?")
                    .collect(Collectors.joining(", "));
            String sql = "UPDATE yee_course_results SET " + setClause + " WHERE id = ?";
            List<Object> params = new ArrayList<>(fields.values());
            params.add(param.getId());
            return BuiltSql.of(sql, params);
        }

        // 普通 INSERT（用于新学生）
        public String buildInsertResultSql() {
            return """
        INSERT INTO yee_course_results (
            courseId, userId, score, videoScore, workScore, examScore, discussScore, extraScore, reportScore,
            stuName, stuNumber, classId, videoResult, workResult, examResult, discussResult, reportResult, schoolId, calcDate
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        }

        // UPDATE 语句（用于已存在学生）
        public String buildUpdateResultSql() {
            return """
        UPDATE yee_course_results SET
            score = ?,               -- 总分字段是 score，不是 totalScore
            videoScore = ?,
            workScore = ?,
            examScore = ?,
            discussScore = ?,
            extraScore = ?,
            reportScore = ?,
            stuName = ?,
            stuNumber = ?,
            videoResult = ?,
            workResult = ?,
            examResult = ?,
            discussResult = ?,
            reportResult = ?,
            schoolId = ?,
            calcDate = ?
        WHERE courseId = ? AND classId = ? AND userId = ?
        """;
        }

        BuiltSql rulesOne(long courseId, long classId) {
            return BuiltSql.of(
                    "SELECT useVideo, videoRatio, videoItems, useWork, workRatio, workItems, " +
                            "useExam, examRatio, examItems, useDiscuss, discussRatio, discussItems, " +
                            "useExtra, extraRatio, useReport, reportRatio " +
                            "FROM yee_course_score_rules WHERE courseId = ? AND classId = ?",
                    courseId, classId
            );
        }

        BuiltSql students(long classId) {
            return BuiltSql.of(
                    "SELECT DISTINCT studentId " +
                            "FROM yee_course_student " +
                            "WHERE classId = ?",
                    classId
            );
        }

        BuiltSql deleteResult(long courseId, long studentId, long classId) {
            return BuiltSql.of(
                    "DELETE FROM yee_course_results WHERE courseId = ? AND userId = ? AND classId = ?",
                    courseId, studentId, classId
            );
        }

        BuiltSql insertResult(
                long courseId, long userId, double score, double videoScore, double workScore,
                double examScore, double discussScore, double extraScore, double reportScore,
                String stuName, String stuNumber, long classId,
                double videoResult, double workResult, double examResult, double discussResult, double reportResult,
                long schoolId, Date calcDate) {
            return BuiltSql.of(
                    "INSERT INTO yee_course_results (" +
                            "courseId, userId, score, videoScore, workScore, examScore, discussScore, extraScore, reportScore, " +
                            "stuName, stuNumber, classId, videoResult, workResult, examResult, discussResult, reportResult, schoolId, calcDate" +
                            ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    courseId, userId, score, videoScore, workScore, examScore, discussScore, extraScore, reportScore,
                    stuName, stuNumber, classId, videoResult, workResult, examResult, discussResult, reportResult, schoolId, calcDate
            );
        }

        BuiltSql updateRulesCalc(long courseId, long classId) {
            return BuiltSql.of(
                    "UPDATE yee_course_score_rules SET calcNumber = calcNumber + 1 WHERE courseId = ? AND classId = ?",
                    courseId, classId
            );
        }
    }

    private enum ExportType {
        NONE, // 非导出，用于列表查询
        FULL_RESULTS, // 导出全部成绩
        EXTRA_SCORE // 导出额外得分
    }

    /**
     * 构建课程成绩基础查询SQL
     * @param exportType 导出类型 (NONE, FULL_RESULTS, EXTRA_SCORE)
     * @return SELECT 子句
     */
    private String buildSelectCols(ExportType exportType) {
        return switch (exportType) {
            case EXTRA_SCORE -> "r.stuNumber, r.stuName, r.extraScore";
            case FULL_RESULTS -> "r.stuNumber, r.stuName, r.score, r.videoScore, r.workScore, r.examScore, r.discussScore, r.extraScore, r.reportScore, r.ranking, cc.name AS className";
            default -> "r.id, r.courseId, r.userId, r.score, r.videoScore, r.examScore, r.workScore, r.discussScore, r.extraScore, r.reportScore, r.stuName, r.stuNumber, r.classId, cc.name AS courseClassName, r.ranking, r.videoResult, r.examResult, r.workResult, r.discussResult, r.reportResult, r.schoolId, r.calcDate";
        };
    }

    /**
     * 应用课程成绩查询条件
     */
    private void applyQueryConditions(QueryBuilder queryBuilder, YeeCourseResultsQueryDTO queryDTO) {
        log.debug("开始应用课程成绩查询条件: {}", queryDTO);

        // 基础条件：课程ID 和 学校ID (通常在调用query时已作为参数传入，这里处理可选条件)
        // 注意：databaseUtil.query(schoolId) 已经隐含了 schoolId 的分库路由，SQL中是否需要显式加 schoolId 取决于表结构
        queryBuilder.eq("r.courseId", queryDTO.getCourseId());

        // 班级
        if (queryDTO.getClassId() > 0) {
            queryBuilder.eq("r.classId", queryDTO.getClassId());
        }

        // 关键字查询（姓名或学号）
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = "%" + queryDTO.getKeyword().trim() + "%";
            queryBuilder.where("(r.stuName LIKE ? OR r.stuNumber LIKE ?)", keyword, keyword);
        }
    }

    // ================ 执行方法 ================

    @Override
    public Result list(YeeCourseResultsQueryDTO queryDTO) {
        // 1. 构建查询列和表连接
        String selectCols = buildSelectCols(ExportType.NONE);
        String from = " FROM yee_course_results r LEFT JOIN yee_course_class cc ON cc.id = r.classId";

        // 2. 手动维护参数列表（解决 QueryBuilder 无 getParams() 的问题）
        List<Object> params = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT " + selectCols + from + " WHERE 1=1");

        // 3. 应用原有查询条件（同时收集参数）
        if (queryDTO.getCourseId() > 0) {
            sqlBuilder.append(" AND r.courseId = ?");
            params.add(queryDTO.getCourseId());
        }
        if (queryDTO.getClassId() > 0) {
            sqlBuilder.append(" AND r.classId = ?");
            params.add(queryDTO.getClassId());
        }
        if (StringUtils.hasText(queryDTO.getKeyword())) {
            String keyword = "%" + queryDTO.getKeyword().trim() + "%";
            sqlBuilder.append(" AND (r.stuName LIKE ? OR r.stuNumber LIKE ?)");
            params.add(keyword);
            params.add(keyword);
        }

        // 4. ✅ 核心：拼接统一数据权限
        AuthDataPermissionUtil.buildDataPermission(
                sqlBuilder,
                params,
                "r.courseId",
                "r.classId"
        );

        // 5. 排序 + 分页
        sqlBuilder.append(" ORDER BY r.ranking ASC, r.score DESC");

        // 6. 执行分页查询（改用 DatabaseUtil 的 queryPage 方法）
        int pageNum = queryDTO.getPageNum() == null ? 1 : queryDTO.getPageNum();
        int pageSize = queryDTO.getPageSize() == null ? 10 : queryDTO.getPageSize();
        int start = (pageNum - 1) * pageSize;

        // 列表SQL
        String dataSql = sqlBuilder + " LIMIT ? OFFSET ?";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(start);

        // 总数SQL
        String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder + ") AS temp";
        List<Object> countParams = new ArrayList<>(params);

        // 7. 执行查询
        BuiltSql dataBuiltSql = BuiltSql.of(dataSql, dataParams.toArray());
        BuiltSql countBuiltSql = BuiltSql.of(countSql, countParams.toArray());

        PageResult<YeeCourseResultsVO> pageResult = databaseUtil.queryPage(
                queryDTO.getSchoolId(),
                dataBuiltSql,
                countBuiltSql,
                YeeCourseResultsVO::fromResultSet
        );

        return Result.success(pageResult.getRows(), pageResult.getTotal());
    }

    /**
     * 导出课程成绩为Excel
     */
    @Override
    public void exportResults(YeeCourseResultsQueryDTO queryDTO, HttpServletResponse response) throws Exception {
        // 无需手动校验学校
        String selectCols = buildSelectCols(ExportType.FULL_RESULTS);
        String from = " FROM yee_course_results r LEFT JOIN yee_course_class cc ON cc.id = r.classId";

        List<YeeCourseResultsExportVO> exportList = databaseUtil.query(queryDTO.getSchoolId())
                .sql("SELECT " + selectCols + from + " WHERE 1=1")
                .apply(qb -> applyQueryConditions(qb, queryDTO))
                .orderBy("r.ranking ASC, r.score DESC")
                .list(YeeCourseResultsExportVO::fromResultSet);

        ExcelExportUtil.exportWithPreprocess(exportList, response, YeeCourseResultsExportVO.class);

    }

    /**
     * 导出课程额外得分为Excel
     */
    @Override
    public void exportExtraScore(YeeCourseResultsQueryDTO queryDTO, HttpServletResponse response) throws Exception {
        String selectCols = buildSelectCols(ExportType.EXTRA_SCORE);
        String from = " FROM yee_course_results r";

        List<YeeCourseExtraScoreExportVO> exportList = databaseUtil.query(queryDTO.getSchoolId())
                .sql("SELECT " + selectCols + from + " WHERE 1=1")
                .apply(qb -> applyQueryConditions(qb, queryDTO))
                .orderBy("r.stuNumber ASC")
                .list(YeeCourseExtraScoreExportVO::fromResultSet);

        ExcelExportUtil.exportWithPreprocess(exportList, response, YeeCourseExtraScoreExportVO.class);

    }



        // ==================== 静态内部类：数据库执行器 ====================
    private static class DbExecutor {

        static class Rules {
            boolean useVideo;
            long videoRatio;
            String videoItems;

            boolean useWork;
            long workRatio;
            String workItems;

            boolean useExam;
            long examRatio;
            String examItems;

            boolean useDiscuss;
            long discussRatio;
            String discussItems;

            boolean useExtra;
            long extraRatio;

            boolean useReport;
            long reportRatio;
        }

        Rules fetchRules(Connection conn, BuiltSql built) throws Exception {
            try (PreparedStatement st = conn.prepareStatement(built.sql())) {
                setParams(st, built.params());
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) return null;
                    Rules rules = new Rules();
                    rules.useVideo = rs.getBoolean("useVideo");
                    rules.videoRatio = rs.getLong("videoRatio");
                    rules.videoItems = rs.getString("videoItems");
                    rules.useWork = rs.getBoolean("useWork");
                    rules.workRatio = rs.getLong("workRatio");
                    rules.workItems = rs.getString("workItems");
                    rules.useExam = rs.getBoolean("useExam");
                    rules.examRatio = rs.getLong("examRatio");
                    rules.examItems = rs.getString("examItems");
                    rules.useDiscuss = rs.getBoolean("useDiscuss");
                    rules.discussRatio = rs.getLong("discussRatio");
                    rules.discussItems = rs.getString("discussItems");
                    rules.useExtra = rs.getBoolean("useExtra");
                    rules.extraRatio = rs.getLong("extraRatio");
                    rules.useReport = rs.getBoolean("useReport");
                    rules.reportRatio = rs.getLong("reportRatio");
                    return rules;
                }
            }
        }

        List<Long> fetchStudentIds(Connection conn, BuiltSql built) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement(built.sql())) {
                setParams(st, built.params());
                try (ResultSet rs = st.executeQuery()) {
                    List<Long> ids = new ArrayList<>();
                    while (rs.next()) {
                        ids.add(rs.getLong("studentId"));
                    }
                    return ids;
                }
            }
        }

        void recalcRanking(Connection conn, long courseId, long classId) throws SQLException {
            // 1. 查询该班级所有学生的 userId 和 score，按分数降序排列
            String selectSql = """
        SELECT userId, score 
        FROM yee_course_results 
        WHERE courseId = ? AND classId = ? 
        ORDER BY score DESC, userId ASC  -- userId 用于稳定排序（避免并列时顺序随机）
        """;

            List<StudentScore> students = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setLong(1, courseId);
                ps.setLong(2, classId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long userId = rs.getLong("userId");
                        BigDecimal score = rs.getBigDecimal("score");
                        if (score == null) score = BigDecimal.ZERO;
                        students.add(new StudentScore(userId, score));
                    }
                }
            }

            // 2. 在 Java 中计算排名（支持并列）
            Map<Long, Integer> userIdToRank = new HashMap<>();
            int rank = 1;
            for (int i = 0; i < students.size(); i++) {
                // 如果不是第一个，且当前分数 < 前一个分数，则更新排名为当前位置+1
                if (i > 0 && students.get(i).score.compareTo(students.get(i - 1).score) < 0) {
                    rank = i + 1;
                }
                userIdToRank.put(students.get(i).userId, rank);
            }

            // 3. 批量更新 ranking 字段
            String updateSql = "UPDATE yee_course_results SET ranking = ? WHERE userId = ? AND courseId = ? AND classId = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                for (Map.Entry<Long, Integer> entry : userIdToRank.entrySet()) {
                    ps.setInt(1, entry.getValue());
                    ps.setLong(2, entry.getKey());
                    ps.setLong(3, courseId);
                    ps.setLong(4, classId);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        }

        // 辅助记录类（Java 14+ record，若版本低可改用普通 class）
        private static class StudentScore {
            final long userId;
            final BigDecimal score;
            StudentScore(long userId, BigDecimal score) {
                this.userId = userId;
                this.score = score != null ? score : BigDecimal.ZERO;
            }
        }

        void executeUpdate(Connection conn, BuiltSql built) throws SQLException {
            try (PreparedStatement st = conn.prepareStatement(built.sql())) {
                setParams(st, built.params());
                st.executeUpdate();
            }
        }

        private static void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
        }
    }


    @Override
    public Result importExtraScore(Integer schoolId, MultipartFile file, String courseId) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }

        long startTime = System.currentTimeMillis();

        try {
            ImportResult result = databaseUtil.executeInTransaction(schoolId, conn -> {
                try {
                    // 事务内加载学生姓名映射
                Map<String, String> stuNameByNumber = new HashMap<>();
                try (PreparedStatement psLoad = conn.prepareStatement(
                        "SELECT stuNumber, stuName FROM yee_course_results WHERE schoolId = ? AND courseId = ?")) {
                    psLoad.setInt(1, schoolId);
                    psLoad.setString(2, courseId);
                    try (ResultSet rs = psLoad.executeQuery()) {
                        while (rs.next()) {
                            String num = rs.getString("stuNumber");
                            String name = rs.getString("stuName");
                            if (num != null && !num.isBlank()) {
                                stuNameByNumber.put(num, name);
                            }
                        }
                    }
                }

                var ctx = ExtraScoreImportValidationFactory.createContext(stuNameByNumber);

                ImportResult r = ExcelImportBuilder
                        .of(ExtraScoreExcelData.class)
                        .from(file.getInputStream())
                        .businessValidator(ExtraScoreImportValidationFactory.createBusinessValidator(ctx))
                        .batchPersist(batch -> {
                            String sql = "UPDATE yee_course_results SET extraScore = ? " +
                                    "WHERE stuNumber = ? AND schoolId = ? AND courseId = ?";
                            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                                for (ExtraScoreExcelData row : batch) {
                                    ps.setBigDecimal(1, row.getExtraScore() != null
                                            ? row.getExtraScore() : BigDecimal.ZERO);
                                    ps.setString(2, row.getStudentNumber());
                                    ps.setInt(3, schoolId);
                                    ps.setString(4, courseId);
                                    ps.addBatch();
                                }
                                int[] counts = ps.executeBatch();
                                int batchSuccess = 0;
                                for (int c : counts) {
                                    if (c > 0 || c == Statement.SUCCESS_NO_INFO) batchSuccess++;
                                }
                                return batchSuccess;
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .execute();

                    if (!r.isSuccess()) {
                        throw new ImportExportException(r.getFailMessage("部分数据校验失败，已全部回滚"));
                    }
                    return r;
                } catch (ImportExportException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            if (result.isSuccess()) {
                return Result.success("导入成功", (Object)result.toMap());
            } else {
                return Result.error(result.getFailMessage("导入失败"), result.toMap());
            }
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null && !(cause instanceof ImportExportException)) {
                cause = cause.getCause();
            }
            if (cause instanceof ImportExportException) {
                log.error("导入额外分数失败: {}", cause.getMessage());
                return Result.error(cause.getMessage());
            }
            log.error("读取或导入Excel失败", e);
            return Result.error("导入失败：" + e.getMessage(),
                    ImportResult.systemError(e.getMessage(), System.currentTimeMillis() - startTime).toMap());
        }
    }

    @Override
    public void add(YeeCourseResults yeeCourseResults) {
        // databaseUtil.update() 内部会校验学校
        Long generatedId = databaseUtil.update((int) yeeCourseResults.getSchoolId())
                .table("yee_course_results")
                .set("schoolId", yeeCourseResults.getSchoolId())
                .setIfPositive("courseId", yeeCourseResults.getCourseId())
                .setIfPositive("userId", yeeCourseResults.getUserId())
                .setIfNotNegative("score", yeeCourseResults.getScore())
                .setIfNotNegative("videoScore", yeeCourseResults.getVideoScore())
                .setIfNotNegative("examScore", yeeCourseResults.getExamScore())
                .setIfNotNegative("workScore", yeeCourseResults.getWorkScore())
                .setIfNotNegative("discussScore", yeeCourseResults.getDiscussScore())
                .setIfNotNegative("extraScore", yeeCourseResults.getExtraScore())
                .setIfNotNegative("reportScore", yeeCourseResults.getReportScore())
                .setIfNotEmpty("stuName", yeeCourseResults.getStuName())
                .setIfNotEmpty("stuNumber", yeeCourseResults.getStuNumber())
                .setIfPositive("classId", yeeCourseResults.getClassId())
                .setIfPositive("ranking", yeeCourseResults.getRanking())
                .setIfNotNegative("videoResult", yeeCourseResults.getVideoResult())
                .setIfNotNegative("workResult", yeeCourseResults.getWorkResult())
                .setIfNotNegative("examResult", yeeCourseResults.getExamResult())
                .setIfNotNegative("discussResult", yeeCourseResults.getDiscussResult())
                .setIfNotNegative("reportResult", yeeCourseResults.getReportResult())
                .setIfNotNull("calcDate", yeeCourseResults.getCalcDate())
                .insert();

        if (generatedId != null) {
            yeeCourseResults.setId(generatedId);
        }
    }

    @Override
    public Result update(YeeCourseResults yeeCourseResults) {
        if (yeeCourseResults == null || yeeCourseResults.getId() <= 0) {
            return Result.error("参数无效");
        }

        int rows = databaseUtil.update((int) yeeCourseResults.getSchoolId())
                .table("yee_course_results")
                .setIfPositive("courseId", yeeCourseResults.getCourseId())
                .setIfPositive("userId", yeeCourseResults.getUserId())
                .setIfNotNegative("score", yeeCourseResults.getScore())
                .setIfNotNegative("videoScore", yeeCourseResults.getVideoScore())
                .setIfNotNegative("examScore", yeeCourseResults.getExamScore())
                .setIfNotNegative("workScore", yeeCourseResults.getWorkScore())
                .setIfNotNegative("discussScore", yeeCourseResults.getDiscussScore())
                .setIfNotNegative("extraScore", yeeCourseResults.getExtraScore())
                .setIfNotNegative("reportScore", yeeCourseResults.getReportScore())
                .setIfNotEmpty("stuName", yeeCourseResults.getStuName())
                .setIfNotEmpty("stuNumber", yeeCourseResults.getStuNumber())
                .setIfPositive("classId", yeeCourseResults.getClassId())
                .setIfPositive("ranking", yeeCourseResults.getRanking())
                .setIfNotNegative("videoResult", yeeCourseResults.getVideoResult())
                .setIfNotNegative("workResult", yeeCourseResults.getWorkResult())
                .setIfNotNegative("examResult", yeeCourseResults.getExamResult())
                .setIfNotNegative("discussResult", yeeCourseResults.getDiscussResult())
                .setIfNotNegative("reportResult", yeeCourseResults.getReportResult())
                .setIfNotNull("calcDate", yeeCourseResults.getCalcDate())
                .eq("id", yeeCourseResults.getId())
                .update();

        if (rows > 0) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败，记录未找到或无变更");
        }
    }

    @Override
    public void delete(long id, long schoolId, long courseId, long classId) {
        int rows = databaseUtil.update((int) schoolId)
                .table("yee_course_results")
                .eq("userId", id)
                .eq("courseId", courseId)
                .eq("classId", classId)
                .delete();

        if (rows == 0) {
            throw new BusinessException("删除失败，记录不存在");
        }
    }

    /**
     * 计算指定班级所有学生的成绩
     * 基于课程计分规则，计算班级内所有学生的各项成绩（视频、作业、考试、讨论、额外分、实践报告）
     * 并重新计算班级排名
     */
    @Override
    @Async("exportExecutor")
    public void calculateScore(int schoolId, long courseId, long classId) {
        try {
            databaseUtil.executeInTransaction(schoolId, conn -> {
                try {
                    SqlBuilder builder = new SqlBuilder();
                    DbExecutor executor = new DbExecutor();

                    // 获取计分规则
                    DbExecutor.Rules rules = executor.fetchRules(conn, builder.rulesOne(courseId, classId));
                    if (rules == null) {
                        log.error("该班级未配置计分规则");
                        return null;
                    }

                    // 获取学生列表
                    List<Long> studentIds = executor.fetchStudentIds(conn, builder.students(classId));
                    if (studentIds.isEmpty()) {
                        return null;
                    }

                    final int BATCH_SIZE = 20;

                    for (int i = 0; i < studentIds.size(); i += BATCH_SIZE) {
                        List<Long> batch = studentIds.subList(i, Math.min(i + BATCH_SIZE, studentIds.size()));
                        processBatch(conn, builder, rules, courseId, classId, schoolId, batch);
                    }

                    // 更新排名和规则统计时间
                    executor.recalcRanking(conn, courseId, classId);
                    executor.executeUpdate(conn, builder.updateRulesCalc(courseId, classId));

                    return null;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Exception e) {
            log.error("计算成绩失败", e);
        }
    }

    // ==================== 批量处理学生 ====================
    private void processBatch(
            Connection conn,
            SqlBuilder builder,
            DbExecutor.Rules rules,
            long courseId,
            long classId,
            long schoolId,
            List<Long> studentIds) throws SQLException {

        if (studentIds.isEmpty()) return;

        // 1. 预加载学生基本信息
        Map<Long, StudentInfo> studentInfoMap = fetchAllStudents(conn, studentIds);

            // 2. 构建权重配置
            WeightConfig videoCfg = new WeightConfig(
                    rules.useVideo,
                    rules.videoItems,
                    () -> {
                        try {
                            return fetchAllVideoNodeIds(conn, courseId);
                        } catch (SQLException e) {
                            return Collections.emptyList();
                        }
                    }
            );

            WeightConfig workCfg = new WeightConfig(
                    rules.useWork,
                    rules.workItems,
                    () -> {
                        try {
                            return fetchAllWorkIds(conn, courseId);
                        } catch (SQLException e) {
                            return Collections.emptyList();
                        }
                    }
            );

            WeightConfig examCfg = new WeightConfig(
                    rules.useExam,
                    rules.examItems,
                    () -> {
                        try {
                            return fetchAllExamIds(conn, courseId);
                        } catch (SQLException e) {
                            return Collections.emptyList();
                        }
                    }
            );
            WeightConfig discussCfg = new WeightConfig(
                    rules.useDiscuss,
                    rules.discussItems,
                    () -> {
                        try {
                            return fetchAllDiscussIds(conn, courseId);
                        } catch (SQLException e) {
                            return Collections.emptyList(); // 安全兜底
                        }
                    }
            );

            List<Long> videoNodeIds = videoCfg.getSelectedIds();
            List<Long> workIds      = workCfg.getSelectedIds();
            List<Long> examIds      = examCfg.getSelectedIds();
            List<Long> discussIds   = discussCfg.getSelectedIds();

            Map<Long, Map<Long, VideoNodeRecord>> videoStudyData =
                    videoCfg.isUsed() ? fetchVideoStudyDataBatch(conn, courseId, studentIds, videoNodeIds) : Collections.emptyMap();

            Map<Long, Map<Long, ScoreRecord>> workScoreData =
                    workCfg.isUsed() ? fetchAllWorkScoreData(conn, courseId, studentIds, workIds) : Collections.emptyMap();

            Map<Long, Map<Long, ScoreRecord>> examScoreData =
                    examCfg.isUsed() ? fetchAllExamScoreData(conn, courseId, studentIds, examIds) : Collections.emptyMap();

            Map<Long, Map<Long, Double>> discussScoreData =
                    discussCfg.isUsed() ? fetchAllDiscussScoreData(conn, courseId, studentIds, discussIds) : Collections.emptyMap();

            Map<Long, Boolean> reportStatus = rules.useReport ? fetchReportStatus(conn, courseId, classId, studentIds) : Collections.emptyMap();

            // 5. 批量 UPDATE 已存在的记录
            Set<Long> updatedStudentIds = batchUpdateExistingResults(
                    conn, builder, rules, courseId, classId, schoolId,
                    studentIds, studentInfoMap,
                    videoCfg, workCfg, examCfg, discussCfg,
                    videoStudyData, workScoreData, examScoreData, discussScoreData,
                    reportStatus
            );

            // 6. 找出需要 INSERT 的新学生
            List<Long> toInsert = new ArrayList<>();
            for (Long sid : studentIds) {
                if (!updatedStudentIds.contains(sid)) {
                    toInsert.add(sid);
                }
            }

            // 7. 批量 INSERT 新学生
            if (!toInsert.isEmpty()) {
                batchInsertNewResults(
                        conn, builder, rules, courseId, classId, schoolId,
                        toInsert, studentInfoMap,
                        videoCfg, workCfg, examCfg, discussCfg,
                        videoStudyData, workScoreData, examScoreData, discussScoreData,
                        reportStatus
                );
            }
    }

    // ===== 批量 UPDATE 已存在记录 =====
    private Set<Long> batchUpdateExistingResults(
            Connection conn,
            SqlBuilder builder,
            DbExecutor.Rules rules,
            long courseId,
            long classId,
            long schoolId,
            List<Long> studentIds,
            Map<Long, StudentInfo> studentInfoMap,
            WeightConfig videoCfg,
            WeightConfig workCfg,
            WeightConfig examCfg,
            WeightConfig discussCfg,
            Map<Long, Map<Long, VideoNodeRecord>> videoStudyData,
            Map<Long, Map<Long, ScoreRecord>> workScoreData,
            Map<Long, Map<Long, ScoreRecord>> examScoreData,
            Map<Long, Map<Long, Double>> discussScoreData,
            Map<Long, Boolean> reportStatus) throws SQLException {

        String updateSql = builder.buildUpdateResultSql();
        Set<Long> updated = new HashSet<>();
        java.sql.Date calcDate = new java.sql.Date(System.currentTimeMillis());

        try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
            for (Long studentId : studentIds) {
                double[] videoScores   = calculateModuleScore(videoStudyData.get(studentId), videoCfg, ModuleType.VIDEO);
                double[] workScores    = calculateModuleScore(workScoreData.get(studentId), workCfg, ModuleType.WORK);
                double[] examScores    = calculateModuleScore(examScoreData.get(studentId), examCfg, ModuleType.EXAM);
                double[] discussScores = calculateModuleScore(discussScoreData.get(studentId), discussCfg, ModuleType.DISCUSS);

                double videoScore   = videoScores[0] * rules.videoRatio / 100.0;
                double workScore    = workScores[0] * rules.workRatio / 100.0;
                double examScore    = examScores[0] * rules.examRatio / 100.0;
                double discussScore = discussScores[0] * rules.discussRatio / 100.0;
                double extraScore   = rules.useExtra ? 100.0 * rules.extraRatio / 100.0 : 0.0; // 额外分满分100，占 extraRatio%
                boolean reportPassed = rules.useReport && reportStatus.getOrDefault(studentId, false);
                double reportResult = reportPassed ? 100.0 : 0.0;
                double reportScore  = rules.useReport ? reportResult * rules.reportRatio / 100.0 : 0.0;
                double totalScore   = videoScore + workScore + examScore + discussScore + extraScore + reportScore;

                StudentInfo info = studentInfoMap.getOrDefault(studentId, new StudentInfo("", ""));

                int idx = 1;
                ps.setDouble(idx++, totalScore);
                ps.setDouble(idx++, videoScore);
                ps.setDouble(idx++, workScore);
                ps.setDouble(idx++, examScore);
                ps.setDouble(idx++, discussScore);
                ps.setDouble(idx++, extraScore);
                ps.setDouble(idx++, reportScore);
                ps.setString(idx++, info.name);
                ps.setString(idx++, info.number);
                ps.setDouble(idx++, videoScores[1]);
                ps.setDouble(idx++, workScores[1]);
                ps.setDouble(idx++, examScores[1]);
                ps.setDouble(idx++, discussScores[1]);
                ps.setDouble(idx++, reportResult);
                ps.setLong(idx++, schoolId);
                ps.setDate(idx++, calcDate);
                ps.setLong(idx++, courseId);
                ps.setLong(idx++, classId);
                ps.setLong(idx++, studentId);

                if (ps.executeUpdate() > 0) {
                    updated.add(studentId);
                }
            }
        }
        return updated;
    }

    // ===== 批量 INSERT 新学生 =====
    private void batchInsertNewResults(
            Connection conn,
            SqlBuilder builder,
            DbExecutor.Rules rules,
            long courseId,
            long classId,
            long schoolId,
            List<Long> studentIds,
            Map<Long, StudentInfo> studentInfoMap,
            WeightConfig videoCfg,
            WeightConfig workCfg,
            WeightConfig examCfg,
            WeightConfig discussCfg,
            Map<Long, Map<Long, VideoNodeRecord>> videoStudyData,
            Map<Long, Map<Long, ScoreRecord>> workScoreData,
            Map<Long, Map<Long, ScoreRecord>> examScoreData,
            Map<Long, Map<Long, Double>> discussScoreData,
            Map<Long, Boolean> reportStatus) throws SQLException {

        String insertSql = builder.buildInsertResultSql();
        java.sql.Date calcDate = new java.sql.Date(System.currentTimeMillis());

        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (Long studentId : studentIds) {
                double[] videoScores   = calculateModuleScore(videoStudyData.get(studentId), videoCfg, ModuleType.VIDEO);
                double[] workScores    = calculateModuleScore(workScoreData.get(studentId), workCfg, ModuleType.WORK);
                double[] examScores    = calculateModuleScore(examScoreData.get(studentId), examCfg, ModuleType.EXAM);
                double[] discussScores = calculateModuleScore(discussScoreData.get(studentId), discussCfg, ModuleType.DISCUSS);

                double videoScore   = videoScores[0] * rules.videoRatio / 100.0;
                double workScore    = workScores[0] * rules.workRatio / 100.0;
                double examScore    = examScores[0] * rules.examRatio / 100.0;
                double discussScore = discussScores[0] * rules.discussRatio / 100.0;
                double extraScore   = rules.useExtra ? 100.0 * rules.extraRatio / 100.0 : 0.0; // 额外分按比例
                boolean reportPassed = rules.useReport && reportStatus.getOrDefault(studentId, false);
                double reportResult = reportPassed ? 100.0 : 0.0;
                double reportScore  = rules.useReport ? reportResult * rules.reportRatio / 100.0 : 0.0;
                double totalScore   = videoScore + workScore + examScore + discussScore + extraScore + reportScore;

                StudentInfo info = studentInfoMap.getOrDefault(studentId, new StudentInfo("", ""));

                int idx = 1;
                ps.setLong(idx++, courseId);
                ps.setLong(idx++, studentId);
                ps.setDouble(idx++, totalScore);
                ps.setDouble(idx++, videoScore);
                ps.setDouble(idx++, workScore);
                ps.setDouble(idx++, examScore);
                ps.setDouble(idx++, discussScore);
                ps.setDouble(idx++, extraScore);
                ps.setDouble(idx++, reportScore);
                ps.setString(idx++, info.name);
                ps.setString(idx++, info.number);
                ps.setLong(idx++, classId);
                ps.setDouble(idx++, videoScores[1]);
                ps.setDouble(idx++, workScores[1]);
                ps.setDouble(idx++, examScores[1]);
                ps.setDouble(idx++, discussScores[1]);
                ps.setDouble(idx++, reportResult);
                ps.setLong(idx++, schoolId);
                ps.setDate(idx++, calcDate);

                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ===== 权重配置辅助类（重构）=====
    private static class WeightConfig {
        private final boolean use;
        private final List<Long> selectedIds;           // 用户实际选中的节点（来自规则）
        private final Map<Long, Double> explicitWeights; // 显式比例（百分比）

        public WeightConfig(boolean use, String jsonItems, Supplier<List<Long>> allCourseIdsSupplier) {
            this.use = use;

            if (!use) {
                // 未启用权重：空列表 + 空 map
                this.selectedIds = Collections.emptyList();
                this.explicitWeights = Collections.emptyMap();
                return;
            }

            // 尝试解析用户配置
            Map<Long, Double> parsed = parseJsonWeights(jsonItems);

            List<Long> resolvedSelectedIds;
            Map<Long, Double> resolvedExplicitWeights;

            if (!parsed.isEmpty()) {
                // 情况1: 用户显式配置了节点和权重
                resolvedSelectedIds = new ArrayList<>(parsed.keySet());
                resolvedExplicitWeights = parsed;
            } else {
                // 情况2: 未配置 → 视为“选中全部”，权重均分（explicitWeights 为空表示自动均分）
                try {
                    List<Long> allIds = allCourseIdsSupplier.get();
                    resolvedSelectedIds = (allIds != null) ? new ArrayList<>(allIds) : Collections.emptyList();
                } catch (Exception e) {
                    // 容错：获取失败时设为空
                    resolvedSelectedIds = Collections.emptyList();
                }
                resolvedExplicitWeights = Collections.emptyMap(); // 表示使用均分逻辑
            }

            // 最终赋值（只赋一次，避免 IDE 警告）
            this.selectedIds = resolvedSelectedIds;
            this.explicitWeights = resolvedExplicitWeights;
        }

        public boolean isUsed() {
            return use && !selectedIds.isEmpty();
        }

        public List<Long> getSelectedIds() {
            return selectedIds;
        }

        public boolean hasCustomWeights() {
            return !explicitWeights.isEmpty();
        }

        public Map<Long, Double> getExplicitWeights() {
            return explicitWeights;
        }
    }

    // ===== 统一模块得分计算器 =====
    private enum ModuleType { VIDEO, WORK, EXAM, DISCUSS }

    // 修改方法签名
    private double[] calculateModuleScore(Object data, WeightConfig config, ModuleType type) {
        if (!config.isUsed()) {
            return new double[]{0.0, 0.0};
        }

        List<Long> selectedIds = config.getSelectedIds();
        boolean hasCustom = config.hasCustomWeights();
        Map<Long, Double> explicitWeights = config.getExplicitWeights();

        double moduleScore = 0.0; // 直接累加 (pct × nodeRatio%)
        double rawTotal = 0.0;

        if (hasCustom) {
            // ✅ 按用户设置的比例计算（比例是百分比，如 60.0）
            for (Long id : selectedIds) {
                Double ratioPct = explicitWeights.get(id); // 如 60.0
                if (ratioPct == null || ratioPct <= 0) continue;

                double pct = getNodePct(data, type, id);
                rawTotal += pct;
                moduleScore += pct * (ratioPct / 100.0); // 转成小数
            }
        } else {
            // ✅ 未设置比例 → 均分（每个占 1/N）
            int N = selectedIds.size();
            if (N == 0) return new double[]{0.0, 0.0};
            double equalRatio = 1.0 / N;

            for (Long id : selectedIds) {
                double pct = getNodePct(data, type, id);
                rawTotal += pct;
                moduleScore += pct * equalRatio;
            }
        }

        moduleScore = Math.min(100.0, Math.max(0.0, moduleScore));
        return new double[]{moduleScore, rawTotal};
    }

    // 提取通用方法（保持不变）
    private double getNodePct(Object data, ModuleType type, Long id) {
        switch (type) {
            case VIDEO -> {
                Map<Long, VideoNodeRecord> map = (Map<Long, VideoNodeRecord>) data;
                VideoNodeRecord rec = (map != null) ? map.get(id) : null;
                return (rec != null && rec.isCompleted()) ? 100.0 : 0.0;
            }
            case WORK, EXAM -> {
                Map<Long, ScoreRecord> map = (Map<Long, ScoreRecord>) data;
                ScoreRecord rec = (map != null) ? map.get(id) : null;
                if (rec != null && rec.totalScore > 0) {
                    double pct = (rec.finalScore / rec.totalScore) * 100.0;
                    return Math.min(100.0, Math.max(0.0, pct));
                }
                return 0.0;
            }
            case DISCUSS -> {
                Map<Long, Double> map = (Map<Long, Double>) data;
                Double score = (map != null) ? map.get(id) : null;
                if (score != null) {
                    return Math.min(100.0, Math.max(0.0, score));
                }
                return 0.0;
            }
            default -> {
                return 0.0;
            }
        }
    }

    // ===== JSON 权重解析（健壮）=====
    private static  Map<Long, Double> parseJsonWeights(String json) {
        if (json == null || json.trim().isEmpty() || "{}".equals(json.trim())) {
            return Collections.emptyMap();
        }
        try {
            JSONObject obj = JSONObject.parseObject(json);
            if (obj == null || obj.isEmpty()) return Collections.emptyMap();

            Map<Long, Double> weights = new HashMap<>();
            for (String key : obj.keySet()) {
                try {
                    long id = Long.parseLong(key.trim());
                    Double w = obj.getDouble(key);
                    if (w != null && w > 0) {
                        weights.put(id, w);
                    }
                } catch (NumberFormatException ignored) {}
            }
            return weights;
        } catch (Exception e) {
            log.warn("解析权重 JSON 失败: {}", json, e);
            return Collections.emptyMap();
        }
    }

    // ===== 获取全量节点 ID 方法 =====
    private List<Long> fetchAllVideoNodeIds(Connection conn, long courseId) throws SQLException {
        String sql = "SELECT id FROM yee_node WHERE courseId = ? AND tabVideo = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            ResultSet rs = ps.executeQuery();
            List<Long> ids = new ArrayList<>();
            while (rs.next()) ids.add(rs.getLong("id"));
            return ids;
        }
    }

    private List<Long> fetchAllWorkIds(Connection conn, long courseId) throws SQLException {
        String sql = "SELECT id FROM yee_work WHERE courseId = ? AND allow = 1"; // ← 添加 allow = 1
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            ResultSet rs = ps.executeQuery();
            List<Long> ids = new ArrayList<>();
            while (rs.next()) ids.add(rs.getLong("id"));
            return ids;
        }
    }

    private List<Long> fetchAllExamIds(Connection conn, long courseId) throws SQLException {
        String sql = "SELECT id FROM yee_exam WHERE courseId = ? AND allow = 1"; // ← 添加 allow = 1
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            ResultSet rs = ps.executeQuery();
            List<Long> ids = new ArrayList<>();
            while (rs.next()) ids.add(rs.getLong("id"));
            return ids;
        }
    }

    private List<Long> fetchAllDiscussIds(Connection conn, long courseId) throws SQLException {
        String sql = "SELECT id FROM yee_discuss WHERE courseId = ? AND isDelete = 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            ResultSet rs = ps.executeQuery();
            List<Long> ids = new ArrayList<>();
            while (rs.next()) ids.add(rs.getLong("id"));
            return ids;
        }
    }

    // ===== 数据预加载方法（保持原样）=====
    private Map<Long, StudentInfo> fetchAllStudents(Connection conn, List<Long> studentIds) throws SQLException {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        String placeholders = studentIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT id, name, number FROM yee_student WHERE id IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < studentIds.size(); i++) {
                ps.setLong(i + 1, studentIds.get(i));
            }
            ResultSet rs = ps.executeQuery();
            Map<Long, StudentInfo> map = new HashMap<>();
            while (rs.next()) {
                map.put(rs.getLong("id"), new StudentInfo(rs.getString("name"), rs.getString("number")));
            }
            return map;
        }
    }

private Map<Long, Map<Long, VideoNodeRecord>> fetchAllVideoStudyData(
        Connection conn, long courseId, List<Long> studentIds, List<Long> nodeIds) throws SQLException {
    if (studentIds.isEmpty() || nodeIds.isEmpty()) return Collections.emptyMap();

    // ===================== 核心配置：批次大小，你想改就改这里 =====================
    final int BATCH_SIZE = 50;
    // ==========================================================================

    Map<Long, Map<Long, VideoNodeRecord>> result = new HashMap<>();
    int totalStudents = studentIds.size();

    for (int i = 0; i < totalStudents; i += BATCH_SIZE) {
        int end = Math.min(i + BATCH_SIZE, totalStudents);
        List<Long> batchStuIds = studentIds.subList(i, end);

        String stuPh = batchStuIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String nodePh = nodeIds.stream().map(id -> "?").collect(Collectors.joining(", "));

        String sql = """
            SELECT st.userId, st.nodeId, COALESCE(SUM(st.duration), 0) AS studiedDuration, n.videoDuration
            FROM yee_study_time st
            JOIN yee_node n ON st.nodeId = n.id
            WHERE st.userId IN (%s) AND st.courseId = ? AND n.tabVideo = 1 AND n.courseId = ? AND n.id IN (%s)
            GROUP BY st.userId, st.nodeId, n.videoDuration
            """.formatted(stuPh, nodePh);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            // 填充当前批次学生
            for (Long sid : batchStuIds) ps.setLong(idx++, sid);
            ps.setLong(idx++, courseId);
            ps.setLong(idx++, courseId);
            // 填充节点
            for (Long nid : nodeIds) ps.setLong(idx++, nid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long uid = rs.getLong("userId");
                    long nid = rs.getLong("nodeId");
                    // 合并到总结果
                    result.computeIfAbsent(uid, k -> new HashMap<>())
                            .put(nid, new VideoNodeRecord(rs.getDouble("studiedDuration"), rs.getInt("videoDuration")));
                }
            }
        }
    }

    return result;
}

    private Map<Long, Map<Long, ScoreRecord>> fetchAllWorkScoreData(
            Connection conn, long courseId, List<Long> studentIds, List<Long> workIds) throws SQLException {

        if (studentIds.isEmpty() || workIds.isEmpty()) return Collections.emptyMap();

        String stuPh = studentIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String workPh = workIds.stream().map(id -> "?").collect(Collectors.joining(", "));

        // JOIN yee_work 获取总分（w.score）
        String sql = """
        SELECT 
            ws.userId, 
            ws.workId, 
            ws.finalScore, 
            w.score AS totalScore
        FROM yee_work_score ws
        JOIN yee_work w ON ws.workId = w.id
        WHERE ws.courseId = ?
          AND w.allow = 1 
          AND ws.userId IN (%s) 
          AND ws.scored = 1 
          AND ws.workId IN (%s)
        """.formatted(stuPh, workPh);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            int idx = 2;
            for (Long sid : studentIds) ps.setLong(idx++, sid);
            for (Long wid : workIds) ps.setLong(idx++, wid);

            ResultSet rs = ps.executeQuery();
            Map<Long, Map<Long, ScoreRecord>> result = new HashMap<>();

            while (rs.next()) {
                long uid = rs.getLong("userId");
                long wid = rs.getLong("workId");
                double finalScore = rs.getDouble("finalScore");
                double totalScore = rs.getDouble("totalScore");
                result.computeIfAbsent(uid, k -> new HashMap<>())
                        .put(wid, new ScoreRecord(finalScore, totalScore));
            }
            return result;
        }
    }

    private Map<Long, Map<Long, ScoreRecord>> fetchAllExamScoreData(
            Connection conn, long courseId, List<Long> studentIds, List<Long> examIds) throws SQLException {

        if (studentIds.isEmpty() || examIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String stuPlaceholders = studentIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String examPlaceholders = examIds.stream().map(id -> "?").collect(Collectors.joining(", "));

        // 关键：JOIN yee_exam 获取总分（e.score）
        String sql = """
        SELECT 
            es.userId,
            es.examId,
            es.finalScore,
            e.score AS totalScore
        FROM yee_exam_score es
        INNER JOIN yee_exam e ON es.examId = e.id
        WHERE es.courseId = ?
          AND e.allow = 1 
          AND es.userId IN (%s)
          AND es.scored = 1
          AND es.examId IN (%s)
        """.formatted(stuPlaceholders, examPlaceholders);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            int idx = 2;
            for (Long sid : studentIds) ps.setLong(idx++, sid);
            for (Long eid : examIds) ps.setLong(idx++, eid);

            ResultSet rs = ps.executeQuery();
            Map<Long, Map<Long, ScoreRecord>> result = new HashMap<>();

            while (rs.next()) {
                long userId = rs.getLong("userId");
                long examId = rs.getLong("examId");
                double finalScore = rs.getDouble("finalScore");
                double totalScore = rs.getDouble("totalScore");

                result.computeIfAbsent(userId, k -> new HashMap<>())
                        .put(examId, new ScoreRecord(finalScore, totalScore));
            }
            return result;
        }
    }

    private Map<Long, Map<Long, Double>> fetchAllDiscussScoreData(Connection conn, long courseId, List<Long> studentIds, List<Long> discussIds) throws SQLException {
        if (studentIds.isEmpty() || discussIds.isEmpty()) return Collections.emptyMap();
        String stuPh = studentIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String disPh = discussIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
    SELECT userId, discussId, score
    FROM yee_discuss_score
    WHERE courseId = ? AND userId IN (%s) AND scored = 1 AND discussId IN (%s)
    """.formatted(stuPh, disPh);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            int idx = 2;
            for (Long sid : studentIds) ps.setLong(idx++, sid);
            for (Long did : discussIds) ps.setLong(idx++, did);
            ResultSet rs = ps.executeQuery();
            Map<Long, Map<Long, Double>> result = new HashMap<>();
            while (rs.next()) {
                long uid = rs.getLong("userId");
                long did = rs.getLong("discussId");
                Object scoreObj = rs.getObject("score");
                double score = scoreObj != null ? ((Number) scoreObj).doubleValue() : 0.0;
                result.computeIfAbsent(uid, k -> new HashMap<>()).put(did, score);
            }
            return result;
        }
    }

    // ===== 实践报告状态获取 =====
    private Map<Long, Boolean> fetchReportStatus(Connection conn, long courseId, long classId, List<Long> studentIds) throws SQLException {
        if (studentIds.isEmpty()) return Collections.emptyMap();
        String stuPh = studentIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
            SELECT studentId, status
            FROM yee_practice_report
            WHERE courseId = ? AND classId = ? AND studentId IN (%s)
            """.formatted(stuPh);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            ps.setLong(2, classId);
            for (int i = 0; i < studentIds.size(); i++) {
                ps.setLong(i + 3, studentIds.get(i));
            }
            ResultSet rs = ps.executeQuery();
            Map<Long, Boolean> result = new HashMap<>();
            while (rs.next()) {
                long sid = rs.getLong("studentId");
                int status = rs.getInt("status");
                result.put(sid, status == 2); // status: 1=待审核, 2=已通过, 3=未通过
            }
            return result;
        }
    }

    // ===== 内部类 =====
        private record StudentInfo(String name, String number) {
    }

    private record ScoreRecord(double finalScore, double totalScore) {
    }

    private record VideoNodeRecord(double studiedDuration, int videoDuration) {
        public boolean isCompleted() {
                return videoDuration > 0 && studiedDuration >= videoDuration * 0.97;
            }
        }

    /**
     * 一次只查 50 个学生，用完即释放，不堆内存
     */
    private Map<Long, Map<Long, VideoNodeRecord>> fetchVideoStudyDataBatch(
            Connection conn, long courseId, List<Long> batchStuIds, List<Long> nodeIds) throws SQLException {

        if (batchStuIds.isEmpty() || nodeIds.isEmpty()) return Collections.emptyMap();

        String stuPh = batchStuIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String nodePh = nodeIds.stream().map(id -> "?").collect(Collectors.joining(", "));

        String sql = """
        SELECT st.userId, st.nodeId, COALESCE(SUM(st.duration), 0) AS studiedDuration, n.videoDuration
        FROM yee_study_time st
        JOIN yee_node n ON st.nodeId = n.id
        WHERE st.userId IN (%s) AND st.courseId = ? AND n.tabVideo = 1 AND n.courseId = ? AND n.id IN (%s)
        GROUP BY st.userId, st.nodeId, n.videoDuration
        """.formatted(stuPh, nodePh);

        Map<Long, Map<Long, VideoNodeRecord>> batchResult = new HashMap<>(batchStuIds.size());

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Long sid : batchStuIds) ps.setLong(idx++, sid);
            ps.setLong(idx++, courseId);
            ps.setLong(idx++, courseId);
            for (Long nid : nodeIds) ps.setLong(idx++, nid);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long uid = rs.getLong("userId");
                    long nid = rs.getLong("nodeId");
                    batchResult.computeIfAbsent(uid, k -> new HashMap<>())
                            .put(nid, new VideoNodeRecord(rs.getDouble("studiedDuration"), rs.getInt("videoDuration")));
                }
            }
        }
        return batchResult;
    }
}







