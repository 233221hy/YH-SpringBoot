package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.dto.DiscussStatsDTO;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.NodeDiscussStatsService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import cn.xfywz.guozespring.excel.ExcelExportStyles;
import cn.xfywz.guozespring.excel.ResponseExportUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.merge.OnceAbsoluteMergeStrategy;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;

import static cn.xfywz.guozespring.excel.ExcelDataPreprocessor.safeReplaceComma;

/**
 * 视频（章节）讨论统计
 * 统计来源表：
 * - 发帖/回复：yee_node_discuss（字段：userId、courseId、nodeId、replyId、addTime、schoolId、isDelete）
 * - 点赞：yee_node_reply_like（与 yee_node_discuss 通过 replyId 关联）
 *
 * 统计逻辑：
 * - 学生列表：以课程学生（yee_course_student）为基表，左连接按 userId 聚合的讨论统计（总量/主贴/回复/点赞），支持按班级、节点、时间过滤。
 * - 老师列表：以本课程任课教师（yee_course_class 去重 teacherId）为基表，左连接按 userId 聚合的讨论统计，支持节点、时间过滤；班级筛选限定任教班级集合。
 */
@Slf4j
@Service
public class NodeDiscussStatsServiceImpl implements NodeDiscussStatsService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    public Result list(DiscussStatsDTO param) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            int pageNum = (param.getPageNum() == null || param.getPageNum() < 1) ? 1 : param.getPageNum();
            int pageSize = (param.getPageSize() == null || param.getPageSize() < 1) ? 10 : param.getPageSize();
            int offset = (pageNum - 1) * pageSize;
            int statsType = (param.getStatsType() == null) ? 1 : param.getStatsType();

            // 老师统计列表（statsType=2）
            if (statsType == 2) {
                return getTeacherStatsList(slSchool, param, pageNum, pageSize, offset);
            }

            // 学生统计列表（默认）
            return getStudentStatsList(slSchool, param, pageNum, pageSize, offset);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result detailList(DiscussStatsDTO param) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            if (param.getUserId() <= 0) {
                return Result.error("缺少目标用户ID");
            }
            int pageNum = (param.getPageNum() == null || param.getPageNum() < 1) ? 1 : param.getPageNum();
            int pageSize = (param.getPageSize() == null || param.getPageSize() < 1) ? 10 : param.getPageSize();
            int offset = (pageNum - 1) * pageSize;
            int listType = (param.getListType() == null || param.getListType() < 1 || param.getListType() > 2) ? 1 : param.getListType();

            return getUserDiscussDetailList(slSchool, param, listType, pageSize, offset);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public void exportData(DiscussStatsDTO param, HttpServletResponse response) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                try {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write("学校不存在或未审核");
                } catch (Exception ignored) {}
                return;
            }

            // 查询课程名称
            String courseName = "";
            try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
                 PreparedStatement cps = conn.prepareStatement("SELECT name FROM yee_course WHERE schoolId = ? AND id = ? LIMIT 1")) {
                int idx = 1;
                cps.setObject(idx++, param.getSchoolId());
                cps.setObject(idx++, param.getCourseId());
                try (ResultSet rs = cps.executeQuery()) {
                    if (rs.next()) courseName = safeReplaceComma(rs.getString(1));
                }
            } catch (Exception ignored) {}

            String dateStr = new SimpleDateFormat("yyyy年MM月dd日").format(new Date());
            // 生成文件名
            Integer statsType = param.getStatsType() == null ? 1 : param.getStatsType();
            String typeName = (statsType == 2) ? "老师视频讨论统计表" : "学生视频讨论统计表";
//            String currentTime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String fileBaseName = typeName + "_" + System.currentTimeMillis();
            ResponseExportUtil.setExcelRespProp(response, fileBaseName);

            // 标题
            String title = typeName + "—（" + dateStr + " 导出）";
            String[] headers = (statsType == 2)
                    ? new String[]{"工号","老师姓名","班级名称","参与总量","主贴数量","回复数量","获赞数量"}
                    : new String[]{"学号","姓名","课程班级","总发帖数","主帖数","回复数","点赞数"};
            List<List<String>> head = new ArrayList<>();
            for (String h : headers) head.add(Arrays.asList(courseName + " 的" + title , h));

            List<List<String>> dataRows = new ArrayList<>();
            try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
                if (statsType == 2) {
                    // 老师统计导出
                    exportTeacherData(conn, param, dataRows);
                } else {
                    // 学生统计导出
                    exportStudentData(conn, param, dataRows);
                }
            }

            HorizontalCellStyleStrategy styleStrategy = ExcelExportStyles.defaultStyleStrategy();
            try {
                EasyExcel.write(response.getOutputStream())
                        .autoCloseStream(false)
                        .head(head)
                        .registerWriteHandler(styleStrategy) // 样式策略
                        .registerWriteHandler(ExcelExportStyles.defaultTitleRow(headers.length))
                        .registerWriteHandler(new OnceAbsoluteMergeStrategy(0, 0, 0, headers.length - 1))
//                        .registerWriteHandler(ExcelExportStyles.createFreezeAndWidthHandler(new int[]{16, 16, 20, 8, 8, 8, 10}, 2))
//                        .registerWriteHandler(ExcelExportStyles.textColumns(new int[]{0}))
                        .sheet("章节讨论统计")
                        .doWrite(dataRows);
                response.flushBuffer();
            } catch (Exception e) {
                try {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write("导出失败:" + e.getMessage());
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            try {
                if (response != null) {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write("导出失败:" + e.getMessage());
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * 获取老师统计数据列表
     */
    private Result getTeacherStatsList(SlSchool slSchool, DiscussStatsDTO param, int pageNum, int pageSize, int offset) throws Exception {
        // 统计任课老师数量（按课程、可选班级与关键字）
        String countSqlT = buildTeacherCountSql(param);
        List<Object> countParamsT = buildTeacherCountParams(param);

        String dataSqlT = buildTeacherDataSqlGeneric(param, true, false);
        List<Object> dataParamsT = buildTeacherDataParams(param, pageSize, offset);

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            long total = 0;
            try (PreparedStatement cps = conn.prepareStatement(countSqlT)) {
                int idx = 1;
                for (Object p : countParamsT) cps.setObject(idx++, p);
                try (ResultSet rs = cps.executeQuery()) {
                    if (rs.next()) total = rs.getLong(1);
                }
            }
            try (PreparedStatement dps = conn.prepareStatement(dataSqlT)) {
                int idx = 1;
                for (Object p : dataParamsT) dps.setObject(idx++, p);
                try (ResultSet rs = dps.executeQuery()) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("teacherId", rs.getLong("teacherId"));
                        row.put("account", rs.getString("account"));
                        row.put("number", rs.getString("account")); // 前端复用字段
                        row.put("name", rs.getString("name"));
                        row.put("className", rs.getString("className"));
                        row.put("allQty", rs.getInt("allQty"));
                        row.put("postQty", rs.getInt("postQty"));
                        row.put("replyQty", rs.getInt("replyQty"));
                        row.put("likeQty", rs.getInt("likeQty"));
                        list.add(row);
                    }
                    return Result.success(list, total);
                }
            }
        }
    }

    /**
     * 构建老师统计数量SQL
     */
    private String buildTeacherCountSql(DiscussStatsDTO param) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    COUNT(DISTINCT ycc.teacherId)
                FROM yee_course_class ycc
                JOIN yee_manage ym ON ym.id = ycc.teacherId AND ym.schoolId = ycc.schoolId
                WHERE ycc.schoolId = ? AND ycc.courseId = ?
                """);

        if (param.getClassId() > 0) {
            sql.append(" AND ycc.classId = ?");
        }

        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            sql.append(" AND (ym.name LIKE ? OR ym.account LIKE ?)");
        }

        String fullSql = sql.toString();
        log.debug("Teacher Count SQL: {}", fullSql);
        return fullSql;
    }

    /**
     * 构建老师统计数量参数
     */
    private List<Object> buildTeacherCountParams(DiscussStatsDTO param) {
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
     * 构建老师统计数据SQL
     */
    private String buildTeacherDataSql(DiscussStatsDTO param) {
        String likeSubT = buildTeacherLikeSubSqlCommon(param, true);
        String statsSubT = buildTeacherStatsSubSqlCommon(param, likeSubT, true);
        String sql = """
                SELECT
                    ym.id AS teacherId,
                    ym.account AS account,
                    ym.name AS name,
                    '' AS className,
                    IFNULL(s.allQty,0) AS allQty,
                    IFNULL(s.postQty,0) AS postQty,
                    IFNULL(s.replyQty,0) AS replyQty,
                    IFNULL(s.likeQty,0) AS likeQty
                FROM (
                    SELECT DISTINCT teacherId, schoolId 
                    FROM yee_course_class 
                    WHERE schoolId = ? AND courseId = ? %s
                ) t
                JOIN yee_manage ym ON ym.id = t.teacherId AND ym.schoolId = t.schoolId
                LEFT JOIN (%s) s ON s.userId = ym.id AND s.schoolId = ym.schoolId
                WHERE 1=1 %s
                ORDER BY ym.id DESC 
                LIMIT ? OFFSET ?
                """.formatted(
                param.getClassId() > 0 ? "AND classId = ?" : "",
                statsSubT,
                buildKeywordConditionForTeacher(param)
        );
        log.debug("Teacher SQL (withPagination={}, exportView={}): {}", true, false, sql);
        return sql;
    }

    /**
     * 构建老师统计数据参数
     */
    private List<Object> buildTeacherDataParams(DiscussStatsDTO param, int pageSize, int offset) {
        List<Object> params = new ArrayList<>();

        // teacherBase 参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());

        // likeSubT 参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getNodeId() > 0) params.add(param.getNodeId());
        addTimeConditionParams(param, params); // 添加时间参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());

        // statsSubT 参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getNodeId() > 0) params.add(param.getNodeId());
        addTimeConditionParams(param, params); // 添加时间参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());

        // 关键字参数
        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            String kw = "%" + param.getKeyword().trim() + "%";
            params.add(kw);
            params.add(kw);
        }

        // 分页参数
        params.add(pageSize);
        params.add(offset);

        return params;
    }

    /**
     * 构建点赞子查询的时间条件
     */
    private String buildTimeConditionForLikeSub(DiscussStatsDTO param) {
        if (param.getStartTime() != null && param.getEndTime() != null) {
            return "AND rr.addTime BETWEEN ? AND ?";
        } else if (param.getStartTime() != null) {
            return "AND rr.addTime >= ?";
        } else if (param.getEndTime() != null) {
            return "AND rr.addTime <= ?";
        }
        return "";
    }

    /**
     * 构建统计子查询的时间条件
     */
    private String buildTimeConditionForStatsSub(DiscussStatsDTO param) {
        if (param.getStartTime() != null && param.getEndTime() != null) {
            return "AND r.addTime BETWEEN ? AND ?";
        } else if (param.getStartTime() != null) {
            return "AND r.addTime >= ?";
        } else if (param.getEndTime() != null) {
            return "AND r.addTime <= ?";
        }
        return "";
    }

    /**
     * 添加时间条件参数
     */
    private void addTimeConditionParams(DiscussStatsDTO param, List<Object> params) {
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
     * 构建老师查询的关键字条件
     */
    private String buildKeywordConditionForTeacher(DiscussStatsDTO param) {
        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            return "AND (ym.name LIKE ? OR ym.account LIKE ?)";
        }
        return "";
    }

    /**
     * 获取学生统计数据列表
     */
    private Result getStudentStatsList(SlSchool slSchool, DiscussStatsDTO param, int pageNum, int pageSize, int offset) throws Exception {
        String countSql = buildStudentCountSql(param);
        List<Object> countParams = buildStudentCountParams(param);

        String dataSql = buildStudentDataSqlGeneric(param, true, false);
        List<Object> dataParams = buildStudentDataParams(param, pageSize, offset);

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            long total = 0;
            try (PreparedStatement cps = conn.prepareStatement(countSql)) {
                int idx = 1;
                for (Object p : countParams) cps.setObject(idx++, p);
                try (ResultSet rs = cps.executeQuery()) {
                    if (rs.next()) total = rs.getLong(1);
                }
            }
            try (PreparedStatement dps = conn.prepareStatement(dataSql)) {
                int idx = 1;
                for (Object p : dataParams) dps.setObject(idx++, p);
                try (ResultSet rs = dps.executeQuery()) {
                    List<Map<String, Object>> list = new ArrayList<>();
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
                        list.add(row);
                    }
                    return Result.success(list, total);
                }
            }
        }
    }

    /**
     * 构建学生统计数量SQL
     */
    private String buildStudentCountSql(DiscussStatsDTO param) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT ycs.studentId)
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

        String fullSql = sql.toString();
        log.debug("Student Count SQL: {}", fullSql);
        return fullSql;
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
     * 构建学生统计数据SQL
     */
    private String buildStudentDataSql(DiscussStatsDTO param) {
        return """
            SELECT
                ys.id AS studentId,
                ys.number AS number,
                ys.name AS name,
                IFNULL(ycc.name,'') AS className,
                IFNULL(COUNT(DISTINCT r.id), 0) AS allQty,
                IFNULL(SUM(CASE WHEN IFNULL(r.replyId,0) = 0 THEN 1 ELSE 0 END), 0) AS postQty,
                IFNULL(SUM(CASE WHEN IFNULL(r.replyId,0) <> 0 THEN 1 ELSE 0 END), 0) AS replyQty,
                IFNULL(COUNT(DISTINCT rl.id), 0) AS likeQty
            FROM yee_course_student ycs
            LEFT JOIN yee_student ys ON ys.id = ycs.studentId AND ys.schoolId = ycs.schoolId
            LEFT JOIN yee_course_class ycc ON ycc.id = ycs.classId AND ycc.schoolId = ycs.schoolId
            LEFT JOIN yee_node_discuss r ON r.userId = ys.id
                AND r.schoolId = ys.schoolId
                AND r.courseId = ?
                AND r.isDelete = 0
                %s
                %s
            LEFT JOIN yee_node_reply_like rl ON rl.replyId = r.id AND rl.schoolId = r.schoolId
            WHERE ycs.schoolId = ?
            AND ycs.courseId = ?
            %s
            %s
            GROUP BY ys.id, ys.number, ys.name, ycc.name
            ORDER BY ys.id
            LIMIT ? OFFSET ?
            """.formatted(
                param.getNodeId() > 0 ? " AND r.nodeId = ?" : "",
                buildTimeConditionForStatsSub(param),
                param.getClassId() > 0 ? " AND ycs.classId = ?" : "",
                buildKeywordConditionForStudent(param)
        );
    }

    /**
     * 构建学生统计数据参数
     */
    private List<Object> buildStudentDataParams(DiscussStatsDTO param, int pageSize, int offset) {
        List<Object> params = new ArrayList<>();
        params.add(param.getCourseId());
        if (param.getNodeId() > 0) params.add(param.getNodeId());
        addTimeConditionParams(param, params);

        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());

        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            String kw = "%" + param.getKeyword().trim() + "%";
            params.add(kw);
            params.add(kw);
        }

        params.add(pageSize);
        params.add(offset);
        return params;
    }

    /**
     * 构建学生查询的关键字条件
     */
    private String buildKeywordConditionForStudent(DiscussStatsDTO param) {
        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            return "AND (ys.name LIKE ? OR ys.number LIKE ?)";
        }
        return "";
    }



    /**
     * 获取用户讨论详情列表
     */
    private Result getUserDiscussDetailList(SlSchool slSchool, DiscussStatsDTO param, int listType, int pageSize, int offset) throws Exception {
        String countSql = buildUserDiscussDetailCountSql(param, listType);
        List<Object> countParams = buildUserDiscussDetailCountParams(param, listType);

        String dataSql = buildUserDiscussDetailDataSql(listType);
        List<Object> dataParams = buildUserDiscussDetailDataParams(param, listType, pageSize, offset);

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            long total = 0;
            try (PreparedStatement cps = conn.prepareStatement(countSql)) {
                int idx = 1;
                for (Object p : countParams) cps.setObject(idx++, p);
                try (ResultSet rs = cps.executeQuery()) {
                    if (rs.next()) total = rs.getLong(1);
                }
            }
            try (PreparedStatement dps = conn.prepareStatement(dataSql)) {
                int idx = 1;
                for (Object p : dataParams) dps.setObject(idx++, p);
                try (ResultSet rs = dps.executeQuery()) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        row.put("replyId", rs.getLong("replyId"));
                        row.put("nodeId", rs.getLong("nodeId"));
                        row.put("nodeName", rs.getString("nodeName"));
                        row.put("content", rs.getString("content"));
                        row.put("addTime", rs.getTimestamp("addTime"));
                        row.put("images", rs.getString("images"));
                        row.put("files", rs.getString("files"));
                        row.put("likeQty", rs.getInt("likeQty"));
                        row.put("replyQty", rs.getInt("replyQty"));
                        list.add(row);
                    }
                    return Result.success(list, total);
                }
            }
        }
    }

    /**
     * 构建用户讨论详情统计数量SQL
     */
    private String buildUserDiscussDetailCountSql(DiscussStatsDTO param, int listType) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    COUNT(*)
                FROM yee_node_discuss r
                JOIN yee_node n ON n.id = r.nodeId AND n.schoolId = r.schoolId
                WHERE r.schoolId = ? AND r.courseId = ? AND r.userId = ? AND r.isDelete = 0
                """);

        if (param.getNodeId() > 0) {
            sql.append(" AND r.nodeId = ?");
        }

        if (param.getStartTime() != null && param.getEndTime() != null) {
            sql.append(" AND r.addTime BETWEEN ? AND ?");
        } else if (param.getStartTime() != null) {
            sql.append(" AND r.addTime >= ?");
        } else if (param.getEndTime() != null) {
            sql.append(" AND r.addTime <= ?");
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

        if (param.getNodeId() > 0) {
            params.add(param.getNodeId());
        }

        if (param.getStartTime() != null && param.getEndTime() != null) {
            params.add(new Timestamp(param.getStartTime()));
            params.add(new Timestamp(param.getEndTime()));
        } else if (param.getStartTime() != null) {
            params.add(new Timestamp(param.getStartTime()));
        } else if (param.getEndTime() != null) {
            params.add(new Timestamp(param.getEndTime()));
        }

        return params;
    }

    /**
     * 构建用户讨论详情数据SQL
     */
    private String buildUserDiscussDetailDataSql(int listType) {
        String likeSub = """
            SELECT
                replyId, schoolId, COUNT(*) AS likeQty
            FROM yee_node_reply_like
            WHERE schoolId = ?
            GROUP BY replyId, schoolId
            """;

        String replyCountSub = """
            SELECT
                replyId, schoolId, COUNT(*) AS replyQty
            FROM yee_node_discuss
            WHERE isDelete = 0
            GROUP BY replyId, schoolId
            """;

        StringBuilder sql = new StringBuilder("""
            SELECT
                r.id AS replyId, r.nodeId, n.name AS nodeName, r.content, r.addTime, r.images, r.files,
                IFNULL(l.likeQty,0) AS likeQty,
                IFNULL(rc.replyQty,0) AS replyQty
            FROM yee_node_discuss r
            JOIN yee_node n ON n.id = r.nodeId AND n.schoolId = r.schoolId
            LEFT JOIN (""" + likeSub + """
            ) l ON l.replyId = r.id AND l.schoolId = r.schoolId
            LEFT JOIN (""" + replyCountSub + """
            ) rc ON rc.replyId = r.id AND rc.schoolId = r.schoolId
            WHERE r.schoolId = ? AND r.courseId = ? AND r.userId = ? AND r.isDelete = 0""");

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
     * 构建用户讨论详情数据参数
     */
    private List<Object> buildUserDiscussDetailDataParams(DiscussStatsDTO param, int listType, int pageSize, int offset) {
        List<Object> params = new ArrayList<>();

        // likeSub 参数
        params.add(param.getSchoolId());

        // 主查询参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        params.add(param.getUserId());

        // 分页参数
        params.add(pageSize);
        params.add(offset);

        return params;
    }



    /**
     * 导出老师统计数据
     */
    private void exportTeacherData(Connection conn, DiscussStatsDTO param, List<List<String>> dataRows) throws SQLException {
        String[] headers = new String[]{"工号","老师姓名","班级名称","参与总量","主贴数量","回复数量","获赞数量"};

        // 构建老师统计的SQL和参数
        String dataSql = buildTeacherDataSqlGeneric(param, false, true);
        List<Object> dataParams = buildExportTeacherDataParams(param);

        try (PreparedStatement dps = conn.prepareStatement(dataSql)) {
            int i = 1;
            for (Object p : dataParams) dps.setObject(i++, p);
            try (ResultSet rs = dps.executeQuery()) {
                while (rs.next()) {
                    List<String> row = new ArrayList<>(headers.length);
                    row.add(safeReplaceComma(rs.getString("account")));
                    row.add(safeReplaceComma(rs.getString("name")));
                    row.add(safeReplaceComma(rs.getString("className")));
                    row.add(String.valueOf(rs.getInt("allQty")));
                    row.add(String.valueOf(rs.getInt("postQty")));
                    row.add(String.valueOf(rs.getInt("replyQty")));
                    row.add(String.valueOf(rs.getInt("likeQty")));
                    dataRows.add(row);
                }
            }
        }
    }

    /**
     * 构建老师统计导出SQL
     */
    private String buildExportTeacherDataSql(DiscussStatsDTO param) {
        // 复用通用子查询构建方法
        String likeSubT = buildTeacherLikeSubSqlCommon(param, false);
        String statsSubT = buildTeacherStatsSubSqlCommon(param, likeSubT, false);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    ym.account AS account,
                    ym.name AS name,
                    '' AS className,
                    IFNULL(s.allQty,0) AS allQty,
                    IFNULL(s.postQty,0) AS postQty,
                    IFNULL(s.replyQty,0) AS replyQty,
                    IFNULL(s.likeQty,0) AS likeQty
                FROM yee_manage ym
                LEFT JOIN (%s) s ON s.userId = ym.id AND s.schoolId = ym.schoolId
                WHERE ym.schoolId = ? AND ym.id IN (
                    SELECT DISTINCT teacherId
                    FROM yee_course_class
                    WHERE schoolId = ? AND courseId = ? %s
                ) %s
                ORDER BY ym.id DESC
                """);

        String fullSql = sql.toString().formatted(
                statsSubT,
                param.getClassId() > 0 ? " AND classId = ?" : "",
                buildKeywordConditionForTeacher(param)
        );
        log.debug("Teacher SQL (withPagination={}, exportView={}): {}", false, true, fullSql);
        return fullSql;
    }

    /**
     * 构建老师统计导出参数
     */
    private List<Object> buildExportTeacherDataParams(DiscussStatsDTO param) {
        List<Object> params = new ArrayList<>();

        // likeSubT 参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getNodeId() > 0) params.add(param.getNodeId());
        addTimeConditionParams(param, params);
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());

        // statsSubT 参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getNodeId() > 0) params.add(param.getNodeId());
        addTimeConditionParams(param, params);
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());

        // 主查询参数
        params.add(param.getSchoolId());
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());

        // 关键字参数
        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            String kw = "%" + param.getKeyword().trim() + "%";
            params.add(kw);
            params.add(kw);
        }

        return params;
    }

    /**
     * 构建老师统计导出点赞子查询SQL
     */
    private String buildTeacherLikeSubSqlCommon(DiscussStatsDTO param, boolean forList) {
        String inClause = buildUserInCourseSubSql(true, param, "rr");
        StringBuilder sql = new StringBuilder("""
                SELECT
                    rr.userId, rr.schoolId, COUNT(*) AS likeQty
                FROM yee_node_reply_like rl
                INNER JOIN yee_node_discuss rr ON rr.id = rl.replyId AND rr.isDelete = 0
                WHERE rl.schoolId = ? AND rr.courseId = ? %s %s
                %s
                GROUP BY rr.userId, rr.schoolId
                """);
        return sql.toString().formatted(
                param.getNodeId() > 0 ? "AND rr.nodeId = ?" : "",
                buildTimeConditionForLikeSub(param),
                inClause
        );
    }

    /**
     * 构建老师统计导出主子查询SQL
     */
    private String buildTeacherStatsSubSqlCommon(DiscussStatsDTO param, String likeSubT, boolean forList) {
        String inClause = buildUserInCourseSubSql(true, param, "r");
        StringBuilder sql = new StringBuilder("""
                SELECT
                    r.userId, r.schoolId,
                    COUNT(*) AS allQty,
                    SUM(CASE WHEN IFNULL(r.replyId,0) = 0 THEN 1 ELSE 0 END) AS postQty,
                    SUM(CASE WHEN IFNULL(r.replyId,0) <> 0 THEN 1 ELSE 0 END) AS replyQty,
                    COALESCE(MAX(l.likeQty), 0) AS likeQty
                FROM yee_node_discuss r
                LEFT JOIN (%s) l ON l.userId = r.userId AND l.schoolId = r.schoolId
                WHERE r.schoolId = ? AND r.courseId = ? AND r.isDelete = 0 %s %s
                %s
                GROUP BY r.userId, r.schoolId
                """);
        return sql.toString().formatted(
                likeSubT,
                param.getNodeId() > 0 ? "AND r.nodeId = ?" : "",
                buildTimeConditionForStatsSub(param),
                inClause
        );
    }

    /**
     * 构建“参与课程的用户”子查询片段（用于 r/rr.userId IN (...)），根据老师/学生切换来源表与字段
     */
    private String buildUserInCourseSubSql(boolean isTeacher, DiscussStatsDTO param, String aliasName) {
        String userIdField = isTeacher ? "teacherId" : "studentId";
        String fromTable = isTeacher ? "yee_course_class" : "yee_course_student";
        String classCond = param.getClassId() > 0 ? " AND classId = ?" : "";
        return (" AND " + aliasName + ".userId IN (\n" +
                "     SELECT " + (isTeacher ? "DISTINCT " : "") + userIdField + "\n" +
                "     FROM " + fromTable + "\n" +
                "     WHERE schoolId = ? AND courseId = ?" + classCond + "\n" +
                " )");
    }

    /**
     * 导出学生统计数据
     */
    private void exportStudentData(Connection conn, DiscussStatsDTO param, List<List<String>> dataRows) throws SQLException {
        String[] headers = new String[]{"学号","姓名","课程班级","总发帖数","主帖数","回复数","点赞数"};

        // 构建学生统计的SQL和参数
        String dataSql = buildStudentDataSqlGeneric(param, false, true);
        List<Object> dataParams = buildExportStudentDataParams(param);

        try (PreparedStatement dps = conn.prepareStatement(dataSql)) {
            int i = 1;
            for (Object p : dataParams) dps.setObject(i++, p);
            try (ResultSet rs = dps.executeQuery()) {
                while (rs.next()) {
                    List<String> row = new ArrayList<>(headers.length);
                    row.add(safeReplaceComma(rs.getString("number")));
                    row.add(safeReplaceComma(rs.getString("name")));
                    row.add(safeReplaceComma(rs.getString("className")));
                    row.add(String.valueOf(rs.getInt("allQty")));
                    row.add(String.valueOf(rs.getInt("postQty")));
                    row.add(String.valueOf(rs.getInt("replyQty")));
                    row.add(String.valueOf(rs.getInt("likeQty")));
                    dataRows.add(row);
                }
            }
        }
    }

    /**
     * 构建学生统计导出SQL
     */
    private String buildExportStudentDataSql(DiscussStatsDTO param) {
        // 复用通用子查询构建方法
        String likeSub = buildStudentLikeSubSqlCommon(param, false);
        String statsSub = buildStudentStatsSubSqlCommon(param, likeSub, false);

        StringBuilder sql = new StringBuilder("""
                SELECT
                    ys.number AS number,
                    ys.name AS name,
                    IFNULL(ycc.name,'') AS className,
                    IFNULL(s.allQty,0) AS allQty,
                    IFNULL(s.postQty,0) AS postQty,
                    IFNULL(s.replyQty,0) AS replyQty,
                    IFNULL(s.likeQty,0) AS likeQty
                FROM yee_course_student ycs
                LEFT JOIN yee_student ys ON ys.id = ycs.studentId AND ys.schoolId = ycs.schoolId 
                LEFT JOIN yee_course_class ycc ON ycc.id = ycs.classId AND ycc.schoolId = ycs.schoolId 
                LEFT JOIN (%s) s ON s.userId = ys.id AND s.schoolId = ys.schoolId 
                WHERE ycs.schoolId = ? AND ycs.courseId = ? %s %s 
                ORDER BY ys.id DESC
                """);

        String fullSql = sql.toString().formatted(
                statsSub,
                param.getClassId() > 0 ? " AND ycs.classId = ?" : "",
                buildKeywordConditionForStudent(param)
        );
        log.debug("Student SQL (withPagination={}, exportView={}): {}", false, true, fullSql);
        return fullSql;
    }

    /**
     * 构建学生统计导出参数
     */
    private List<Object> buildExportStudentDataParams(DiscussStatsDTO param) {
        List<Object> params = new ArrayList<>();

        // likeSub 参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getNodeId() > 0) params.add(param.getNodeId());
        addTimeConditionParams(param, params);
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());

        // statsSub 参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getNodeId() > 0) params.add(param.getNodeId());
        addTimeConditionParams(param, params);
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());

        // 主查询参数
        params.add(param.getSchoolId());
        params.add(param.getCourseId());
        if (param.getClassId() > 0) params.add(param.getClassId());

        // 关键字参数
        if (param.getKeyword() != null && !param.getKeyword().trim().isEmpty()) {
            String kw = "%" + param.getKeyword().trim() + "%";
            params.add(kw);
            params.add(kw);
        }

        return params;
    }

    /**
     * 构建学生统计导出点赞子查询SQL
     */
    private String buildStudentLikeSubSqlCommon(DiscussStatsDTO param, boolean forList) {
        String inClause = buildUserInCourseSubSql(false, param, "rr");
        StringBuilder sql = new StringBuilder("""
                SELECT
                    rr.userId, rr.schoolId, COUNT(*) AS likeQty
                FROM yee_node_reply_like rl
                INNER JOIN yee_node_discuss rr ON rr.id = rl.replyId AND rr.isDelete = 0
                WHERE rl.schoolId = ? AND rr.courseId = ? %s %s
                %s
                GROUP BY rr.userId, rr.schoolId
                """);
        return sql.toString().formatted(
                param.getNodeId() > 0 ? "AND rr.nodeId = ?" : "",
                buildTimeConditionForLikeSub(param),
                inClause
        );
    }

    /**
     * 构建学生统计导出主子查询SQL
     */
    private String buildStudentStatsSubSqlCommon(DiscussStatsDTO param, String likeSub, boolean forList) {
        String inClause = buildUserInCourseSubSql(false, param, "r");
        StringBuilder sql = new StringBuilder("""
                SELECT
                    r.userId, r.schoolId,
                    COUNT(*) AS allQty,
                    SUM(CASE WHEN IFNULL(r.replyId,0) = 0 THEN 1 ELSE 0 END) AS postQty,
                    SUM(CASE WHEN IFNULL(r.replyId,0) <> 0 THEN 1 ELSE 0 END) AS replyQty,
                    COALESCE(MAX(l.likeQty), 0) AS likeQty
                FROM yee_node_discuss r
                LEFT JOIN (%s) l ON l.userId = r.userId AND l.schoolId = r.schoolId
                WHERE r.schoolId = ? AND r.courseId = ? AND r.isDelete = 0 %s %s
                %s
                GROUP BY r.userId, r.schoolId
                """);
        return sql.toString().formatted(
                likeSub,
                param.getNodeId() > 0 ? "AND r.nodeId = ?" : "",
                buildTimeConditionForStatsSub(param),
                inClause
        );
    }

    // 通用 SQL 构造入口（包装现有实现），用于统一开关参数
    private String buildTeacherDataSqlGeneric(DiscussStatsDTO param, boolean withPagination, boolean exportView) {
        String sql = exportView ? buildExportTeacherDataSql(param) : buildTeacherDataSql(param);
        // 日志在各自方法中已打印，这里仍保留一次以便统一追踪
        log.debug("Teacher SQL (withPagination={}, exportView={}): {}", withPagination, exportView, sql);
        return sql;
    }

    private String buildStudentDataSqlGeneric(DiscussStatsDTO param, boolean withPagination, boolean exportView) {
        String sql = exportView ? buildExportStudentDataSql(param) : buildStudentDataSql(param);
        log.debug("Student SQL (withPagination={}, exportView={}): {}", withPagination, exportView, sql);
        return sql;
    }
}
