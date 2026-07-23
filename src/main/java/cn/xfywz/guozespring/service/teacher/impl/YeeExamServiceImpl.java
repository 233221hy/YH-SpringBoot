package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.constant.DataAuth;
import cn.xfywz.guozespring.entity.dto.YeeExamExportDTO;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.*;
import cn.xfywz.guozespring.entity.vo.QuestionExportVO;
import cn.xfywz.guozespring.entity.vo.WorkReportVO;
import cn.xfywz.guozespring.excel.ExcelExportStyles;
import cn.xfywz.guozespring.excel.ResponseExportUtil;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeExamService;
import cn.xfywz.guozespring.util.AuthDataPermissionUtil;
import cn.xfywz.guozespring.util.JsonUtil;
import cn.xfywz.guozespring.util.ParseJsonUtil;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.merge.OnceAbsoluteMergeStrategy;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.ResultSetMetaData;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.Font;

/**
 * @Author: ChengLin
 */
@Slf4j
@Service
public class YeeExamServiceImpl implements YeeExamService {


    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final ParseJsonUtil parseJsonUtil = new ParseJsonUtil();


    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private DatabaseUtil databaseUtil;

    @Override
    public Result selectAll(int schoolId, Integer courseId, Integer classId, String title) throws Exception {
        // 横批显示
        Map<String, Integer> totalWorkStats = getTotalWorkStats(schoolId, courseId, classId);

        // ****************************
        // 原始列表
        List<Map<String, Object>> workList = getWorkDetailsByCourseAndSchool(schoolId, courseId, classId, title);

        // 过滤掉chapterName 为null的数据 workList
        workList = workList.stream().filter(work -> work.get("chapterName") != null).collect(Collectors.toList());


        // 结构化列表
        List<WorkReportVO> workReportVOS = buildWorkReport(workList);

        // 查询yee_exam_record 表 记录
        List<Map<String, Object>> workRecordList = getWorkRecordDetails(schoolId, courseId, classId);

        // 遍历workReportVOS中的works数组, 再根据works数组里面的id 和 workRecordList 数组中的examId进行关联 然后 赋值submitted,unSubmitted,marked, unMarked 这四个字段根据workRecordList 数组中的值
        workReportVOS.stream().forEach(workItem -> {
            workItem.getWorks().stream().forEach(work -> {
                int submitted = 0;
                int unSubmitted = work.getTotalNum();
                int marked = 0;
                int unMarked = 0;
                workRecordList.stream().forEach(workRecord -> {
                    if (workRecord.get("examId").equals(work.getId())) {
                        work.setSubmitted(submitted + 1);
                        work.setUnSubmitted(unSubmitted - 1);
                        work.setMarked(workRecord.get("state").equals(3) ? marked + 1 : marked);
                        work.setUnMarked(workRecord.get("state").equals(2) ? unMarked + 1 : unMarked);
                    }
                });
                // 如果未能匹配上提交数据 则是未提交
                if (work.getSubmitted() == 0) {
                    work.setUnSubmitted(work.getTotalNum());
                }
            });
        });


        // 列表返回
        Map<String, Object> result = new HashMap<>();
        result.put("row", totalWorkStats);
//        result.put("workList", workList);
        result.put("exam", workReportVOS);
//        result.put("workRecordList", workRecordList);
        return Result.success(result);
    }

    @Override
    public Result selectRecordAll(int schoolId, Integer courseId, Integer nodeId, Integer classId) {
//         根据考试id获取考试表信息
        Map<String, Object> workInfo = getWorkInfoById(schoolId, nodeId, courseId);
        return Result.success(workInfo);
    }

    @Override
    public Result selectRecordAllExamId(int schoolId, Integer courseId, Integer examId, Integer classId) {
        // 根据考试id获取考试表信息
        Map<String, Object> workInfo = getWorkInfoByExamId(schoolId, examId, courseId);
        return Result.success(workInfo);
    }

    @Override
    public Result selectSearchRecordAll(int schoolId, Integer courseId, Integer examId, String title,
                                        Integer classId, Integer subState, Integer reviewState, Integer scoredState,
                                        Integer pageNum, Integer pageSize) throws Exception {

        // 1. 分页参数容错
        int page = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int size = (pageSize == null || pageSize < 1) ? 20 : pageSize;

        // 2. 一次性查出所有符合条件的学生（无分页）
        List<Map<String, Object>> allStudents = getWorkDetailsForExportAll(
                schoolId, courseId, classId, title, examId);

        if (allStudents.isEmpty()) {
            return Result.success(Map.of(
                    "result", new ArrayList<>(),
                    "total", 0,
                    "pageNum", page,
                    "pageSize", size
            ));
        }

        // 3. 批量查询成绩（SQL层已过滤 scoredState）
        List<Integer> userIdList = allStudents.stream()
                .map(item -> (Integer) item.get("id"))
                .toList();
        List<Map<String, Object>> scoreDataList = getRecordsScoresByUserIds(
                schoolId, courseId, examId, scoredState, userIdList);

        Map<Integer, Map<String, Object>> scoreDataMap = scoreDataList.stream()
                .collect(Collectors.toMap(
                        record -> ((Number) record.get("userId")).intValue(),
                        record -> record,
                        (oldVal, newVal) -> oldVal
                ));

        // 4. 合并数据
        List<Map<String, Object>> mergeList = new ArrayList<>();
        for (Map<String, Object> student : allStudents) {
            Map<String, Object> mergeItem = new HashMap<>(student);
            Integer uid = (Integer) student.get("id");
            Map<String, Object> scoreInfo = scoreDataMap.get(uid);

            if (scoreInfo != null) {
                scoreInfo.forEach((k, v) -> {
                    if (!"id".equals(k)) {
                        mergeItem.put(k, v);
                    }
                });
            } else {
                mergeItem.put("userId", uid);
                mergeItem.put("finalScore", null);
                mergeItem.put("submitTime", null);
                mergeItem.put("scored", 0);
                mergeItem.put("state", null);
            }
            mergeList.add(mergeItem);
        }

        // 5. 内存过滤：只过滤 交卷状态 + 批阅状态
        // scoredState 已经在 SQL 层过滤，这里不再判断！
        List<Map<String, Object>> filteredList = new ArrayList<>();
        for (Map<String, Object> item : mergeList) {
            boolean pass = true;
            Object stateObj = item.get("state");
            int stateVal = -1;
            if (stateObj instanceof Number num) {
                stateVal = num.intValue();
            }

            // 交卷状态
            if (subState != null) {
                if (subState == 1) {
                    pass = (stateObj == null || stateVal == 1);
                } else if (subState == 2) {
                    pass = (stateObj != null && stateVal == 2);
                } else if (subState == 3) {
                    pass = (stateObj != null && stateVal == 3);
                }
            }

            // 批阅状态
            if (pass && reviewState != null) {
                if (reviewState == 3) {
                    pass = (stateObj != null && stateVal == 3);
                } else {
                    pass = (stateObj == null || stateVal != 3);
                }
            }

            if (pass) {
                filteredList.add(item);
            }
        }

        // 6. 内存分页（总数 = 真实筛选后的总数）
        int total = filteredList.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);
        List<Map<String, Object>> pageList = new ArrayList<>();
        if (fromIndex < total) {
            pageList = filteredList.subList(fromIndex, toIndex);
        }

        // 7. 返回
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("result", pageList);
        resultMap.put("total", total);
        resultMap.put("pageNum", page);
        resultMap.put("pageSize", size);
        return Result.success(resultMap);
    }

    private List<Integer> getExamClassIds(Connection conn, Integer examId, Integer courseId) throws Exception {
        List<Integer> examClassIds = new ArrayList<>();
        try (PreparedStatement examSt = conn.prepareStatement(
                "SELECT classList FROM yee_exam WHERE id = ? AND courseId = ?")) {
            examSt.setInt(1, examId);
            examSt.setInt(2, courseId);
            try (ResultSet examRs = examSt.executeQuery()) {
                if (examRs.next()) {
                    String classListStr = examRs.getString("classList");
                    if (classListStr != null && !classListStr.trim().isEmpty() && !classListStr.equals("[]")) {
                        JSONArray jsonArray = JSON.parseArray(classListStr);
                        for (int i = 0; i < jsonArray.size(); i++) {
                            examClassIds.add(jsonArray.getInteger(i));
                        }
                    }
                }
            }
        }
        return examClassIds;
    }

    // 新增：只查课程学生总数，不做任何复杂关联
    private int getStudentTotalCount(int schoolId, Integer courseId, Integer examId, String title, Integer classId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            conn = databaseUtil.getConnection(schoolId);

            List<Integer> examClassIds = getExamClassIds(conn, examId, courseId);

            // 构建基础SQL：只统计课程内的学生，不关联考试记录，避免重复和参数错误
            StringBuilder sql = new StringBuilder("""
                SELECT COUNT(DISTINCT cs.studentId) AS total
                FROM yee_course_student cs
                LEFT JOIN yee_student s ON s.id = cs.studentId
                WHERE cs.courseId = ?
                """);

            // 班级过滤
            if (classId != null && classId > 0) {
                sql.append(" AND cs.classId = ? ");
            } else if (!examClassIds.isEmpty()) {
                sql.append(" AND cs.classId IN (");
                for (int i = 0; i < examClassIds.size(); i++) {
                    sql.append("?");
                    if (i < examClassIds.size() - 1) sql.append(",");
                }
                sql.append(")");
            }

            // 姓名/学号模糊搜索
            if (title != null && !title.isBlank()) {
                sql.append(" AND (s.name LIKE ? OR s.number LIKE ?) ");
            }

            st = conn.prepareStatement(sql.toString());
            int idx = 1;
            st.setInt(idx++, courseId);

            if (classId != null && classId > 0) {
                st.setInt(idx++, classId);
            } else {
                for (Integer cid : examClassIds) st.setInt(idx++, cid);
            }

            if (title != null && !title.isBlank()) {
                String kw = "%" + title.trim() + "%";
                st.setString(idx++, kw);
                st.setString(idx++, kw);
            }

            rs = st.executeQuery();
            if (rs.next()) return rs.getInt("total");
            return 0;
        } finally {
            if (rs != null) rs.close();
            if (st != null) st.close();
            if (conn != null) conn.close();
        }
    }

    /**
     * 答题记录 复批/查阅
     */
    @Override
    public Result selectWorkRecordConsult(int schoolId, Integer userId, Integer examId, Integer courseId) throws Exception {
        // 考试批阅 学生信息情况
        List<Map<String, Object>> studentResult = getWorkRecordDetailByUserAndWork(schoolId, userId, examId, courseId);

        // 考试题目列表以及得分情况和学生答案
        List<Map<String, Object>> workResult =  getWorkTopicDetailsByUserAndWork(schoolId, userId, examId, courseId);


        // 数据组装
        Map resultMap = new HashMap<>();
        resultMap.put("studentResult", studentResult);
        resultMap.put("workResult", workResult);

        return Result.success(resultMap);
    }

    /**
     * 答题记录 复批/查阅 批量
     */
    public Result selectWorkRecordConsultBatch(int schoolId, Integer examId, Integer courseId, List<Integer> userIdList) throws Exception {
        // 考试批阅 所有学生信息情况
        List<Map<String, Object>> studentResult = getWorkRecordDetailByExamWithUserIds(schoolId, examId, courseId, userIdList);

        // 考试题目列表以及得分情况和学生答案
        List<Map<String, Object>> workResult =  getWorkTopicDetailsByExamWithUserIds(schoolId, examId, courseId, userIdList);

        // 将 studentResult 和 workResult 通过 wrId 进行分组，把相同 wrId 的数据合并，不同的（如题目相关数据）放入 workTopics 列表
        Map<Object, Map<String, Object>> groupedResult = new HashMap<>();

        // 首先处理 studentResult，建立以 wrId 为 key 的基础数据
        for (Map<String, Object> student : studentResult) {
            Object wrId = student.get("wrId");
            if (wrId != null) {
                groupedResult.put(wrId, new HashMap<>(student));
            }
        }

        // 然后处理 workResult，将题目相关数据添加到对应 wrId 的记录中
        for (Map<String, Object> work : workResult) {
            Object wrId = work.get("wrId");
            if (wrId != null) {
                Map<String, Object> existing = groupedResult.get(wrId);
                if (existing != null) {
                    // 将题目相关数据添加到 workTopics 列表中
                    List<Map<String, Object>> workTopics = (List<Map<String, Object>>) existing.getOrDefault("workTopics", new ArrayList<>());
                    // 创建一个新的 map 只包含题目相关字段
                    Map<String, Object> topicData = new HashMap<>();
                    for (Map.Entry<String, Object> entry : work.entrySet()) {
                        String key = entry.getKey();
                        // 排除 wrId 和 userId，因为这些是公共字段
                        if (!"wrId".equals(key) && !"userId".equals(key)) {
                            topicData.put(key, entry.getValue());
                        }
                    }
                    workTopics.add(topicData);
                    existing.put("workTopics", workTopics);
                }
            }
        }

        // 转换为列表
        List<Map<String, Object>> finalResult = new ArrayList<>(groupedResult.values());


        return Result.success(finalResult);
    }

    private List<Map<String, Object>> getWorkTopicDetailsByUserAndWork(
            int schoolId,
            Integer userId,
            Integer examId, Integer courseId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建 SQL：查询考试题目、学生作答、题目分值、学生得分等
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT 
                wt.type,
                wt.cateBid,
                wt.cateMid,
                wt.`level`,
                wt.score AS topicScore,
                wa.score AS studentScore,
                wt.topic,
                wt.`option`,
                wa.answer,
                wa.topicId,
                wa.id AS waId,
                er.id AS wrId,
                er.userId,
                wa.files,
                wa.images
            FROM 
                yee_exam_record er
                LEFT JOIN yee_exam_answer wa ON er.id = wa.recordId
                LEFT JOIN yee_exam_topic wt ON wt.id = wa.topicId
            WHERE 
                er.userId = ?
                AND er.examId = ?
                AND er.courseId = ?
            """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setLong(paramIndex++, userId);   // er.userId
            st.setInt(paramIndex++, examId);    // er.examId
            st.setInt(paramIndex++, courseId); // er.courseId

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            ObjectMapper objectMapper = new ObjectMapper();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i); // 使用 label 支持别名
                    Object value = rs.getObject(i);

                    // 特殊处理：将 option 字段从 JSON 字符串转为 List<Map>
                    if (("option".equalsIgnoreCase(columnName) && value != null) || ("files".equalsIgnoreCase(columnName) && value != null) || ("images".equalsIgnoreCase(columnName) && value != null) ) {
                        try {
                            value = objectMapper.readValue(value.toString(), List.class);
                        } catch (Exception e) {
                            // 解析失败则保留原始字符串
                            e.printStackTrace();
                        }
                    }

                    // 新增：处理 answer 字段（如果是多选题）
                    if ("answer".equalsIgnoreCase(columnName) && value instanceof String) {
                        String answerStr = (String) value;
                        try {
                            // 尝试解析成 List<String>
                            if (answerStr.trim().startsWith("[")) {
                                value = objectMapper.readValue(answerStr, List.class);
                            }
                            // 如果不是数组格式，保持原样（如单选 "A"）
                        } catch (Exception e) {
                            // 解析失败，保留原始字符串
                            System.err.println("解析 answer 失败: " + answerStr);
                        }
                    }


                    row.put(columnName, value);
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("根据用户和考试查询题目作答详情失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", examId=" + examId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

private List<Map<String, Object>> getWorkTopicDetailsByExamWithUserIds(
        int schoolId,
        Integer examId, Integer courseId, List<Integer> userIdList) throws Exception {

    if (userIdList == null || userIdList.isEmpty()) {
        return new ArrayList<>();
    }

    // 1. 先查所有学生的基本信息 (er.id, userId)
    List<Map<String, Object>> studentRecords = getExamRecordsByUserIds(schoolId, examId, courseId, userIdList);
    if (studentRecords.isEmpty()) {
        return new ArrayList<>();
    }

    // 提取所有学生的 recordId (er.id)
    List<Integer> recordIds = studentRecords.stream()
            .map(rec -> (Integer) rec.get("wrId"))
            .collect(Collectors.toList());

    // 2. 再根据 recordIds 批量查询所有题目和答案
    List<Map<String, Object>> topicAnswers = getTopicAnswersByRecordIds(schoolId, recordIds);

    // 3. 在内存中合并学生记录和题目答案 (核心优化)
    return mergeStudentRecordsWithTopicAnswers(studentRecords, topicAnswers);
}

    /**
     * 第一步：批量查询学生的考试记录 (只查关键信息，速度极快)
     */
    private List<Map<String, Object>> getExamRecordsByUserIds(
            int schoolId, Integer examId, Integer courseId, List<Integer> userIdList) throws Exception {

        List<List<Integer>> partitions = ListUtils.partition(userIdList, 50);
        List<Map<String, Object>> result = new ArrayList<>();

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            for (List<Integer> part : partitions) {
                String placeholders = String.join(",", Collections.nCopies(part.size(), "?"));

                // 只查询 er.id 和 userId，使用覆盖索引，速度最快
                String sql = "SELECT id AS wrId, userId FROM yee_exam_record " +
                        "WHERE examId = ? AND courseId = ? AND userId IN (" + placeholders + ")";

                st = conn.prepareStatement(sql);
                int idx = 1;
                st.setInt(idx++, examId);
                st.setInt(idx++, courseId);
                for (Integer uid : part) {
                    st.setInt(idx++, uid);
                }

                rs = st.executeQuery();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("wrId", rs.getInt("wrId"));       // 对应原SQL的 er.id AS wrId
                    row.put("userId", rs.getInt("userId"));   // 学生ID
                    result.add(row);
                }
                rs.close();
                st.close();
            }
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    /**
     * 第二步：根据 recordIds 批量查询所有题目和答案
     */
    private List<Map<String, Object>> getTopicAnswersByRecordIds(int schoolId, List<Integer> recordIds) throws Exception {

        List<List<Integer>> partitions = ListUtils.partition(recordIds, 100); // 这里可以批量更大
        List<Map<String, Object>> result = new ArrayList<>();

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            for (List<Integer> part : partitions) {
                String placeholders = String.join(",", Collections.nCopies(part.size(), "?"));

                // 只查题目和答案，以及关联的 recordId
                String sql = """
                SELECT 
                    wa.recordId,       -- 用于和学生记录关联
                    wt.type,
                    wt.cateBid,
                    wt.cateMid,
                    wt.`level`,
                    wt.score AS topicScore,
                    wa.score AS studentScore,
                    wt.topic,
                    wt.`option`,
                    wa.answer,
                    wa.topicId,
                    wa.id AS waId,
                    wa.files,
                    wa.images
                FROM 
                    yee_exam_answer wa
                    LEFT JOIN yee_exam_topic wt ON wt.id = wa.topicId
                WHERE 
                    wa.recordId IN (""" + placeholders + ")";

                st = conn.prepareStatement(sql);
                int idx = 1;
                for (Integer rid : part) {
                    st.setInt(idx++, rid);
                }

                rs = st.executeQuery();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);

                        // 以下是你原来的JSON解析逻辑，保持不变
                        if ("option".equalsIgnoreCase(columnName) && value != null) {
                            try {
                                value = objectMapper.readValue(value.toString(), List.class);
                            } catch (Exception e) {}
                        }
                        if ("answer".equalsIgnoreCase(columnName) && value instanceof String) {
                            String answerStr = (String) value;
                            try {
                                if (answerStr.trim().startsWith("[")) {
                                    value = objectMapper.readValue(answerStr, List.class);
                                }
                            } catch (Exception e) {}
                        }
                        row.put(columnName, value);
                    }
                    result.add(row);
                }
                rs.close();
                st.close();
            }
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    /**
     * 第三步：在内存中合并学生记录和题目答案
     */
    private List<Map<String, Object>> mergeStudentRecordsWithTopicAnswers(
            List<Map<String, Object>> studentRecords,
            List<Map<String, Object>> topicAnswers) {

        List<Map<String, Object>> finalResult = new ArrayList<>();

        // 1. 把题目答案按 recordId 分组，方便快速查找
        Map<Integer, List<Map<String, Object>>> topicAnswersByRecordId = topicAnswers.stream()
                .collect(Collectors.groupingBy(ta -> (Integer) ta.get("recordId")));

        // 2. 遍历每个学生记录
        for (Map<String, Object> student : studentRecords) {
            Integer wrId = (Integer) student.get("wrId");
            Integer userId = (Integer) student.get("userId");

            // 3. 找到该学生对应的所有题目答案
            List<Map<String, Object>> answersForStudent = topicAnswersByRecordId.getOrDefault(wrId, new ArrayList<>());

            if (answersForStudent.isEmpty()) {
                // 4. 如果学生没有任何题目答案，也要生成一条空记录，保证数据完整性
                Map<String, Object> emptyRow = new HashMap<>();
                emptyRow.put("wrId", wrId);
                emptyRow.put("userId", userId);
                // 其他字段留空
                finalResult.add(emptyRow);
            } else {
                // 5. 如果有答案，就把学生信息 (wrId, userId) 合并到每一条答案记录中
                for (Map<String, Object> answer : answersForStudent) {
                    answer.put("wrId", wrId);       // 合并学生ID
                    answer.put("userId", userId);   // 合并用户ID
                    finalResult.add(answer);
                }
            }
        }

        return finalResult;
    }


    @Override
    public Result selectWorkRecordRecheckNew(int schoolId, Integer userId, Integer workId, Integer courseId, BigDecimal recheckScore, Integer teacherId, List<Map<String, Object>> workResult) throws Exception {
        // √ 根据recheckScore 更新yee_work_record表中的score字段 分数情况
        updateWorkRecordScore(schoolId, userId, workId, recheckScore, teacherId, workResult);

        // √ 根据workResult更新yee_work_answer表中的各个题目得分
        updateWorkAnswerScores(schoolId, userId, workId, workResult);

        // 更新yee_work_score表中的最终得分
        updateWorkScore(schoolId, userId, workId, courseId, recheckScore);

        return Result.success("修改成功");
    }

    @Override
    public Result selectWorkRecordManual(int schoolId, Integer userId, Integer examId, BigDecimal manualScore, Integer courseId) throws Exception {
        // 根据 manualScore 更新 yee_exam_score 表中的 finalScore 字段 分数情况
        updateWorkScore(schoolId, userId, examId, courseId, manualScore);
        return Result.success( "修改成功");
    }

    @Override
    public Result selectWorkRecordManualList(int schoolId, Integer userId, Integer examId, Integer courseId) throws Exception {
        // 查询出 考试名称:高等数学, 学生姓名/学号: 王xx(19103019512387), 最终得分: 22.5
        Map<String, Object> studentFinalScore = getStudentFinalScoreByUserIdAndWorkId(schoolId, userId, examId);

        return Result.success(studentFinalScore);
    }

    @Override
    public Result selectWorkRecordConsultPre(int schoolId, Integer examId, Integer courseId) throws Exception {
        // 作业批阅 学生信息情况
        List<Map<String, Object>> studentResult = getWorkRecordDetailByUserAndWorkPre(schoolId, examId);

        // 作业题目列表以及得分情况和学生答案
        List<Map<String, Object>> workResult =  getWorkTopicDetailsByUserAndWorkPre(schoolId, examId);


        // 数据组装
        Map resultMap = new HashMap<>();
        resultMap.put("studentResultPre", studentResult);
        resultMap.put("workResultPre", workResult);

        return Result.success(resultMap);
    }

    @Override
    public void exportQuestions(HttpServletResponse response, Integer schoolId, String topic, Integer createId, Integer type, Integer level, Integer cateBid, Integer cateMid, Integer examId) throws Exception {
        OutputStream outputStream = null;
        ExcelWriter excelWriter = null;
        try {

            // 1. 设置响应头
            LocalDateTime now = LocalDateTime.now();
            String fileName = "考试题目导出_" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

            // 2. 获取输出流
            outputStream = response.getOutputStream();

            // 3. 创建ExcelWriter并写入数据（流式写入）
            ExcelWriterBuilder writerBuilder = EasyExcel.write(outputStream, QuestionExportVO.class)
                    .registerWriteHandler(ExcelExportStyles.defaultStyleStrategy());
            excelWriter = writerBuilder.build();
            WriteSheet writeSheet = EasyExcel.writerSheet("试题数据").build();

            // 4. 分页查询并流式写入数据
            int pageSize = 10000; // 每页10000条数据
            int pageNum = 1;
            long totalExported = 0;

            // 获取学校信息和数据库连接
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            if (connection == null) {
                throw new Exception("无法获取数据库连接");
            }




            try {
                while (true) {
                    // 测试执行时间
                    long startTime = System.currentTimeMillis();
                    // 获取分页数据，复用数据库连接
                    Object result = exportAllWithPagination(
                            connection, schoolId, pageSize, pageNum, topic, createId, type, level, cateBid, cateMid, examId);

                    // 提取试题列表
                    List<YeeQuestion> questions = new ArrayList<>();
                    if (result instanceof Result) {
                        Object data = ((Result) result).getData();
                        if (data instanceof List) {
                            questions = (List<YeeQuestion>) data;
                        }
                    } else if (result instanceof Map && ((Map<?, ?>) result).containsKey("data")) {
                        Object data = ((Map<?, ?>) result).get("data");
                        if (data instanceof List) {
                            questions = (List<YeeQuestion>) data;
                        }
                    }

                    // 如果没有数据了，跳出循环
                    if (questions.isEmpty()) {
                        break;
                    }

                    // 转换为导出VO对象
                    List<QuestionExportVO> exportList = convertToExportVO(questions);

                    // 写入数据到Excel（流式写入）
                    excelWriter.write(exportList, writeSheet);

                    // 测试执行时间
                    long endTime = System.currentTimeMillis();
                    totalExported += exportList.size();

                    // 如果当前页数据少于pageSize，说明已经是最后一页，跳出循环
                    if (questions.size() < pageSize) {
                        break;
                    }

                    // 继续下一页
                    pageNum++;
                }
            } finally {

                // 确保连接关闭
                if (connection != null && !connection.isClosed()) {
                    try {
                        connection.close();
                    } catch (SQLException ignored) {}
                }
            }

        } catch (Exception e) {
            log.error("导出试题数据时发生异常", e);
            throw e;
        } finally {
            // 确保ExcelWriter正确关闭，这会自动刷新和关闭输出流
            if (excelWriter != null) {
                try {
                    excelWriter.finish();
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public Result add(YeeExam yeeExam) throws Exception {
        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(yeeExam.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            throw new Exception("无法获取数据库连接");
        }

        try {
            // 关闭自动提交，开启事务
            connection.setAutoCommit(false);

            // 4. 插入试卷主表
            String insertSql = """
                    INSERT INTO yee_exam 
                    (userId, title, topicNumber, score,limitedTime, remarks, addTime, sequence, nodeId, courseId, startTime, endTime, paperId, createUserId,
                     isPrivate, classList, teacherType, allow, frequency, random, hasCollect, randData, schoolId, parsing, randNumber)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            Integer examId;
            try (PreparedStatement insertSt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertSt.setObject(1, yeeExam.getUserId());
                insertSt.setObject(2, yeeExam.getTitle());
                insertSt.setObject(3, yeeExam.getTopicNumber() != null ? yeeExam.getTopicNumber() : 0);
                insertSt.setObject(4, yeeExam.getScore() != null ? yeeExam.getScore() : 0);
                insertSt.setObject(5, yeeExam.getLimitedTime());
                insertSt.setObject(6, yeeExam.getRemarks());
                insertSt.setObject(7, yeeExam.getAddTime());
                insertSt.setObject(8, yeeExam.getSequence());
                insertSt.setObject(9, yeeExam.getNodeId());
                insertSt.setObject(10, yeeExam.getCourseId());
                insertSt.setObject(11, yeeExam.getStartTime());
                insertSt.setObject(12, yeeExam.getEndTime());
                insertSt.setObject(13, yeeExam.getPaperId());
                insertSt.setObject(14, yeeExam.getCreateUserId());
                insertSt.setObject(15, yeeExam.getIsPrivate());
                insertSt.setString(16, JSON.toJSONString(yeeExam.getClassList()));
                insertSt.setObject(17, yeeExam.getTeacherType());
                insertSt.setObject(18, yeeExam.getAllow());
                insertSt.setObject(19, yeeExam.getFrequency());
                insertSt.setObject(20, yeeExam.getRandom());
                insertSt.setObject(21, yeeExam.getHasCollect());
                insertSt.setString(22, JSON.toJSONString(yeeExam.getRandData()));
                insertSt.setObject(23, yeeExam.getSchoolId());
                insertSt.setObject(24, yeeExam.getParsing());
                insertSt.setObject(25, yeeExam.getRandNumber());

                int rows = insertSt.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("添加试卷失败");
                }

                try (ResultSet generatedKeys = insertSt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        examId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("创建试卷失败，未获取到主键");
                    }
                }
            }


            // 5. 查询题篮中的题目 ID 列表
            String basketSql = "SELECT exId FROM yee_basket WHERE userId = ?";
            List<Integer> exIdList = new ArrayList<>();
            try (PreparedStatement basketSt = connection.prepareStatement(basketSql)) {
                basketSt.setObject(1, yeeExam.getUserId());
                try (ResultSet rs = basketSt.executeQuery()) {
                    while (rs.next()) {
                        exIdList.add(rs.getInt("exId"));
                    }
                }
            }

            if (exIdList.isEmpty()) {
                // 提交事务（即使无题目，也算成功）
                connection.commit();
                return Result.success("试卷创建成功，但无题目", examId);
            }


            // 6. 动态生成 IN 查询
            String placeholders = String.join(",", Collections.nCopies(exIdList.size(), "?"));
            String questionSql = "SELECT * FROM yee_question WHERE id IN (" + placeholders + ")";

            List<YeeExamTopic> topicList = new ArrayList<>();
            try (PreparedStatement questionSt = connection.prepareStatement(questionSql)) {
                for (int i = 0; i < exIdList.size(); i++) {
                    questionSt.setInt(i + 1, exIdList.get(i));
                }
                int i = 1;
                try (ResultSet rs = questionSt.executeQuery()) {
                    while (rs.next()) {
                        YeeExamTopic topic = new YeeExamTopic();
                        topic.setOid(rs.getInt("oid"));
                        topic.setTopic(rs.getString("topic"));
                        topic.setType(rs.getInt("type"));
                        topic.setLevel(rs.getInt("level"));
                        topic.setScore(rs.getInt("score"));
                        topic.setAnalysis(rs.getString("analysis"));
                        topic.setPid(rs.getInt("pid"));
                        topic.setExamId(examId);
                        topic.setTitle(rs.getString("title"));
                        topic.setUpload(rs.getString("upload"));
                        topic.setScoreMode(rs.getInt("scoreMode"));
                        topic.setSchoolId(yeeExam.getSchoolId());
                        topic.setCateBid(rs.getInt("cateBid"));
                        topic.setCateMid(rs.getInt("cateMid"));
                        topic.setNumber(i++);

                        topic.setOption(JsonUtil.parseOption(rs.getString("option")));
                        topic.setCategoryId(JsonUtil.parseList(rs.getString("categoryId"), Integer.class));
                        topic.setMissScore(JSON.parseObject(rs.getString("missScore"), List.class));
                        topic.setOption1(null);
                        topic.setOption2(null);
                        topic.setOption3(null);

                        topicList.add(topic);
                    }
                }
            }
            // 7. 判断 是否勾选了 随机抽题, 如果勾选了 需要检验 各自类型的题目的分数是否一致, 校验提示哪个类型分数不一致
            Result extracted = extracted(yeeExam, topicList);
            if (extracted.getCode() == 500) {
                // 回滚事务
                connection.rollback();
                return extracted;
            }


            // 8. 批量插入题目
            String insertTopicSql = """
                    INSERT INTO yee_exam_topic 
                    (oid, topic, type, level, score, missScore, option1, option2, option3, analysis, pid, examId, title, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid, number)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement insertTopicSt = connection.prepareStatement(insertTopicSql)) {
                for (YeeExamTopic topic : topicList) {
                    insertTopicSt.setObject(1, topic.getOid());
                    insertTopicSt.setObject(2, topic.getTopic());
                    insertTopicSt.setObject(3, topic.getType());
                    insertTopicSt.setObject(4, topic.getLevel());
                    insertTopicSt.setObject(5, topic.getScore());
                    insertTopicSt.setObject(6, JsonUtil.toJson(topic.getMissScore()));
                    insertTopicSt.setObject(7, null);
                    insertTopicSt.setObject(8, null);
                    insertTopicSt.setObject(9, null);
                    insertTopicSt.setObject(10, topic.getAnalysis());
                    insertTopicSt.setObject(11, topic.getPid());
                    insertTopicSt.setObject(12, topic.getExamId());
                    insertTopicSt.setObject(13, topic.getTitle());
                    insertTopicSt.setObject(14, topic.getUpload());
                    insertTopicSt.setObject(15, JsonUtil.toJson(topic.getOption()));
                    insertTopicSt.setObject(16, topic.getScoreMode());
                    insertTopicSt.setObject(17, topic.getSchoolId());
                    insertTopicSt.setObject(18, JsonUtil.toJson(topic.getCategoryId()));
                    insertTopicSt.setObject(19, topic.getCateBid());
                    insertTopicSt.setObject(20, topic.getCateMid());
                    insertTopicSt.setObject(21, topic.getNumber());

                    insertTopicSt.addBatch();
                }
                insertTopicSt.executeBatch();
            }


            // 更新试卷的 topicNumber 和 score 字段
            int topicNumber;
            int totalScore;

            // 判断是否为随机抽题模式
            if (yeeExam.getRandom() != null && yeeExam.getRandom() == 1 && yeeExam.getRandData() != null) {
                // 随机抽题模式：根据 randData 计算题目数量和总分
                Map<String, Integer> randData = yeeExam.getRandData();

                // 创建题型到题目数量的映射
                Map<Integer, Integer> typeToCountMap = new HashMap<>();
                typeToCountMap.put(1, randData.getOrDefault("t1", 0)); // 单选题
                typeToCountMap.put(2, randData.getOrDefault("t2", 0)); // 多选题
                typeToCountMap.put(3, randData.getOrDefault("t3", 0)); // 判断题
                typeToCountMap.put(5, randData.getOrDefault("t4", 0)); // 填空题
                typeToCountMap.put(4, randData.getOrDefault("t5", 0)); // 简答题

                // 统计总的题目数量
                topicNumber = typeToCountMap.values().stream().mapToInt(Integer::intValue).sum();

                // 按题型分组收集分数，计算每种题型的单题分数
                Map<Integer, Integer> typeToScoreMap = new HashMap<>();
                for (YeeExamTopic topic : topicList) {
                    Integer type = (int) topic.getType();
                    Integer score = (int) topic.getScore();
                    // 只记录一次该题型的分数（假设同一题型分数一致）
                    typeToScoreMap.putIfAbsent(type, score);
                }

                // 计算总分：每种题型的数量 * 该题型的单题分数
                totalScore = 0;
                for (Map.Entry<Integer, Integer> entry : typeToCountMap.entrySet()) {
                    Integer type = entry.getKey();
                    Integer count = entry.getValue();
                    Integer score = typeToScoreMap.getOrDefault(type, 0);
                    totalScore += count * score;
                }

                // 设置实际抽题总数量
                yeeExam.setRandNumber(topicNumber);

            } else {
                // 非随机抽题模式：使用原有逻辑
                topicNumber = topicList.size();
                totalScore = Math.toIntExact(topicList.stream().mapToLong(YeeExamTopic::getScore).sum());

            }

            String updatePaperSql = "UPDATE yee_exam SET topicNumber = ?, score = ? WHERE id = ?";
            try (PreparedStatement updatePaperSt = connection.prepareStatement(updatePaperSql)) {
                updatePaperSt.setInt(1, topicNumber);
                updatePaperSt.setInt(2, totalScore);
                updatePaperSt.setInt(3, examId);

                int updateRows = updatePaperSt.executeUpdate();
                if (updateRows != 1) {
                    throw new SQLException("更新试卷信息失败");
                }
            }

            // 提交事务
            connection.commit();

            // 根据 courseId 查询 yee_course_student 是否有学生选课, 如果有的话:根据courseId查询yee_exam表中有多少考试数量, ,更新到yee_course_student表的examCount字段 ,没有的话不更新
            // 需要在事务提交后执行，以确保当前添加的考试被计算在内
//            updateWorkCountForCourse(slSchool, yeeExam.getCourseId());

            return Result.success("试卷创建成功", examId);

        } catch (SQLException e) {
            // 回滚事务
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("回滚失败: " + rollbackEx.getMessage());
            }
            throw new Exception("添加试卷失败: " + e.getMessage(), e);
        } catch (Exception e) {
            // 非 SQL 异常也尝试回滚
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("回滚失败: " + rollbackEx.getMessage());
            }
            throw e;
        } finally {
            // 恢复 autoCommit 状态并关闭连接
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.setAutoCommit(true); // 恢复默认
                    connection.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    @Override
    public Result addMore(YeeExam yeeExam) throws Exception {
        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(yeeExam.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            throw new Exception("无法获取数据库连接");
        }

        try {
            // 关闭自动提交，开启事务
            connection.setAutoCommit(false);

            // 4. 插入试卷主表
            String insertSql = """
                    INSERT INTO yee_exam 
                    (userId, title, topicNumber, score,limitedTime, remarks, addTime, sequence, nodeId, courseId, startTime, endTime, paperId, createUserId,
                     isPrivate, classList, teacherType, allow, frequency, random, hasCollect, randData, schoolId, parsing, randNumber)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            Integer examId;
            try (PreparedStatement insertSt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertSt.setObject(1, yeeExam.getUserId());
                insertSt.setObject(2, yeeExam.getTitle());
                insertSt.setObject(3, yeeExam.getTopicNumber() != null ? yeeExam.getTopicNumber() : 0);
                insertSt.setObject(4, yeeExam.getScore() != null ? yeeExam.getScore() : 0);
                insertSt.setObject(5, yeeExam.getLimitedTime());
                insertSt.setObject(6, yeeExam.getRemarks());
                insertSt.setObject(7, yeeExam.getAddTime());
                insertSt.setObject(8, yeeExam.getSequence());
                insertSt.setObject(9, yeeExam.getNodeId());
                insertSt.setObject(10, yeeExam.getCourseId());
                insertSt.setObject(11, yeeExam.getStartTime());
                insertSt.setObject(12, yeeExam.getEndTime());
                insertSt.setObject(13, yeeExam.getPaperId());
                insertSt.setObject(14, yeeExam.getCreateUserId());
                insertSt.setObject(15, yeeExam.getIsPrivate());
                insertSt.setString(16, JSON.toJSONString(yeeExam.getClassList()));
                insertSt.setObject(17, yeeExam.getTeacherType());
                insertSt.setObject(18, yeeExam.getAllow());
                insertSt.setObject(19, yeeExam.getFrequency());
                insertSt.setObject(20, yeeExam.getRandom());
                insertSt.setObject(21, yeeExam.getHasCollect());
                insertSt.setString(22, JSON.toJSONString(yeeExam.getRandData()));
                insertSt.setObject(23, yeeExam.getSchoolId());
                insertSt.setObject(24, yeeExam.getParsing());
                insertSt.setObject(25, yeeExam.getRandNumber());

                int rows = insertSt.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("添加试卷失败");
                }

                try (ResultSet generatedKeys = insertSt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        examId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("创建试卷失败，未获取到主键");
                    }
                }
            }


            String questionSql = "SELECT * FROM yee_paper_topic WHERE paperId = ?";

            List<YeeExamTopic> topicList = new ArrayList<>();
            try (PreparedStatement questionSt = connection.prepareStatement(questionSql)) {
                questionSt.setInt(1, yeeExam.getPaperId());
                int j = 1;
                try (ResultSet rs = questionSt.executeQuery()) {
                    if (yeeExam.getSequence() == 2){
                        while (rs.next()) {
                            YeeExamTopic topic = new YeeExamTopic();
                            topic.setOid(rs.getInt("oid"));
                            topic.setTopic(rs.getString("topic"));
                            topic.setType(rs.getInt("type"));
                            topic.setLevel(rs.getInt("level"));
                            topic.setScore(rs.getInt("score"));
                            topic.setAnalysis(rs.getString("analysis"));
                            topic.setPid(rs.getInt("pid"));
                            topic.setExamId(examId);
                            topic.setTitle(rs.getString("title"));
                            topic.setUpload(rs.getString("upload"));
                            topic.setScoreMode(rs.getInt("scoreMode"));
                            topic.setSchoolId(yeeExam.getSchoolId());
                            topic.setCateBid(rs.getInt("cateBid"));
                            topic.setCateMid(rs.getInt("cateMid"));
                            topic.setNumber(ThreadLocalRandom.current().nextInt(1, 50 + 1));

                            topic.setOption(JsonUtil.parseOption(rs.getString("option")));
                            topic.setCategoryId(JsonUtil.parseList(rs.getString("categoryId"), Integer.class));
                            topic.setMissScore(JSON.parseObject(rs.getString("missScore"), List.class));
                            topic.setOption1(null);
                            topic.setOption2(null);
                            topic.setOption3(null);

                            topicList.add(topic);
                        }
                    } else {
                        while (rs.next()) {
                            YeeExamTopic topic = new YeeExamTopic();
                            topic.setOid(rs.getInt("oid"));
                            topic.setTopic(rs.getString("topic"));
                            topic.setType(rs.getInt("type"));
                            topic.setLevel(rs.getInt("level"));
                            topic.setScore(rs.getInt("score"));
                            topic.setAnalysis(rs.getString("analysis"));
                            topic.setPid(rs.getInt("pid"));
                            topic.setExamId(examId);
                            topic.setTitle(rs.getString("title"));
                            topic.setUpload(rs.getString("upload"));
                            topic.setScoreMode(rs.getInt("scoreMode"));
                            topic.setSchoolId(yeeExam.getSchoolId());
                            topic.setCateBid(rs.getInt("cateBid"));
                            topic.setCateMid(rs.getInt("cateMid"));
                            topic.setNumber(j++);

                            topic.setOption(JsonUtil.parseOption(rs.getString("option")));
                            topic.setCategoryId(JsonUtil.parseList(rs.getString("categoryId"), Integer.class));
                            topic.setMissScore(JSON.parseObject(rs.getString("missScore"), List.class));
                            topic.setOption1(null);
                            topic.setOption2(null);
                            topic.setOption3(null);

                            topicList.add(topic);
                        }
                    }
                }
            }

            // 7. 判断 是否勾选了 随机抽题, 如果勾选了 需要检验 各自类型的题目的分数是否一致, 校验提示哪个类型分数不一致
            Result extracted = extracted(yeeExam, topicList);
            if (extracted.getCode() == 500) {
                // 回滚事务
                connection.rollback();
                return extracted;
            }

            // 8. 批量插入题目
            String insertTopicSql = """
                    INSERT INTO yee_exam_topic 
                    (oid, topic, type, level, score, missScore, option1, option2, option3, analysis, pid, examId, title, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid, number)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement insertTopicSt = connection.prepareStatement(insertTopicSql)) {
                for (YeeExamTopic topic : topicList) {
                    insertTopicSt.setObject(1, topic.getOid());
                    insertTopicSt.setObject(2, topic.getTopic());
                    insertTopicSt.setObject(3, topic.getType());
                    insertTopicSt.setObject(4, topic.getLevel());
                    insertTopicSt.setObject(5, topic.getScore());
                    insertTopicSt.setObject(6, JsonUtil.toJson(topic.getMissScore()));
                    insertTopicSt.setObject(7, null);
                    insertTopicSt.setObject(8, null);
                    insertTopicSt.setObject(9, null);
                    insertTopicSt.setObject(10, topic.getAnalysis());
                    insertTopicSt.setObject(11, topic.getPid());
                    insertTopicSt.setObject(12, topic.getExamId());
                    insertTopicSt.setObject(13, topic.getTitle());
                    insertTopicSt.setObject(14, topic.getUpload());
                    insertTopicSt.setObject(15, JsonUtil.toJson(topic.getOption()));
                    insertTopicSt.setObject(16, topic.getScoreMode());
                    insertTopicSt.setObject(17, topic.getSchoolId());
                    insertTopicSt.setObject(18, JsonUtil.toJson(topic.getCategoryId()));
                    insertTopicSt.setObject(19, topic.getCateBid());
                    insertTopicSt.setObject(20, topic.getCateMid());
                    insertTopicSt.setObject(21, topic.getNumber());

                    insertTopicSt.addBatch();
                }
                insertTopicSt.executeBatch();
            }



            // 更新试卷的 topicNumber 和 score 字段
            int topicNumber;
            int totalScore;

            // 判断是否为随机抽题模式
            if (yeeExam.getRandom() != null && yeeExam.getRandom() == 1 && yeeExam.getRandData() != null) {
                // 随机抽题模式：根据 randData 计算题目数量和总分
                Map<String, Integer> randData = yeeExam.getRandData();

                // 创建题型到题目数量的映射
                Map<Integer, Integer> typeToCountMap = new HashMap<>();
                typeToCountMap.put(1, randData.getOrDefault("t1", 0)); // 单选题
                typeToCountMap.put(2, randData.getOrDefault("t2", 0)); // 多选题
                typeToCountMap.put(3, randData.getOrDefault("t3", 0)); // 判断题
                typeToCountMap.put(5, randData.getOrDefault("t4", 0)); // 填空题
                typeToCountMap.put(4, randData.getOrDefault("t5", 0)); // 简答题

                // 统计总的题目数量
                topicNumber = typeToCountMap.values().stream().mapToInt(Integer::intValue).sum();

                // 按题型分组收集分数，计算每种题型的单题分数
                Map<Integer, Integer> typeToScoreMap = new HashMap<>();
                for (YeeExamTopic topic : topicList) {
                    Integer type = (int) topic.getType();
                    Integer score = (int) topic.getScore();
                    // 只记录一次该题型的分数（假设同一题型分数一致）
                    typeToScoreMap.putIfAbsent(type, score);
                }

                // 计算总分：每种题型的数量 * 该题型的单题分数
                totalScore = 0;
                for (Map.Entry<Integer, Integer> entry : typeToCountMap.entrySet()) {
                    Integer type = entry.getKey();
                    Integer count = entry.getValue();
                    Integer score = typeToScoreMap.getOrDefault(type, 0);
                    totalScore += count * score;
                }

                // 设置实际抽题总数量
                yeeExam.setRandNumber(topicNumber);

            } else {
                // 非随机抽题模式：使用原有逻辑
                topicNumber = topicList.size();
                totalScore = Math.toIntExact(topicList.stream().mapToLong(YeeExamTopic::getScore).sum());

            }

            String updatePaperSql = "UPDATE yee_exam SET topicNumber = ?, score = ? WHERE id = ?";
            try (PreparedStatement updatePaperSt = connection.prepareStatement(updatePaperSql)) {
                updatePaperSt.setInt(1, topicNumber);
                updatePaperSt.setInt(2, totalScore);
                updatePaperSt.setInt(3, examId);

                int updateRows = updatePaperSt.executeUpdate();
                if (updateRows != 1) {
                    throw new SQLException("更新试卷信息失败");
                }
            }

            // 提交事务
            connection.commit();

            // 根据 courseId 查询 yee_course_student 是否有学生选课, 如果有的话:根据courseId查询yee_exam表中有多少考试数量, ,更新到yee_course_student表的examCount字段 ,没有的话不更新
            // 需要在事务提交后执行，以确保当前添加的考试被计算在内
//            updateWorkCountForCourse(slSchool, yeeExam.getCourseId());

            return Result.success("试卷创建成功", examId);

        } catch (SQLException e) {
            // 回滚事务
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("回滚失败: " + rollbackEx.getMessage());
            }
            throw new Exception("添加试卷失败: " + e.getMessage(), e);
        } catch (Exception e) {
            // 非 SQL 异常也尝试回滚
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("回滚失败: " + rollbackEx.getMessage());
            }
            throw e;
        } finally {
            // 恢复 autoCommit 状态并关闭连接
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.setAutoCommit(true); // 恢复默认
                    connection.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private Result extracted(YeeExam yeeExam, List<YeeExamTopic> topicList) throws Exception {
        Map<String, Integer> randData = yeeExam.getRandData();
        if (yeeExam.getRandom() != null && yeeExam.getRandom() == 1 && randData != null) {
            // 校验题目分数一致性
            Map<Integer, Set<Integer>> typeToScoresMap = new HashMap<>();

            // 按题型分组收集分数
            for (YeeExamTopic topic : topicList) {
                Integer type = (int) topic.getType();
                Integer score = (int) topic.getScore();

                typeToScoresMap.computeIfAbsent(type, k -> new HashSet<>()).add(score);
            }

            // 检查每种题型的分数是否一致
            for (Map.Entry<Integer, Set<Integer>> entry : typeToScoresMap.entrySet()) {
                Integer type = entry.getKey();
                Set<Integer> scores = entry.getValue();

                if (scores.size() > 1) {
                    // 分数不一致，返回错误信息
                    String typeName = getQuestionTypeName(type);
                    return Result.error("随机抽题模式下，" + typeName + "的题目分数不一致，请确保同一题型的分数相同");
                }
            }

            // 校验题目数量是否超过可用题目数
            // 创建题型到题目数量的映射
            Map<Integer, Integer> typeToCountMap = new HashMap<>();

            // 统计各题型的实际题目数量
            for (YeeExamTopic topic : topicList) {
                Integer type = (int) topic.getType();
                typeToCountMap.put(type, typeToCountMap.getOrDefault(type, 0) + 1);
            }

            // 将randData中的t1-t5键映射到实际的题型
            Map<String, Integer> typeMapping = new HashMap<>();
            typeMapping.put("t1", 1); // 单选题
            typeMapping.put("t2", 2); // 多选题
            typeMapping.put("t3", 3); // 判断题
            typeMapping.put("t4", 5); // 填空题
            typeMapping.put("t5", 4); // 简答题

            // 检查每种题型要求的数量是否超过实际可用数量
            for (Map.Entry<String, Integer> entry : randData.entrySet()) {
                String key = entry.getKey();
                Integer requiredCount = entry.getValue();

                if (typeMapping.containsKey(key)) {
                    Integer actualType = typeMapping.get(key);
                    Integer availableCount = typeToCountMap.getOrDefault(actualType, 0);

                    if (requiredCount > availableCount) {
                        String typeName = getQuestionTypeName(actualType);
                        return Result.error("随机抽题模式下，" + typeName + "要求抽取" + requiredCount + "道题目，但实际只有" + availableCount + "道题目可供选择");
                    }
                }
            }
        }
        return Result.success();
    }

    @Override
    public Result selectAllNode(int schoolId, Integer courseId, Integer classId, String title, Integer nodeId, Integer allow) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // ===================== ✅ 权限获取 =====================
            Long teacherId = AuthDataPermissionUtil.getCurrentUserId();
            DataAuth auth = AuthDataPermissionUtil.getCurrentDataAuth();
            // ======================================================

            // 2. 构建动态 SQL：查询考试信息
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT
                w.id,
                w.userId,
                w.title,
                w.topicNumber,
                w.score,
                w.remarks,
                w.addTime,
                w.limitedTime,
                w.sequence,
                w.nodeId,
                w.courseId,
                w.startTime,
                w.endTime,
                w.paperId,
                w.createUserId,
                w.isPrivate,
                w.classList,
                w.teacherType,
                w.allow,
                w.frequency,
                w.hasCollect,
                w.schoolId,
                w.parsing,
                w.addDate,
                w.random,
                w.randData,
                w.randNumber,
                GROUP_CONCAT(cc.name ORDER BY cc.id SEPARATOR ', ') AS classNameList
            FROM yee_exam w
            LEFT JOIN yee_course_class cc
                ON w.schoolId = cc.schoolId
                AND w.courseId = cc.courseId
                AND JSON_CONTAINS(w.classList, CAST(cc.id AS JSON))
                AND cc.allow = 1
            WHERE
                w.schoolId = ?
                AND w.courseId = ?
        """);

            // ===================== ✅ 考试权限核心 =====================
            if (!DataAuth.ALL.equals(auth)) {
                sqlBuilder.append(" AND ( ");
                // 1. 考试对所有班级开放
                sqlBuilder.append(" JSON_LENGTH(w.classList) = 0 ");
                sqlBuilder.append(" OR ");
                // 2. 老师负责的班级
                sqlBuilder.append(" EXISTS ( ");
                sqlBuilder.append("   SELECT 1 FROM yee_course_class ycc ");
                sqlBuilder.append("   WHERE ycc.teacherId = ? ");
                sqlBuilder.append("   AND JSON_CONTAINS(w.classList, CAST(ycc.id AS JSON)) ");
                sqlBuilder.append(" ) ");
                sqlBuilder.append(" OR ");
                // 3. 老师是课程创建者
                sqlBuilder.append(" EXISTS (SELECT 1 FROM yee_course yc WHERE yc.id = w.courseId AND yc.createId = ?) ");
                sqlBuilder.append(" ) ");
            }
            // ============================================================

            // 条件：nodeId 可选
            if (nodeId != null && nodeId > 0) {
                sqlBuilder.append(" AND nodeId = ? ");
            }

            // 条件：title 模糊查询
            if (title != null && !title.trim().isEmpty()) {
                sqlBuilder.append(" AND title LIKE ? ");
            }

            // 条件：allow 可选
            if (allow != null) {
                sqlBuilder.append(" AND allow = ? ");
            }

            sqlBuilder.append("""
            GROUP BY
                w.id,
                w.userId,
                w.title,
                w.topicNumber,
                w.score,
                w.remarks,
                w.addTime,
                w.limitedTime,
                w.sequence,
                w.nodeId,
                w.courseId,
                w.startTime,
                w.endTime,
                w.paperId,
                w.createUserId,
                w.isPrivate,
                w.classList,
                w.teacherType,
                w.allow,
                w.frequency,
                w.hasCollect,
                w.schoolId,
                w.parsing,
                w.addDate,
                w.random,
                w.randData,
                w.randNumber
        """);

            sqlBuilder.append(" ORDER BY addTime DESC ");

            // 3. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 4. 设置参数
            int paramIndex = 1;
            st.setInt(paramIndex++, schoolId);
            st.setInt(paramIndex++, courseId);

            // ===================== ✅ 注入权限参数 =====================
            if (!DataAuth.ALL.equals(auth)) {
                st.setLong(paramIndex++, teacherId);
                st.setLong(paramIndex++, teacherId);
            }
            // ============================================================

            // 动态添加可选参数
            if (nodeId != null && nodeId > 0) {
                st.setInt(paramIndex++, nodeId);
            }

            if (title != null && !title.trim().isEmpty()) {
                st.setString(paramIndex++, "%" + title.trim() + "%");
            }

            if (allow != null) {
                st.setInt(paramIndex++, allow);
            }

            // 5. 执行查询
            rs = st.executeQuery();

            // 6. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    if (columnName.equals("classList")) {
                        String classList = rs.getString(columnName);
                        row.put(columnName, JSON.parseArray(classList));
                    } else if (columnName.equals("addTime")) {
                        row.put(columnName, rs.getTimestamp(columnName).toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    } else {
                        row.put(columnName, rs.getObject(i));
                    }
                }
                result.add(row);
            }

            return Result.success(result);

        } catch (Exception e) {
            log.error("查询考试信息失败 schoolId={} courseId={} classId={} title={} nodeId={} allow={}",
                    schoolId, courseId, classId, title, nodeId, allow, e);
            throw new Exception("查询考试信息失败", e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }


    @Override
    public Result update(YeeExam yeeExam) throws Exception {

        Result queryExamResult = selectById(yeeExam.getSchoolId(), yeeExam.getId());
        YeeExam queryExam = (YeeExam) queryExamResult.getData();

        if (queryExam == null) {
            return Result.error("考试不存在");
        }

        // 检查是否需要重新计算题目数量和分值
        boolean needRecalculate = false;
        boolean switchToNonRandom = false; // 是否从随机抽题切换为不随机抽题

        if (yeeExam.getRandom() != null && yeeExam.getRandom() == 1) {
            // 比较 randData 是否发生变化
            Map<String, Integer> newRandData = yeeExam.getRandData();
            Map<String, Integer> oldRandData = queryExam.getRandData();

            if (!Objects.equals(newRandData, oldRandData)) {
                needRecalculate = true;
            }
        } else if (yeeExam.getRandom() != null && yeeExam.getRandom() == 0 && queryExam.getRandom() != null && queryExam.getRandom() == 1) {
            // 从随机抽题切换为不随机抽题
            needRecalculate = true;
            switchToNonRandom = true;
        }

        // 如果需要重新计算，从试卷题目中计算实际的题目数量和总分
        if (needRecalculate) {
            SlSchool slSchoolForCalc = slSchoolMapper.selectById(yeeExam.getSchoolId());
            if (slSchoolForCalc != null && slSchoolForCalc.getAllow() != 0) {
                try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchoolForCalc)) {
                    String topicSql = "SELECT * FROM yee_exam_topic WHERE examId = ?";
                    try (PreparedStatement st = conn.prepareStatement(topicSql)) {
                        st.setInt(1, yeeExam.getId());
                        List<YeeExamTopic> topicList = new ArrayList<>();
                        try (ResultSet rs = st.executeQuery()) {
                            while (rs.next()) {
                                YeeExamTopic topic = new YeeExamTopic();
                                topic.setOid(rs.getInt("oid"));
                                topic.setTopic(rs.getString("topic"));
                                topic.setType(rs.getInt("type"));
                                topic.setLevel(rs.getInt("level"));
                                topic.setScore(rs.getInt("score"));
                                topic.setAnalysis(rs.getString("analysis"));
                                topic.setPid(rs.getInt("pid"));
                                topic.setExamId(rs.getInt("examId"));
                                topic.setTitle(rs.getString("title"));
                                topic.setUpload(rs.getString("upload"));
                                topic.setScoreMode(rs.getInt("scoreMode"));
                                topic.setSchoolId(rs.getInt("schoolId"));
                                topic.setCateBid(rs.getInt("cateBid"));
                                topic.setCateMid(rs.getInt("cateMid"));
                                topic.setNumber(ThreadLocalRandom.current().nextInt(1, 50 + 1));
                                topic.setOption(JsonUtil.parseOption(rs.getString("option")));
                                topic.setCategoryId(JsonUtil.parseList(rs.getString("categoryId"), Integer.class));
                                topic.setMissScore(JSON.parseObject(rs.getString("missScore"), List.class));
                                topic.setOption1(null);
                                topic.setOption2(null);
                                topic.setOption3(null);
                                topicList.add(topic);
                            }
                        }

                        // 计算题目数量和总分
                        int topicNumber;
                        int totalScore;

                        if (switchToNonRandom) {
                            // 从随机抽题切换为不随机抽题：使用所有题目，清空随机设置
                            topicNumber = topicList.size();
                            totalScore = Math.toIntExact(topicList.stream().mapToLong(YeeExamTopic::getScore).sum());

                            // 设置随机相关字段为无效值
                            yeeExam.setRandNumber(0);
                            yeeExam.setRandData(null);
                            yeeExam.setRandom(0);

                        } else if (yeeExam.getSequence() == 2 && yeeExam.getRandom() == 1) {
                            // 随机抽题模式：根据 randData 中指定的各题型数量重新计算
                            Map<String, Integer> randData = yeeExam.getRandData();
                            if (randData != null) {
                                // 创建 randData 键到实际题型的映射
                                Map<String, Integer> typeMapping = new HashMap<>();
                                typeMapping.put("t1", 1); // 单选题
                                typeMapping.put("t2", 2); // 多选题
                                typeMapping.put("t3", 3); // 判断题
                                typeMapping.put("t4", 5); // 填空题 (注意：t4 对应 type=5)
                                typeMapping.put("t5", 4); // 简答题 (注意：t5 对应 type=4)

                                // 统计各题型的实际可用数量
                                Map<Integer, Integer> typeToAvailableCountMap = new HashMap<>();
                                for (YeeExamTopic topic : topicList) {
                                    Integer type = (int) topic.getType();
                                    typeToAvailableCountMap.put(type, typeToAvailableCountMap.getOrDefault(type, 0) + 1);
                                }

                                // 统计各题型的实际分数（假设同一题型分数一致）
                                Map<Integer, Integer> typeToScoreMap = new HashMap<>();
                                for (YeeExamTopic topic : topicList) {
                                    Integer type = (int) topic.getType();
                                    Integer score = (int) topic.getScore();
                                    typeToScoreMap.putIfAbsent(type, score);
                                }

                                // 验证并计算：根据 randData 中要求的数量计算总分
                                topicNumber = 0;
                                totalScore = 0;
                                for (Map.Entry<String, Integer> entry : randData.entrySet()) {
                                    String key = entry.getKey();
                                    Integer requiredCount = entry.getValue();

                                    if (typeMapping.containsKey(key)) {
                                        Integer actualType = typeMapping.get(key);
                                        Integer availableCount = typeToAvailableCountMap.getOrDefault(actualType, 0);

                                        // 验证：要求的数量不能超过实际可用数量
                                        if (requiredCount > availableCount) {
                                            return Result.error(
                                                String.format("随机抽题设置错误：%s要求抽取%d道题目，但实际只有%d道题目可供选择",
                                                    getQuestionTypeName(actualType), requiredCount, availableCount));
                                        }

                                        Integer scorePerQuestion = typeToScoreMap.getOrDefault(actualType, 0);

                                        topicNumber += requiredCount;
                                        totalScore += requiredCount * scorePerQuestion;
                                    }
                                }
                            } else {
                                // randData 为空，按实际题目数量计算
                                topicNumber = topicList.size();
                                totalScore = Math.toIntExact(topicList.stream().mapToLong(YeeExamTopic::getScore).sum());
                            }

                        } else {
                            // 非随机抽题模式：直接累加
                            topicNumber = topicList.size();
                            totalScore = Math.toIntExact(topicList.stream().mapToLong(YeeExamTopic::getScore).sum());

                        }

                        // 设置计算后的值到 yeeExam 对象
                        yeeExam.setTopicNumber(topicNumber);
                        yeeExam.setScore(totalScore);
                        if (!switchToNonRandom) {
                            yeeExam.setRandNumber(topicNumber);
                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 计算失败不影响主流程，记录日志即可
                }
            }
        }

        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(yeeExam.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 关闭自动提交，开启事务
            conn.setAutoCommit(false);

            // 构建动态 SQL：更新作业信息
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("UPDATE yee_exam SET ");

            List<String> updates = new ArrayList<>();
            List<Object> parameters = new ArrayList<>();

            // 动态添加可更新字段
            if (yeeExam.getUserId() != null) {
                updates.add("userId = ?");
                parameters.add(yeeExam.getUserId());
            }

            if (yeeExam.getTitle() != null) {
                updates.add("title = ?");
                parameters.add(yeeExam.getTitle());
            }

            if (yeeExam.getTopicNumber() != null) {
                updates.add("topicNumber = ?");
                parameters.add(yeeExam.getTopicNumber());
            }

            if (yeeExam.getScore() != null) {
                updates.add("score = ?");
                parameters.add(yeeExam.getScore());
            }

            if (yeeExam.getLimitedTime() != null) {
                updates.add("limitedTime = ?");
                parameters.add(yeeExam.getLimitedTime());
            }

            if (yeeExam.getRemarks() != null) {
                updates.add("remarks = ?");
                parameters.add(yeeExam.getRemarks());
            }

            if (yeeExam.getAddTime() != null) {
                updates.add("addTime = ?");
                parameters.add(yeeExam.getAddTime());
            }

            if (yeeExam.getSequence() != null) {
                updates.add("sequence = ?");
                parameters.add(yeeExam.getSequence());
            }

            if (yeeExam.getNodeId() != null) {
                updates.add("nodeId = ?");
                parameters.add(yeeExam.getNodeId());
            }

            if (yeeExam.getCourseId() != null) {
                updates.add("courseId = ?");
                parameters.add(yeeExam.getCourseId());
            }

            if (yeeExam.getStartTime() != null) {
                updates.add("startTime = ?");
                parameters.add(yeeExam.getStartTime());
            }

            if (yeeExam.getEndTime() != null) {
                updates.add("endTime = ?");
                parameters.add(yeeExam.getEndTime());
            }

            if (yeeExam.getPaperId() != null) {
                updates.add("paperId = ?");
                parameters.add(yeeExam.getPaperId());
            }

            if (yeeExam.getCreateUserId() != null) {
                updates.add("createUserId = ?");
                parameters.add(yeeExam.getCreateUserId());
            }

            if (yeeExam.getIsPrivate() != null) {
                updates.add("isPrivate = ?");
                parameters.add(yeeExam.getIsPrivate());
            }

            if (yeeExam.getClassList() != null) {
                updates.add("classList = ?");
                parameters.add(JSON.toJSONString(yeeExam.getClassList()));
            }

            if (yeeExam.getTeacherType() != null) {
                updates.add("teacherType = ?");
                parameters.add(yeeExam.getTeacherType());
            }

            if (yeeExam.getAllow() != null) {
                updates.add("allow = ?");
                parameters.add(yeeExam.getAllow());
            }

            if (yeeExam.getFrequency() != null) {
                updates.add("frequency = ?");
                parameters.add(yeeExam.getFrequency());
            }

            if (yeeExam.getRandom() != null) {
                updates.add("random = ?");
                parameters.add(yeeExam.getRandom());
            }

            if (yeeExam.getHasCollect() != null) {
                updates.add("hasCollect = ?");
                parameters.add(yeeExam.getHasCollect());
            }

            if (yeeExam.getRandData() != null) {
                updates.add("randData = ?");
                parameters.add(JSON.toJSONString(yeeExam.getRandData()));
            }

            if (yeeExam.getSchoolId() != null) {
                updates.add("schoolId = ?");
                parameters.add(yeeExam.getSchoolId());
            }

            if (yeeExam.getParsing() != null) {
                updates.add("parsing = ?");
                parameters.add(yeeExam.getParsing());
            }

            if (yeeExam.getRandNumber() != null) {
                updates.add("randNumber = ?");
                parameters.add(yeeExam.getRandNumber());
            }

            // 检查是否有可更新的字段
            if (updates.isEmpty()) {
                return Result.error("没有可更新的字段");
            }

            // 添加更新字段到SQL
            sqlBuilder.append(String.join(", ", updates));
            sqlBuilder.append(" WHERE id = ?");
            parameters.add(yeeExam.getId());

            // 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 设置参数
            for (int i = 0; i < parameters.size(); i++) {
                st.setObject(i + 1, parameters.get(i));
            }

            // 执行更新
            int rowsUpdated = st.executeUpdate();

            // 提交事务
            conn.commit();

            if (rowsUpdated > 0) {
                if (yeeExam.getAllow() != null) {
                    if (!queryExam.getAllow().equals(yeeExam.getAllow())) {
                        // 根据 courseId 查询 yee_course_student 是否有学生选课, 如果有的话:根据courseId查询yee_work表中有多少作业数量, ,更新到yee_course_student表的workCount字段 ,没有的话不更新
                        updateWorkCountForCourse(slSchool, yeeExam.getCourseId());
                    }
                }
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败：未找到匹配的记录");
            }

        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        } finally {
            // 恢复 autoCommit 状态并关闭连接
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    closeResultSetAndStatement(rs, st);
                    closeConnection(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Result selectById(Integer schoolId, Integer id) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 2. 查询作业信息
            String sql = "SELECT * FROM yee_exam WHERE id = ? AND schoolId = ?";
            st = conn.prepareStatement(sql);
            st.setInt(1, id);
            st.setInt(2, schoolId);
            rs = st.executeQuery();

            // 3. 封装结果
            if (rs.next()) {
                YeeExam work = new YeeExam();
                work.setId(rs.getInt("id"));
                work.setUserId(rs.getObject("userId", Integer.class));
                work.setTitle(rs.getString("title"));
                work.setTopicNumber(rs.getObject("topicNumber", Integer.class));
                work.setScore(rs.getObject("score", Integer.class));
                work.setLimitedTime(rs.getObject("limitedTime", Integer.class));
                work.setRemarks(rs.getString("remarks"));
                work.setAddTime(rs.getTimestamp("addTime"));
                work.setSequence(rs.getObject("sequence", Integer.class));
                work.setNodeId(rs.getObject("nodeId", Integer.class));
                work.setCourseId(rs.getObject("courseId", Integer.class));
                work.setStartTime(rs.getObject("startTime", Integer.class));
                work.setEndTime(rs.getObject("endTime", Integer.class));
                work.setPaperId(rs.getObject("paperId", Integer.class));
                work.setCreateUserId(rs.getObject("createUserId", Integer.class));
                work.setIsPrivate(rs.getObject("isPrivate", Integer.class));
                work.setClassList(JSON.parseArray(rs.getString("classList")));
                work.setTeacherType(rs.getObject("teacherType", Integer.class));
                work.setAllow(rs.getObject("allow", Integer.class));
                work.setFrequency(rs.getObject("frequency", Integer.class));
                work.setRandom(rs.getObject("random", Integer.class));
                work.setHasCollect(rs.getObject("hasCollect", Integer.class));
                work.setRandData(JSON.parseObject(rs.getString("randData"), new TypeReference<Map<String, Integer>>() {}));
                work.setSchoolId(rs.getObject("schoolId", Integer.class));
                work.setParsing(rs.getObject("parsing", Integer.class));
                work.setAddDate(rs.getDate("addDate"));
                work.setRandNumber(rs.getObject("randNumber", Integer.class));

                return Result.success(work);
            } else {
                return Result.error("未找到指定的作业");
            }

        } catch (Exception e) {
            log.error("查询作业失败", e);
            return Result.error("查询作业失败：" + e.getMessage());
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    @Override
    public Result recoverWork(Integer schoolId, Integer examId) throws Exception {

        Result queryExamResult = selectById(schoolId, examId);
        YeeExam queryExam = (YeeExam) queryExamResult.getData();

        if (queryExam == null) {
            return Result.error("考试不存在");
        }

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 关闭自动提交，开启事务
            conn.setAutoCommit(false);

            // 2. 更新作业表allow字段为0（禁用作业）
            String updateWorkSql = "UPDATE yee_exam SET allow = 0 WHERE id = ? AND schoolId = ?";
            st = conn.prepareStatement(updateWorkSql);
            st.setInt(1, examId);
            st.setInt(2, schoolId);
            int workRows = st.executeUpdate();

            closeStatement(st);
            st = null;

            // 检查是否找到了对应的作业
            if (workRows == 0) {
                conn.rollback();
                return Result.error("未找到对应的作业或作业不属于该学校");
            }

            // 3. 查询需要删除的yee_work_record记录的ID
            String selectRecordIdsSql = "SELECT id FROM yee_exam_record WHERE examId = ?";
            st = conn.prepareStatement(selectRecordIdsSql);
            st.setInt(1, examId);
            rs = st.executeQuery();

            List<Integer> recordIds = new ArrayList<>();
            while (rs.next()) {
                recordIds.add(rs.getInt("id"));
            }

            closeResultSetAndStatement(rs, st);
            rs = null;
            st = null;

            // 4. 删除 yee_exam_answer 表中对应的记录
            if (!recordIds.isEmpty()) {
                // 构建IN查询语句
                String placeholders = String.join(",", Collections.nCopies(recordIds.size(), "?"));
                String deleteAnswerSql = "DELETE FROM yee_exam_answer WHERE recordId IN (" + placeholders + ")";
                st = conn.prepareStatement(deleteAnswerSql);

                for (int i = 0; i < recordIds.size(); i++) {
                    st.setInt(i + 1, recordIds.get(i));
                }

                int answerRows = st.executeUpdate();
                closeStatement(st);
                st = null;
            }

            // 5. 删除 yee_exam_score 表中对应的记录
            String deleteRecordSql = "DELETE FROM yee_exam_record WHERE examId = ?";
            st = conn.prepareStatement(deleteRecordSql);
            st.setInt(1, examId);
            int recordRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 6. 删除 yee_exam_score 表中对应的记录
            String deleteScoreSql = "DELETE FROM yee_exam_score WHERE examId = ?";
            st = conn.prepareStatement(deleteScoreSql);
            st.setInt(1, examId);
            int scoreRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 7. 提交事务
            conn.commit();

            updateWorkCountForCourse(slSchool, queryExam.getCourseId());

            Map<String, Object> result = new HashMap<>();
            result.put("workRows", workRows);
            result.put("recordRows", recordRows);
            result.put("message", "测回成功，已更新作业状态并删除相关记录");

            return Result.success(result);

        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return Result.error("测回失败：" + e.getMessage());
        } finally {
            // 恢复 autoCommit 状态并关闭连接
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    closeResultSetAndStatement(rs, st);
                    closeConnection(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Result deleteWork(Integer schoolId, Integer examId) throws Exception {

        Result queryExamResult = selectById(schoolId, examId);
        YeeExam queryExam = (YeeExam) queryExamResult.getData();

        if (queryExam == null) {
            return Result.error("考试不存在");
        }

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 关闭自动提交，开启事务
            conn.setAutoCommit(false);

            // 2. 查询需要删除的yee_work_record记录的ID
            String selectRecordIdsSql = "SELECT id FROM yee_exam_record WHERE examId = ?";
            st = conn.prepareStatement(selectRecordIdsSql);
            st.setInt(1, examId);
            rs = st.executeQuery();

            List<Integer> recordIds = new ArrayList<>();
            while (rs.next()) {
                recordIds.add(rs.getInt("id"));
            }

            closeResultSetAndStatement(rs, st);
            rs = null;
            st = null;

            // 3. 删除yee_exam_answer表中对应的记录
            if (!recordIds.isEmpty()) {
                // 构建IN查询语句
                String placeholders = String.join(",", Collections.nCopies(recordIds.size(), "?"));
                String deleteAnswerSql = "DELETE FROM yee_exam_answer WHERE recordId IN (" + placeholders + ")";
                st = conn.prepareStatement(deleteAnswerSql);

                for (int i = 0; i < recordIds.size(); i++) {
                    st.setInt(i + 1, recordIds.get(i));
                }

                int answerRows = st.executeUpdate();
                closeStatement(st);
                st = null;
            }

            // 4. 删除yee_exam_record表中对应的记录
            String deleteRecordSql = "DELETE FROM yee_exam_record WHERE examId = ?";
            st = conn.prepareStatement(deleteRecordSql);
            st.setInt(1, examId);
            int recordRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 5. 删除yee_exam_topic表中对应的记录
            String deleteTopicSql = "DELETE FROM yee_exam_topic WHERE examId = ?";
            st = conn.prepareStatement(deleteTopicSql);
            st.setInt(1, examId);
            int topicRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 6. 删除yee_exam表中的记录
            String deleteWorkSql = "DELETE FROM yee_exam WHERE id = ? AND schoolId = ?";
            st = conn.prepareStatement(deleteWorkSql);
            st.setInt(1, examId);
            st.setInt(2, schoolId);
            int examRows = st.executeUpdate();

            closeStatement(st);
            st = null;

            // 检查是否找到了对应的考试
            if (examRows == 0) {
                conn.rollback();
                return Result.error("未找到对应的考试或考试不属于该学校");
            }

            // 6. 删除 yee_exam_score 表中对应的记录
            String deleteScoreSql = "DELETE FROM yee_exam_score WHERE examId = ?";
            st = conn.prepareStatement(deleteScoreSql);
            st.setInt(1, examId);
            int scoreRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 7. 提交事务
            conn.commit();

            updateWorkCountForCourse(slSchool, queryExam.getCourseId());

            Map<String, Object> result = new HashMap<>();
            result.put("examRows", examRows);
            result.put("recordRows", recordRows);
            result.put("topicRows", topicRows);
            result.put("message", "删除成功，已删除考试及相关记录");

            return Result.success(result);

        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        } finally {
            // 恢复 autoCommit 状态并关闭连接
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    closeResultSetAndStatement(rs, st);
                    closeConnection(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Result redoExam(Integer schoolId, Integer examId, Integer userId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 关闭自动提交，开启事务
            conn.setAutoCommit(false);

            // 2. 查询需要删除的yee_exam_record记录的ID（根据考试ID和用户ID）
            String selectRecordIdsSql;
            if (userId != null && userId > 0) {
                // 指定学生的考试记录
                selectRecordIdsSql = "SELECT id FROM yee_exam_record WHERE examId = ? AND userId = ?";
                st = conn.prepareStatement(selectRecordIdsSql);
                st.setInt(1, examId);
                st.setInt(2, userId);
            } else {
                // 所有学生的考试记录
                selectRecordIdsSql = "SELECT id FROM yee_exam_record WHERE examId = ?";
                st = conn.prepareStatement(selectRecordIdsSql);
                st.setInt(1, examId);
            }

            rs = st.executeQuery();

            List<Integer> recordIds = new ArrayList<>();
            while (rs.next()) {
                recordIds.add(rs.getInt("id"));
            }

            closeResultSetAndStatement(rs, st);
            rs = null;
            st = null;

            // 3. 删除yee_exam_answer表中对应的记录
            if (!recordIds.isEmpty()) {
                // 构建IN查询语句
                String placeholders = String.join(",", Collections.nCopies(recordIds.size(), "?"));
                String deleteAnswerSql = "DELETE FROM yee_exam_answer WHERE recordId IN (" + placeholders + ")";
                st = conn.prepareStatement(deleteAnswerSql);

                for (int i = 0; i < recordIds.size(); i++) {
                    st.setInt(i + 1, recordIds.get(i));
                }

                int answerRows = st.executeUpdate();
                closeStatement(st);
                st = null;
            }

            // 4. 删除yee_exam_record表中对应的记录
            String deleteRecordSql;
            if (userId != null && userId > 0) {
                deleteRecordSql = "DELETE FROM yee_exam_record WHERE examId = ? AND userId = ?";
                st = conn.prepareStatement(deleteRecordSql);
                st.setInt(1, examId);
                st.setInt(2, userId);
            } else {
                deleteRecordSql = "DELETE FROM yee_exam_record WHERE examId = ?";
                st = conn.prepareStatement(deleteRecordSql);
                st.setInt(1, examId);
            }

            int recordRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 5. 删除 yee_exam_score 表中对应的记录
            String deleteScoreSql = "DELETE FROM yee_exam_score WHERE examId = ? AND userId = ?";
            st = conn.prepareStatement(deleteScoreSql);
            st.setInt(1, examId);
            st.setInt(2, userId);
            int scoreRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // ========== 新增逻辑：更新yee_course_student表的examLearned字段 ==========
            if (userId != null && userId > 0) {
                // 5.1 查询该考试所属的课程ID（根据实际业务补充，假设考试表yee_exam有courseId字段）
                String selectCourseIdSql = "SELECT courseId FROM yee_exam WHERE id = ?";
                st = conn.prepareStatement(selectCourseIdSql);
                st.setInt(1, examId);
                rs = st.executeQuery();
                Integer courseId = null;
                if (rs.next()) {
                    courseId = rs.getInt("courseId");
                }
                closeResultSetAndStatement(rs, st);
                rs = null;
                st = null;

                // 5.2 如果能获取到课程ID，更新examLearned（减1，且保证不小于0）
                if (courseId != null) {
                    String updateExamLearnedSql =
                            "UPDATE yee_course_student SET examLearned = GREATEST(examLearned - 1, 0) " +
                                    "WHERE courseId = ? AND studentId = ?";
                    st = conn.prepareStatement(updateExamLearnedSql);
                    st.setInt(1, courseId);
                    st.setInt(2, userId);
                    int updateRows = st.executeUpdate();
                    closeStatement(st);
                    st = null;
                } else {
                }
            }
            // ========== 新增逻辑结束 ==========

            // 6. 提交事务
            conn.commit();

            Map<String, Object> result = new HashMap<>();
            result.put("recordRows", recordRows);
            result.put("message", "打回重做成功，已删除考试记录和答案，学生可重新答题");

            return Result.success(result);

        } catch (Exception e) {
            // 回滚事务
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return Result.error("打回重做失败：" + e.getMessage());
        } finally {
            // 恢复 autoCommit 状态并关闭连接
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    closeResultSetAndStatement(rs, st);
                    closeConnection(conn);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    // ========== 辅助方法：合并过滤逻辑（优化性能） ==========
    private List<Map<String, Object>> filterData(List<Map<String, Object>> data, Integer subState, Integer reviewState) {
        return data.stream()
                .filter(record -> filterBySubState(record, subState))
                .filter(record -> filterByReviewState(record, reviewState))
                .collect(Collectors.toList());
    }

    // 交卷状态过滤
    private boolean filterBySubState(Map<String, Object> record, Integer subState) {
        if (subState == null) return true;

        Object stateObj = record.get("state");
        if (!(stateObj instanceof Number)) {
            return subState == 1; // subState=1时保留null/非数字的state
        }

        int state = ((Number) stateObj).intValue();
        if (subState == 1) {
            return state != 3 && state != 2;
        } else {
            return state == 3 || state == 2;
        }
    }

    // 批阅状态过滤
    private boolean filterByReviewState(Map<String, Object> record, Integer reviewState) {
        if (reviewState == null) return true;

        Object stateObj = record.get("state");
        if (!(stateObj instanceof Number)) {
            return reviewState != 3; // reviewState≠3时保留null/非数字的state
        }

        int state = ((Number) stateObj).intValue();
        return reviewState == 3 ? (state == 3) : (state != 3);
    }

    /**
     * 导出作业成绩为Excel
     */
    @Override
    public void exportWorkScore(HttpServletResponse response, YeeExamExportDTO queryDTO) {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            SlSchool slSchool = slSchoolMapper.selectById(queryDTO.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new RuntimeException("学校不存在或未审核");
            }
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            Map<String, Object> workInfo = getWorkInfoByWorkId(queryDTO.getSchoolId(), queryDTO.getExamId(), queryDTO.getCourseId());
            if (workInfo == null) {
                throw new RuntimeException("未找到考试信息");
            }
            String workTitle = String.valueOf(workInfo.get("title"));
            Number totalScoreNum = (Number) workInfo.get("score");
            double totalScore = totalScoreNum != null ? totalScoreNum.doubleValue() : 100.0;
            double passLine = totalScore * 0.6;
            StringBuilder sql = new StringBuilder();
            sql.append("""
                SELECT 
                    s.number AS studentNumber,
                    s.name AS studentName,
                    cc.name AS className,
                    ws.finalScore,
                    ws.submitTime,
                    ws.scored AS scoredState,
                    wr.state AS reviewState
                FROM yee_course_student ycs
                JOIN yee_student s ON s.id = ycs.studentId AND s.schoolId = ycs.schoolId
                LEFT JOIN yee_course_class cc ON cc.id = ycs.classId AND cc.schoolId = ycs.schoolId
                LEFT JOIN yee_exam_record wr ON wr.userId = ycs.studentId AND wr.examId = ? AND wr.courseId = ycs.courseId
                LEFT JOIN yee_exam_score ws ON ws.userId = ycs.studentId AND ws.examId = ?
                WHERE ycs.schoolId = ? AND ycs.courseId = ?
            """);
            List<Object> params = new ArrayList<>();
            params.add(queryDTO.getExamId());
            params.add(queryDTO.getExamId());
            params.add(queryDTO.getSchoolId());
            params.add(queryDTO.getCourseId());
            if (queryDTO.getClassId() != null && queryDTO.getClassId() > 0) {
                sql.append(" AND ycs.classId = ?");
                params.add(queryDTO.getClassId());
            }
            if (queryDTO.getKeyword() != null && !queryDTO.getKeyword().trim().isEmpty()) {
                sql.append(" AND (s.number LIKE ? OR s.name LIKE ?)");
                String like = "%" + queryDTO.getKeyword().trim() + "%";
                params.add(like);
                params.add(like);
            }
            if (queryDTO.getSubmitted() != null) {
                if (queryDTO.getSubmitted() == 1) {
                    sql.append(" AND ws.submitTime IS NOT NULL");
                } else if (queryDTO.getSubmitted() == 0) {
                    sql.append(" AND ws.submitTime IS NULL");
                }
            }
            if (queryDTO.getReviewState() != null) {
                sql.append(" AND wr.state = ?");
                params.add(queryDTO.getReviewState());
            }
            if (queryDTO.getScored() != null) {
                sql.append(" AND ws.scored = ?");
                params.add(queryDTO.getScored());
            }
            sql.append(" ORDER BY s.number ASC");
            st = conn.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                st.setObject(i + 1, params.get(i));
            }
            rs = st.executeQuery();
            List<List<Object>> dataRows = new ArrayList<>();
            while (rs.next()) {
                String number = rs.getString("studentNumber");
                String name = rs.getString("studentName");
                String className = rs.getString("className");
                Object finalScoreObj = rs.getObject("finalScore");
                String finalScoreStr = finalScoreObj == null ? "-" : String.valueOf(finalScoreObj);
                String passText;
                if (finalScoreObj == null) {
                    passText = "未评分";
                } else {
                    double fs = 0.0;
                    if (finalScoreObj instanceof Number) {
                        fs = ((Number) finalScoreObj).doubleValue();
                    } else {
                        try {
                            fs = Double.parseDouble(finalScoreObj.toString());
                        } catch (Exception ignore) {}
                    }
                    passText = fs >= passLine ? "合格" : "不合格";
                }
                dataRows.add(Arrays.asList(number, name, className, finalScoreStr, passText));
            }
            String dateStr = new SimpleDateFormat("yyyy年MM月dd日").format(new java.util.Date());
            String title = workTitle + "-（" + dateStr + " 导出）";
            String[] headers = new String[]{"学号","学生姓名","课程班级","成绩","是否合格"};
            List<List<String>> head = new ArrayList<>();
            for (String h : headers) {
                head.add(Arrays.asList(title, h));
            }
            ResponseExportUtil.setExcelRespProp(response, "学生考试成绩_" + System.currentTimeMillis());
            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .registerWriteHandler(new OnceAbsoluteMergeStrategy(0, 0, 0, headers.length - 1))
                    .registerWriteHandler(ExcelExportStyles.defaultTitleRow(headers.length))
//                    .registerWriteHandler(ExcelExportStyles.createFreezeAndWidthHandler(new int[]{14, 16, 18, 10, 12}, 2))
//                    .registerWriteHandler(ExcelExportStyles.textColumns(new int[]{0}))
                    .registerWriteHandler(ExcelExportStyles.defaultStyleStrategy())
                    .sheet("考试成绩")
                    .doWrite(dataRows);
            response.getOutputStream().flush();
        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"导出考试成绩失败: " + e.getMessage() + "\"}");
            } catch (Exception ignore) {}
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignore) {}
            try { if (st != null) st.close(); } catch (Exception ignore) {}
            try { if (conn != null) conn.close(); } catch (Exception ignore) {}
        }
    }


    private Map<String, Object> getWorkInfoByWorkId(int schoolId, Integer workId, Integer courseId) {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        // 获取作业表信息
        Map<String, Object> workInfo = new HashMap<>();
        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            String sql = "SELECT w.*, m.name FROM yee_exam w LEFT JOIN yee_manage m on w.createUserId = m.id WHERE w.id = ? and w.courseId = ?";
            st = conn.prepareStatement(sql);
            st.setInt(1, workId);
            st.setInt(2, courseId);
            rs = st.executeQuery();
            List<Object> objects = rsToWorkInfo(rs);
            if (objects == null || objects.isEmpty()) {
                workInfo = null;
            } else {
                workInfo = (Map<String, Object>) objects.get(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
        return workInfo;
    }

    /**
     * 将YeeQuestion列表转换为QuestionExportVO列表
     * @param questions 试题列表
     * @return 导出VO列表
     */
    private List<QuestionExportVO> convertToExportVO(List<YeeQuestion> questions) {
        List<QuestionExportVO> exportList = new ArrayList<>();

        for (YeeQuestion question : questions) {
            QuestionExportVO vo = new QuestionExportVO();

            // 基础字段
            vo.setTitle(question.getTitle());
            vo.setTopic(removeHtmlTags(question.getTopic()));
            vo.setType(question.getType());
            vo.setLevel(question.getLevel());
            vo.setScore(question.getScore());
            vo.setAnalysis(removeHtmlTags(question.getAnalysis()));
            vo.setScoreMode(question.getScoreMode());
            vo.setAddTime(question.getAddTime());

            // 类型名称转换
            vo.setTypeName(getTypeName(question.getType()));

            // 难度等级转换
            vo.setLevelName(getLevelName(question.getLevel()));

            // 处理选项和得分比
            processOptionsAndScores(vo, question);

            // 处理漏选分值（仅对多选题）
            if (question.getType() != null && question.getType() == 2 && question.getScoreMode() == 2) {
                processMissScores(vo, question);
            }

            // 设置计分模式名称（对多选题）
            if (question.getType() != null && (question.getType() == 2 )) {
                vo.setScoreModeName(getScoreModeName(question.getScoreMode()));
            }

            exportList.add(vo);
        }

        return exportList;
    }


    /**
     * 处理选项和得分比
     * @param vo 导出VO对象
     * @param question 试题对象
     */
    private void processOptionsAndScores(QuestionExportVO vo, YeeQuestion question) {
        // 特殊处理填空题
        if (question.getType() != null && question.getType() == 5) {
            processFillBlankOptions(vo, question);
            return;
        }

        if (question.getOption() == null || question.getOption().isEmpty()) {
            return;
        }

        List<Map<String, Object>> options = question.getOption();
        for (int i = 0; i < options.size() && i < 6; i++) { // 最多处理6个选项
            Map<String, Object> option = options.get(i);
            String answer = (String) option.getOrDefault("answer", "");

            // 设置选项内容
            switch (i) {
                case 0: vo.setOptionA(answer); break;
                case 1: vo.setOptionB(answer); break;
                case 2: vo.setOptionC(answer); break;
                case 3: vo.setOptionD(answer); break;
                case 4: vo.setOptionE(answer); break;
                case 5: vo.setOptionF(answer); break;
            }

            // ================= 安全解析 scale（彻底修复） =================
            double scale = 0.0;
            try {
                Object scaleObj = option.get("scale");
                if (scaleObj != null) {
                    String scaleStr = scaleObj.toString().trim();
                    if (!scaleStr.isEmpty()) {
                        scale = Double.parseDouble(scaleStr);
                    }
                }
            } catch (Exception e) {
                scale = 0.0;
            }

            // 设置得分比（使用安全后的 double）
            switch (i) {
                case 0: vo.setScoreRatioA(scale); break;
                case 1: vo.setScoreRatioB(scale); break;
                case 2: vo.setScoreRatioC(scale); break;
                case 3: vo.setScoreRatioD(scale); break;
                case 4: vo.setScoreRatioE(scale); break;
                case 5: vo.setScoreRatioF(scale); break;
            }
        }
    }

    /**
     * 处理填空题选项
     * @param vo 导出VO对象
     * @param question 试题对象
     */
    private void processFillBlankOptions(QuestionExportVO vo, YeeQuestion question) {
        // 整个方法加 try-catch，彻底防止整条数据被吞
        try {
            if (question.getOption() == null || question.getOption().isEmpty()) {
                return;
            }

            List<Map<String, Object>> options = question.getOption();
            // 安全排序：防止 option 为 null、idx 为 null 导致的空指针
            options.sort((o1, o2) -> {
                try {
                    if (o1 == null && o2 == null) return 0;
                    if (o1 == null) return -1;
                    if (o2 == null) return 1;

                    Object idx1 = o1.get("idx");
                    Object idx2 = o2.get("idx");

                    if (idx1 == null && idx2 == null) return 0;
                    if (idx1 == null) return -1;
                    if (idx2 == null) return 1;

                    if (idx1 instanceof Integer && idx2 instanceof Integer) {
                        return ((Integer) idx1).compareTo((Integer) idx2);
                    } else if (idx1 instanceof String && idx2 instanceof String) {
                        return ((String) idx1).compareTo((String) idx2);
                    }
                    return 0;
                } catch (Exception e) {
                    return 0;
                }
            });

            for (int i = 0; i < options.size() && i < 6; i++) {
                Map<String, Object> option = options.get(i);
                if (option == null) continue;

                // 设置选项内容
                String answer = (String) option.getOrDefault("answer", "");
                switch (i) {
                    case 0: vo.setOptionA(answer); break;
                    case 1: vo.setOptionB(answer); break;
                    case 2: vo.setOptionC(answer); break;
                    case 3: vo.setOptionD(answer); break;
                    case 4: vo.setOptionE(answer); break;
                    case 5: vo.setOptionF(answer); break;
                }

                // 安全解析 scale（专门处理 ""、null、非数字等所有异常）
                double scale = 0.0;
                try {
                    Object scaleObj = option.get("scale");
                    if (scaleObj != null) {
                        String scaleStr = scaleObj.toString().trim();
                        if (!scaleStr.isEmpty()) {
                            scale = Double.parseDouble(scaleStr);
                        }
                    }
                } catch (NumberFormatException e) {
                    // 非数字格式，直接用默认值 0
                    scale = 0.0;
                } catch (Exception e) {
                    scale = 0.0;
                }

                // 设置得分比
                switch (i) {
                    case 0: vo.setScoreRatioA(scale); break;
                    case 1: vo.setScoreRatioB(scale); break;
                    case 2: vo.setScoreRatioC(scale); break;
                    case 3: vo.setScoreRatioD(scale); break;
                    case 4: vo.setScoreRatioE(scale); break;
                    case 5: vo.setScoreRatioF(scale); break;
                }
            }
        } catch (Exception e) {
            // 任何异常都不中断流程，只是记录日志
            e.printStackTrace();
        }
    }

    /**
     * 处理漏选分值
     * @param vo 导出VO对象
     * @param question 试题对象
     */
    private void processMissScores(QuestionExportVO vo, YeeQuestion question) {
        try {
            List<Integer> missScores = question.getMissScore();
            if (missScores == null || missScores.isEmpty()) {
                return;
            }

            for (int i = 0; i < missScores.size() && i < 5; i++) {
                String scoreStr = "";
                Integer score = missScores.get(i);
                if (score != null) {
                    scoreStr = score.toString();
                }

                switch (i) {
                    case 0: vo.setMissScore1(scoreStr); break;
                    case 1: vo.setMissScore2(scoreStr); break;
                    case 2: vo.setMissScore3(scoreStr); break;
                    case 3: vo.setMissScore4(scoreStr); break;
                    case 4: vo.setMissScore5(scoreStr); break;
                }
            }

            // 设置计分模式名称
            if (question.getScoreMode() != null) {
                vo.setScoreModeName(getScoreModeName(question.getScoreMode()));
            }
        } catch (Exception e) {
            // 出任何错都不崩流程
            e.printStackTrace();
        }
    }

    /**
     * 获取题型名称
     * @param type 题型代码
     * @return 题型名称
     */
    private String getTypeName(Integer type) {
        if (type == null) return "";
        switch (type) {
            case 1: return "单选";
            case 2: return "多选";
            case 3: return "判断";
            case 4: return "简答";
            case 5: return "填空";
            default: return "";
        }
    }

    /**
     * 获取难度等级名称
     * @param level 难度等级代码
     * @return 难度等级名称
     */
    private String getLevelName(Integer level) {
        if (level == null) return "";
        switch (level) {
            case 1: return "易";
            case 2: return "中";
            case 3: return "难";
            default: return "";
        }
    }

    /**
     * 获取计分模式名称
     * @param scoreMode 计分模式代码
     * @return 计分模式名称
     */
    private String getScoreModeName(Integer scoreMode) {
        if (scoreMode == null) return "";
        switch (scoreMode) {
            case 1: return "否";
            case 2: return "是";
            default: return "";
        }
    }

    /**
     * 移除HTML标签并保留空格和换行
     * @param html HTML内容
     * @return 纯文本内容
     */
    private String removeHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        // 保留换行符和空格，移除其他HTML标签
        return html.replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&deg;", "°")
                .replaceAll("&ldquo;", "\"")
                .replaceAll("&rdquo;", "\"")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s+", " ") // 合并多个空白字符为一个空格
                .trim();
    }

    /**
     * 分页查询试题数据（复用数据库连接）
     * @param connection 数据库连接
     * @param schoolId 学校ID
     * @param pageSize 每页大小
     * @param pageNum 页码
     * @param topic 题目内容搜索关键词
     * @param createId 创建人ID
     * @param type 题型
     * @param level 难度等级
     * @param cateBid 分类大类ID
     * @param cateMid 分类中类ID
     * @return 分页查询结果
     * @throws Exception 数据库查询异常
     */
    public Result exportAllWithPagination(Connection connection, int schoolId, Integer pageSize, Integer pageNum,
                                          String topic, Integer createId, Integer type,
                                          Integer level, Integer cateBid, Integer cateMid, Integer paperId) throws Exception {
        // 验证参数
        if (connection == null) {
            throw new Exception("数据库连接不能为空");
        }

        // 调用新的selectAll方法获取分页数据
        return selectAll(connection, schoolId, pageSize, pageNum, topic, createId, type, level, cateBid, cateMid, paperId);
    }

    public Result selectAll(Connection connection, int schoolId, Integer pageSize, Integer pageNum,
                            String topic, Integer createId, Integer type,
                            Integer level, Integer cateBid, Integer cateMid, Integer paperId) throws Exception {
        try {
            // 3. 基础 SQL
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_exam_topic WHERE 1=1");
            StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) FROM yee_exam_topic WHERE 1=1");

            List<Object> parameters = new ArrayList<>();

            // 特别处理：topic 同时在 topic 和 title 字段模糊匹配
            if (topic != null && !topic.trim().isEmpty()) {
                sqlBuilder.append(" AND (topic LIKE ? OR title LIKE ?)");
                countSqlBuilder.append(" AND (topic LIKE ? OR title LIKE ?)");
                String likeValue = "%" + topic.trim() + "%";
                parameters.add(likeValue);
                parameters.add(likeValue);
            }

            // 其他条件：只有非 null 才加入（这是标准做法）
            if (paperId != null) {
                sqlBuilder.append(" AND examId = ?");
                countSqlBuilder.append(" AND examId = ?");
                parameters.add(paperId);
            }

            if (createId != null) {
                sqlBuilder.append(" AND createId = ?");
                countSqlBuilder.append(" AND createId = ?");
                parameters.add(createId);
            }
            if (type != null) {
                sqlBuilder.append(" AND type = ?");
                countSqlBuilder.append(" AND type = ?");
                parameters.add(type);
            }
            if (level != null) {
                sqlBuilder.append(" AND level = ?");
                countSqlBuilder.append(" AND level = ?");
                parameters.add(level);
            }
            if (cateBid != null) {
                sqlBuilder.append(" AND cateBid = ?");
                countSqlBuilder.append(" AND cateBid = ?");
                parameters.add(cateBid);
            }
            if (cateMid != null) {
                sqlBuilder.append(" AND cateMid = ?");
                countSqlBuilder.append(" AND cateMid = ?");
                parameters.add(cateMid);
            }

            // 分页参数：使用 pageSize
            int size = pageSize != null && pageSize > 0 ? pageSize : 10;
            int page = pageNum != null && pageNum >= 1 ? pageNum : 1;
            int offset = (page - 1) * size;

            // 排序 + 分页
            sqlBuilder.append(" ORDER BY id DESC LIMIT ? OFFSET ?");

            // 预编译
            PreparedStatement countSt = connection.prepareStatement(countSqlBuilder.toString());
            PreparedStatement st = connection.prepareStatement(sqlBuilder.toString());

            // 设置 COUNT 查询参数
            for (int i = 0; i < parameters.size(); i++) {
                countSt.setObject(i + 1, parameters.get(i));
            }

            // 设置主查询参数
            for (int i = 0; i < parameters.size(); i++) {
                st.setObject(i + 1, parameters.get(i));
            }
            st.setObject(parameters.size() + 1, size);    // LIMIT
            st.setObject(parameters.size() + 2, offset);  // OFFSET

            // 执行 COUNT 查询
            ResultSet countRs = countSt.executeQuery();
            long total = 0;
            if (countRs.next()) {
                total = countRs.getLong(1);
            }
            countRs.close();
            countSt.close();

            // 执行主查询
            ResultSet rs = st.executeQuery();
            Object result = rsToYeeQuestion(rs);
            rs.close();
            st.close();

            return Result.success(result, total);

        } catch (Exception e) {
            throw new Exception("查询试题列表失败: " + e.getMessage(), e);
        }
    }

    private Object rsToYeeQuestion(ResultSet rs) throws SQLException {
        ArrayList<YeeQuestion> yeeQuestions = new ArrayList<>();

        // 防止 JSON 解析问题，可启用此配置（可选）
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        while (rs.next()) {
            YeeQuestion yeeQuestion = new YeeQuestion();
            yeeQuestion.setId(rs.getInt("id"));

            // option -> List<Map<String, Object>>
//            String optionJson = rs.getString("option");
            String optionJson = null;
            try {
                // 安全读取关键字字段
                Object optionObj = rs.getObject("option");
                if (optionObj != null) {
                    optionJson = optionObj.toString();
                }
            } catch (Exception e) {
                // 不抛错，不中断
                optionJson = null;
            }
            List<Map<String, Object>> option = parseJsonUtil.parseJsonToList(optionJson, "option", yeeQuestion.getId());
            yeeQuestion.setOption(option);
            yeeQuestion.setPid(rs.getInt("pid"));
            yeeQuestion.setType(rs.getInt("type"));
            yeeQuestion.setUpload(rs.getString("upload"));
            yeeQuestion.setAnalysis(rs.getString("analysis"));
            yeeQuestion.setLevel(rs.getInt("level"));
            yeeQuestion.setScore(rs.getInt("score"));
            yeeQuestion.setTitle(rs.getString("title"));
            yeeQuestion.setTopic(rs.getString("topic"));
            // 新增返回 9.4
            yeeQuestion.setScoreMode(rs.getInt("scoreMode"));
            yeeQuestion.setSchoolId(rs.getInt("schoolId"));
            yeeQuestion.setCateMid(rs.getInt("cateMid"));
            yeeQuestion.setCateBid(rs.getInt("cateBid"));

            JSONArray missScoreJson = JSON.parseArray(rs.getString("missScore"));
            if (missScoreJson != null) {
                yeeQuestion.setMissScore(missScoreJson.toList(Integer.class));
            }


            yeeQuestions.add(yeeQuestion);
        }
        return yeeQuestions;
    }

    private List<Map<String, Object>> getWorkRecordDetailByUserAndWorkPre(
            int schoolId,
            Integer examId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建 SQL：查询作业记录 + 作业配置信息
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT 
                  w.title,
                  w.topicNumber,
                  w.score AS totalScore,
                  w.hasCollect,
                  w.startTime,
                  w.endTime,
                  w.frequency,
                  w.createUserId,
                  w.addTime,
                  w.classList,
                  w.remarks

            FROM 
                yee_exam w 
            WHERE 
                w.id = ?
            """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setInt(paramIndex++, examId);    // wr.examId

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i); // 使用 label 支持别名
                    Object value = rs.getObject(i);

                    // 特殊处理：如果字段是 blob/clob 等类型可在此转换，此处暂无
                    // 可根据业务需要对特定字段做格式化

                    // startTime 字段 考试开始时间 转换一下 yyyy-MM-dd HH:mm:ss
                    if ("startTime".equals(columnName) || "endTime".equals(columnName)) {
                        long startTimeSeconds = rs.getLong(i); // 注意：这是秒级时间戳（不是毫秒！）

                        // 判断是否为 0 或无效时间戳
                        if (startTimeSeconds <= 0) {
                            value = 0;
                        } else {
                            value = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(startTimeSeconds), // 注意：用 ofEpochSecond
                                    ZoneId.systemDefault()
                            ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        }
                    }
                    if ("addTime".equals(columnName)) {
                        // addTime 字段 添加时间 转换一下 yyyy-MM-dd HH:mm:ss
                        value = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(rs.getTimestamp(i).getTime()), // 注意：用 ofEpochMilli
                                ZoneId.systemDefault()
                                ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    }

                    row.put(columnName, value);
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("根据用户和考试查询记录失败，参数：schoolId=" + schoolId +
                    ", examId=" + examId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> getWorkTopicDetailsByUserAndWorkPre(
            int schoolId,
            Integer examId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建 SQL：查询作业题目、题目分值等
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT 
                wt.title,
                wt.type,
                wt.level,
                wt.score,
                wt.topic,
                wt.option
            FROM 
                yee_exam_topic wt
                
            WHERE 
                wt.examId = ?
            """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setInt(paramIndex++, examId);    // wt.examId

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            ObjectMapper objectMapper = new ObjectMapper();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i); // 使用 label 支持别名
                    Object value = rs.getObject(i);

                    // 特殊处理：将 option 字段从 JSON 字符串转为 List<Map>
                    if ("option".equalsIgnoreCase(columnName) && value != null) {
                        try {
                            value = objectMapper.readValue(value.toString(), List.class);
                        } catch (Exception e) {
                            // 解析失败则保留原始字符串
                            e.printStackTrace();
                        }
                    }

                    // 新增：处理 answer 字段（如果是多选题）
                    if ("answer".equalsIgnoreCase(columnName) && value instanceof String) {
                        String answerStr = (String) value;
                        try {
                            // 尝试解析成 List<String>
                            if (answerStr.trim().startsWith("[")) {
                                value = objectMapper.readValue(answerStr, List.class);
                            }
                            // 如果不是数组格式，保持原样（如单选 "A"）
                        } catch (Exception e) {
                            // 解析失败，保留原始字符串
                            System.err.println("解析 answer 失败: " + answerStr);
                        }
                    }


                    row.put(columnName, value);
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("根据用户和考试查询题目作答详情失败，参数：schoolId=" + schoolId +
                    ", examId=" + examId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private Map<String, Object> getStudentFinalScoreByUserIdAndWorkId(int schoolId, Integer userId, Integer examId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        Map<String, Object> result = null; // 单条记录，使用 Map

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool); // 查询使用从库

            // 2. 构建 SQL：查询学生信息 + 考试最终得分
            String sql = """
            SELECT 
                w.title,
                s.name,
                s.number,
                ws.finalScore,
                w.score AS totalScore
            FROM 
                yee_student s
                LEFT JOIN yee_exam_score ws ON ws.userId = s.id
                LEFT JOIN yee_exam w on w.id = ws.examId
            WHERE 
                s.id = ? 
                AND ws.examId = ?
            """;

            st = conn.prepareStatement(sql);

            // 3. 设置参数
            st.setLong(1, userId);   // s.id = userId
            st.setInt(2, examId);    // ws.examId = examId

            // 4. 执行查询
            rs = st.executeQuery();

            // 5. 封装结果（最多一条记录）
            if (rs.next()) {
                result = new HashMap<>();
                result.put("title", rs.getObject("title"));
                result.put("name", rs.getObject("name"));
                result.put("number", rs.getObject("number"));
                result.put("finalScore", rs.getObject("finalScore"));
                result.put("totalScore", rs.getObject("totalScore"));
            }

            return result; // 若无记录，返回 null

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询学生最终成绩失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", examId=" + examId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private void updateWorkScore(int schoolId, Integer userId, Integer examId, Integer courseId, BigDecimal manualScore) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 2. 第一步：查询 yee_exam_score 的 id
            String queryIdSql = """
            SELECT id 
            FROM yee_exam_score 
            WHERE userId = ? AND examId = ? 
            LIMIT 1
            """;

            st = conn.prepareStatement(queryIdSql);
            st.setLong(1, userId);
            st.setInt(2, examId);

            rs = st.executeQuery();

            Long recordId = null;
            if (rs.next()) {
                recordId = rs.getLong("id");
            }

            closeResultSetAndStatement(rs, st);
            st = null;
            rs = null;

            if (recordId == null) {
//                throw new Exception("未找到对应的考试记录：userId=" + userId + ", examId=" + examId);
                // 记录不存在时，创建新记录而不是抛异常
                String insertSql = """
                INSERT INTO yee_exam_score (userId, examId, courseId, finalScore, state, scored)
                VALUES (?, ?, ?, ?, 3, 1)
                """;

                st = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                st.setLong(1, userId);
                st.setInt(2, examId);
                st.setInt(3, courseId);
                st.setObject(4, manualScore, Types.DECIMAL);

                int rowsInserted = st.executeUpdate();
                if (rowsInserted == 0) {
                    throw new Exception("创建考试分数记录失败：userId=" + userId + ", examId=" + examId);
                }

                // 获取生成的主键
                rs = st.getGeneratedKeys();
                if (rs.next()) {
                    recordId = rs.getLong(1);
                }

                closeResultSetAndStatement(rs, st);
                st = null;
                rs = null;
            }

            // 3. 第二步：根据 id 更新 finalScore 字段
            String updateScoreSql = """
            UPDATE yee_exam_score 
            SET finalScore = ?,
                state = 3,
                scored = 1
            WHERE id = ?
            """;

            st = conn.prepareStatement(updateScoreSql);
            st.setObject(1, manualScore, Types.DECIMAL); // 使用 setObject 避免 null 问题
            st.setLong(2, recordId);

            int rowsAffected = st.executeUpdate();

            if (rowsAffected == 0) {
                throw new Exception("更新考试分数-分数失败，未影响任何记录：recordId=" + recordId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("更新考试分数-分数失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", examId=" + examId +
                    ", manualScore=" + manualScore, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

private void updateWorkRecordScore(int schoolId, Integer userId, Integer examId, BigDecimal recheckScore, Integer teacherId, List<Map<String, Object>> workResult) throws Exception {
    Connection conn = null;
    PreparedStatement querySt = null;
    PreparedStatement updateSt = null;
    ResultSet rs = null;

    try {
        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new Exception("学校不存在或未审核");
        }

        // 获取租户连接
        conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
        conn.setAutoCommit(false); // 事务

        // 2. 查询考试记录 ID + 原有客观分 obScore
        String queryIdSql = """
            SELECT id, obScore
            FROM yee_exam_record
            WHERE userId = ? AND examId = ?
            LIMIT 1
        """;

        querySt = conn.prepareStatement(queryIdSql);
        querySt.setLong(1, userId);
        querySt.setInt(2, examId);

        rs = querySt.executeQuery();

        Long recordId = null;
        BigDecimal oldObScore = BigDecimal.ZERO;
        if (rs.next()) {
            recordId = rs.getLong("id");
            oldObScore = rs.getBigDecimal("obScore"); // 取出原有客观分
        }

        if (recordId == null) {
            throw new Exception("未找到对应的考试记录：userId=" + userId + ", examId=" + examId);
        }

        // ===================== 核心复核逻辑 =====================
        // 主观题得分 = 复核总分 - 客观题得分
        BigDecimal finalSubScore = recheckScore.subtract(oldObScore);
        if (finalSubScore.compareTo(BigDecimal.ZERO) < 0) {
            finalSubScore = BigDecimal.ZERO;
        }

        // ===================== 更新考试记录 =====================
        String updateScoreSql = """
            UPDATE yee_exam_record
            SET score = ?,
                obScore = ?,
                teacherId = ?,
                markTime = ?,
                state = 3,
                subScore = ?
            WHERE id = ?
        """;

        updateSt = conn.prepareStatement(updateScoreSql);
        updateSt.setBigDecimal(1, recheckScore);       // 最终总分（老师复核）
        updateSt.setBigDecimal(2, oldObScore);         // 客观分不变
        updateSt.setInt(3, teacherId);                 // 批阅老师
        updateSt.setLong(4, System.currentTimeMillis() / 1000); // 批阅时间
        updateSt.setBigDecimal(5, finalSubScore);      // 计算后的主观分
        updateSt.setLong(6, recordId);                 // 记录ID

        int rowsAffected = updateSt.executeUpdate();

        if (rowsAffected == 0) {
            throw new Exception("更新考试记录分数失败，未影响任何记录：recordId=" + recordId);
        }

        // 提交事务
        conn.commit();

    } catch (Exception e) {
        e.printStackTrace();
        if (conn != null) {
            try { conn.rollback(); } catch (Exception ignored) {}
        }
        throw new Exception("更新考试记录分数失败", e);
    } finally {
        try { if (rs != null) rs.close(); } catch (Exception ignored) {}
        try { if (querySt != null) querySt.close(); } catch (Exception ignored) {}
        try { if (updateSt != null) updateSt.close(); } catch (Exception ignored) {}
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
    }
}
    private List<Map<String, Object>> getWorkRecordDetailByUserAndWork(
            int schoolId,
            Integer userId,
            Integer examId, Integer courseId) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 构建 SQL：查询考试记录 + 学生信息 + 考试配置信息
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT 
                  s.name,
                  s.number,
                  er.id AS wrId,
                  er.startTime,
                  er.score AS score,
                  er.state,
                  w.title,
                  w.topicNumber,
                  w.limitedTime
            FROM 
                yee_exam_record er
                LEFT JOIN yee_student s ON er.userId = s.id
                LEFT JOIN yee_exam w ON w.id = er.examId
            WHERE 
                er.userId = ?
                AND er.examId = ?
                AND er.courseId = ?
            """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setLong(paramIndex++, userId);   // er.userId
            st.setInt(paramIndex++, examId);    // er.examId
            st.setInt(paramIndex++, courseId);  // er.courseId

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i); // 使用 label 支持别名
                    Object value = rs.getObject(i);

                    // 特殊处理：
                    // state 字段 3代表已批,2代表待批,1代表未提交
                    if ("state".equals(columnName)) {
                        value = rs.getInt(i);
                    }
                    // startTime 字段 考试开始时间 转换一下 yyyy-MM-dd HH:mm:ss
                    if ("startTime".equals(columnName)) {
                        long startTimeSeconds = rs.getLong(i); // 注意：这是秒级时间戳（不是毫秒！）

                        // 判断是否为 0 或无效时间戳
                        if (startTimeSeconds <= 0) {
                            value = 0;
                        } else {
                            value = LocalDateTime.ofInstant(
                                    Instant.ofEpochSecond(startTimeSeconds), // 注意：用 ofEpochSecond
                                    ZoneId.systemDefault()
                            ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                        }
                    }

                    row.put(columnName, value);
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("根据用户和考试查询记录失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", examId=" + examId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> getWorkRecordDetailByExamWithUserIds(
            int schoolId,
            Integer examId, Integer courseId, List<Integer> userIdList) throws Exception {

        if (userIdList == null || userIdList.isEmpty()) {
            return new ArrayList<>();
        }

        List<List<Integer>> partitions = ListUtils.partition(userIdList, 50);
        List<Map<String, Object>> result = new ArrayList<>();

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            for (List<Integer> part : partitions) {
                String placeholders = String.join(",", Collections.nCopies(part.size(), "?"));

                String sql = """
                SELECT 
                      s.name,
                      s.number,
                      er.id AS wrId,
                      er.userId,
                      er.startTime,
                      er.score,
                      er.state,
                      w.title,
                      w.topicNumber,
                      w.score AS totalScore,
                      w.limitedTime
                FROM 
                    yee_exam_record er
                    LEFT JOIN yee_student s ON er.userId = s.id
                    LEFT JOIN yee_exam w ON w.id = er.examId
                WHERE 
                    er.examId = ?
                    AND er.courseId = ?
                    AND er.userId IN (""" + placeholders + ")";


                st = conn.prepareStatement(sql);
                int idx = 1;
                st.setInt(idx++, examId);
                st.setInt(idx++, courseId);
                for (Integer uid : part) {
                    st.setInt(idx++, uid);
                }

                rs = st.executeQuery();
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnLabel(i);
                        Object value = rs.getObject(i);

                        if ("state".equals(columnName)) {
                            value = rs.getInt(i);
                        }
                        if ("startTime".equals(columnName)) {
                            long startTimeSeconds = rs.getLong(i);
                            if (startTimeSeconds <= 0) {
                                value = 0;
                            } else {
                                value = LocalDateTime.ofInstant(
                                        Instant.ofEpochSecond(startTimeSeconds),
                                        ZoneId.systemDefault()
                                ).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                            }
                        }
                        row.put(columnName, value);
                    }
                    result.add(row);
                }
                rs.close();
                st.close();
            }
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }


    private Map<String, Object> getWorkInfoByExamId(int schoolId, Integer examId, Integer courseId) {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        // 获取考试表信息
        Map<String, Object> workInfo = new HashMap<>();
        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            String sql = "SELECT w.*, m.name FROM yee_exam w LEFT JOIN yee_manage m on w.createUserId = m.id WHERE w.id = ? and w.courseId = ?";
            st = conn.prepareStatement(sql);
            st.setInt(1, examId);
            st.setInt(2, courseId);
            rs = st.executeQuery();

            List<Object> resultList = rsToWorkInfo(rs);

            // 如果查询到考试信息，则获取相关的班级信息
            if (resultList != null && !resultList.isEmpty()) {
                Map<String, Object> examData = (Map<String, Object>) resultList.get(0);

                // 获取 classId 列表
                Object classIdsObj = examData.get("classId");
                List<Integer> classIds = new ArrayList<>();
                boolean hasSpecificClassIds = false;

                if (classIdsObj != null) {
                    if (classIdsObj instanceof List) {
                        for (Object obj : (List<?>) classIdsObj) {
                            classIds.add(Integer.valueOf(obj.toString()));
                        }
                        hasSpecificClassIds = !classIds.isEmpty();
                    }
                }

                List<String> classNameList = new ArrayList<>();

                if (hasSpecificClassIds) {
                    // 查询指定的班级信息，并保持与 classIds 相同的顺序
                    Map<Integer, String> classIdNameMap = new HashMap<>();
                    StringBuilder classSql = new StringBuilder("SELECT id, name FROM yee_course_class WHERE id IN (");
                    for (int i = 0; i < classIds.size(); i++) {
                        classSql.append("?");
                        if (i < classIds.size() - 1) {
                            classSql.append(",");
                        }
                    }
                    classSql.append(")");

                    PreparedStatement classSt = conn.prepareStatement(classSql.toString());
                    for (int i = 0; i < classIds.size(); i++) {
                        classSt.setInt(i + 1, classIds.get(i));
                    }

                    ResultSet classRs = classSt.executeQuery();

                    // 先将查询结果存入 Map
                    while (classRs.next()) {
                        Integer id = classRs.getInt("id");
                        String className = classRs.getString("name");
                        classIdNameMap.put(id, className);
                    }

                    // 按照原始 classIds 的顺序获取班级名称
                    for (Integer classId : classIds) {
                        String className = classIdNameMap.get(classId);
                        if (className != null) {
                            classNameList.add(className);
                        }
                    }

                    classRs.close();
                    classSt.close();
                } else {
                    // 如果 classId 为空数组，则显示 "全部班级"
                    classNameList.add("全部班级");
                }

                examData.put("classListDetail", classNameList);
            }

            if (resultList != null && !resultList.isEmpty()) {
                workInfo = (Map<String, Object>) resultList.get(0);
            } else {
                // 查询不到数据，返回空 map 或抛异常
                workInfo = null; // 或 new HashMap<>(); 根据业务需求
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
        return workInfo;
    }

    private Map<String, Object> getWorkInfoById(int schoolId, Integer nodeId, Integer courseId) {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        // 获取考试表信息
        Map<String, Object> workInfo = new HashMap<>();
        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            String sql = "SELECT * FROM yee_exam WHERE nodeId = ? and courseId = ?";
            st = conn.prepareStatement(sql);
            st.setInt(1, nodeId);
            st.setInt(2, courseId);
            rs = st.executeQuery();

            List<Object> resultList = rsToWorkInfo(rs);

            if (resultList != null && !resultList.isEmpty()) {
                workInfo = (Map<String, Object>) resultList.get(0);
            } else {
                // 查询不到数据，返回空 map 或抛异常
                workInfo = null; // 或 new HashMap<>(); 根据业务需求
            }


        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
        return workInfo;
    }

    private List<Object> rsToWorkInfo(ResultSet rs) {

        List<Object> result = new ArrayList<>();
        try {
            while (rs.next()) {
                Map<String, Object> workInfo = new HashMap<>();
                workInfo.put("id", rs.getInt("id"));
                workInfo.put("title", rs.getString("title"));
                workInfo.put("topicNumber", rs.getInt("topicNumber"));
                workInfo.put("score", rs.getInt("score"));
                workInfo.put("hasCollect", rs.getInt("hasCollect") == 0 ? "待收卷" : "已有收卷");
                // 数据库是时间戳 需要转换成时间
                // 创建自定义格式器
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

                long startTimeSeconds = rs.getLong("startTime");
                long endTimeSeconds = rs.getLong("endTime");

                workInfo.put("startTime", LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(startTimeSeconds),
                        ZoneId.systemDefault()
                ).format(formatter)); // 调用 .format() 转为字符串

                workInfo.put("endTime", LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(endTimeSeconds),
                        ZoneId.systemDefault()
                ).format(formatter));

                workInfo.put("frequency", rs.getInt("frequency"));
                workInfo.put("createUserId", rs.getInt("createUserId"));
                workInfo.put("name", rs.getString("name"));
                workInfo.put("limitedTime", rs.getInt("limitedTime"));
                workInfo.put("addDate", rs.getDate("addDate").toLocalDate());
                workInfo.put("classId", JSON.parseArray(rs.getString("classList")));
                workInfo.put("remarks", rs.getString("remarks"));

                result.add(workInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private List<Map<String, Object>> getWorkRecordDetails(int schoolId, Integer courseId, Integer classId) throws Exception{
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // ===================== ✅ 权限 =====================
            Long teacherId = AuthDataPermissionUtil.getCurrentUserId();
            DataAuth auth = AuthDataPermissionUtil.getCurrentDataAuth();
            boolean hasCondition = false;

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT * FROM yee_exam_record");

            // 拼接条件
            if (courseId != null) {
                sqlBuilder.append(hasCondition ? " AND courseId = ?" : " WHERE courseId = ?");
                hasCondition = true;
            }
            if (classId != null) {
                sqlBuilder.append(hasCondition ? " AND classId = ?" : " WHERE classId = ?");
                hasCondition = true;
            }
            if (schoolId != 0) {
                sqlBuilder.append(hasCondition ? " AND schoolId = ?" : " WHERE schoolId = ?");
                hasCondition = true;
            }

            // 权限过滤
            if (!DataAuth.ALL.equals(auth)) {
                sqlBuilder.append(hasCondition ? " AND (" : " WHERE (");
                sqlBuilder.append(" EXISTS (");
                sqlBuilder.append("   SELECT 1 FROM yee_course_class ycc ");
                sqlBuilder.append("   WHERE ycc.id = yee_exam_record.classId AND ycc.teacherId = ?");
                sqlBuilder.append(" ) OR EXISTS (");
                sqlBuilder.append("   SELECT 1 FROM yee_course yc ");
                sqlBuilder.append("   WHERE yc.id = yee_exam_record.courseId AND yc.createId = ?");
                sqlBuilder.append(" ) )");
            }
            // ======================================================

            st = conn.prepareStatement(sqlBuilder.toString());
            int parameterIndex = 1;

            if (courseId != null) st.setInt(parameterIndex++, courseId);
            if (classId != null) st.setInt(parameterIndex++, classId);
            if (schoolId != 0) st.setInt(parameterIndex++, schoolId);

            if (!DataAuth.ALL.equals(auth)) {
                st.setLong(parameterIndex++, teacherId);
                st.setLong(parameterIndex++, teacherId);
            }

            rs = st.executeQuery();
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("examId", rs.getInt("examId"));
                map.put("userId", rs.getInt("userId"));
                map.put("state", rs.getInt("state"));
                map.put("classId", rs.getObject("classId"));
                map.put("schoolId", rs.getInt("schoolId"));
                map.put("courseId", rs.getInt("courseId"));
                result.add(map);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
        return result;
    }

    private List<Map<String, Object>> getWorkDetailsByCourseAndSchool(
            Integer schoolId,
            Integer courseId,
            Integer classId,
            String title) throws Exception {

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // ===================== ✅ 权限获取 =====================
            Long teacherId = AuthDataPermissionUtil.getCurrentUserId();
            DataAuth auth = AuthDataPermissionUtil.getCurrentDataAuth();
            // ======================================================

            // 3. 构建动态 SQL（支持 classId 和 title 可选条件）
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
                    SELECT 
                        w.id,
                        w.title,
                        w.topicNumber,
                        w.score,
                        w.addTime,
                        w.startTime,
                        w.endTime,
                        w.nodeId,
                        w.courseId,
                        w.allow,
                        w.sequence,
                        w.classList,
                        n.chapterId,
                        n.name AS nodeName,
                        c.name AS chapterName,
                        cs.classId,
                        cs.studentId,
                        cs.videoLearned,
                        cs.videoCount,
                        cs.lastNodeId,
                        cs.workLearned,
                        cs.workCount,
                        cs.examLearned,
                        cs.examCount,
                        cs.discussJoin,
                        cs.discussCount,
                        cs.studyTime,
                        cs.calculate
                    FROM 
                        yee_exam w
                        LEFT JOIN yee_node n ON w.nodeId = n.id
                        LEFT JOIN yee_chapter c ON n.chapterId = c.id
                        LEFT JOIN yee_course_student cs 
                            ON cs.courseId = w.courseId 
                           AND (
                                JSON_LENGTH(w.classList) = 0 
                                OR JSON_CONTAINS(w.classList, CAST(cs.classId AS JSON))
                              )
                    WHERE 
                        w.courseId = ?
                        AND w.allow = 1
                        AND w.schoolId = ?
                    """);

            // ===================== ✅ 考试权限核心 =====================
            if (!DataAuth.ALL.equals(auth)) {
                sqlBuilder.append(" AND ( ");
                // 1. 考试对所有班级开放
                sqlBuilder.append(" JSON_LENGTH(w.classList) = 0 ");
                sqlBuilder.append(" OR ");
                // 2. 当前老师是班级责任教师
                sqlBuilder.append(" EXISTS ( ");
                sqlBuilder.append("   SELECT 1 FROM yee_course_class ycc ");
                sqlBuilder.append("   WHERE ycc.id = cs.classId ");
                sqlBuilder.append("     AND ycc.teacherId = ? ");
                sqlBuilder.append(" ) ");
                sqlBuilder.append(" OR ");
                // 3. 当前老师是课程创建者
                sqlBuilder.append(" EXISTS ( ");
                sqlBuilder.append("   SELECT 1 FROM yee_course yc ");
                sqlBuilder.append("   WHERE yc.id = w.courseId ");
                sqlBuilder.append("     AND yc.createId = ? ");
                sqlBuilder.append(" ) ");
                sqlBuilder.append(" ) ");
            }
            // ==============================================================

            // 条件：classId 可选
            if (classId != null && classId > 0) {
                sqlBuilder.append(" AND cs.classId = ? ");
            }

            // 条件：title 模糊查询
            if (title != null && !title.trim().isEmpty()) {
                sqlBuilder.append(" AND w.title LIKE ? ");
            }

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setLong(paramIndex++, courseId);
            st.setInt(paramIndex++, schoolId);

            // 权限参数
            if (!DataAuth.ALL.equals(auth)) {
                st.setLong(paramIndex++, teacherId);
                st.setLong(paramIndex++, teacherId);
            }

            // 动态添加可选参数
            if (classId != null && classId > 0) {
                st.setInt(paramIndex++, classId);
            }

            if (title != null && !title.trim().isEmpty()) {
                st.setString(paramIndex++, "%" + title.trim() + "%");
            }

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 封装结果
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    row.put(columnName, rs.getObject(i));
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询考试详情失败，参数：schoolId=" + schoolId +
                    ", courseId=" + courseId +
                    ", classId=" + classId +
                    ", title=" + title, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    /**
     * 将原始数据转换为章节分组、考试去重的统计结构
     */
    public static List<WorkReportVO> buildWorkReport(List<Map<String, Object>> workList) {
        return workList.stream()
                .collect(Collectors.groupingBy(
                        row -> (String) row.get("chapterName"),
                        LinkedHashMap::new,
                        Collectors.toList()
                ))
                .entrySet().stream()
                .map(entry -> {
                    WorkReportVO chapter = new WorkReportVO();
                    chapter.setChapterName(entry.getKey());
                    chapter.setWorks(new ArrayList<>());

                    Map<Integer, WorkReportVO.WorkItem> workMap = new LinkedHashMap<>();

                    for (Map<String, Object> row : entry.getValue()) {
                        Integer workId = ((Number) row.get("id")).intValue();

                        WorkReportVO.WorkItem item = workMap.computeIfAbsent(workId, k -> {
                            WorkReportVO.WorkItem newItem = new WorkReportVO.WorkItem();
                            newItem.setId(workId);
                            newItem.setTitle((String) row.get("title"));
//                            newItem.setType((Integer) row.get("type")); // 2:"考试", 1:"练习"
                            newItem.setEndTime(WorkReportVO.formatTimestamp((Integer)row.get("endTime")));
                            return newItem;
                        });

                        // 统计总人数
                        item.setTotalNum(item.getTotalNum() + 1);

                    }

                    chapter.setWorks(new ArrayList<>(workMap.values()));
                    return chapter;
                })
                .collect(Collectors.toList());
    }


    /**
     * 获取课程考试统计信息
     *
     * @param schoolId 学校ID
     * @param courseId 课程ID
     * @param classId  班级ID（可选，null 或 0 表示查所有班级）
     * @return 统计数据 Map
     * @throws Exception
     */
    public Map<String, Integer> getTotalWorkStats(int schoolId, int courseId, Integer classId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        Map<String, Integer> stats = new HashMap<>();

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 2. 获取数据库连接
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 动态构建 SQL（支持 classId 可选）
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT
                pre.wTotal,
                pre.uTotal,
        
                -- 实际提交完成次数（state >= 2）
                COUNT(er.id) AS complete,
        
                -- PC / Mobile 提交次数
                SUM(CASE WHEN er.platform = 'pc' AND er.state >= 2 THEN 1 ELSE 0 END) AS pcTotal,
                SUM(CASE WHEN er.platform = 'mobile' AND er.state >= 2 THEN 1 ELSE 0 END) AS mbTotal,
        
                -- 待批改统计（state = 2）
                COUNT(DISTINCT CASE WHEN er.state = 2 THEN er.userId END) AS uOnce2,
                COUNT(CASE WHEN er.state = 2 THEN 1 END) AS complete2,
                SUM(CASE WHEN er.platform = 'pc' AND er.state = 2 THEN 1 ELSE 0 END) AS pcTotal2,
                SUM(CASE WHEN er.platform = 'mobile' AND er.state = 2 THEN 1 ELSE 0 END) AS mbTotal2,
        
                -- 真正完成所有考试的学生数
                COUNT(DISTINCT CASE
                    WHEN cs.examLearned > 0 AND cs.examLearned = cs.examCount
                    THEN cs.studentId
                END) AS uFinished
        
            FROM
                -- 预先计算 wTotal 和 uTotal
                (SELECT
                    COUNT(cs_inner.examCount) AS wTotal,
                    COUNT(DISTINCT cs_inner.studentId) AS uTotal
                 FROM yee_course_student cs_inner
                 WHERE cs_inner.courseId = ?
                ) pre
        
                CROSS JOIN yee_exam e
                LEFT JOIN yee_course_student cs ON cs.courseId = e.courseId
                LEFT JOIN yee_exam_record er ON e.id = er.examId AND cs.studentId = er.userId
        
            WHERE
                e.courseId = ?
            """);

            // 动态添加 classId 过滤条件（加在 WHERE 后）
            boolean hasClassFilter = (classId != null && classId > 0);
            if (hasClassFilter) {
                sqlBuilder.append(" AND cs.classId = ? ");
            }

            // 只保留这一个 GROUP BY
            sqlBuilder.append(" GROUP BY pre.wTotal, pre.uTotal ");

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setInt(paramIndex++, courseId);  // 第一个 ?：派生表中的 courseId
            st.setInt(paramIndex++, courseId);  // 第二个 ?：主查询中的 e.courseId = ?

            if (hasClassFilter) {
                st.setInt(paramIndex++, classId); // 第三个 ?：classId
            }

            // 6. 执行查询
            rs = st.executeQuery();

            // 7. 解析结果
            if (rs.next()) {
                int uTotal = rs.getInt("uTotal");
                int wTotal = rs.getInt("wTotal");

                int complete = rs.getInt("complete");
                int uFinished = rs.getInt("uFinished");

                // === 基础统计 ===
                stats.put("uTotal", uTotal);
                stats.put("wTotal", wTotal);

                // === 未提交 ===
                stats.put("uNotComplete", uTotal - uFinished);
                stats.put("notComplete", wTotal - complete);

                // === 已提交 ===
                stats.put("uComplete", uFinished);
                stats.put("complete", complete);

                stats.put("pcTotal", rs.getInt("pcTotal"));
                stats.put("mbTotal", rs.getInt("mbTotal"));

                // === 待批改 ===
                stats.put("uOnce2", rs.getInt("uOnce2"));
                stats.put("complete2", rs.getInt("complete2"));
                stats.put("pcTotal2", rs.getInt("pcTotal2"));
                stats.put("mbTotal2", rs.getInt("mbTotal2"));

            } else {
                // 无数据时返回默认值
                stats.put("wTotal", 0);
                stats.put("uTotal", 0);
                stats.put("uNotComplete", 0);
                stats.put("notComplete", 0);
                stats.put("uComplete", 0);
                stats.put("complete", 0);
                stats.put("pcTotal", 0);
                stats.put("mbTotal", 0);
                stats.put("uOnce2", 0);
                stats.put("complete2", 0);
                stats.put("pcTotal2", 0);
                stats.put("mbTotal2", 0);
                stats.put("uFinished", 0);
            }

            return stats;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询考试统计失败", e);
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

// ---------------- 工具方法 ----------------

    /**
     * 安全关闭 ResultSet 和 PreparedStatement
     */
    private void closeResultSetAndStatement(ResultSet rs, PreparedStatement stmt) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace(); // 建议使用日志框架
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 安全关闭 Connection
     */
    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void closeStatement(PreparedStatement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据题型ID获取题型名称
     * @param type 题型ID
     * @return 题型名称
     */
    private String getQuestionTypeName(Integer type) {
        if (type != null) {
            if (type == 1) {
                return "单选题";
            } else if (type == 2) {
                return "多选题";
            } else if (type == 3) {
                return "判断题";
            } else if (type == 4) {
                return "简答题";
            } else if (type == 5) {
                return "填空题";
            }
        }
        return "未知题型";
    }

    /**
     * 为单个学生生成考试答卷PDF
     * @param studentRecord 学生答题记录
     * @return PDF字节流
     */
    public ByteArrayOutputStream generateStudentExamPdf(Map<String, Object> studentRecord) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // 设置中文字体
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 18, Font.BOLD);
            Font headerFont = new Font(bfChinese, 12, Font.BOLD);
            Font normalFont = new Font(bfChinese, 10, Font.NORMAL);
            Font smallFont = new Font(bfChinese, 9, Font.NORMAL);

            // Colored fonts for numbers and scores
            Font numberFont = new Font(bfChinese, 10, Font.NORMAL, BaseColor.BLUE);
            Font scoreFont = new Font(bfChinese, 10, Font.BOLD, BaseColor.RED);
            Font positiveScoreFont = new Font(bfChinese, 10, Font.BOLD, BaseColor.GREEN);
            Font negativeScoreFont = new Font(bfChinese, 10, Font.BOLD, BaseColor.RED);
            Font totalScoreFont = new Font(bfChinese, 10, Font.BOLD, BaseColor.ORANGE);

            // 标题
            String examTitle = (String) studentRecord.get("title");
            Paragraph title = new Paragraph(examTitle, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 学生信息表格
            PdfPTable warnTable = new PdfPTable(6);
            warnTable.setWidthPercentage(100);
            warnTable.setSpacingAfter(15);
            List<Map<String, Object>> workTopics = (List<Map<String, Object>>) studentRecord.get("workTopics");
            if (workTopics == null) workTopics = new ArrayList<>();

            addTableCell(warnTable, "姓名：" + studentRecord.get("name"), normalFont);
            addTableCell(warnTable, "学号：" + studentRecord.get("number"), normalFont);
            addTableCell(warnTable, "时限：" + studentRecord.get("limitedTime"), normalFont);

            addTableCell(warnTable, "题目数量：" + workTopics.size(), normalFont);
            addTableCell(warnTable, "总分：" + studentRecord.get("totalScore"), normalFont);
            addTableCell(warnTable, "考生得分：" + studentRecord.get("score"), scoreFont);

            document.add(warnTable);

            // 题目列表
            if (workTopics != null && !workTopics.isEmpty()) {
                int questionNumber = 1;

                for (Map<String, Object> topic : workTopics) {
                    // 题目编号和类型
                    Paragraph questionHead = new Paragraph();
                    // Add question number in blue
                    questionHead.add(new Chunk(String.valueOf(questionNumber) + ". ", numberFont));
                    questionNumber++; // Increment after using the current number
                    // Add question type
                    questionHead.add(new Chunk("[" + getQuestionTypeName(safeToInteger(topic.get("type"))) + "] ", normalFont));
                    // Add score in orange
                    questionHead.add(new Chunk("(分值: " + topic.get("topicScore") + ")", totalScoreFont));
                    questionHead.setSpacingBefore(10);
                    document.add(questionHead);

                    // 题目内容
                    String topicText = removeHtmlTags((String) topic.get("topic"));
                    Paragraph questionText = new Paragraph(topicText, normalFont);
                    questionText.setSpacingBefore(5);
                    document.add(questionText);

                    // 选项
                    List<Map<String, Object>> options = (List<Map<String, Object>>) topic.get("option");
                    if (options != null && !options.isEmpty()) {
                        for (Map<String, Object> option : options) {
                            Paragraph optionPara = new Paragraph();
                            // Add option index in blue
                            optionPara.add(new Chunk("  " + safeToString(option.get("idx")) + ". ", numberFont));
                            // Add option answer in normal font
                            optionPara.add(new Chunk(String.valueOf(option.get("answer")), smallFont));
                            optionPara.setSpacingBefore(3);
                            document.add(optionPara);
                        }

                        // 正确答案
                        List<String> correctAnswers = new ArrayList<>();
                        for (Map<String, Object> option : options) {
                            Object scale = option.get("scale");
                            if (scale != null) {
                                int scaleValue = 0;
                                if (scale instanceof Number) {
                                    scaleValue = ((Number) scale).intValue();
                                } else if (scale instanceof String) {
                                    try {
                                        scaleValue = Integer.parseInt((String) scale);
                                    } catch (NumberFormatException e) {
                                        scaleValue = 0;
                                    }
                                }
                                if (scaleValue > 0) {
                                    correctAnswers.add(safeToString(option.get("idx")));
                                }
                            }
                        }

                        String correctAnswerStr = String.join(", ", correctAnswers);
                        Paragraph correctAns = new Paragraph();
                        correctAns.add(new Chunk("正确答案: ", normalFont));
                        // Add each correct answer option in blue
                        for (int i = 0; i < correctAnswers.size(); i++) {
                            if (i > 0) correctAns.add(new Chunk(", ", normalFont));
                            correctAns.add(new Chunk(correctAnswers.get(i), numberFont));
                        }
                        correctAns.setSpacingBefore(5);
                        document.add(correctAns);
                    }

                    // 学生答案
                    Object studentAnswer = topic.get("answer");
                    Integer questionType = safeToInteger(topic.get("type"));

                    // 如果是简答题(type=4)，需要处理images和files字段
                    if (questionType != null && questionType == 4) {
                        // 处理学生答案文本
                        String studentAnswerStr = studentAnswer != null ? studentAnswer.toString() : "未作答";
                        Paragraph stuAns = new Paragraph("学生答案: " + studentAnswerStr, smallFont);
                        stuAns.setSpacingBefore(3);
                        document.add(stuAns);

                        // 处理图片列表
                        Object imagesObj = topic.get("images");
                        if (imagesObj != null) {
                            try {
                                List<?> imageList = (List<?>) imagesObj;
                                if (imageList != null && !imageList.isEmpty()) {
                                    Paragraph imagesParagraph = new Paragraph("上传图片: ", smallFont);
                                    imagesParagraph.setSpacingBefore(3);
                                    document.add(imagesParagraph);

                                    for (int i = 0; i < imageList.size(); i++) {
                                        Object imageItem = imageList.get(i);
                                        if (imageItem != null) {
                                            String imagePath = "";
                                            String imageName = "";
                                            // 检查imageItem是否为Map对象（包含url和name）
                                            if (imageItem instanceof Map) {
                                                Map<String, Object> imageMap = (Map<String, Object>) imageItem;
                                                Object urlObj = imageMap.get("url");
                                                Object nameObj = imageMap.get("name");
                                                if (urlObj != null) {
                                                    imagePath = urlObj.toString();
                                                } else if (nameObj != null) {
                                                    imagePath = nameObj.toString();
                                                }
                                                if (nameObj != null) {
                                                    imageName = nameObj.toString();
                                                } else if (urlObj != null) {
                                                    imageName = urlObj.toString();
                                                }
                                            } else {
                                                // 如果是简单字符串，直接使用
                                                imagePath = imageItem.toString();
                                                imageName = imageItem.toString();
                                            }

                                            if (!imagePath.trim().isEmpty()) {
                                                // 检查是否为图片文件
                                                if (isImageFile(imagePath)) {
                                                    try {
                                                        // 尝试从URL加载图片并嵌入PDF
                                                        Image img = Image.getInstance(imagePath);
                                                        // 调整图片大小以适应PDF页面
                                                        if (img.getWidth() > 300) {
                                                            img.scaleToFit(300, 300);
                                                        }
                                                        img.setAlignment(Element.ALIGN_LEFT);
                                                        document.add(img);

                                                        // 添加图片名称作为标签
                                                        Paragraph imageLabel = new Paragraph("  图片" + (i+1) + ": " + imageName, smallFont);
                                                        imageLabel.setSpacingBefore(5);
                                                        document.add(imageLabel);
                                                    } catch (Exception imgEx) {
                                                        // 如果无法加载图片，显示路径文本
                                                        Paragraph imageItemPara = new Paragraph("  图片" + (i+1) + ": " + imagePath, smallFont);
                                                        imageItemPara.setSpacingBefore(2);
                                                        document.add(imageItemPara);
                                                    }
                                                } else {
                                                    // 非图片文件，只显示文本链接
                                                    Paragraph imageItemPara = new Paragraph("  图片" + (i+1) + ": " + imagePath, smallFont);
                                                    imageItemPara.setSpacingBefore(2);
                                                    document.add(imageItemPara);
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (ClassCastException e) {
                                // 如果转换失败，按普通字符串处理
                                String imagesStr = imagesObj.toString();
                                if (imagesStr != null && !imagesStr.trim().isEmpty()) {
                                    Paragraph imagesParagraph = new Paragraph("上传图片: " + imagesStr, smallFont);
                                    imagesParagraph.setSpacingBefore(3);
                                    document.add(imagesParagraph);
                                }
                            }
                        }

                        // 处理文件列表
                        Object filesObj = topic.get("files");
                        if (filesObj != null) {
                            try {
                                List<?> fileList = (List<?>) filesObj;
                                if (fileList != null && !fileList.isEmpty()) {
                                    Paragraph filesParagraph = new Paragraph("上传文件: ", smallFont);
                                    filesParagraph.setSpacingBefore(3);
                                    document.add(filesParagraph);

                                    for (int i = 0; i < fileList.size(); i++) {
                                        Object fileItem = fileList.get(i);
                                        if (fileItem != null) {
                                            String filePath = "";
                                            String fileName = "";
                                            // 检查fileItem是否为Map对象（包含url和name）
                                            if (fileItem instanceof Map) {
                                                Map<String, Object> fileMap = (Map<String, Object>) fileItem;
                                                Object urlObj = fileMap.get("url");
                                                Object nameObj = fileMap.get("name");
                                                if (urlObj != null) {
                                                    filePath = urlObj.toString();
                                                } else if (nameObj != null) {
                                                    filePath = nameObj.toString();
                                                }
                                                if (nameObj != null) {
                                                    fileName = nameObj.toString();
                                                } else if (urlObj != null) {
                                                    fileName = urlObj.toString();
                                                }
                                            } else {
                                                // 如果是简单字符串，直接使用
                                                filePath = fileItem.toString();
                                                fileName = fileItem.toString();
                                            }

                                            if (!filePath.trim().isEmpty()) {
                                                // 文件只显示文本链接，不预览
                                                Paragraph fileItemPara = new Paragraph("  文件" + (i+1) + ": " + fileName, smallFont);
                                                fileItemPara.setSpacingBefore(2);
                                                document.add(fileItemPara);
                                            }
                                        }
                                    }
                                }
                            } catch (ClassCastException e) {
                                // 如果转换失败，按普通字符串处理
                                String filesStr = filesObj.toString();
                                if (filesStr != null && !filesStr.trim().isEmpty()) {
                                    Paragraph filesParagraph = new Paragraph("上传文件: " + filesStr, smallFont);
                                    filesParagraph.setSpacingBefore(3);
                                    document.add(filesParagraph);
                                }
                            }
                        }
                    } else {
                        // 非简答题，按原有方式处理
                        String studentAnswerStr = studentAnswer != null ? studentAnswer.toString() : "未作答";
                        Paragraph stuAns = new Paragraph("学生答案: " + studentAnswerStr, smallFont);
                        stuAns.setSpacingBefore(3);
                        document.add(stuAns);
                    }

                    // 得分
                    Object scoreObj = topic.get("studentScore");
                    Font scoreDisplayFont = scoreFont;
                    if (scoreObj != null) {
                        try {
                            double scoreValue = Double.parseDouble(scoreObj.toString());
                            if (scoreValue > 0) {
                                scoreDisplayFont = positiveScoreFont;
                            } else if (scoreValue < 0) {
                                scoreDisplayFont = negativeScoreFont;
                            }
                        } catch (NumberFormatException e) {
                            // If not a number, use default score font
                            scoreDisplayFont = scoreFont;
                        }
                    }
                    Paragraph score = new Paragraph("得分: " + scoreObj, scoreDisplayFont);
                    score.setSpacingBefore(3);
                    document.add(score);

                    // 分隔线
                    document.add(new Paragraph("\n"));
                }
            }

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("生成PDF失败: " + e.getMessage(), e);
        }

        return outputStream;
    }

    /**
     * 安全转换 Object 为 Integer
     * @param obj 要转换的对象
     * @return Integer 值，如果转换失败则返回 null
     */
    private Integer safeToInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof String) {
            try {
                return Integer.valueOf((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 安全转换Object为String
     * @param obj 要转换的对象
     * @return String值，如果转换失败则返回null
     */
    private String safeToString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof String) {
            return (String) obj;
        }
        return String.valueOf(obj);
    }

    /**
     * 添加表格单元格
     */
    private void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    /**
     * 检查文件路径是否为图片文件
     * @param filePath 文件路径
     * @return 是否为图片文件
     */
    private boolean isImageFile(String filePath) {
        if (filePath == null) {
            return false;
        }
        String lowerPath = filePath.toLowerCase();
        return lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg") ||
               lowerPath.endsWith(".png") || lowerPath.endsWith(".gif") ||
               lowerPath.endsWith(".bmp") || lowerPath.endsWith(".webp");
    }

    /**
     * 根据课程ID更新课程学生表中的作业数量
     * @param slSchool 学校信息
     * @param courseId 课程ID
     * @throws SQLException
     * @throws Exception
     */
    private void updateWorkCountForCourse(SlSchool slSchool, Integer courseId) throws SQLException, Exception {
        // 在事务提交后执行，需要单独获取数据库连接
        Connection workCountConn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            workCountConn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            workCountConn.setAutoCommit(true); // 使用自动提交模式

            // 1. 检查 yee_course_student 表中是否有该课程的学生选课记录
            String checkStudentSql = "SELECT COUNT(*) as count FROM yee_course_student WHERE courseId = ?";
            st = workCountConn.prepareStatement(checkStudentSql);
            st.setInt(1, courseId);
            rs = st.executeQuery();

            int studentCount = 0;
            if (rs.next()) {
                studentCount = rs.getInt("count");
            }

            closeResultSetAndStatement(rs, st);
            rs = null;
            st = null;

            // 如果没有学生选课，则不进行更新操作
            if (studentCount == 0) {
                return;
            }
            // 2. 根据courseId 查询 yee_course_class 表中 班级列表
            String selectClassSql = "SELECT id FROM yee_course_class WHERE courseId = ?";
            st = workCountConn.prepareStatement(selectClassSql);
            st.setInt(1, courseId);
            rs = st.executeQuery();
            List<Integer> classIds = new ArrayList<>();
            while (rs.next()) {
                classIds.add(rs.getInt("id"));
            }
            closeResultSetAndStatement(rs, st);
            rs = null;
            st = null;

            // 3. 查询该课程涉及的 yee_exam 表中的所有考试信息
            String selectExamSql = "SELECT * FROM yee_exam WHERE courseId = ? AND schoolId = ? AND allow = 1";
            st = workCountConn.prepareStatement(selectExamSql);
            st.setInt(1, courseId);
            st.setLong(2, slSchool.getId());
            rs = st.executeQuery();

            // 收集考试信息到列表中
            List<Map<String, Object>> examList = new ArrayList<>();

            // 先获取元数据 (避免在循环中重复获取)
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> examInfo = new HashMap<>();

                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    examInfo.put(columnName, value);
                }
                examList.add(examInfo);
            }

            closeResultSetAndStatement(rs, st);
            // 4. 按照examList中的考试记录和classList更新 yee_course_student 表中的 examCount 字段
            // 处理每个考试的班级列表，如果classList为空数组([])，则表示适用于所有班级
            rs = null;
            st = null;

            // 统计每个班级的考试数量
            Map<Integer, Integer> classExamCounts = new HashMap<>();

            for (Map<String, Object> examInfo : examList) {
                Object classListObj = examInfo.get("classList");
                List<Integer> examClassList = null;

                if (classListObj instanceof String) {
                    // 如果 classList 是 JSON 字符串，解析它
                    String classListStr = (String) classListObj;
                    if (classListStr != null && !classListStr.trim().isEmpty() && !classListStr.equals("[]")) {
                        try {
                            examClassList = JSON.parseArray(classListStr, Integer.class);
                        } catch (Exception e) {
                            System.err.println("解析 classList JSON 失败: " + classListStr + ", 错误: " + e.getMessage());
                            examClassList = new ArrayList<>();
                        }
                    } else {
                        // 如果是空数组或 null，表示适用于所有班级
                        examClassList = new ArrayList<>();
                    }
                } else if (classListObj instanceof List) {
                    // 如果 classList 已经是 List 类型
                    examClassList = (List<Integer>) classListObj;
                } else {
                    // 其他情况，初始化为空列表表示适用于所有班级
                    examClassList = new ArrayList<>();
                }

                // 如果 examClassList 为空，表示此考试适用于所有班级
                if (examClassList.isEmpty()) {
                    for (Integer classId : classIds) {
                        classExamCounts.put(classId, classExamCounts.getOrDefault(classId, 0) + 1);
                    }
                } else {
                    // 否则只适用于特定班级
                    for (Integer classId : examClassList) {
                        classExamCounts.put(classId, classExamCounts.getOrDefault(classId, 0) + 1);
                    }
                }
            }

            // 批量更新每个班级的考试数量
            int totalUpdateRows = 0;

            // 如果没有考试记录 (examList 为空),则需要将所有班级的 examCount 设置为 0
            if (examList.isEmpty()) {
                // 查询该课程下所有班级
                String selectAllClassesSql = "SELECT id FROM yee_course_class WHERE courseId = ?";
                st = workCountConn.prepareStatement(selectAllClassesSql);
                st.setInt(1, courseId);
                rs = st.executeQuery();

                while (rs.next()) {
                    Integer classId = rs.getInt("id");

                    String updateSql = "UPDATE yee_course_student SET examCount = 0 WHERE courseId = ? AND classId = ?";
                    st = workCountConn.prepareStatement(updateSql);
                    st.setInt(1, courseId);
                    st.setInt(2, classId);
                    int updateRows = st.executeUpdate();
                    totalUpdateRows += updateRows;

                    st.close();
                    st = null;

                }

                closeResultSetAndStatement(rs, st);
                rs = null;
                st = null;
            } else {
                // 有考试记录，按原有逻辑处理
                for (Map.Entry<Integer, Integer> entry : classExamCounts.entrySet()) {
                    Integer classId = entry.getKey();
                    Integer count = entry.getValue();

                    String updateSql = "UPDATE yee_course_student SET examCount = ? WHERE courseId = ? AND classId = ?";
                    st = workCountConn.prepareStatement(updateSql);
                    st.setInt(1, count);
                    st.setInt(2, courseId);
                    st.setInt(3, classId);
                    int updateRows = st.executeUpdate();
                    totalUpdateRows += updateRows;

                    st.close();
                    st = null;

                }
            }


        } catch (SQLException e) {
            System.err.println("更新课程考试数量失败: " + e.getMessage());
            throw e;
        } finally {
            closeResultSetAndStatement(rs, st);
            if (workCountConn != null) {
                try {
                    workCountConn.close();
                } catch (SQLException e) {
                    System.err.println("关闭workCountConn连接失败: " + e.getMessage());
                }
            }
        }
    }

    private void updateWorkAnswerScores(int schoolId, Integer userId, Integer workId, List<Map<String, Object>> workResult) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;

        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 2. 准备批量更新
            String updateAnswerSql = """
            UPDATE yee_exam_answer 
            SET score = ?, marked = 1, hit = ?
            WHERE userId = ? AND examId = ? AND topicId = ?
            """;

            st = conn.prepareStatement(updateAnswerSql);

            // 3. 遍历 workResult，为每个题目设置更新参数并添加到批处理
            // 如果 reSubScore = topicScore 则将hit 字段设置为1, 如果 reSubScore < topicScore &&  reSubScore > 0 则将hit 字段设置为2,  reSubScore = 0 则将hit 字段设置为3
            // 只更新 type = 4 或者 type = 5 的数据
            for (Map<String, Object> workItem : workResult) {
                Integer topicId = (Integer) workItem.get("topicId");
                Object typeObj = workItem.get("type");
                Integer type = null;
                if (typeObj != null) {
                    if (typeObj instanceof Integer) {
                        type = (Integer) typeObj;
                    } else {
                        type = Integer.parseInt(typeObj.toString());
                    }
                }

                // 只处理 type = 4 或 5 的主观题
                if (type != null && (type == 4 || type == 5)) {
                    BigDecimal scoreToUse = BigDecimal.ZERO;
                    int hitValue = 3; // 默认为3（错误）

                    Object reSubScoreObj = workItem.get("reSubScore");
                    Object topicScoreObj = workItem.get("topicScore");

                    BigDecimal reSubScore = null;
                    BigDecimal topicScore = null;

                    if (reSubScoreObj != null) {
                        if (reSubScoreObj instanceof BigDecimal) {
                            reSubScore = (BigDecimal) reSubScoreObj;
                        } else if (reSubScoreObj instanceof Number) {
                            reSubScore = new BigDecimal(((Number) reSubScoreObj).doubleValue());
                        } else {
                            reSubScore = new BigDecimal(reSubScoreObj.toString());
                        }
                    }

                    if (topicScoreObj != null) {
                        if (topicScoreObj instanceof BigDecimal) {
                            topicScore = (BigDecimal) topicScoreObj;
                        } else if (topicScoreObj instanceof Number) {
                            topicScore = new BigDecimal(((Number) topicScoreObj).doubleValue());
                        } else {
                            topicScore = new BigDecimal(topicScoreObj.toString());
                        }
                    }

                    scoreToUse = reSubScore != null ? reSubScore : BigDecimal.ZERO;

                    // 根据 reSubScore 与 topicScore 的比较设置 hit 值
                    if (topicScore != null && reSubScore != null) {
                        if (reSubScore.compareTo(topicScore) == 0) {  // reSubScore = topicScore
                            hitValue = 1;  // 完全正确
                        } else if (reSubScore.compareTo(topicScore) < 0 && reSubScore.compareTo(BigDecimal.ZERO) > 0) {  // 0 < reSubScore < topicScore
                            hitValue = 2;  // 部分正确
                        } else if (reSubScore.compareTo(BigDecimal.ZERO) == 0) {  // reSubScore = 0
                            hitValue = 3;  // 错误
                        } else {
                            hitValue = 3;  // 其他情况也设为错误
                        }
                    } else if (reSubScore != null && reSubScore.compareTo(BigDecimal.ZERO) == 0) {  // reSubScore = 0
                        hitValue = 3;  // 错误
                    }

                    // 设置参数
                    st.setBigDecimal(1, scoreToUse);
                    st.setInt(2, hitValue);
                    st.setInt(3, userId);
                    st.setInt(4, workId);
                    st.setInt(5, topicId);

                    // 添加到批处理
                    st.addBatch();
                }
                // 不处理 type = 1, 2, 3 的客观题，跳过它们
            }

            // 4. 执行批量更新
            int[] results = st.executeBatch();

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("批量更新作业题目分数失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", workId=" + workId +
                    ", workResult数量=" + (workResult != null ? workResult.size() : 0), e);
        } finally {
            // 安全关闭资源
            closeStatement(st);
            closeConnection(conn);
        }
    }

    // ==================== 学生查询（带分页，无权限，不报错） ====================
    private List<Map<String, Object>> getWorkDetailsForExport(
            Integer schoolId,
            Integer courseId,
            Integer classId,
            String title,
            Integer examId,
            int pageNum,
            int pageSize
    ) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            conn = databaseUtil.getConnection(schoolId);

            List<Integer> examClassIds = getExamClassIds(conn, examId, courseId);

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
                SELECT
                    s.number,
                    s.name,
                    cc.name AS className,
                    s.id
                FROM 
                    yee_course_student cs
                    LEFT JOIN yee_student s ON s.id = cs.studentId
                    LEFT JOIN yee_course_class cc ON cc.id = cs.classId
                WHERE 
                    cs.courseId = ?
                """);

            if (classId != null && classId > 0) {
                sqlBuilder.append(" AND cs.classId = ? ");
            } else if (!examClassIds.isEmpty()) {
                sqlBuilder.append(" AND cs.classId IN (");
                for (int i = 0; i < examClassIds.size(); i++) {
                    sqlBuilder.append("?");
                    if (i < examClassIds.size() - 1) sqlBuilder.append(",");
                }
                sqlBuilder.append(")");
            }

            if (title != null && !title.isBlank()) {
                sqlBuilder.append(" AND (s.name LIKE ? OR s.number LIKE ?) ");
            }

            sqlBuilder.append(" ORDER BY cs.classId, s.name ");
            sqlBuilder.append(" LIMIT ?, ?");

            st = conn.prepareStatement(sqlBuilder.toString());
            int idx = 1;
            st.setInt(idx++, courseId);

            if (classId != null && classId > 0) {
                st.setInt(idx++, classId);
            } else {
                for (Integer cid : examClassIds) st.setInt(idx++, cid);
            }

            if (title != null && !title.isBlank()) {
                String like = "%" + title.trim() + "%";
                st.setString(idx++, like);
                st.setString(idx++, like);
            }

            st.setInt(idx++, (pageNum - 1) * pageSize);
            st.setInt(idx++, pageSize);

            rs = st.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 1; i <= cols; i++) {
                    map.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                result.add(map);
            }

            return result;

        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    /**
     * 只查询【当前页学生】的成绩 → 25人一批，绝对安全
     * 不会全量查询！不会OOM！
     */
    private List<Map<String, Object>> getWorkRecordsScoresByUserIds(
            int schoolId,
            Integer courseId,
            Integer examId,
            List<Integer> userIds
    ) throws Exception {

        if (userIds.isEmpty()) {
            return List.of();
        }

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String inParams = String.join(",", Collections.nCopies(userIds.size(), "?"));

            String sql = """
            SELECT 
                er.examId,
                er.userId,
                er.courseId,
                er.frequency,
                er.state,
                COALESCE(ws.submitTime, er.finishTime) AS submitTime,
                er.teacherId,
                er.markTime,
                COALESCE(ws.finalScore, er.score) AS finalScore,
                CASE WHEN ws.scored = 1 OR er.score IS NOT NULL THEN 1 ELSE 0 END AS scored
            FROM yee_exam_record er
            LEFT JOIN yee_exam_score ws 
              ON er.userId = ws.userId AND er.examId = ws.examId
            WHERE er.courseId = ?
              AND er.examId = ?
              AND er.userId IN (""" + inParams + ")";

            st = conn.prepareStatement(sql);
            int idx = 1;
            st.setInt(idx++, courseId);
            st.setInt(idx++, examId);
            for (Integer uid : userIds) {
                st.setInt(idx++, uid);
            }

            rs = st.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int cols = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= cols; i++) {
                    String col = metaData.getColumnLabel(i);
                    if ("scored".equals(col) || "state".equals(col) || "frequency".equals(col)) {
                        row.put(col, rs.getInt(i));
                    } else {
                        row.put(col, rs.getObject(i));
                    }
                }
                result.add(row);
            }

            return result;

        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> getWorkDetailsForExportAll(
            Integer schoolId,
            Integer courseId,
            Integer classId,
            String title,
            Integer examId
    ) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        List<Map<String, Object>> result = new ArrayList<>();

        try {
            conn = databaseUtil.getConnection(schoolId);
            List<Integer> examClassIds = getExamClassIds(conn, examId, courseId);

            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT
                s.number,
                s.name,
                cc.name AS className,
                s.id
            FROM 
                yee_course_student cs
                LEFT JOIN yee_student s ON s.id = cs.studentId
                LEFT JOIN yee_course_class cc ON cc.id = cs.classId
            WHERE 
                cs.courseId = ?
            """);

            if (classId != null && classId > 0) {
                sqlBuilder.append(" AND cs.classId = ? ");
            } else if (!examClassIds.isEmpty()) {
                sqlBuilder.append(" AND cs.classId IN (");
                for (int i = 0; i < examClassIds.size(); i++) {
                    sqlBuilder.append("?");
                    if (i < examClassIds.size() - 1) sqlBuilder.append(",");
                }
                sqlBuilder.append(")");
            }

            if (title != null && !title.isBlank()) {
                sqlBuilder.append(" AND (s.name LIKE ? OR s.number LIKE ?) ");
            }

            sqlBuilder.append(" ORDER BY cs.classId, s.name ");

            st = conn.prepareStatement(sqlBuilder.toString());
            int idx = 1;
            st.setInt(idx++, courseId);

            if (classId != null && classId > 0) {
                st.setInt(idx++, classId);
            } else {
                for (Integer cid : examClassIds) st.setInt(idx++, cid);
            }

            if (title != null && !title.isBlank()) {
                String like = "%" + title.trim() + "%";
                st.setString(idx++, like);
                st.setString(idx++, like);
            }

            rs = st.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 1; i <= cols; i++) {
                    map.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                result.add(map);
            }

            return result;

        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> getRecordsScoresByUserIds(
            int schoolId,
            Integer courseId,
            Integer examId,
            Integer scoredState,
            List<Integer> userIds
    ) throws Exception {

        // 1. 【关键修改】过滤掉列表中的 null 值，生成一个干净的新列表
        // 这样既解决了空指针问题，也保证生成的 SQL 问号数量和后续设置的参数数量一致
        if (userIds == null || userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Integer> validUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 如果过滤完发现没有有效ID（比如原列表全是null），直接返回空结果
        if (validUserIds.isEmpty()) {
            return new ArrayList<>();
        }

        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new Exception("学校不存在或未审核");
        }

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            // 使用过滤后的 validUserIds 来生成问号占位符
            String inParams = String.join(",", Collections.nCopies(validUserIds.size(), "?"));

            String sql = """
                SELECT 
                    er.examId,
                    er.userId,
                    er.courseId,
                    er.frequency,
                    er.state,
                    COALESCE(ws.submitTime, er.finishTime) AS submitTime,
                    er.teacherId,
                    er.markTime,
                    COALESCE(ws.finalScore, er.score) AS finalScore,
                    CASE 
                        WHEN ws.scored = 1 THEN 1
                        WHEN er.score IS NOT NULL THEN 1
                        ELSE 0 
                    END AS scored
                FROM yee_exam_record er
                LEFT JOIN yee_exam_score ws 
                  ON er.userId = ws.userId AND er.examId = ws.examId
                WHERE er.courseId = ?
                  AND er.examId = ?
                  AND er.userId IN (""" + inParams + ")";

            if (scoredState != null) {
                sql += " AND (CASE WHEN ws.scored = 1 OR er.score IS NOT NULL THEN 1 ELSE 0 END) = ?";
            }

            try (PreparedStatement st = conn.prepareStatement(sql)) {
                int idx = 1;
                st.setInt(idx++, courseId);
                st.setInt(idx++, examId);

                // 2. 【关键修改】只遍历一次，且使用的是过滤后的 validUserIds
                for (Integer uid : validUserIds) {
                    st.setInt(idx++, uid);
                }

                if (scoredState != null) {
                    st.setInt(idx++, scoredState);
                }

                try (ResultSet rs = st.executeQuery()) {
                    List<Map<String, Object>> list = new ArrayList<>();
                    ResultSetMetaData meta = rs.getMetaData();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            String col = meta.getColumnLabel(i);
                            // 根据字段类型处理返回值
                            if ("scored".equals(col) || "state".equals(col) || "frequency".equals(col)) {
                                row.put(col, rs.getInt(i));
                            } else {
                                row.put(col, rs.getObject(i));
                            }
                        }
                        list.add(row);
                    }
                    return list;
                }
            }
        }
    }
//    private List<Map<String, Object>> getRecordsScoresByUserIds(
//            int schoolId,
//            Integer courseId,
//            Integer examId,
//            Integer scoredState,
//            List<Integer> userIds
//    ) throws Exception {
//        if (userIds.isEmpty()) return new ArrayList<>();
//
//        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//        if (slSchool == null || slSchool.getAllow() == 0)
//            throw new Exception("学校不存在或未审核");
//
//        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
//            String inParams = String.join(",", Collections.nCopies(userIds.size(), "?"));
//
//            String sql = """
//        SELECT
//            er.examId,
//            er.userId,
//            er.courseId,
//            er.frequency,
//            er.state,
//            COALESCE(ws.submitTime, er.finishTime) AS submitTime,
//            er.teacherId,
//            er.markTime,
//            COALESCE(ws.finalScore, er.score) AS finalScore,
//            CASE
//                WHEN ws.scored = 1 THEN 1
//                WHEN er.score IS NOT NULL THEN 1
//                ELSE 0
//            END AS scored
//        FROM yee_exam_record er
//        LEFT JOIN yee_exam_score ws
//          ON er.userId = ws.userId AND er.examId = ws.examId
//        WHERE er.courseId = ?
//          AND er.examId = ?
//          AND er.userId IN (""" + inParams + ")";
//
//            if (scoredState != null) {
//                sql += " AND (CASE WHEN ws.scored = 1 OR er.score IS NOT NULL THEN 1 ELSE 0 END) = ?";
//            }
//
//            try (PreparedStatement st = conn.prepareStatement(sql)) {
//                int idx = 1;
//                st.setInt(idx++, courseId);
//                st.setInt(idx++, examId);
//                for (Integer uid : userIds) {
//                    if (uid != null) {
//                        st.setInt(idx++, uid);
//                    }
//                }
//
//                for (Integer uid : userIds) st.setInt(idx++, uid);
//                if (scoredState != null) st.setInt(idx++, scoredState);
//
//                try (ResultSet rs = st.executeQuery()) {
//                    List<Map<String, Object>> list = new ArrayList<>();
//                    ResultSetMetaData meta = rs.getMetaData();
//                    while (rs.next()) {
//                        Map<String, Object> row = new HashMap<>();
//                        for (int i = 1; i <= meta.getColumnCount(); i++) {
//                            String col = meta.getColumnLabel(i);
//                            if ("scored".equals(col) || "state".equals(col) || "frequency".equals(col)) {
//                                row.put(col, rs.getInt(i));
//                            } else {
//                                row.put(col, rs.getObject(i));
//                            }
//                        }
//                        list.add(row);
//                    }
//                    return list;
//                }
//            }
//        }
//    }

    /**
     * 获取所有有效学生完整数据（含成绩合并、状态过滤），供导出流程直接分批使用，
     * 避免后续 getExamStudentsByUserIds 再次全量查询。
     */
    public List<Map<String, Object>> getAllValidStudentsForExport(
            int schoolId, Integer courseId, Integer examId, String title,
            Integer classId, Integer subState, Integer reviewState, Integer scoredState
    ) throws Exception {
        Result result = selectSearchRecordAll(
                schoolId, courseId, examId, title, classId,
                subState, reviewState, scoredState,
                1, Integer.MAX_VALUE
        );

        Map<String, Object> data = (Map<String, Object>) result.getData();
        return (List<Map<String, Object>>) data.get("result");
    }

    public List<Map<String, Object>> getExamStudentsByUserIds(
            int schoolId, Integer courseId, Integer examId, List<Integer> userIds
    ) throws Exception {
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Map<String, Object>> allStudents = getWorkDetailsForExport(
                schoolId, courseId, null, null, examId, 1, Integer.MAX_VALUE
        );

        Map<Integer, Map<String, Object>> studentMap = allStudents.stream()
                .collect(Collectors.toMap(
                        s -> (Integer) s.get("id"),
                        s -> s,
                        (a, b) -> a
                ));

        List<Map<String, Object>> scoreList = getWorkRecordsScoresByUserIds(
                schoolId, courseId, examId, userIds
        );

        Map<Integer, Map<String, Object>> scoreMap = scoreList.stream()
                .collect(Collectors.toMap(
                        r -> ((Number) r.get("userId")).intValue(),
                        r -> r,
                        (a, b) -> a
                ));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Integer uid : userIds) {
            Map<String, Object> student = studentMap.get(uid);
            if (student == null) {
                continue;
            }

            Map<String, Object> fullData = new HashMap<>(student);
            Map<String, Object> score = scoreMap.get(uid);

            if (score != null) {
                fullData.putAll(score);
            } else {
                // 未提交学生，保留基础信息
                fullData.put("userId", uid);
                fullData.put("finalScore", null);
                fullData.put("submitTime", null);
                fullData.put("scored", 0);
                fullData.put("state", null);
            }

            result.add(fullData);
        }

        return result;
    }
}

