package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.constant.DataAuth;
import cn.xfywz.guozespring.entity.dto.YeeWorkExportDTO;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeePaperTopic;
import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import cn.xfywz.guozespring.entity.mhsch.YeeWork;
import cn.xfywz.guozespring.entity.mhsch.YeeWorkTopic;
import cn.xfywz.guozespring.entity.vo.QuestionExportVO;
import cn.xfywz.guozespring.entity.vo.WorkReportVO;
import cn.xfywz.guozespring.excel.ExcelExportStyles;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeWorkService;
import cn.xfywz.guozespring.util.AuthDataPermissionUtil;
import cn.xfywz.guozespring.util.JsonUtil;
import cn.xfywz.guozespring.util.ParseJsonUtil;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import cn.xfywz.guozespring.excel.ResponseExportUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.merge.OnceAbsoluteMergeStrategy;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * @Author: ChengLin
 */
@Service
public class YeeWorkServiceImpl implements YeeWorkService {

    private static final Logger logger = LoggerFactory.getLogger(YeeExamServiceImpl.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final ParseJsonUtil parseJsonUtil = new ParseJsonUtil();

    private static final JsonUtil jsonUtil = new JsonUtil();


    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result selectAll(int schoolId, Integer courseId, Integer classId, String title) throws Exception {
        // 横批显示
        Map<String, Integer> totalWorkStats = getTotalWorkStats(schoolId, courseId, classId);

        // 原始列表
        List<Map<String, Object>> workList = getWorkDetailsByCourseAndSchool(schoolId, courseId, classId, title);

        // 过滤掉chapterName 为null的数据 workList
        workList = workList.stream().filter(work -> work.get("chapterName") != null).collect(Collectors.toList());

        // 结构化列表
        List<WorkReportVO> workReportVOS = buildWorkReport(workList);

        // 查询yee_work_record 表 记录
        List<Map<String, Object>> workRecordList = getWorkRecordDetails(schoolId, courseId, classId);

        // 遍历workReportVOS中的works数组, 再根据works数组里面的id 和 workRecordList 数组中的workId进行关联 然后 赋值submitted,unSubmitted,marked, unMarked 这四个字段根据workRecordList 数组中的值
        workReportVOS.stream().forEach(workItem -> {
            workItem.getWorks().stream().forEach(work -> {
                int submitted = 0;
                int unSubmitted = work.getTotalNum();
                int marked = 0;
                int unMarked = 0;
                workRecordList.stream().forEach(workRecord -> {
                    if (workRecord.get("workId").equals(work.getId())) {
                        work.setSubmitted(submitted + 1);
                        work.setUnSubmitted(unSubmitted - 1);
                        work.setMarked(workRecord.get("state").equals(3) ? marked + 1 : 0);
                        work.setUnMarked(workRecord.get("state").equals(2) ? unMarked + 1 : 0);
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
        result.put("work", workReportVOS);
        return Result.success(result);
    }

    @Override
    public Result selectRecordAll(int schoolId, Integer courseId, Integer nodeId, Integer classId) {
        // 根据作业id获取作业表信息
        List<Object> workInfo = getWorkInfoById(schoolId, nodeId, courseId);
        return Result.success(workInfo);
    }

    @Override
    public Result selectRecordAllWorkId(int schoolId, Integer courseId, Integer workId, Integer classId) {
        // 根据作业id获取作业表信息
        Map<String, Object> workInfo = getWorkInfoByWorkIdAuth(schoolId, workId, courseId);
        return Result.success(workInfo);
    }

    @Override
    public Result selectSearchRecordAll(int schoolId, Integer courseId, Integer workId, String title,
                                        Integer classId, Integer subState, Integer reviewState, Integer scoredState, Integer pageNum, Integer pageSize) throws  Exception
    {
        // 1. 查询学生基本信息
        List<Map<String, Object>> studentBasicList = getWorkDetails(schoolId, courseId, classId, title, workId);

        // 2. 查询作业提交信息
        List<Map<String, Object>> workRecordList = getWorkRecordsWithScores(schoolId, courseId, workId, scoredState);

        // 3. 聚合以上查询数据组装 studentBasicList中的id和workRecordList中的userId进行关联 用steam流 新建数据结构List集合 设置返回值
        Map<Long, Map<String, Object>> workRecordMap = workRecordList.stream()
                .collect(Collectors.toMap(
                        record -> ((Number) record.get("userId")).longValue(),  // 转为 Long
                        record -> record,
                        (existing, replacement) -> existing  // 如果重复，保留第一个
                ));

        // 4. 使用 Stream 聚合：遍历学生列表，关联作业记录，构建新结构
        List<Map<String, Object>> result = studentBasicList.stream()
                .map(student -> {
                    Map<String, Object> merged = new HashMap<>(student); // 先复制学生信息

                    Long studentId = ((Number) student.get("id")).longValue();
                    Map<String, Object> workRecord = workRecordMap.get(studentId);

                    if (workRecord != null) {
                        // 合并作业记录字段（避免覆盖 id/name 等）
                        workRecord.forEach((key, value) -> {
                            // 可选：避免覆盖已有字段，如不想覆盖 "id", "name" 等
                            if (!merged.containsKey(key) || !key.equals("id")) {
                                merged.put(key, value);
                            }
                        });
                    } else {
                        // 可选：填充默认值
                        merged.put("userId", studentId);
                        merged.put("finalScore", null);
                        merged.put("submitTime", null);
                        merged.put("scored", 0);
                        merged.put("finishTime", null);
                        merged.put("state", null);
                    }

                    return merged;
                })
                .collect(Collectors.toList());

        // 5. 根据 批阅状态(reviewState:{3:"已批"}) 进行过滤信息

        // 6. 根据请求条件过滤信息, subState 和 reviewState

        // 6.1 交卷状态条件查询 根据 subState 过滤结果
        if (subState == null){

        } else if (subState == 1) {
            // 只保留 state = 1
            result = result.stream()
                    .filter(record -> {
                        Object stateObj = record.get("state");
                        return stateObj == null || stateObj instanceof Number && ((Number) stateObj).intValue() == 1;
                    }).collect(Collectors.toList());
        } else if (subState == 2) {
            // 只保留 state = 2
            result = result.stream()
                    .filter(record -> {
                        Object stateObj = record.get("state");
                        return stateObj instanceof Number && ((Number) stateObj).intValue() == 2;
                    }).collect(Collectors.toList());
        } else if (subState == 3){
            // 只保留 state = 3
            result = result.stream()
                    .filter(record -> {
                        Object stateObj = record.get("state");
                        return stateObj instanceof Number && ((Number) stateObj).intValue() == 3;
                    }).collect(Collectors.toList());
        }

        // 7. 处理分页
        long total = result.size();
        List<Map<String, Object>> pagedResult;
        
        if (pageNum != null && pageSize != null && pageSize > 0) {
            int page = Math.max(1, pageNum); // 页码至少为1
            int offset = (page - 1) * pageSize;
            int endIndex = Math.min(offset + pageSize, result.size());
            
            if (offset < result.size()) {
                pagedResult = result.subList(offset, endIndex);
            } else {
                pagedResult = new ArrayList<>(); // 如果offset超出范围，返回空列表
            }
        } else {
            // 如果没有提供分页参数，则返回全部结果
            pagedResult = result;
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("result", pagedResult);
        resultMap.put("total", total);
        if (pageNum != null && pageSize != null) {
            resultMap.put("pageNum", pageNum);
            resultMap.put("pageSize", pageSize);
        }
//        resultMap.put("studentBasicList", studentBasicList);
//        resultMap.put("workRecordList", workRecordList);

        return Result.success(resultMap);

    }


    /**
     * 答题记录 复批/查阅
     */
    @Override
    public Result selectWorkRecordConsult(int schoolId, Integer userId, Integer workId) throws Exception {
        // 作业批阅 学生信息情况
        List<Map<String, Object>> studentResult = getWorkRecordDetailByUserAndWork(schoolId, userId, workId);

        // 作业题目列表以及得分情况和学生答案
        List<Map<String, Object>> workResult =  getWorkTopicDetailsByUserAndWork(schoolId, userId, workId);


        // 数据组装
        Map resultMap = new HashMap<>();
        resultMap.put("studentResult", studentResult);
        resultMap.put("workResult", workResult);

        return Result.success(resultMap);
    }

    private List<Map<String, Object>> getWorkTopicDetailsByUserAndWork(
            int schoolId,
            Integer userId,
            Integer workId) throws Exception {

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

            // 3. 构建 SQL：查询作业题目、学生作答、题目分值、学生得分等
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
                wr.id AS wrId,
                wa.files,
                wa.images
            FROM 
                yee_work_record wr
                LEFT JOIN yee_work_answer wa ON wr.id = wa.recordId
                LEFT JOIN yee_work_topic wt ON wt.id = wa.topicId
            WHERE 
                wr.userId = ?
                AND wr.workId = ?
            """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setLong(paramIndex++, userId);   // wr.userId
            st.setInt(paramIndex++, workId);    // wr.workId

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
            throw new Exception("根据用户和作业查询题目作答详情失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", workId=" + workId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private List<Map<String, Object>> getWorkTopicDetailsByUserAndWorkPre(
            int schoolId,
            Integer workId) throws Exception {

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
                yee_work_topic wt
                
            WHERE 
                wt.workId = ?
            """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setInt(paramIndex++, workId);    // wt.workId

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
            throw new Exception("根据用户和作业查询题目作答详情失败，参数：schoolId=" + schoolId +
                    ", workId=" + workId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

//    @Override
//    public Result selectWorkRecordRecheck(int schoolId, Integer userId, Integer workId, BigDecimal recheckScore) throws Exception {
//
//        // 根据recheckScore 更新yee_work_record表中的score字段 分数情况
//        updateWorkRecordScore(schoolId, userId, workId, recheckScore,null,  null);
//
//        return Result.success("修改成功");
//    }

    @Override
    public Result selectWorkRecordRecheckNew(int schoolId, Integer userId, Integer workId, BigDecimal recheckScore, Integer teacherId, List<Map<String, Object>> workResult) throws Exception {
        // √ 根据recheckScore 更新yee_work_record表中的score字段 分数情况
        updateWorkRecordScore(schoolId, userId, workId, recheckScore, teacherId, workResult);

        // √ 根据 workResult 更新 yee_work_answer 表中的各个题目得分
        updateWorkAnswerScores(schoolId, userId, workId, workResult);
      
        // 查询作业所属的 courseId
        Integer courseId = getCourseIdByWorkId(schoolId, workId);
              
        // 更新 yee_work_score 表中的最终得分
      if (courseId != null) {
            updateWorkScore(schoolId, userId, workId, courseId, recheckScore);
        }
      
        return Result.success("修改成功");
    }

    @Override
    public Result selectWorkRecordManual(int schoolId, Integer userId, Integer workId, BigDecimal manualScore) throws Exception {
        // 查询作业所属的 courseId
        Integer courseId = getCourseIdByWorkId(schoolId, workId);
        
        // 根据 manualScore 更新 yee_work_score 表中的 finalScore 字段 分数情况
       if (courseId != null) {
            updateWorkScore(schoolId, userId, workId, courseId, manualScore);
        }
        return Result.success("修改成功");
    }
    
    /**
     * 根据作业 ID 查询所属课程 ID
     */
    private Integer getCourseIdByWorkId(int schoolId, Integer workId) throws Exception {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
            
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
          if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }
                
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
                
            String sql = "SELECT courseId FROM yee_work WHERE id = ? AND schoolId = ?";
            st = conn.prepareStatement(sql);
            st.setInt(1, workId);
            st.setInt(2, schoolId);
                
            rs = st.executeQuery();
          if (rs.next()) {
                return rs.getInt("courseId");
            }
                
            return null;
        } catch (Exception e) {
            throw new Exception("查询作业所属课程失败：workId=" + workId, e);
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }
    
    @Override
    public Result selectWorkRecordManualList(int schoolId, Integer userId, Integer workId) throws Exception {
        // 查询出 作业名称:高等数学, 学生姓名/学号: 王xx(19103019512387), 最终得分: 22.5
        Map<String, Object> studentFinalScore = getStudentFinalScoreByUserIdAndWorkId(schoolId, userId, workId);

        return Result.success(studentFinalScore);
    }

    @Override
    public Result selectWorkRecordConsultPre(int schoolId, Integer workId) throws Exception {
        // 作业批阅 学生信息情况
        List<Map<String, Object>> studentResult = getWorkRecordDetailByUserAndWorkPre(schoolId, workId);

        // 作业题目列表以及得分情况和学生答案
        List<Map<String, Object>> workResult =  getWorkTopicDetailsByUserAndWorkPre(schoolId, workId);


        // 数据组装
        Map resultMap = new HashMap<>();
        resultMap.put("studentResultPre", studentResult);
        resultMap.put("workResultPre", workResult);

        return Result.success(resultMap);
    }

    @Override
    public void exportQuestions(HttpServletResponse response, Integer schoolId, String topic, Integer createId, Integer type, Integer level, Integer cateBid, Integer cateMid, Integer workId) throws Exception {
        OutputStream outputStream = null;
        ExcelWriter excelWriter = null;
        try {

            // 1. 设置响应头
            LocalDateTime now = LocalDateTime.now();
            String fileName = "作业题目导出_" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
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
                            connection, schoolId, pageSize, pageNum, topic, createId, type, level, cateBid, cateMid, workId);

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
            logger.error("导出试题数据时发生异常", e);
            throw e;
        } finally {
            // 确保ExcelWriter正确关闭，这会自动刷新和关闭输出流
            if (excelWriter != null) {
                try {
                    excelWriter.finish();
                } catch (Exception e) {
                    logger.error("关闭ExcelWriter时发生异常", e);
                    // 检查异常是否与流已关闭有关
                    if (e.getMessage() != null && e.getMessage().contains("Stream closed")) {
                        logger.warn("检测到流已关闭，可能是客户端已断开连接，这是正常现象");
                    } else {
                        // 对于其他异常，记录但不抛出，避免影响主流程
                        logger.warn("ExcelWriter关闭时发生非流关闭异常，但不会影响导出文件的完整性");
                    }
                }
            }
        }
    }

    @Override
    public Result add(YeeWork yeeWork) throws Exception {
        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(yeeWork.getSchoolId());
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
                    INSERT INTO yee_work 
                    (userId, title, topicNumber, score, type, remarks, addTime, sequence, nodeId, courseId, startTime, endTime, paperId, createUserId,
                     isPrivate, classList, teacherType, allow, frequency, scoringRules, hasCollect, `lock`, schoolId, parsing)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            Integer workId;
            try (PreparedStatement insertSt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertSt.setObject(1, yeeWork.getUserId());
                insertSt.setObject(2, yeeWork.getTitle());
                insertSt.setObject(3, yeeWork.getTopicNumber() != null ? yeeWork.getTopicNumber() : 0);
                insertSt.setObject(4, yeeWork.getScore() != null ? yeeWork.getScore() : 0);
                insertSt.setObject(5, yeeWork.getType());
                insertSt.setObject(6, yeeWork.getRemarks());
                insertSt.setObject(7, yeeWork.getAddTime());
                insertSt.setObject(8, yeeWork.getSequence());
                insertSt.setObject(9, yeeWork.getNodeId());
                insertSt.setObject(10, yeeWork.getCourseId());
                insertSt.setObject(11, yeeWork.getStartTime());
                insertSt.setObject(12, yeeWork.getEndTime());
                insertSt.setObject(13, yeeWork.getPaperId());
                insertSt.setObject(14, yeeWork.getCreateUserId());
                insertSt.setObject(15, yeeWork.getIsPrivate());
                insertSt.setString(16, JSON.toJSONString(yeeWork.getClassList()));
                insertSt.setObject(17, yeeWork.getTeacherType());
                insertSt.setObject(18, yeeWork.getAllow());
                insertSt.setObject(19, yeeWork.getFrequency());
                insertSt.setObject(20, yeeWork.getScoringRules());
                insertSt.setObject(21, yeeWork.getHasCollect());
                insertSt.setObject(22, yeeWork.getLock());
                insertSt.setObject(23, yeeWork.getSchoolId());
                insertSt.setObject(24, yeeWork.getParsing());

                int rows = insertSt.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("添加试卷失败");
                }

                try (ResultSet generatedKeys = insertSt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        workId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("创建试卷失败，未获取到主键");
                    }
                }
            }

            // 5. 查询题篮中的题目 ID 列表
            String basketSql = "SELECT exId FROM yee_basket WHERE userId = ?";
            List<Integer> exIdList = new ArrayList<>();
            try (PreparedStatement basketSt = connection.prepareStatement(basketSql)) {
                basketSt.setObject(1, yeeWork.getUserId());
                try (ResultSet rs = basketSt.executeQuery()) {
                    while (rs.next()) {
                        exIdList.add(rs.getInt("exId"));
                    }
                }
            }

            if (exIdList.isEmpty()) {
                // 提交事务（即使无题目，也算成功）
                connection.commit();
                return Result.success("试卷创建成功，但无题目", workId);
            }


            // 6. 动态生成 IN 查询
            String placeholders = String.join(",", Collections.nCopies(exIdList.size(), "?"));
            String questionSql = "SELECT * FROM yee_question WHERE id IN (" + placeholders + ")";

            List<YeeWorkTopic> topicList = new ArrayList<>();
            try (PreparedStatement questionSt = connection.prepareStatement(questionSql)) {
                for (int i = 0; i < exIdList.size(); i++) {
                    questionSt.setInt(i + 1, exIdList.get(i));
                }
                int i = 1;
                try (ResultSet rs = questionSt.executeQuery()) {
                    while (rs.next()) {
                        YeeWorkTopic topic = new YeeWorkTopic();
                        topic.setOid(rs.getInt("oid"));
                        topic.setTopic(rs.getString("topic"));
                        topic.setType(rs.getInt("type"));
                        topic.setLevel(rs.getInt("level"));
                        topic.setScore(rs.getInt("score"));
                        topic.setAnalysis(rs.getString("analysis"));
                        topic.setPid(rs.getInt("pid"));
                        topic.setWorkId(workId);
                        topic.setTitle(rs.getString("title"));
                        topic.setUpload(rs.getString("upload"));
                        topic.setScoreMode(rs.getInt("scoreMode"));
                        topic.setSchoolId(yeeWork.getSchoolId());
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

            // 7. 批量插入题目
            String insertTopicSql = """
                    INSERT INTO yee_work_topic 
                    (oid, topic, type, level, score, missScore, option1, option2, option3, analysis, pid, workId, title, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid, number)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement insertTopicSt = connection.prepareStatement(insertTopicSql)) {
                for (YeeWorkTopic topic : topicList) {
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
                    insertTopicSt.setObject(12, topic.getWorkId());
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


            // 更新试卷的topicNumber和score字段
            int topicNumber = topicList.size();
            int totalScore = topicList.stream().mapToInt(YeeWorkTopic::getScore).sum();

            String updatePaperSql = "UPDATE yee_work SET topicNumber = ?, score = ? WHERE id = ?";
            try (PreparedStatement updatePaperSt = connection.prepareStatement(updatePaperSql)) {
                updatePaperSt.setInt(1, topicNumber);
                updatePaperSt.setInt(2, totalScore);
                updatePaperSt.setInt(3, workId);

                int updateRows = updatePaperSt.executeUpdate();
                if (updateRows != 1) {
                    throw new SQLException("更新试卷信息失败");
                }
            }

            // 提交事务
            connection.commit();

            // 根据 courseId 查询 yee_course_student 是否有学生选课, 如果有的话:根据courseId查询yee_work表中有多少作业数量, ,更新到yee_course_student表的workCount字段 ,没有的话不更新
            // 需要在事务提交后执行，以确保当前添加的作业被计算在内
//            updateWorkCountForCourse(slSchool, yeeWork.getCourseId());

            return Result.success("试卷创建成功", workId);

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

            // 2. 构建动态 SQL：查询作业信息
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT
                w.id,
                w.userId,
                w.title,
                w.topicNumber,
                w.score,
                w.type,
                w.remarks,
                w.addTime,
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
                w.scoringRules,
                w.hasCollect,
                w.`lock`,
                w.schoolId,
                w.parsing,
                w.addDate,
                GROUP_CONCAT(cc.name ORDER BY cc.id SEPARATOR ', ') AS classNameList
            FROM yee_work w
            LEFT JOIN yee_course_class cc
                ON w.schoolId = cc.schoolId
                AND w.courseId = cc.courseId
                AND JSON_CONTAINS(w.classList, CAST(cc.id AS JSON))
                AND cc.allow = 1
            WHERE
                w.schoolId = ?
                AND w.courseId = ?
        """);

            // ===================== 统一权限控制 =====================
            Long teacherId = AuthDataPermissionUtil.getCurrentUserId();
            DataAuth auth = AuthDataPermissionUtil.getCurrentDataAuth();

            // 只有 不是超管 && 不是课程创建者 才加权限条件
            if (!DataAuth.ALL.equals(auth)) {
                sqlBuilder.append(" AND ( ");
                // 1. 全部班级可见
                sqlBuilder.append(" JSON_LENGTH(w.classList) = 0 ");
                sqlBuilder.append(" OR ");
                // 2. 自己负责的班级
                sqlBuilder.append(" EXISTS ( ");
                sqlBuilder.append("   SELECT 1 FROM yee_course_class ycc ");
                sqlBuilder.append("   WHERE ycc.teacherId = ? ");
                sqlBuilder.append("   AND JSON_CONTAINS(w.classList, CAST(ycc.id AS JSON)) ");
                sqlBuilder.append(" ) ");
                sqlBuilder.append(" OR ");
                // 3. 自己是课程创建者
                sqlBuilder.append(" EXISTS (SELECT 1 FROM yee_course yc WHERE yc.id = w.courseId AND yc.createId = ?) ");
                sqlBuilder.append(" ) ");
            }

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
                w.type,
                w.remarks,
                w.addTime,
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
                w.scoringRules,
                w.hasCollect,
                w.`lock`,
                w.schoolId,
                w.parsing,
                w.addDate
        """);

            sqlBuilder.append(" ORDER BY addTime DESC ");

            // 3. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 4. 设置参数
            int paramIndex = 1;
            st.setInt(paramIndex++, schoolId);
            st.setInt(paramIndex++, courseId);

            // ===================== ✅ 权限参数注入 =====================
            if (!DataAuth.ALL.equals(auth)) {
                st.setLong(paramIndex++, teacherId);
                st.setLong(paramIndex++, teacherId);
            }

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
            e.printStackTrace();
            throw new Exception("查询作业信息失败，参数：schoolId=" + schoolId +
                    ", courseId=" + courseId +
                    ", classId=" + classId +
                    ", title=" + title +
                    ", nodeId=" + nodeId +
                    ", allow=" + allow, e);
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }


    @Override
    public Result addMore(YeeWork yeeWork) throws Exception {
        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(yeeWork.getSchoolId());
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
                    INSERT INTO yee_work 
                    (userId, title, topicNumber, score, type, remarks, addTime, sequence, nodeId, courseId, startTime, endTime, paperId, createUserId,
                     isPrivate, classList, teacherType, allow, frequency, scoringRules, hasCollect, `lock`, schoolId, parsing)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            Integer workId;
            try (PreparedStatement insertSt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertSt.setObject(1, yeeWork.getUserId());
                insertSt.setObject(2, yeeWork.getTitle());
                insertSt.setObject(3, yeeWork.getTopicNumber() != null ? yeeWork.getTopicNumber() : 0);
                insertSt.setObject(4, yeeWork.getScore() != null ? yeeWork.getScore() : 0);
                insertSt.setObject(5, yeeWork.getType());
                insertSt.setObject(6, yeeWork.getRemarks());
                insertSt.setObject(7, yeeWork.getAddTime());
                insertSt.setObject(8, yeeWork.getSequence()); // 试题顺序
                insertSt.setObject(9, yeeWork.getNodeId());
                insertSt.setObject(10, yeeWork.getCourseId());
                insertSt.setObject(11, yeeWork.getStartTime());
                insertSt.setObject(12, yeeWork.getEndTime());
                insertSt.setObject(13, yeeWork.getPaperId());
                insertSt.setObject(14, yeeWork.getCreateUserId());
                insertSt.setObject(15, yeeWork.getIsPrivate());
                insertSt.setString(16, JSON.toJSONString(yeeWork.getClassList()));
                insertSt.setObject(17, yeeWork.getTeacherType());
                insertSt.setObject(18, yeeWork.getAllow());
                insertSt.setObject(19, yeeWork.getFrequency());
                insertSt.setObject(20, yeeWork.getScoringRules());
                insertSt.setObject(21, yeeWork.getHasCollect());
                insertSt.setObject(22, yeeWork.getLock());
                insertSt.setObject(23, yeeWork.getSchoolId());
                insertSt.setObject(24, yeeWork.getParsing());

                int rows = insertSt.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("添加试卷失败");
                }

                try (ResultSet generatedKeys = insertSt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        workId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("创建试卷失败，未获取到主键");
                    }
                }
            }

            String questionSql = "SELECT * FROM yee_paper_topic WHERE paperId = ?";

            List<YeeWorkTopic> topicList = new ArrayList<>();
            try (PreparedStatement questionSt = connection.prepareStatement(questionSql)) {
                questionSt.setInt(1, yeeWork.getPaperId());
                int j = 1;
                try (ResultSet rs = questionSt.executeQuery()) {
                    if (yeeWork.getSequence() == 2){
                        while (rs.next()) {
                            YeeWorkTopic topic = new YeeWorkTopic();
                            topic.setOid(rs.getInt("oid"));
                            topic.setTopic(rs.getString("topic"));
                            topic.setType(rs.getInt("type"));
                            topic.setLevel(rs.getInt("level"));
                            topic.setScore(rs.getInt("score"));
                            topic.setAnalysis(rs.getString("analysis"));
                            topic.setPid(rs.getInt("pid"));
                            topic.setWorkId(workId);
                            topic.setTitle(rs.getString("title"));
                            topic.setUpload(rs.getString("upload"));
                            topic.setScoreMode(rs.getInt("scoreMode"));
                            topic.setSchoolId(yeeWork.getSchoolId());
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
                            YeeWorkTopic topic = new YeeWorkTopic();
                            topic.setOid(rs.getInt("oid"));
                            topic.setTopic(rs.getString("topic"));
                            topic.setType(rs.getInt("type"));
                            topic.setLevel(rs.getInt("level"));
                            topic.setScore(rs.getInt("score"));
                            topic.setAnalysis(rs.getString("analysis"));
                            topic.setPid(rs.getInt("pid"));
                            topic.setWorkId(workId);
                            topic.setTitle(rs.getString("title"));
                            topic.setUpload(rs.getString("upload"));
                            topic.setScoreMode(rs.getInt("scoreMode"));
                            topic.setSchoolId(yeeWork.getSchoolId());
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

            // 7. 批量插入题目
            String insertTopicSql = """
                    INSERT INTO yee_work_topic 
                    (oid, topic, type, level, score, missScore, option1, option2, option3, analysis, pid, workId, title, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid, number)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement insertTopicSt = connection.prepareStatement(insertTopicSql)) {
                for (YeeWorkTopic topic : topicList) {
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
                    insertTopicSt.setObject(12, topic.getWorkId());
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


            // 更新试卷的topicNumber和score字段
            int topicNumber = topicList.size();
            int totalScore = topicList.stream().mapToInt(YeeWorkTopic::getScore).sum();

            String updatePaperSql = "UPDATE yee_work SET topicNumber = ?, score = ? WHERE id = ?";
            try (PreparedStatement updatePaperSt = connection.prepareStatement(updatePaperSql)) {
                updatePaperSt.setInt(1, topicNumber);
                updatePaperSt.setInt(2, totalScore);
                updatePaperSt.setInt(3, workId);

                int updateRows = updatePaperSt.executeUpdate();
                if (updateRows != 1) {
                    throw new SQLException("更新试卷信息失败");
                }
            }

            // 提交事务
            connection.commit();

            // 根据 courseId 查询 yee_course_student 是否有学生选课, 如果有的话:根据courseId查询yee_work表中有多少作业数量, ,更新到yee_course_student表的workCount字段 ,没有的话不更新
//            updateWorkCountForCourse(slSchool, yeeWork.getCourseId());

            return Result.success("试卷创建成功", workId);

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
    public Result update(YeeWork yeeWork) throws Exception {

        Result result = selectById(yeeWork.getSchoolId(), yeeWork.getId());
        YeeWork queryWork = (YeeWork) result.getData();

        if (queryWork == null) {
            return Result.error("作业不存在");
        }

        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(yeeWork.getSchoolId());
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
            sqlBuilder.append("UPDATE yee_work SET ");
            
            List<String> updates = new ArrayList<>();
            List<Object> parameters = new ArrayList<>();
            
            // 动态添加可更新字段
            if (yeeWork.getUserId() != null) {
                updates.add("userId = ?");
                parameters.add(yeeWork.getUserId());
            }
            
            if (yeeWork.getTitle() != null) {
                updates.add("title = ?");
                parameters.add(yeeWork.getTitle());
            }
            
            if (yeeWork.getTopicNumber() != null) {
                updates.add("topicNumber = ?");
                parameters.add(yeeWork.getTopicNumber());
            }
            
            if (yeeWork.getScore() != null) {
                updates.add("score = ?");
                parameters.add(yeeWork.getScore());
            }
            
            if (yeeWork.getType() != null) {
                updates.add("type = ?");
                parameters.add(yeeWork.getType());
            }
            
            if (yeeWork.getRemarks() != null) {
                updates.add("remarks = ?");
                parameters.add(yeeWork.getRemarks());
            }
            
            if (yeeWork.getAddTime() != null) {
                updates.add("addTime = ?");
                parameters.add(yeeWork.getAddTime());
            }
            
            if (yeeWork.getSequence() != null) {
                updates.add("sequence = ?");
                parameters.add(yeeWork.getSequence());
            }
            
            if (yeeWork.getNodeId() != null) {
                updates.add("nodeId = ?");
                parameters.add(yeeWork.getNodeId());
            }
            
            if (yeeWork.getCourseId() != null) {
                updates.add("courseId = ?");
                parameters.add(yeeWork.getCourseId());
            }
            
            if (yeeWork.getStartTime() != null) {
                updates.add("startTime = ?");
                parameters.add(yeeWork.getStartTime());
            }
            
            if (yeeWork.getEndTime() != null) {
                updates.add("endTime = ?");
                parameters.add(yeeWork.getEndTime());
            }
            
            if (yeeWork.getPaperId() != null) {
                updates.add("paperId = ?");
                parameters.add(yeeWork.getPaperId());
            }
            
            if (yeeWork.getCreateUserId() != null) {
                updates.add("createUserId = ?");
                parameters.add(yeeWork.getCreateUserId());
            }
            
            if (yeeWork.getIsPrivate() != null) {
                updates.add("isPrivate = ?");
                parameters.add(yeeWork.getIsPrivate());
            }
            
            if (yeeWork.getClassList() != null) {
                updates.add("classList = ?");
                parameters.add(JSON.toJSONString(yeeWork.getClassList()));
            }
            
            if (yeeWork.getTeacherType() != null) {
                updates.add("teacherType = ?");
                parameters.add(yeeWork.getTeacherType());
            }
            
            if (yeeWork.getAllow() != null) {
                updates.add("allow = ?");
                parameters.add(yeeWork.getAllow());
            }
            
            if (yeeWork.getFrequency() != null) {
                updates.add("frequency = ?");
                parameters.add(yeeWork.getFrequency());
            }
            
            if (yeeWork.getScoringRules() != null) {
                updates.add("scoringRules = ?");
                parameters.add(yeeWork.getScoringRules());
            }
            
            if (yeeWork.getHasCollect() != null) {
                updates.add("hasCollect = ?");
                parameters.add(yeeWork.getHasCollect());
            }
            
            if (yeeWork.getLock() != null) {
                updates.add("lock = ?");
                parameters.add(yeeWork.getLock());
            }
            
            if (yeeWork.getSchoolId() != null) {
                updates.add("schoolId = ?");
                parameters.add(yeeWork.getSchoolId());
            }
            
            if (yeeWork.getParsing() != null) {
                updates.add("parsing = ?");
                parameters.add(yeeWork.getParsing());
            }
            
            // 检查是否有可更新的字段
            if (updates.isEmpty()) {
                return Result.error("没有可更新的字段");
            }
            
            // 添加更新字段到SQL
            sqlBuilder.append(String.join(", ", updates));
            sqlBuilder.append(" WHERE id = ?");
            parameters.add(yeeWork.getId());
            
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
                if (yeeWork.getAllow() != null) {
                    if (!queryWork.getAllow().equals(yeeWork.getAllow())) {
                        // 根据 courseId 查询 yee_course_student 是否有学生选课, 如果有的话:根据courseId查询yee_work表中有多少作业数量, ,更新到yee_course_student表的workCount字段 ,没有的话不更新
                        updateWorkCountForCourse(slSchool, yeeWork.getCourseId());
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
            String sql = "SELECT * FROM yee_work WHERE id = ? AND schoolId = ?";
            st = conn.prepareStatement(sql);
            st.setInt(1, id);
            st.setInt(2, schoolId);
            rs = st.executeQuery();

            // 3. 封装结果
            if (rs.next()) {
                YeeWork work = new YeeWork();
                work.setId(rs.getInt("id"));
                work.setUserId(rs.getObject("userId", Integer.class));
                work.setTitle(rs.getString("title"));
                work.setTopicNumber(rs.getObject("topicNumber", Integer.class));
                work.setScore(rs.getObject("score", Integer.class));
                work.setType(rs.getObject("type", Integer.class));
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
                work.setScoringRules(rs.getObject("scoringRules", Integer.class));
                work.setHasCollect(rs.getObject("hasCollect", Integer.class));
                work.setLock(rs.getObject("lock", Integer.class));
                work.setSchoolId(rs.getObject("schoolId", Integer.class));
                work.setParsing(rs.getObject("parsing", Integer.class));
                work.setAddDate(rs.getDate("addDate"));

                return Result.success(work);
            } else {
                return Result.error("未找到指定的作业");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询作业失败：" + e.getMessage());
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
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
     * 构建漏选分值
     * @param questionVO 试题导出VO
     * @return 漏选分值Map
     */
    private List<Integer> buildMissScore(QuestionExportVO questionVO) {

        List<Integer> missScoreList = new ArrayList<>();

        // 添加漏选分值1-5
        if (questionVO.getMissScore1() != null) {
            missScoreList.add(Integer.valueOf(questionVO.getMissScore1()));
        }
        if (questionVO.getMissScore2() != null) {
            missScoreList.add(Integer.valueOf(questionVO.getMissScore2()));
        }
        if (questionVO.getMissScore3() != null) {
            missScoreList.add(Integer.valueOf(questionVO.getMissScore3()));
        }
        if (questionVO.getMissScore4() != null) {
            missScoreList.add(Integer.valueOf(questionVO.getMissScore4()));
        }
        if (questionVO.getMissScore5() != null) {
            missScoreList.add(Integer.valueOf(questionVO.getMissScore5()));
        }

        return missScoreList;
    }

    /**
     * 构建单选题选项
     * @param questionVO 试题导出VO
     * @return 选项列表
     */
    private List<Map<String, Object>> buildOptionsForChoiceQuestion(QuestionExportVO questionVO) {
        List<Map<String, Object>> options = new ArrayList<>();

        if (questionVO.getOptionA() != null) {
            addOptionIfNotEmpty(options, "A", questionVO.getOptionA(), Double.valueOf(questionVO.getScoreRatioA()));
        }
        if (questionVO.getOptionB() != null) {
            addOptionIfNotEmpty(options, "B", questionVO.getOptionB(), Double.valueOf(questionVO.getScoreRatioB()));
        }
        if (questionVO.getOptionC() != null) {
            addOptionIfNotEmpty(options, "C", questionVO.getOptionC(), Double.valueOf(questionVO.getScoreRatioC()));
        }
        if (questionVO.getOptionD() != null) {
            addOptionIfNotEmpty(options, "D", questionVO.getOptionD(), Double.valueOf(questionVO.getScoreRatioD()));
        }
        if (questionVO.getOptionE() != null) {
            addOptionIfNotEmpty(options, "E", questionVO.getOptionE(), Double.valueOf(questionVO.getScoreRatioE()));
        }
        if (questionVO.getOptionF() != null) {
            addOptionIfNotEmpty(options, "F", questionVO.getOptionF(), Double.valueOf(questionVO.getScoreRatioF()));
        }

        return options;
    }

    /**
     * 添加选项（如果非空）
     * @param options 选项列表
     * @param idx 选项索引
     * @param answer 答案
     * @param scale 分值比例
     */
    private void addOptionIfNotEmpty(List<Map<String, Object>> options, String idx, String answer, Double scale) {
        if (answer != null && !answer.trim().isEmpty()) {
            Map<String, Object> option = new HashMap<>();
            option.put("idx", idx);
            option.put("answer", answer.trim());
            option.put("scale", scale != null ? scale : 0);
            option.put("img", ""); // 默认无图片
            options.add(option);
        }
    }

    /**
     * 构建填空题选项
     * @param questionVO 试题导出VO
     * @return 选项列表
     */
    private List<Map<String, Object>> buildOptionsForFillQuestion(QuestionExportVO questionVO) {
        List<Map<String, Object>> options = new ArrayList<>();

        if (questionVO.getOptionA() != null) {
            addFillOptionIfNotEmpty(options, 1, questionVO.getOptionA(), Double.valueOf(questionVO.getScoreRatioA()));
        }
        if (questionVO.getOptionB() != null) {
            addFillOptionIfNotEmpty(options, 2, questionVO.getOptionB(), Double.valueOf(questionVO.getScoreRatioB()));
        }
        if (questionVO.getOptionC() != null) {
            addFillOptionIfNotEmpty(options, 3, questionVO.getOptionC(), Double.valueOf(questionVO.getScoreRatioC()));
        }
        if (questionVO.getOptionD() != null) {
            addFillOptionIfNotEmpty(options, 4, questionVO.getOptionD(), Double.valueOf(questionVO.getScoreRatioD()));
        }
        if (questionVO.getOptionE() != null) {
            addFillOptionIfNotEmpty(options, 5, questionVO.getOptionE(), Double.valueOf(questionVO.getScoreRatioE()));
        }
        if (questionVO.getOptionF() != null) {
            addFillOptionIfNotEmpty(options, 6, questionVO.getOptionF(), Double.valueOf(questionVO.getScoreRatioF()));
        }
        return options;
    }

    /**
     * 添加填空项（如果非空）
     * @param options 选项列表
     * @param idx 选项索引
     * @param answer 答案
     * @param scale 分值比例
     */
    private void addFillOptionIfNotEmpty(List<Map<String, Object>> options, int idx, String answer, Double scale) {
        if (answer != null && !answer.trim().isEmpty()) {
            Map<String, Object> option = new HashMap<>();
            option.put("idx", idx);
            option.put("answer", answer.trim());
            option.put("scale", scale != null ? scale : 0);
            option.put("size", answer.trim().length()); // 默认长度
            options.add(option);
        }
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
        if (type == null) {
            return "";
        }
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
        if (level == null) {
            return "";
        }
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
        if (scoreMode == null) {
            return "";
        }
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
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_work_topic WHERE 1=1");
            StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) FROM yee_work_topic WHERE 1=1");

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
                sqlBuilder.append(" AND workId = ?");
                countSqlBuilder.append(" AND workId = ?");
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
            String optionJson = rs.getString("option");
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

    private Map<String, Object> getStudentFinalScoreByUserIdAndWorkId(int schoolId, Integer userId, Integer workId) throws Exception {
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

            // 2. 构建 SQL：查询学生信息 + 作业最终得分
            String sql = """
            SELECT 
                w.title,
                s.name,
                s.number,
                ws.finalScore,
                w.score AS totalScore
            FROM 
                yee_student s
                LEFT JOIN yee_work_score ws ON ws.userId = s.id
                LEFT JOIN yee_work w on w.id = ws.workId
            WHERE 
                s.id = ? 
                AND ws.workId = ?
            """;

            st = conn.prepareStatement(sql);

            // 3. 设置参数
            st.setLong(1, userId);   // s.id = userId
            st.setInt(2, workId);    // ws.workId = workId

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
                    ", workId=" + workId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    private void updateWorkScore(int schoolId, Integer userId, Integer workId,Integer courseId, BigDecimal manualScore) throws Exception {
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

            // 2. 第一步：查询 yee_work_score 的 id
            String queryIdSql = """
            SELECT id 
            FROM yee_work_score 
            WHERE userId = ? AND workId = ? 
            LIMIT 1
            """;

            st = conn.prepareStatement(queryIdSql);
            st.setLong(1, userId);
            st.setInt(2, workId);

            rs = st.executeQuery();

            Long recordId = null;
            if (rs.next()) {
                recordId = rs.getLong("id");
            }

            closeResultSetAndStatement(rs, st);
            st = null;
            rs = null;

            if (recordId == null) {
//                throw new Exception("未找到对应的作业记录：userId=" + userId + ", workId=" + workId);
                // 记录不存在时，创建新记录而不是抛异常
                String insertSql = """
                INSERT INTO yee_work_score (userId, workId, courseId, finalScore, state, scored)
                VALUES (?, ?, ?, ?, 3, 1)
                """;

                st = conn.prepareStatement(insertSql, java.sql.Statement.RETURN_GENERATED_KEYS);
                st.setLong(1, userId);
                st.setInt(2, workId);
                st.setInt(3, courseId);
                st.setObject(4, manualScore, Types.DECIMAL);

                int rowsInserted = st.executeUpdate();
                if (rowsInserted == 0) {
                    throw new Exception("创建作业分数记录失败：userId=" + userId + ", workId=" + workId);
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
            UPDATE yee_work_score 
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
                throw new Exception("更新作业分数-分数失败，未影响任何记录：recordId=" + recordId);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("更新作业分数-分数失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", workId=" + workId +
                    ", manualScore=" + manualScore, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

//    private void updateWorkRecordScore(int schoolId, Integer userId, Integer workId, BigDecimal recheckScore, Integer teacherId, List<Map<String, Object>> workResult) throws Exception {
//        Connection conn = null;
//        PreparedStatement st = null;
//        ResultSet rs = null;
//
//        try {
//            // 1. 验证学校
//            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//            if (slSchool == null || slSchool.getAllow() == 0) {
//                throw new Exception("学校不存在或未审核");
//            }
//
//            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
//
//            // 2. 第一步：查询 yee_work_record 的 id
//            String queryIdSql = """
//            SELECT id
//            FROM yee_work_record
//            WHERE userId = ? AND workId = ?
//            LIMIT 1
//            """;
//
//            st = conn.prepareStatement(queryIdSql);
//            st.setLong(1, userId);
//            st.setInt(2, workId);
//
//            rs = st.executeQuery();
//
//            Long recordId = null;
//            if (rs.next()) {
//                recordId = rs.getLong("id");
//            }
//
//            closeResultSetAndStatement(rs, st);
//            st = null;
//            rs = null;
//
//            if (recordId == null) {
//                throw new Exception("未找到对应的作业记录：userId=" + userId + ", workId=" + workId);
//            }
//
//            // 汇总一下 workResult 中的 type = 4 或者 type = 5 的 reSubScore 字段 代表的主观题分数,type=1, type=2, type=3 的 studentScore 字段代表 客观题分数, 再把主观题客观题 总的分数加起来
//            BigDecimal subjectiveScore = BigDecimal.ZERO;  // 主观题分数 (type = 4, 5)
//            BigDecimal objectiveScore = BigDecimal.ZERO;   // 客观题分数 (type = 1, 2, 3)
//
//            if (workResult != null) {
//                for (Map<String, Object> workItem : workResult) {
//                    Object typeObj = workItem.get("type");
//                    Integer type = null;
//                    if (typeObj != null) {
//                        if (typeObj instanceof Integer) {
//                            type = (Integer) typeObj;
//                        } else {
//                            type = Integer.parseInt(typeObj.toString());
//                        }
//                    }
//
//                    if (type != null) {
//                        if (type == 4 || type == 5) {  // 主观题
//                            Object reSubScoreObj = workItem.get("reSubScore");
//                            if (reSubScoreObj != null) {
//                                BigDecimal reSubScore = null;
//                                if (reSubScoreObj instanceof BigDecimal) {
//                                    reSubScore = (BigDecimal) reSubScoreObj;
//                                } else if (reSubScoreObj instanceof Number) {
//                                    reSubScore = new BigDecimal(((Number) reSubScoreObj).doubleValue());
//                                } else {
//                                    reSubScore = new BigDecimal(reSubScoreObj.toString());
//                                }
//                                subjectiveScore = subjectiveScore.add(reSubScore);
//                            }
//                        } else if (type >= 1 && type <= 3) {  // 客观题
//                            Object studentScoreObj = workItem.get("studentScore");
//                            if (studentScoreObj != null) {
//                                BigDecimal studentScore = null;
//                                if (studentScoreObj instanceof BigDecimal) {
//                                    studentScore = (BigDecimal) studentScoreObj;
//                                } else if (studentScoreObj instanceof Number) {
//                                    studentScore = new BigDecimal(((Number) studentScoreObj).doubleValue());
//                                } else {
//                                    studentScore = new BigDecimal(studentScoreObj.toString());
//                                }
//                                objectiveScore = objectiveScore.add(studentScore);
//                            }
//                        }
//                    }
//                }
//            }
//
//            // 计算最终总分 = 主观题分数 + 客观题分数
//            BigDecimal calculatedTotalScore = subjectiveScore.add(objectiveScore);
//
//            // 3. 第二步：根据 id 更新 score 字段
//            String updateScoreSql = """
//            UPDATE yee_work_record
//            SET score = ?,
//                teacherId = ?,
//                markTime = ?,
//                state = 3,
//                subScore = ?
//            WHERE id = ?
//            """;
//
//            st = conn.prepareStatement(updateScoreSql);
//            st.setObject(1, calculatedTotalScore, Types.DECIMAL); // 使用计算出的总分 = 主观题分数 + 客观题分数
//            st.setObject(2, teacherId, Types.INTEGER); // 使用 setObject 避免 null 问题
//            st.setObject(3, System.currentTimeMillis() / 1000, Types.BIGINT); // 使用 setObject 避免 null 问题
//            st.setObject(4, subjectiveScore, Types.DECIMAL); // 主观题分数
//            st.setLong(5, recordId);
//
//            int rowsAffected = st.executeUpdate();
//
//            if (rowsAffected == 0) {
//                throw new Exception("更新作业记录分数失败，未影响任何记录：recordId=" + recordId);
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new Exception("更新作业记录分数失败，参数：schoolId=" + schoolId +
//                    ", userId=" + userId +
//                    ", workId=" + workId +
//                    ", recheckScore=" + recheckScore, e);
//        } finally {
//            // 安全关闭资源
//            closeResultSetAndStatement(rs, st);
//            closeConnection(conn);
//        }
//    }
private void updateWorkRecordScore(int schoolId, Integer userId, Integer workId, BigDecimal recheckScore, Integer teacherId, List<Map<String, Object>> workResult) throws Exception {
    Connection conn = null;
    PreparedStatement querySt = null;
    PreparedStatement updateSt = null;
    ResultSet rs = null;

    try {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new Exception("学校不存在或未审核");
        }

        conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
        conn.setAutoCommit(false);

        String querySql = "SELECT id, obScore FROM yee_work_record WHERE userId = ? AND workId = ? LIMIT 1";
        querySt = conn.prepareStatement(querySql);
        querySt.setLong(1, userId);
        querySt.setInt(2, workId);
        rs = querySt.executeQuery();

        Long recordId = null;
        BigDecimal oldObScore = BigDecimal.ZERO;
        if (rs.next()) {
            recordId = rs.getLong("id");
            oldObScore = rs.getBigDecimal("obScore");
        }

        if (recordId == null) {
            throw new Exception("未找到作业记录：userId=" + userId + ", workId=" + workId);
        }

        BigDecimal finalObScore = oldObScore;
        BigDecimal finalSubScore = recheckScore.subtract(oldObScore);
        if (finalSubScore.compareTo(BigDecimal.ZERO) < 0) {
            finalSubScore = BigDecimal.ZERO;
        }

        String updateSql = "UPDATE yee_work_record SET score = ?, obScore = ?, subScore = ?, teacherId = ?, markTime = ?, state = 3 WHERE id = ?";

        updateSt = conn.prepareStatement(updateSql);
        updateSt.setBigDecimal(1, recheckScore);
        updateSt.setBigDecimal(2, finalObScore);
        updateSt.setBigDecimal(3, finalSubScore);
        updateSt.setInt(4, teacherId);
        updateSt.setLong(5, System.currentTimeMillis() / 1000);
        updateSt.setLong(6, recordId);

        int rows = updateSt.executeUpdate();
        if (rows == 0) {
            throw new Exception("更新作业记录失败，recordId=" + recordId);
        }

        conn.commit();

    } catch (Exception e) {
        if (conn != null) {
            try { conn.rollback(); } catch (Exception ignored) {}
        }
        e.printStackTrace();
        throw new Exception("更新作业记录分数失败", e);
    } finally {
        try { if (rs != null) rs.close(); } catch (Exception ignored) {}
        try { if (querySt != null) querySt.close(); } catch (Exception ignored) {}
        try { if (updateSt != null) updateSt.close(); } catch (Exception ignored) {}
        try { if (conn != null) conn.close(); } catch (Exception ignored) {}
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
            UPDATE yee_work_answer 
            SET score = ?, marked = 1, hit = ?
            WHERE userId = ? AND workId = ? AND topicId = ?
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

    private List<Map<String, Object>> getWorkRecordDetailByUserAndWork(
            int schoolId,
            Integer userId,
            Integer workId) throws Exception {

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

            // 3. 构建 SQL：查询作业记录 + 学生信息 + 作业配置信息
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
            SELECT 
                  s.name,
                  s.number,
                  wr.startTime,
                  wr.score,
                  wr.state,
                  w.title,
                  w.topicNumber,
                  w.score AS totalScore,
                  wr.frequency
            FROM 
                yee_work_record wr
                LEFT JOIN yee_student s ON wr.userId = s.id
                LEFT JOIN yee_work w ON w.id = wr.workId
            WHERE 
                wr.userId = ?
                AND wr.workId = ?
            """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setLong(paramIndex++, userId);   // wr.userId
            st.setInt(paramIndex++, workId);    // wr.workId

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
            throw new Exception("根据用户和作业查询记录失败，参数：schoolId=" + schoolId +
                    ", userId=" + userId +
                    ", workId=" + workId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }



    private List<Map<String, Object>> getWorkRecordDetailByUserAndWorkPre(
            int schoolId,
            Integer workId) throws Exception {

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
                  w.type,
                  w.createUserId,
                  w.addTime,
                  w.classList,
                  w.remarks

            FROM 
                yee_work w 
            WHERE 
                w.id = ?
            """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setInt(paramIndex++, workId);    // wr.workId

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
            throw new Exception("根据用户和作业查询记录失败，参数：schoolId=" + schoolId +
                    ", workId=" + workId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    /**
     * 根据课程ID、作业ID、打分状态，查询作业提交记录及评分信息
     *
     */
    private List<Map<String, Object>> getWorkRecordsWithScores(
            int schoolId,
            Integer courseId,
            Integer workId,
            Integer scored) throws Exception {

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

            // 3. 构建动态 SQL：查询作业记录及评分信息
            StringBuilder sqlBuilder = new StringBuilder();

            // --- 第一部分：正常的作业记录 ---
            sqlBuilder.append("""
            SELECT 
                wr.workId,
                wr.userId,
                wr.courseId,
                wr.frequency,
                wr.state,
                COALESCE(ws.submitTime, wr.finishTime) AS submitTime,
                wr.teacherId,
                wr.markTime,
                COALESCE(ws.finalScore, wr.score) AS score,
                ws.finalScore,
                COALESCE(ws.scored, CASE WHEN wr.score IS NOT NULL THEN 1 ELSE 0 END) AS scoredState,
                wr.addDate  
            FROM 
                yee_work_record wr
                LEFT JOIN yee_work_score ws ON wr.userId = ws.userId 
                    AND wr.workId = ws.workId
            WHERE 
                wr.courseId = ?
                AND wr.workId = ?
            
            UNION ALL 
            
            -- --- 第二部分：仅有评分表记录，无作业记录的情况 ---
            SELECT 
                ws.workId,
                ws.userId,
                ? AS courseId,
                0 AS frequency,
                0 AS state,
                ws.submitTime,
                NULL AS teacherId,
                NULL AS markTime,
                ws.finalScore AS score,
                ws.finalScore,
                ws.scored AS scoredState,
                ws.submitTime AS addDate 
            FROM 
                yee_work_score ws
                LEFT JOIN yee_work_record wr ON ws.userId = wr.userId 
                    AND ws.workId = wr.workId
                    AND wr.courseId = ?
            WHERE 
                ws.workId = ?
                AND wr.userId IS NULL 
            """);

            // 条件：scored 打分状态（可选）- 使用 HAVING 过滤聚合/别名结果
            if (scored != null) {
                sqlBuilder.append(" HAVING scoredState = ?");
            }

            sqlBuilder.append(" ORDER BY addDate DESC");

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;

            // 第一部分参数
            st.setLong(paramIndex++, courseId);   // 1. wr.courseId
            st.setLong(paramIndex++, workId);     // 2. wr.workId

            // 第二部分参数
            st.setLong(paramIndex++, courseId);   // 3. SELECT 中的 courseId
            st.setLong(paramIndex++, courseId);   // 4. WHERE wr.courseId (in JOIN)
            st.setLong(paramIndex++, workId);     // 5. WHERE ws.workId

            if (scored != null) {
                st.setInt(paramIndex++, scored); // 6
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
                    if ("scoredState".equals(columnName)) {
                        // 兼容处理：可能是 TinyInt(1) 或 Boolean
                        int val = rs.getInt(i);
                        row.put("scoredState", rs.wasNull() ? 0 : val);
                    } else if ("state".equals(columnName) || "frequency".equals(columnName)) {
                        row.put(columnName, rs.getInt(i));
                    } else {
                        row.put(columnName, rs.getObject(i));
                    }
                }
                result.add(row);
            }

            return result;

        } catch(Exception e) {
            e.printStackTrace();
            throw new Exception("查询作业记录及评分信息失败，参数：courseId=" + courseId +
                    ", workId=" + workId +
                    ", scored=" + scored, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

//    private List<Map<String, Object>> getWorkRecordsWithScores(
//            int schoolId,
//            Integer courseId,
//            Integer workId,
//            Integer scored) throws Exception {
//
//        Connection conn = null;
//        PreparedStatement st = null;
//        ResultSet rs = null;
//        List<Map<String, Object>> result = new ArrayList<>();
//
//        try {
//            // 1. 验证学校
//            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//            if (slSchool == null || slSchool.getAllow() == 0) {
//                throw new Exception("学校不存在或未审核");
//            }
//
//            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
//
//            // 3. 构建动态 SQL：查询作业记录及评分信息
//            StringBuilder sqlBuilder = new StringBuilder();
//            sqlBuilder.append("""
//                SELECT
//                    wr.workId,
//                    wr.userId,
//                    wr.courseId,
//                    wr.frequency,
//                    wr.state,
//                    ws.submitTime,
//                    wr.teacherId,
//                    wr.markTime,
//                    wr.score,
//                    ws.finalScore,
//                    ws.scored AS scoredState
//                FROM
//                    yee_work_record wr
//                    LEFT JOIN yee_work_score ws ON wr.userId = ws.userId
//                        AND wr.workId = ws.workId  -- 推荐加上 workId 关联，避免跨作业匹配
//                WHERE
//                    wr.courseId = ?
//                    AND wr.workId = ?
//                """);
//
//            // 条件：scored 打分状态（可选）
//            if (scored != null) {
//                sqlBuilder.append(" AND ws.scored = ? ");
//            }
//
//            sqlBuilder.append("""
//                ORDER BY
//                    wr.addDate DESC
//                """);
//
//            // 4. 预编译 SQL
//            st = conn.prepareStatement(sqlBuilder.toString());
//
//            // 5. 设置参数
//            int paramIndex = 1;
//            st.setLong(paramIndex++, courseId);
//            st.setLong(paramIndex++, workId);
//
//            if (scored != null) {
//                st.setInt(paramIndex++, scored);
//            }
//
//            // 6. 执行查询
//            rs = st.executeQuery();
//
//            // 7. 封装结果
//            ResultSetMetaData metaData = rs.getMetaData();
//            int columnCount = metaData.getColumnCount();
//
//            while (rs.next()) {
//                Map<String, Object> row = new HashMap<>();
//                for (int i = 1; i <= columnCount; i++) {
//                    String columnName = metaData.getColumnLabel(i);
//                    if (columnName.equals("scoredState")) {
//                        row.put("scoredState", rs.getBoolean("scoredState") == true ? 1 : 0);
//                    } else {
//                        row.put(columnName, rs.getObject(i));
//                    }
//                }
//                result.add(row);
//            }
//
//            return result;
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new Exception("查询作业记录及评分信息失败，参数：courseId=" + courseId +
//                    ", workId=" + workId +
//                    ", scored=" + scored, e);
//        } finally {
//            // 安全关闭资源
//            closeResultSetAndStatement(rs, st);
//            closeConnection(conn);
//        }
//    }

    /**
     * 根据课程ID、班级ID、姓名/学号关键词，查询该课程下的学生信息（名称、班级、学号）
     */
    private List<Map<String, Object>> getWorkDetails(
            Integer schoolId,
            Integer courseId,
            Integer classId,
            String title,
            Integer workId) throws Exception {

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

            // 3. 根据 examId 查询考试信息并获取 classList
            List<Integer> examClassIds = new ArrayList<>();
            String examSql = "SELECT classList FROM yee_work WHERE id = ? AND courseId = ?";
            PreparedStatement examSt = conn.prepareStatement(examSql);
            examSt.setInt(1, workId);
            examSt.setInt(2, courseId);
            ResultSet examRs = examSt.executeQuery();

            if (examRs.next()) {
                String classListStr = examRs.getString("classList");
                if (classListStr != null && !classListStr.trim().isEmpty() && !classListStr.equals("[]")) {
                    // 解析 JSON 数组
                    JSONArray jsonArray = JSON.parseArray(classListStr);
                    for (int i = 0; i < jsonArray.size(); i++) {
                        examClassIds.add(jsonArray.getInteger(i));
                    }
                }
            }

            examRs.close();
            examSt.close();

            // ===================== ✅ 权限信息获取 =====================
            Long teacherId = AuthDataPermissionUtil.getCurrentUserId();
            DataAuth auth = AuthDataPermissionUtil.getCurrentDataAuth();
            // ==========================================================

            // 4. 构建动态 SQL：查询课程下的学生信息（姓名、学号、班级等）
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("""
                SELECT 
                    s.number AS number,
                    s.`name` AS `name`,
                    cc.`name` AS className,
                    s.id
                FROM 
                    yee_course_student cs
                    LEFT JOIN yee_student s ON s.id = cs.studentId
                    LEFT JOIN yee_course_class cc on cc.id = cs.classId
                WHERE 
                    cs.courseId = ?
                """);

            // ===================== ✅ 统一权限控制（核心） =====================
            if (!DataAuth.ALL.equals(auth)) {
                sqlBuilder.append(" AND ( ");
                // 1. 老师是当前班级的责任教师
                sqlBuilder.append(" EXISTS ( ");
                sqlBuilder.append("   SELECT 1 FROM yee_course_class ycc ");
                sqlBuilder.append("   WHERE ycc.id = cs.classId ");
                sqlBuilder.append("     AND ycc.teacherId = ? ");
                sqlBuilder.append(" ) ");
                sqlBuilder.append(" OR ");
                // 2. 老师是课程创建者
                sqlBuilder.append(" EXISTS (SELECT 1 FROM yee_course yc WHERE yc.id = cs.courseId AND yc.createId = ?) ");
                sqlBuilder.append(" ) ");
            }
            // ====================================================================

            // 如果传入了 classId 参数，优先使用传入的 classId 进行过滤
            if (classId != null && classId > 0) {
                sqlBuilder.append(" AND cs.classId = ? ");
            }
            // 如果没有传入 classId 参数，则根据从考试中获取的班级 ID 进行过滤
            else if (!examClassIds.isEmpty()) {
                sqlBuilder.append(" AND cs.classId IN (");
                for (int i = 0; i < examClassIds.size(); i++) {
                    sqlBuilder.append("?");
                    if (i < examClassIds.size() - 1) {
                        sqlBuilder.append(",");
                    }
                }
                sqlBuilder.append(")");
            }

            // 条件：title 支持模糊匹配 name 或 number（学号）
            if (title != null && !title.trim().isEmpty()) {
                String trimmedTitle = "%" + title.trim() + "%";
                sqlBuilder.append(" AND (s.name LIKE ? OR s.number LIKE ?) ");
            }

            sqlBuilder.append("""
                ORDER BY 
                    cs.classId, 
                    s.name
                """);

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setLong(paramIndex++, courseId);

            // ===================== ✅ 注入权限参数 =====================
            if (!DataAuth.ALL.equals(auth)) {
                st.setLong(paramIndex++, teacherId);
                st.setLong(paramIndex++, teacherId);
            }
            // ============================================================

            // 设置 classId 参数：如果传入了 classId，优先使用；否则使用从考试中获取的班级 ID 列表
            if (classId != null && classId > 0) {
                st.setInt(paramIndex++, classId);
            } else {
                for (Integer classIdValue : examClassIds) {
                    st.setInt(paramIndex++, classIdValue);
                }
            }

            if (title != null && !title.trim().isEmpty()) {
                String likeValue = "%" + title.trim() + "%";
                st.setString(paramIndex++, likeValue); // 匹配 name
                st.setString(paramIndex++, likeValue); // 匹配 number
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
            throw new Exception("查询课程下学生信息失败，参数：schoolId=" + schoolId +
                    ", courseId=" + courseId +
                    ", classId=" + classId +
                    ", title=" + title +
                    ", workId=" + workId, e);
        } finally {
            // 安全关闭资源
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }
    /**
     * 导出作业成绩为Excel
     */
    @Override
    public void exportWorkScore(HttpServletResponse response, YeeWorkExportDTO queryDTO) {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            SlSchool slSchool = slSchoolMapper.selectById(queryDTO.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new RuntimeException("学校不存在或未审核");
            }
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            Map<String, Object> workInfo = getWorkInfoByWorkId(queryDTO.getSchoolId(), queryDTO.getWorkId(), queryDTO.getCourseId());
            if (workInfo == null) {
                throw new RuntimeException("未找到作业信息");
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
                LEFT JOIN yee_work_record wr ON wr.userId = ycs.studentId AND wr.workId = ? AND wr.courseId = ycs.courseId
                LEFT JOIN yee_work_score ws ON ws.userId = ycs.studentId AND ws.workId = ?
                WHERE ycs.schoolId = ? AND ycs.courseId = ?
            """);
            List<Object> params = new ArrayList<>();
            params.add(queryDTO.getWorkId());
            params.add(queryDTO.getWorkId());
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
            ResponseExportUtil.setExcelRespProp(response, "学生作业成绩_" + System.currentTimeMillis());
            EasyExcel.write(response.getOutputStream())
                    .head(head)
                    .registerWriteHandler(new OnceAbsoluteMergeStrategy(0, 0, 0, headers.length - 1))
                    .registerWriteHandler(ExcelExportStyles.defaultTitleRow(headers.length))
//                    .registerWriteHandler(ExcelExportStyles.createFreezeAndWidthHandler(new int[]{14, 16, 18, 10, 12}, 2))
//                    .registerWriteHandler(ExcelExportStyles.textColumns(new int[]{0}))
                    .registerWriteHandler(ExcelExportStyles.defaultStyleStrategy())
                    .sheet("作业成绩")
                    .doWrite(dataRows);
            response.getOutputStream().flush();
        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"error\":\"导出作业成绩失败: " + e.getMessage() + "\"}");
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

            String sql = "SELECT w.*, m.name FROM yee_work w LEFT JOIN yee_manage m on w.createUserId = m.id WHERE w.id = ? and w.courseId = ?";
            st = conn.prepareStatement(sql);
            st.setInt(1, workId);
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

            if (resultList == null || resultList.isEmpty()) {
                workInfo = null;
            } else {
                workInfo = (Map<String, Object>) resultList.get(0);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
        return workInfo;
    }
    private Map<String, Object> getWorkInfoByWorkIdAuth(int schoolId, Integer workId, Integer courseId) {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        Map<String, Object> workInfo = new HashMap<>();
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // ===================== ✅ 权限逻辑开始 =====================
            Long teacherId = AuthDataPermissionUtil.getCurrentUserId();
            DataAuth auth = AuthDataPermissionUtil.getCurrentDataAuth();

            // 基础 SQL
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT w.*, m.name FROM yee_work w ");
            sql.append("LEFT JOIN yee_manage m on w.createUserId = m.id ");
            sql.append("WHERE w.id = ? AND w.courseId = ? ");

            // 不是超管 + 不是课程创建者 → 加权限条件
            if (!DataAuth.ALL.equals(auth)) {
                sql.append(" AND ( ");
                // 1. 全部班级可见
                sql.append(" JSON_LENGTH(w.classList) = 0 ");
                sql.append(" OR ");
                // 2. 教师负责的班级
                sql.append(" EXISTS ( ");
                sql.append("   SELECT 1 FROM yee_course_class ycc ");
                sql.append("   WHERE ycc.teacherId = ? ");
                sql.append("   AND JSON_CONTAINS(w.classList, CAST(ycc.id AS JSON)) ");
                sql.append(" ) ");
                sql.append(" OR ");
                // 3. 教师是课程创建者
                sql.append(" EXISTS (SELECT 1 FROM yee_course yc WHERE yc.id = w.courseId AND yc.createId = ?) ");
                sql.append(" ) ");
            }

            // ===================== 拼装参数 =====================
            st = conn.prepareStatement(sql.toString());
            int idx = 1;
            st.setInt(idx++, workId);
            st.setInt(idx++, courseId);

            if (!DataAuth.ALL.equals(auth)) {
                st.setLong(idx++, teacherId);
                st.setLong(idx++, teacherId);
            }

            // ===================== 执行查询 =====================
            rs = st.executeQuery();
            List<Object> resultList = rsToWorkInfo(rs);

            if (resultList != null && !resultList.isEmpty()) {
                Map<String, Object> examData = (Map<String, Object>) resultList.get(0);
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
                    while (classRs.next()) {
                        Integer id = classRs.getInt("id");
                        String className = classRs.getString("name");
                        classIdNameMap.put(id, className);
                    }

                    for (Integer classId : classIds) {
                        String className = classIdNameMap.get(classId);
                        if (className != null) {
                            classNameList.add(className);
                        }
                    }
                    classRs.close();
                    classSt.close();
                } else {
                    classNameList.add("全部班级");
                }
                examData.put("classListDetail", classNameList);
                workInfo = examData;
            } else {
                workInfo = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
        return workInfo;
    }

    private List<Object> getWorkInfoById(int schoolId, Integer nodeId, Integer courseId) {
        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        // 获取作业表信息
        List<Object> workInfo = new ArrayList<>();
        try {
            // 1. 验证学校
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            String sql = "SELECT * FROM yee_work WHERE nodeId = ? and courseId = ?";
            st = conn.prepareStatement(sql);
            st.setInt(1, nodeId);
            st.setInt(2, courseId);
            rs = st.executeQuery();
            List<Object> objects = rsToWorkInfo(rs);
            if (objects == null || objects.isEmpty()) {
                workInfo = null;
            } else {
                workInfo = objects;
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
                workInfo.put("type", rs.getInt("type") == 2 ? "课堂作业" : "自由练习");
                workInfo.put("createUserId", rs.getInt("createUserId"));
                workInfo.put("name", rs.getString("name"));
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

            StringBuilder sqlBuilder = new StringBuilder();
            // ✅ 这里必须加别名 w ！！！
            sqlBuilder.append("SELECT * FROM yee_work_record w ");

            boolean hasCondition = false;
            if (courseId != null) {
                sqlBuilder.append(hasCondition ? " AND " : " WHERE ").append("w.courseId = ?");
                hasCondition = true;
            }
            if (classId != null) {
                sqlBuilder.append(hasCondition ? " AND " : " WHERE ").append("w.classId = ?");
                hasCondition = true;
            }
            if (schoolId > 0) {
                sqlBuilder.append(hasCondition ? " AND " : " WHERE ").append("w.schoolId = ?");
                hasCondition = true;
            }

            // ================== ✅ 正确权限调用（关键修复） ==================
            List<Object> params = new ArrayList<>();
            // 作业记录表 直接用 classId 过滤，不传 work！
            AuthDataPermissionUtil.buildDataPermission(sqlBuilder, params, "w.courseId", "w.classId");
            // =================================================================

            st = conn.prepareStatement(sqlBuilder.toString());

            int parameterIndex = 1;
            if (courseId != null) {
                st.setInt(parameterIndex++, courseId);
            }
            if (classId != null) {
                st.setInt(parameterIndex++, classId);
            }
            if (schoolId > 0) {
                st.setInt(parameterIndex++, schoolId);
            }

            // ✅ 必须设置权限参数
            for (Object param : params) {
                st.setObject(parameterIndex++, param);
            }

            rs = st.executeQuery();

            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", rs.getInt("id"));
                map.put("workId", rs.getInt("workId"));
                map.put("userId", rs.getInt("userId"));
                map.put("state", rs.getInt("state"));
                map.put("classId", rs.getObject("classId"));
                map.put("schoolId", rs.getInt("schoolId"));
                map.put("courseId", rs.getInt("courseId"));
                result.add(map);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询作业记录失败", e);
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
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            StringBuilder sqlBuilder = new StringBuilder();

            sqlBuilder.append("""
                SELECT 
                    w.id,
                    w.title,
                    w.topicNumber,
                    w.score,
                    w.type,
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
                    c.name AS chapterName
                FROM 
                    yee_work w
                    LEFT JOIN yee_node n ON w.nodeId = n.id
                    LEFT JOIN yee_chapter c ON n.chapterId = c.id
                WHERE 
                    w.courseId = ?
                    AND w.allow = 1
                    AND w.schoolId = ?
                """);

            // ===================== ✅ 最终终极权限逻辑 =====================
            Long teacherId = AuthDataPermissionUtil.getCurrentUserId();
            DataAuth auth = AuthDataPermissionUtil.getCurrentDataAuth();

            // 只有 【不是ALL权限】 + 【不是课程创建者】 才需要加班级条件
            if (!DataAuth.ALL.equals(auth)) {
                sqlBuilder.append(" AND ( ");
                // 1. 全部班级可见
                sqlBuilder.append(" JSON_LENGTH(w.classList) = 0 ");
                sqlBuilder.append(" OR ");
                // 2. 老师管理的班级
                sqlBuilder.append(" EXISTS ( ");
                sqlBuilder.append("   SELECT 1 FROM yee_course_class ycc ");
                sqlBuilder.append("   WHERE ycc.teacherId = ").append(teacherId);
                sqlBuilder.append("   AND JSON_CONTAINS(w.classList, CAST(ycc.id AS JSON)) ");
                sqlBuilder.append(" ) ");
                sqlBuilder.append(" OR ");
                // 3. 老师是课程创建者
                sqlBuilder.append(" EXISTS (SELECT 1 FROM yee_course yc WHERE yc.id = w.courseId AND yc.createId = ").append(teacherId).append(") ");
                sqlBuilder.append(" ) ");
            }
            // ==================================================================

            // 标题搜索
            if (title != null && !title.trim().isEmpty()) {
                sqlBuilder.append(" AND w.title LIKE ? ");
            }

            sqlBuilder.append("""
                ORDER BY 
                    n.chapterId, n.sort, w.sequence, w.addTime DESC
                """);

            st = conn.prepareStatement(sqlBuilder.toString());
            int paramIndex = 1;
            st.setLong(paramIndex++, courseId);
            st.setInt(paramIndex++, schoolId);

            if (title != null && !title.trim().isEmpty()) {
                st.setString(paramIndex++, "%" + title.trim() + "%");
            }

            rs = st.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnLabel(i), rs.getObject(i));
                }
                result.add(row);
            }

            return result;

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("查询作业详情失败", e);
        } finally {
            closeResultSetAndStatement(rs, st);
            closeConnection(conn);
        }
    }

    /**
     * 将原始数据转换为章节分组、作业去重的统计结构
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
                            newItem.setType((Integer) row.get("type")); // 2:"作业", 1:"练习"
                            newItem.setEndTime(WorkReportVO.formatTimestamp((Integer)row.get("endTime")));
                            return newItem;
                        });

                        // 统计总人数
                        item.setTotalNum(item.getTotalNum() + 1);

                        // 是否提交 查询 "作业记录表" (yee_work_record) 根据wordId todo


                        // 是否已批改 根据yee_work_record.state 为3代表 已批 根据wordId todo

                    }

                    chapter.setWorks(new ArrayList<>(workMap.values()));
                    return chapter;
                })
                .collect(Collectors.toList());
    }


    /**
     * 获取课程作业统计信息
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
                COUNT(cs.workCount) AS wTotal,                             -- 应交作业总份数（每人×每份）
                COUNT(DISTINCT cs.studentId) AS uTotal,                   -- 选课总人数
                
                -- 总提交次数
                COUNT(wr.id) AS complete,
            
                -- PC / Mobile 提交次数
                SUM(CASE WHEN wr.platform = 'pc' AND wr.state >= 2 THEN 1 ELSE 0 END) AS pcTotal,
                SUM(CASE WHEN wr.platform = 'mobile' AND wr.state >= 2 THEN 1 ELSE 0 END) AS mbTotal,
            
                -- 待批改统计（state = 2）
                COUNT(DISTINCT CASE WHEN wr.state = 2 THEN wr.userId END) AS uOnce2,
                COUNT(CASE WHEN wr.state = 2 THEN 1 END) AS complete2,
                SUM(CASE WHEN wr.platform = 'pc' AND wr.state = 2 THEN 1 ELSE 0 END) AS pcTotal2,
                SUM(CASE WHEN wr.platform = 'mobile' AND wr.state = 2 THEN 1 ELSE 0 END) AS mbTotal2,
            
                -- 真正完成所有作业的学生数（workLearned = workCount）
                COUNT(DISTINCT CASE
                    WHEN cs.workLearned > 0 AND cs.workLearned = cs.workCount
                    THEN cs.studentId
                END) AS uFinished
            
            FROM
                yee_work w
                INNER JOIN yee_course_student cs ON w.courseId = cs.courseId
                LEFT JOIN yee_work_record wr ON w.id = wr.workId AND cs.studentId = wr.userId
            WHERE
                w.courseId = ?                    -- 课程id
                AND w.type = 2                    -- 作业类型
                AND w.isPrivate = 0               -- 公开作业
            """);

            // 动态添加 classId 条件
            boolean hasClassFilter = (classId != null && classId > 0);
            if (hasClassFilter) {
                sqlBuilder.append(" AND cs.classId = ? ");
            }

            sqlBuilder.append(" GROUP BY w.courseId ");

            // 4. 预编译 SQL
            st = conn.prepareStatement(sqlBuilder.toString());

            // 5. 设置参数
            int paramIndex = 1;
            st.setInt(paramIndex++, courseId);
            if (hasClassFilter) {
                st.setInt(paramIndex++, classId);
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
            throw new Exception("查询作业统计失败", e);
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

    @Override
    public Result recoverWork(Integer schoolId, Integer workId) throws Exception {
        Result queryWorkResult = selectById(schoolId, workId);
        YeeWork queryWork = (YeeWork) queryWorkResult.getData();
        if (queryWork == null) {
            return Result.error("作业不存在");
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
            String updateWorkSql = "UPDATE yee_work SET allow = 0 WHERE id = ? AND schoolId = ?";
            st = conn.prepareStatement(updateWorkSql);
            st.setInt(1, workId);
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
            String selectRecordIdsSql = "SELECT id FROM yee_work_record WHERE workId = ?";
            st = conn.prepareStatement(selectRecordIdsSql);
            st.setInt(1, workId);
            rs = st.executeQuery();
            
            List<Integer> recordIds = new ArrayList<>();
            while (rs.next()) {
                recordIds.add(rs.getInt("id"));
            }
            
            closeResultSetAndStatement(rs, st);
            rs = null;
            st = null;
            
            // 4. 删除yee_work_answer表中对应的记录
            if (!recordIds.isEmpty()) {
                // 构建IN查询语句
                String placeholders = String.join(",", Collections.nCopies(recordIds.size(), "?"));
                String deleteAnswerSql = "DELETE FROM yee_work_answer WHERE recordId IN (" + placeholders + ")";
                st = conn.prepareStatement(deleteAnswerSql);
                
                for (int i = 0; i < recordIds.size(); i++) {
                    st.setInt(i + 1, recordIds.get(i));
                }
                
                int answerRows = st.executeUpdate();
                closeStatement(st);
                st = null;
            }

            // 5. 删除yee_work_record表中对应的记录
            String deleteRecordSql = "DELETE FROM yee_work_record WHERE workId = ?";
            st = conn.prepareStatement(deleteRecordSql);
            st.setInt(1, workId);
            int recordRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 6. 删除 yee_work_score 表中对应的记录
            String deleteScoreSql = "DELETE FROM yee_work_score WHERE workId = ?";
            st = conn.prepareStatement(deleteScoreSql);
            st.setInt(1, workId);
            int scoreRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 7. 提交事务
            conn.commit();


            // 根据 courseId 查询 yee_course_student 是否有学生选课, 如果有的话:根据courseId查询yee_work表中有多少作业数量, ,更新到yee_course_student表的workCount字段 ,没有的话不更新
            updateWorkCountForCourse(slSchool, queryWork.getCourseId());


            
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
    public Result deleteWork(Integer schoolId, Integer workId) throws Exception {

        Result queryWorkResult = selectById(schoolId, workId);
        YeeWork queryWork = (YeeWork) queryWorkResult.getData();
        if (queryWork == null) {
            return Result.error("作业不存在");
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
            String selectRecordIdsSql = "SELECT id FROM yee_work_record WHERE workId = ?";
            st = conn.prepareStatement(selectRecordIdsSql);
            st.setInt(1, workId);
            rs = st.executeQuery();
            
            List<Integer> recordIds = new ArrayList<>();
            while (rs.next()) {
                recordIds.add(rs.getInt("id"));
            }
            
            closeResultSetAndStatement(rs, st);
            rs = null;
            st = null;
            
            // 3. 删除yee_work_answer表中对应的记录
            if (!recordIds.isEmpty()) {
                // 构建IN查询语句
                String placeholders = String.join(",", Collections.nCopies(recordIds.size(), "?"));
                String deleteAnswerSql = "DELETE FROM yee_work_answer WHERE recordId IN (" + placeholders + ")";
                st = conn.prepareStatement(deleteAnswerSql);
                
                for (int i = 0; i < recordIds.size(); i++) {
                    st.setInt(i + 1, recordIds.get(i));
                }
                
                int answerRows = st.executeUpdate();
                closeStatement(st);
                st = null;
            }

            // 4. 删除yee_work_record表中对应的记录
            String deleteRecordSql = "DELETE FROM yee_work_record WHERE workId = ?";
            st = conn.prepareStatement(deleteRecordSql);
            st.setInt(1, workId);
            int recordRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 5. 删除yee_work_topic表中对应的记录
            String deleteTopicSql = "DELETE FROM yee_work_topic WHERE workId = ?";
            st = conn.prepareStatement(deleteTopicSql);
            st.setInt(1, workId);
            int topicRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 6. 删除yee_work表中的记录
            String deleteWorkSql = "DELETE FROM yee_work WHERE id = ? AND schoolId = ?";
            st = conn.prepareStatement(deleteWorkSql);
            st.setInt(1, workId);
            st.setInt(2, schoolId);
            int workRows = st.executeUpdate();
            
            closeStatement(st);
            st = null;
            
            // 检查是否找到了对应的作业
            if (workRows == 0) {
                conn.rollback();
                return Result.error("未找到对应的作业或作业不属于该学校");
            }

            // 6. 删除 yee_work_score 表中对应的记录
            String deleteScoreSql = "DELETE FROM yee_work_score WHERE workId = ?";
            st = conn.prepareStatement(deleteScoreSql);
            st.setInt(1, workId);
            int scoreRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 7. 提交事务
            conn.commit();

            // 更新作业数量
            updateWorkCountForCourse(slSchool, queryWork.getCourseId());

            Map<String, Object> result = new HashMap<>();
            result.put("workRows", workRows);
            result.put("recordRows", recordRows);
            result.put("topicRows", topicRows);
            result.put("message", "删除成功，已删除作业及相关记录");
            
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
    public Result redoWork(Integer schoolId, Integer workId, Integer userId) throws Exception {
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

            // 2. 查询需要删除的yee_work_record记录的ID（根据作业ID和用户ID）
            String selectRecordIdsSql;
            if (userId != null && userId > 0) {
                // 指定学生的作业记录
                selectRecordIdsSql = "SELECT id FROM yee_work_record WHERE workId = ? AND userId = ?";
                st = conn.prepareStatement(selectRecordIdsSql);
                st.setInt(1, workId);
                st.setInt(2, userId);
            } else {
                // 所有学生的作业记录
                selectRecordIdsSql = "SELECT id FROM yee_work_record WHERE workId = ?";
                st = conn.prepareStatement(selectRecordIdsSql);
                st.setInt(1, workId);
            }

            rs = st.executeQuery();

            List<Integer> recordIds = new ArrayList<>();
            while (rs.next()) {
                recordIds.add(rs.getInt("id"));
            }

            closeResultSetAndStatement(rs, st);
            rs = null;
            st = null;

            // 3. 删除yee_work_answer表中对应的记录
            if (!recordIds.isEmpty()) {
                // 构建IN查询语句
                String placeholders = String.join(",", Collections.nCopies(recordIds.size(), "?"));
                String deleteAnswerSql = "DELETE FROM yee_work_answer WHERE recordId IN (" + placeholders + ")";
                st = conn.prepareStatement(deleteAnswerSql);

                for (int i = 0; i < recordIds.size(); i++) {
                    st.setInt(i + 1, recordIds.get(i));
                }

                int answerRows = st.executeUpdate();
                closeStatement(st);
                st = null;
            }

            // 4. 删除yee_work_record表中对应的记录
            String deleteRecordSql;
            if (userId != null && userId > 0) {
                deleteRecordSql = "DELETE FROM yee_work_record WHERE workId = ? AND userId = ?";
                st = conn.prepareStatement(deleteRecordSql);
                st.setInt(1, workId);
                st.setInt(2, userId);
            } else {
                deleteRecordSql = "DELETE FROM yee_work_record WHERE workId = ?";
                st = conn.prepareStatement(deleteRecordSql);
                st.setInt(1, workId);
            }

            int recordRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // 5. 删除 yee_work_score 表中对应的记录
            String deleteScoreSql = "DELETE FROM yee_work_score WHERE workId = ? AND userId = ?";
            st = conn.prepareStatement(deleteScoreSql);
            st.setInt(1, workId);
            st.setInt(2, userId);
            int scoreRows = st.executeUpdate();
            closeStatement(st);
            st = null;

            // ========== 新增逻辑：更新yee_course_student表的workLearned字段 ==========
            if (userId != null && userId > 0) {
                // 5.1 查询该作业所属的课程ID（需要根据实际业务补充，假设通过workId能关联到courseId）
                // 注意：这里需要你根据实际数据库结构调整查询语句，确保能获取到作业对应的courseId
                String selectCourseIdSql = "SELECT courseId FROM yee_work WHERE id = ?";
                st = conn.prepareStatement(selectCourseIdSql);
                st.setInt(1, workId);
                rs = st.executeQuery();
                Integer courseId = null;
                if (rs.next()) {
                    courseId = rs.getInt("courseId");
                }
                closeResultSetAndStatement(rs, st);
                rs = null;
                st = null;

                // 5.2 如果能获取到课程ID，更新workLearned（减1，且保证不小于0）
                if (courseId != null) {
                    String updateWorkLearnedSql =
                            "UPDATE yee_course_student SET workLearned = GREATEST(workLearned - 1, 0) " +
                                    "WHERE courseId = ? AND studentId = ?";
                    st = conn.prepareStatement(updateWorkLearnedSql);
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
            result.put("message", "打回重做成功，已删除作业记录和答案，学生可重新答题");

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

            // 3. 查询该课程涉及的 yee_exam 表中的所有作业信息
            String selectExamSql = "SELECT * FROM yee_work WHERE courseId = ? AND schoolId = ? AND allow = 1";
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
                        
            // 如果没有作业记录 (examList 为空),则需要将所有班级的 workCount 设置为 0
            if (examList.isEmpty()) {
                // 查询该课程下所有班级
                String selectAllClassesSql = "SELECT id FROM yee_course_class WHERE courseId = ?";
                st = workCountConn.prepareStatement(selectAllClassesSql);
                st.setInt(1, courseId);
                rs = st.executeQuery();
                            
                while (rs.next()) {
                    Integer classId = rs.getInt("id");
                                
                    String updateSql = "UPDATE yee_course_student SET workCount = 0 WHERE courseId = ? AND classId = ?";
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
                // 有作业记录，按原有逻辑处理
                for (Map.Entry<Integer, Integer> entry : classExamCounts.entrySet()) {
                    Integer classId = entry.getKey();
                    Integer count = entry.getValue();
                                
                    String updateSql = "UPDATE yee_course_student SET workCount = ? WHERE courseId = ? AND classId = ?";
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
            System.err.println("更新课程作业数量失败: " + e.getMessage());
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

}
