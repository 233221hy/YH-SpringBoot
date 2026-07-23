package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeePaperTopic;
import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import cn.xfywz.guozespring.entity.vo.QuestionExportVO;
import cn.xfywz.guozespring.excel.ExcelExportStyles;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeePaperTopicService;
import cn.xfywz.guozespring.util.JsonUtil;
import cn.xfywz.guozespring.util.ParseJsonUtil;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @Author: ChengLin
 */
@Service
public class YeePaperTopicServiceImpl implements YeePaperTopicService {

    private static final Logger logger = LoggerFactory.getLogger(YeePaperTopicServiceImpl.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final ParseJsonUtil parseJsonUtil = new ParseJsonUtil();

    private static final JsonUtil jsonUtil = new JsonUtil();



    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result selectAll(int schoolId, Integer pageSize, Integer pageNum, Integer paperId, String topic, Integer type, Integer level, Integer cateBid, Integer cateMid)  throws Exception{

        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 获取连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        // 3. 构建 SQL
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_paper_topic WHERE schoolId = ?");
        StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) FROM yee_paper_topic WHERE schoolId = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(schoolId);

        if (topic != null && !topic.trim().isEmpty()) {
            String likeValue = "%" + topic.trim() + "%";
            sqlBuilder.append(" AND (topic LIKE ? OR title LIKE ?)");
            countSqlBuilder.append(" AND (topic LIKE ? OR title LIKE ?)");
            parameters.add(likeValue);
            parameters.add(likeValue);
        }
        if (paperId != null) {
            sqlBuilder.append(" AND paperId = ?");
            countSqlBuilder.append(" AND paperId = ?");
            parameters.add(paperId);
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

        // 分页参数
        int size = pageSize != null && pageSize > 0 ? pageSize : 10;
        int page = pageNum != null && pageNum >= 1 ? pageNum : 1;
        int offset = (page - 1) * size;

        // 主查询 SQL 加分页
        sqlBuilder.append(" ORDER BY number ASC LIMIT ? OFFSET ?");
        parameters.add(size);
        parameters.add(offset);

        // 拆分参数：count SQL 不需要分页参数
        List<Object> countParams = parameters.subList(0, parameters.size() - 2); // 前 n-2 个
        List<Object> queryParams = parameters; // 全部参数

        try (connection;
             PreparedStatement countSt = connection.prepareStatement(countSqlBuilder.toString());
             PreparedStatement st = connection.prepareStatement(sqlBuilder.toString())) {

            // 为 countSt 设置参数（不含分页）
            for (int i = 0; i < countParams.size(); i++) {
                countSt.setObject(i + 1, countParams.get(i));
            }

            // 为 st 设置参数（含分页）
            for (int i = 0; i < queryParams.size(); i++) {
                st.setObject(i + 1, queryParams.get(i));
            }

            // 执行 count 查询
            long total = 0;
            try (ResultSet rs = countSt.executeQuery()) {
                if (rs.next()) {
                    total = rs.getLong(1);
                }
            }

            // 执行主查询
            try (ResultSet rs = st.executeQuery()) {
                List<YeePaperTopic> list = mapPaperTopics(rs);
                return Result.success(list, total);
            }

        } catch (SQLException e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result add(YeePaperTopic topic) throws Exception {
        Integer schoolId = topic.getSchoolId();
        Integer paperId = topic.getPaperId();
        Integer score = topic.getScore(); // 单题分数

        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        try {
            connection.setAutoCommit(false); // 开启事务

            // 3. 检查是否已存在同名题目（可选）
//            String checkSql = "SELECT COUNT(*) FROM yee_paper_topic WHERE paperId = ? AND title = ? AND schoolId = ?";
//            try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
//                checkSt.setObject(1, paperId);
//                checkSt.setObject(2, topic.getTitle());
//                checkSt.setObject(3, schoolId);
//                try (ResultSet rs = checkSt.executeQuery()) {
//                    if (rs.next() && rs.getLong(1) > 0) {
//                        connection.rollback(); // 可选：提前回滚
//                        return Result.error("该试卷中已存在同名题目");
//                    }
//                }
//            }

            // 4. 获取当前试卷最大 number，用于自增
            String maxSql = "SELECT MAX(number) FROM yee_paper_topic WHERE paperId = ?";
            int newNumber = 1;
            try (PreparedStatement maxSt = connection.prepareStatement(maxSql)) {
                maxSt.setObject(1, paperId);
                try (ResultSet rs = maxSt.executeQuery()) {
                    if (rs.next()) {
                        Integer maxNum = rs.getInt(1);
                        if (rs.wasNull()) maxNum = 0;
                        newNumber = maxNum + 1;
                    }
                }
            }
            topic.setNumber(newNumber);

            // 5. 插入题目
            String insertSql = """
            INSERT INTO yee_paper_topic 
            (oid, topic, type, level, score, missScore, option1, option2, option3, analysis, 
             pid, paperId, title, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid, number)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (PreparedStatement st = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                st.setObject(1, topic.getOid());
                st.setObject(2, topic.getTopic());
                st.setObject(3, topic.getType());
                st.setObject(4, topic.getLevel());
                st.setObject(5, score);
                st.setObject(6, JsonUtil.toJson(topic.getMissScore()));
                st.setObject(7, JsonUtil.toJson(topic.getOption1()));
                st.setObject(8, JsonUtil.toJson(topic.getOption2()));
                st.setObject(9, JsonUtil.toJson(topic.getOption3()));
                st.setObject(10, topic.getAnalysis());
                st.setObject(11, topic.getPid());
                st.setObject(12, paperId);
                st.setObject(13, topic.getTitle());
                st.setObject(14, JsonUtil.toJson(topic.getUpload()));
                st.setObject(15, JsonUtil.toJson(topic.getOption()));
                st.setObject(16, topic.getScoreMode());
                st.setObject(17, schoolId);
                st.setObject(18, JsonUtil.toJson(topic.getCategoryId()));
                st.setObject(19, topic.getCateBid());
                st.setObject(20, topic.getCateMid());
                st.setObject(21, newNumber);

                int rows = st.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("新增题目失败");
                }

                Integer newId = null;
                try (ResultSet rs = st.getGeneratedKeys()) {
                    if (rs.next()) {
                        newId = rs.getInt(1);
                    } else {
                        throw new SQLException("新增题目失败，未获取到主键");
                    }
                }

                // 6. 更新 yee_paper 表：topicNumber + 1, score += 当前题分数
                String updatePaperSql = """
                UPDATE yee_paper 
                SET topicNumber = COALESCE(topicNumber, 0) + 1,
                    score = COALESCE(score, 0) + ?
                WHERE id = ? AND schoolId = ?
                """;

                try (PreparedStatement updateSt = connection.prepareStatement(updatePaperSql)) {
                    updateSt.setObject(1, score != null ? score : 0);
                    updateSt.setObject(2, paperId);
                    updateSt.setObject(3, schoolId);

                    int paperRows = updateSt.executeUpdate();
                    if (paperRows == 0) {
                        throw new SQLException("更新试卷统计失败：试卷不存在或不属于该学校");
                    }
                }

                // 提交事务
                connection.commit();

                return Result.success("题目新增成功", newId);
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            e.printStackTrace();
            return Result.error("新增题目失败: " + e.getMessage());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close(); // 归还连接
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public Result update(YeePaperTopic topic) throws Exception {
        Integer schoolId = topic.getSchoolId();
        Integer topicId = topic.getId();
        Integer newScore = topic.getScore(); // 新的分值

        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 获取连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        try {
            connection.setAutoCommit(false);

            // 3. 检查题目是否存在，并获取旧分值
            String checkSql = "SELECT score, paperId FROM yee_paper_topic WHERE id = ? AND schoolId = ?";
            Integer oldScore = null;
            Integer paperId = null;

            try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
                checkSt.setObject(1, topicId);
                checkSt.setObject(2, schoolId);
                try (ResultSet rs = checkSt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("题目不存在");
                    }
                    oldScore = rs.getObject("score", Integer.class);
                    paperId = rs.getObject("paperId", Integer.class);
                }
            }

            // 如果 paperId 为空，无法更新试卷，报错
            if (paperId == null) {
                return Result.error("题目所属试卷信息缺失");
            }

            // 4. 执行题目更新
            String updateSql = """
            UPDATE yee_paper_topic SET 
            topic = ?, type = ?, level = ?, score = ?, missScore = ?, 
            option1 = ?, option2 = ?, option3 = ?, analysis = ?, pid = ?, 
            title = ?, upload = ?, `option` = ?, scoreMode = ?, 
            categoryId = ?, cateBid = ?, cateMid = ?, number = ?
            WHERE id = ? AND schoolId = ?
            """;

            try (PreparedStatement st = connection.prepareStatement(updateSql)) {
                int index = 1;
                st.setObject(index++, topic.getTopic());
                st.setObject(index++, topic.getType());
                st.setObject(index++, topic.getLevel());
                st.setObject(index++, newScore);
                st.setObject(index++, JsonUtil.toJson(topic.getMissScore()));
                st.setObject(index++, JsonUtil.toJson(topic.getOption1()));
                st.setObject(index++, JsonUtil.toJson(topic.getOption2()));
                st.setObject(index++, JsonUtil.toJson(topic.getOption3()));
                st.setObject(index++, topic.getAnalysis());
                st.setObject(index++, topic.getPid());
                st.setObject(index++, topic.getTitle());
                st.setObject(index++, topic.getUpload());
                st.setObject(index++, JsonUtil.toJson(topic.getOption()));
                st.setObject(index++, topic.getScoreMode());
                st.setObject(index++, JsonUtil.toJson(topic.getCategoryId()));
                st.setObject(index++, topic.getCateBid());
                st.setObject(index++, topic.getCateMid());
                st.setObject(index++, topic.getNumber());
                st.setObject(index++, topicId);
                st.setObject(index++, schoolId);

                int rows = st.executeUpdate();
                if (rows == 0) {
                    connection.rollback();
                    return Result.error("更新失败");
                }
            }

            // 5. 更新试卷总分（仅当分数变化时）
            if (newScore != null && oldScore != null && !newScore.equals(oldScore)) {
                String updatePaperSql = """
                UPDATE yee_paper 
                SET score = COALESCE(score, 0) + ?
                WHERE id = ? AND schoolId = ?
                """;

                try (PreparedStatement updateSt = connection.prepareStatement(updatePaperSql)) {
                    updateSt.setObject(1, newScore - oldScore); // 增量更新
                    updateSt.setObject(2, paperId);
                    updateSt.setObject(3, schoolId);

                    int paperRows = updateSt.executeUpdate();
                    if (paperRows == 0) {
                        throw new SQLException("更新试卷总分失败：试卷不存在或不属于该学校");
                    }
                }
            }

            // 提交事务
            connection.commit();
            return Result.success("更新成功");

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            e.printStackTrace();
            return Result.error("更新题目失败: " + e.getMessage());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public Result delete(int schoolId, int id) throws Exception {
        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 获取连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        try {
            connection.setAutoCommit(false);

            // 3. 查询题目信息（获取 score 和 paperId）
            String querySql = "SELECT score, paperId FROM yee_paper_topic WHERE id = ? AND schoolId = ?";
            Integer topicScore = null;
            Integer paperId = null;

            try (PreparedStatement querySt = connection.prepareStatement(querySql)) {
                querySt.setObject(1, id);
                querySt.setObject(2, schoolId);
                try (ResultSet rs = querySt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("题目不存在");
                    }
                    topicScore = rs.getObject("score", Integer.class);
                    paperId = rs.getObject("paperId", Integer.class);
                }
            }

            if (paperId == null) {
                return Result.error("题目所属试卷信息缺失");
            }

            // 4. 删除题目
            String deleteSql = "DELETE FROM yee_paper_topic WHERE id = ? AND schoolId = ?";
            try (PreparedStatement st = connection.prepareStatement(deleteSql)) {
                st.setObject(1, id);
                st.setObject(2, schoolId);

                int rows = st.executeUpdate();
                if (rows == 0) {
                    connection.rollback();
                    return Result.error("删除失败");
                }
            }

            // 5. 更新试卷：题目数 -1，总分减去该题分数
            String updatePaperSql = """
            UPDATE yee_paper 
            SET topicNumber = COALESCE(topicNumber, 0) - 1,
                score = COALESCE(score, 0) - ?
            WHERE id = ? AND schoolId = ?
            """;

            try (PreparedStatement updateSt = connection.prepareStatement(updatePaperSql)) {
                updateSt.setObject(1, topicScore != null ? topicScore : 0);
                updateSt.setObject(2, paperId);
                updateSt.setObject(3, schoolId);

                int paperRows = updateSt.executeUpdate();
                if (paperRows == 0) {
                    throw new SQLException("更新试卷统计失败：试卷不存在或不属于该学校");
                }
            }

            // 提交事务
            connection.commit();
            return Result.success("删除成功");

        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                e.addSuppressed(rollbackEx);
            }
            e.printStackTrace();
            return Result.error("删除题目失败: " + e.getMessage());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Result getById(int schoolId, int id) throws Exception {
        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            String sql = "SELECT * FROM yee_paper_topic WHERE id = ? AND schoolId = ?";
            st = connection.prepareStatement(sql);
            st.setObject(1, id);
            st.setObject(2, schoolId);

            rs = st.executeQuery();
            if (rs.next()) {
                YeePaperTopic topic = new YeePaperTopic();

                topic.setId(rs.getInt("id"));
                topic.setOid(rs.getInt("oid"));
                topic.setTopic(rs.getString("topic"));
                topic.setType(rs.getInt("type"));
                topic.setLevel(rs.getInt("level"));
                topic.setScore(rs.getInt("score"));

                topic.setOption(JsonUtil.parseOption(rs.getString("option")));

                topic.setAnalysis(rs.getString("analysis"));
                topic.setPid(rs.getInt("pid"));
                topic.setPaperId(rs.getInt("paperId"));
                topic.setTitle(rs.getString("title"));
                topic.setUpload(rs.getString("upload"));
                topic.setScoreMode(rs.getInt("scoreMode"));
                topic.setSchoolId(rs.getInt("schoolId"));
                topic.setCateBid(rs.getInt("cateBid"));
                topic.setCateMid(rs.getInt("cateMid"));
                topic.setNumber(rs.getInt("number"));

                return Result.success(topic);
            } else {
                return Result.error("题目不存在或不属于该学校");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return Result.error("数据库查询失败：" + e.getMessage());
        } finally {
            // 安全关闭资源
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.close(); // 归还连接
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Result sortByNumber(int schoolId, int id1, int id2, int number1, int number2) throws Exception {
        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        PreparedStatement st = null;
        try {
            connection.setAutoCommit(false); // 开启事务

            String updateSql = """
            UPDATE yee_paper_topic 
            SET number = ? 
            WHERE id = ? AND schoolId = ?
            """;

            st = connection.prepareStatement(updateSql);

            // 第一次更新：id1 的 number 改为 number2
            st.setObject(1, number2);
            st.setObject(2, id1);
            st.setObject(3, schoolId);
            int rows1 = st.executeUpdate();

            if (rows1 == 0) {
                connection.rollback();
                return Result.error("更新失败：题目 " + id1 + " 不存在或不属于该学校");
            }

            // 第二次更新：id2 的 number 改为 number1
            st.setObject(1, number1);
            st.setObject(2, id2);
            st.setObject(3, schoolId);
            int rows2 = st.executeUpdate();

            if (rows2 == 0) {
                connection.rollback();
                return Result.error("更新失败：题目 " + id2 + " 不存在或不属于该学校");
            }

            // 两个更新都成功，提交事务
            connection.commit();
            return Result.success("排序更新成功");

        } catch (SQLException e) {
            // 发生异常，回滚
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
            return Result.error("数据库操作失败：" + e.getMessage());
        } finally {
            // 🧹 清理资源
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // 恢复默认
                    connection.close(); // 归还连接
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void exportQuestions(HttpServletResponse response, Integer schoolId, String topic, Integer createId, Integer type, Integer level, Integer cateBid, Integer cateMid, Integer paperId) throws Exception {
        OutputStream outputStream = null;
        ExcelWriter excelWriter = null;
        try {
            logger.info("开始导出试题数据: schoolId={}, topic={}, createId={}, type={}, level={}, cateBid={}, cateMid={}",
                    schoolId, topic, createId, type, level, cateBid, cateMid);

            // 1. 设置响应头
            LocalDateTime now = LocalDateTime.now();
            String fileName = "试卷题目导出_" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));

            logger.info("设置响应头完成，文件名: {}", fileName);

            // 2. 获取输出流
            outputStream = response.getOutputStream();

            logger.info("获取输出流成功，准备创建ExcelWriter");

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
                            connection, schoolId, pageSize, pageNum, topic, createId, type, level, cateBid, cateMid, paperId);

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

                    logger.info("获取到第{}页{}条试题数据，开始转换为导出VO对象", pageNum, questions.size());

                    // 转换为导出VO对象
                    List<QuestionExportVO> exportList = convertToExportVO(questions);

                    logger.info("开始写入第{}页{}条数据到Excel", pageNum, exportList.size());

                    // 写入数据到Excel（流式写入）
                    excelWriter.write(exportList, writeSheet);

                    // 测试执行时间
                    long endTime = System.currentTimeMillis();
                    logger.info("执行时间：{}ms", endTime - startTime);

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

            logger.info("导出完成，总共导出{}条数据", totalExported);

        } catch (Exception e) {
            logger.error("导出试题数据时发生异常", e);
            throw e;
        } finally {
            // 确保ExcelWriter正确关闭，这会自动刷新和关闭输出流
            if (excelWriter != null) {
                try {
                    logger.info("开始关闭ExcelWriter");
                    excelWriter.finish();
                    logger.info("ExcelWriter关闭完成");
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
            // 注意：不需要手动刷新或关闭outputStream，因为excelWriter.finish()已经处理了
            logger.info("导出操作完成");
        }
    }

    @Override
    public Result importQuestions(Integer schoolId, Integer createId, MultipartFile file, Integer cateBid, Integer cateMid, Integer paperId) throws Exception {
        // 验证参数
        if (schoolId <= 0) {
            return Result.error("学校ID无效");
        }
        if (createId <= 0) {
            return Result.error("创建人ID无效");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("请选择要导入的Excel文件");
        }

        if (cateBid == null) {
            return Result.error("请选择学科大类");
        }
        if (cateMid == null) {
            return Result.error("请选择学科小类");
        }

        // 验证学校是否存在且已审核
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        try {
            // 开启事务
            connection.setAutoCommit(false);

            // 解析Excel文件
            List<QuestionExportVO> questionList = EasyExcel.read(file.getInputStream())
                    .head(QuestionExportVO.class)
                    .sheet()
                    .doReadSync();

            if (questionList == null || questionList.isEmpty()) {
                return Result.error("Excel文件中没有数据");
            }

            // 转换并保存试题数据
            int successCount = 0;
            int failCount = 0;
            Double totalScore = 0.00;
            List<String> errorMessages = new ArrayList<>();

            for (int i = 0; i < questionList.size(); i++) {
                QuestionExportVO questionVO = questionList.get(i);
                try {
                    // 数据验证
                    String validationError = validateQuestionData(questionVO, i + 1);
                    if (validationError != null) {
                        failCount++;
                        errorMessages.add("第" + (i + 1) + "行数据验证失败: " + validationError + "VO为:" + questionVO);
                        continue;
                    }

                    // 转换VO到实体
                    YeePaperTopic question = convertToYeeQuestion(questionVO, schoolId, createId, cateBid, cateMid, paperId, i + 1);

                    // 插入数据库
                    if (add(connection, question)) {
                        successCount++;
                        totalScore += question.getScore();
                    } else {
                        failCount++;
                        errorMessages.add("第" + (i + 1) + "行数据保存失败" + "VO为:" + questionVO);
                    }
                } catch (Exception e) {
                    failCount++;
                    errorMessages.add("第" + (i + 1) + "行数据转换或保存异常: " + "VO为:" + questionVO + e.getMessage());
                }
            }

            // 提交事务
            connection.commit();

            // 根据paperId 查询试卷最新题目列表状态
            List<YeePaperTopic> paperTopicList = selectListByPaperId(connection, paperId);

            // 获取paperTopicList 中的 topicNumber总和 和 score总和
            int topicNumber = paperTopicList.size();
            double score = paperTopicList.stream().mapToDouble(YeePaperTopic::getScore).sum();

            // 更新试卷yee_paper
            updatePaper(connection, paperId, topicNumber, score);

            // 返回结果
            if (failCount == 0) {
                logger.info("成功导入{}条数据, 总分:{}", successCount, totalScore);
                return Result.success("成功导入" + successCount + "条试题数据, " + "总分:" + totalScore);
            } else {
                return Result.success("成功导入" + successCount + "条试题数据，失败" + failCount + "条。错误信息：" + String.join("；", errorMessages));
            }


        } catch (Exception e) {
            // 回滚事务
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }

            e.printStackTrace();
            return Result.error("Excel文件解析或数据导入失败: " + e.getMessage());
        } finally {
            // 恢复并关闭数据库连接
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("关闭连接失败: " + e.getMessage());
            }
        }
    }

    private void updatePaper(Connection connection, Integer paperId, int topicNumber, double score) {
        if (connection == null || paperId == null) {
            return;
        }

        String sql = "UPDATE yee_paper SET topicNumber = ?, score = ? WHERE id = ?";
        
        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setObject(1, topicNumber);
            st.setObject(2, score);
            st.setObject(3, paperId);
            
            st.executeUpdate();
        } catch (SQLException e) {
            System.err.println("更新试卷信息失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<YeePaperTopic> selectListByPaperId(Connection connection, Integer paperId) {
        if (connection == null || paperId == null) {
            return new ArrayList<>();
        }

        String sql = "SELECT * FROM yee_paper_topic WHERE paperId = ? ORDER BY number ASC";
        List<YeePaperTopic> result = new ArrayList<>();

        try (PreparedStatement st = connection.prepareStatement(sql)) {
            st.setObject(1, paperId);
            
            try (ResultSet rs = st.executeQuery()) {
                result = mapPaperTopics(rs);
            }
        } catch (SQLException e) {
            System.err.println("查询试卷题目列表失败: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
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

    /**
     * 分页查询试题数据
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
    public Result selectAll(Connection connection, int schoolId, Integer pageSize, Integer pageNum,
                            String topic, Integer createId, Integer type,
                            Integer level, Integer cateBid, Integer cateMid, Integer paperId) throws Exception {
        try {
            // 3. 基础 SQL
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_paper_topic WHERE 1=1");
            StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) FROM yee_paper_topic WHERE 1=1");

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
                sqlBuilder.append(" AND paperId = ?");
                countSqlBuilder.append(" AND paperId = ?");
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

    /**
     * 添加试题（使用已有的数据库连接）
     * @param connection 数据库连接
     * @param yeeQuestion 试题实体
     * @return 是否添加成功
     * @throws Exception 数据库操作异常
     */
    public boolean add(Connection connection, YeePaperTopic yeeQuestion) throws Exception {
        // 1. 参数校验
        if (yeeQuestion == null) {
            throw new Exception("试题数据不能为空");
        }

        // 可选：更细校验
        if (StringUtils.isEmpty(yeeQuestion.getTopic())) {
            throw new Exception("题目内容不能为空");
        }
        if (yeeQuestion.getType() == null) {
            throw new Exception("题型不能为空");
        }
        if (yeeQuestion.getScore() == null || yeeQuestion.getScore() < 0) {
            throw new Exception("分数必须大于等于0");
        }

        // 2. 插入 SQL（addDate 是生成列，不插入）
        String sql = """
            INSERT INTO yee_paper_topic (
                topic, type, level, score, missScore, analysis, 
                pid, paperId, title, oid, upload, `option`, scoreMode, 
                schoolId, categoryId, cateBid, cateMid, number
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        // 使用 try-with-resources 确保资源释放
        try (PreparedStatement st = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            // 设置参数
            int index = 1;
            st.setObject(index++, yeeQuestion.getTopic());
            st.setObject(index++, yeeQuestion.getType());
            st.setObject(index++, yeeQuestion.getLevel());
            st.setObject(index++, yeeQuestion.getScore());
            st.setObject(index++, toJsonSafe(yeeQuestion.getMissScore())); // 安全序列化
            st.setObject(index++, yeeQuestion.getAnalysis());
            st.setObject(index++, yeeQuestion.getPid());
            st.setObject(index++, yeeQuestion.getPaperId());
            st.setObject(index++, yeeQuestion.getTitle());
            st.setObject(index++, yeeQuestion.getOid());
            st.setObject(index++, yeeQuestion.getUpload());
            st.setObject(index++, toJsonSafe(yeeQuestion.getOption()));
            st.setObject(index++, yeeQuestion.getScoreMode());
            st.setObject(index++, yeeQuestion.getSchoolId());
            st.setObject(index++, toJsonSafe(yeeQuestion.getCategoryId()));
            st.setObject(index++, yeeQuestion.getCateBid());
            st.setObject(index++, yeeQuestion.getCateMid());
            st.setObject(index++, yeeQuestion.getNumber());


            // 执行插入
            int rows = st.executeUpdate();
            if (rows == 0) {
                return false;
            }

            // 获取主键
            Integer newId = null;
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) {
                    newId = rs.getInt(1);
                }
            }

            // 设置回实体
            yeeQuestion.setId(newId);
            return true;
        }
    }

    // 安全 JSON 序列化
    private String toJsonSafe(Object obj) {
        try {
            return obj != null ? jsonUtil.toJson(obj) : null;
        } catch (Exception e) {
            throw new RuntimeException("数据格式异常", e);
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

    /**
     * 将 ResultSet 映射为 List<YeePaperTopic>
     */
    private List<YeePaperTopic> mapPaperTopics(ResultSet rs) throws SQLException {
        List<YeePaperTopic> list = new ArrayList<>();

        while (rs.next()) {
            YeePaperTopic topic = new YeePaperTopic();

            topic.setId(rs.getInt("id"));
            topic.setOid(rs.getInt("oid"));
            topic.setTopic(rs.getString("topic"));
            topic.setType(rs.getInt("type"));
            topic.setLevel(rs.getInt("level"));
            topic.setScore(rs.getInt("score"));

            // JSON 字段：使用 JsonUtil 反序列化
            topic.setOption(JsonUtil.parseOption(rs.getString("option")));

            topic.setAnalysis(rs.getString("analysis"));
            topic.setPid(rs.getInt("pid"));
            topic.setPaperId(rs.getInt("paperId"));
            topic.setTitle(rs.getString("title"));
            topic.setUpload(rs.getString("upload"));
            topic.setScoreMode(rs.getInt("scoreMode"));
            topic.setSchoolId(rs.getInt("schoolId"));
            topic.setCateBid(rs.getInt("cateBid"));
            topic.setCateMid(rs.getInt("cateMid"));
            topic.setNumber(rs.getInt("number"));

            list.add(topic);
        }

        return list;
    }

    /**
     * 验证试题数据
     * @param questionVO 试题导出VO
     * @param rowIndex 行索引（用于错误提示）
     * @return 验证错误信息，如果没有错误则返回null
     */
    private String validateQuestionData(QuestionExportVO questionVO, int rowIndex) {
        // 验证题目内容
        if (StringUtils.isEmpty(questionVO.getTopic())) {
            return "题目内容不能为空";
        }

        // 验证题型
        if (questionVO.getTypeName() == null) {
            return "题型不能为空";
        }

        // 验证分数
        if (questionVO.getScore() == null || questionVO.getScore() < 0) {
            return "分数必须大于等于0";
        }

        // 根据题型验证选项
        String questionType = questionVO.getTypeName();
        switch (questionType) {
            case "单选": // 单选题
                if (StringUtils.isEmpty(questionVO.getOptionA()) &&
                        StringUtils.isEmpty(questionVO.getOptionB()) &&
                        StringUtils.isEmpty(questionVO.getOptionC()) &&
                        StringUtils.isEmpty(questionVO.getOptionD()) &&
                        StringUtils.isEmpty(questionVO.getOptionE()) &&
                        StringUtils.isEmpty(questionVO.getOptionF())
                ) {
                    return "单选题至少需要一个选项";
                }
                break;
            case "多选": // 多选题
                // 验证至少有一个选项
                if (StringUtils.isEmpty(questionVO.getOptionA()) &&
                        StringUtils.isEmpty(questionVO.getOptionB()) &&
                        StringUtils.isEmpty(questionVO.getOptionC()) &&
                        StringUtils.isEmpty(questionVO.getOptionD()) &&
                        StringUtils.isEmpty(questionVO.getOptionE()) &&
                        StringUtils.isEmpty(questionVO.getOptionF())
                ) {
                    return "选择题至少需要一个选项";
                }
                break;
            case "判断": // 判断题
                // 判断题通常不需要额外选项验证
                if (StringUtils.isEmpty(questionVO.getOptionA()) &&
                        StringUtils.isEmpty(questionVO.getOptionB()) &&
                        StringUtils.isEmpty(questionVO.getOptionC()) &&
                        StringUtils.isEmpty(questionVO.getOptionD()) &&
                        StringUtils.isEmpty(questionVO.getOptionE()) &&
                        StringUtils.isEmpty(questionVO.getOptionF())
                ) {
                    return "判断题至少需要一个选项";
                }
                break;
            case "简答": // 简答题
                // 简答题不需要选项验证
                break;
            case "填空": // 填空题
                // 验证至少有一个填空项
                if (StringUtils.isEmpty(questionVO.getOptionA()) &&
                        StringUtils.isEmpty(questionVO.getOptionB()) &&
                        StringUtils.isEmpty(questionVO.getOptionC()) &&
                        StringUtils.isEmpty(questionVO.getOptionD()) &&
                        StringUtils.isEmpty(questionVO.getOptionE()) &&
                        StringUtils.isEmpty(questionVO.getOptionF())
                ) {
                    return "填空题至少需要一个填空项";
                }
                break;
            default:
                return "不支持的题型: " + questionType;
        }

        // 验证难度等级
        if (questionVO.getLevel() != null && (questionVO.getLevel() < 1 || questionVO.getLevel() > 5)) {
            return "难度等级必须在1-5之间";
        }

        // 验证计分模式 (比例模式, 漏选模式)
        if (questionVO.getScoreMode() != null && (questionVO.getScoreMode() < 1 || questionVO.getScoreMode() > 3)) {
            return "计分模式必须在1-3之间";
        }

        return null; // 没有错误
    }

    /**
     * 将QuestionExportVO转换为YeeQuestion实体
     * @param questionVO 试题导出VO
     * @param schoolId 学校ID
     * @param createId 创建人ID
     * @return YeeQuestion实体
     */
    private YeePaperTopic convertToYeeQuestion(QuestionExportVO questionVO, int schoolId, int createId, Integer cateBid, Integer cateMid, Integer paperId, Integer number) {
        YeePaperTopic question = new YeePaperTopic();

        // 基本字段
        question.setTopic(questionVO.getTopic());
        // typeName 转为对应的type
        String questionType = questionVO.getTypeName();
        if (questionType != null && questionType.equals("单选")){
            question.setType(1);
        } else if ( questionType != null && questionType.equals("多选")){
            question.setType(2);
        } else if ( questionType != null && questionType.equals("判断")){
            question.setType(3);
        } else if ( questionType != null && questionType.equals("简答")){
            question.setType(4);
        } else if ( questionType != null && questionType.equals("填空")){
            question.setType(5);
        }
        String levelName = questionVO.getLevelName();
        if (levelName != null && levelName.equals("易")){
            question.setLevel(1);
        } else if ( levelName != null && levelName.equals("中")){
            question.setLevel(2);
        } else if ( levelName != null && levelName.equals("难")){
            question.setLevel(3);
        }
        question.setScore(questionVO.getScore());
        question.setAnalysis(questionVO.getAnalysis());
        question.setOid(0);
        question.setTitle(questionVO.getTitle());
        question.setPid(0);
        question.setSchoolId(schoolId);
        question.setCateBid(cateBid);
        question.setCateMid(cateMid);


        // 选项字段（根据题型处理）
        List<Map<String, Object>> options = new ArrayList<>();

        switch (question.getType()) {
            case 1: // 单选题
                options = buildOptionsForChoiceQuestion(questionVO);
                break;
            case 2: // 多选题
                options = buildOptionsForChoiceQuestion(questionVO);
                break;
            case 3: // 判断题
                options = buildOptionsForChoiceQuestion(questionVO);
                break;
            case 4: // 简答题
                // 简答题没有选项
                break;
            case 5: // 填空题
                options = buildOptionsForFillQuestion(questionVO);
                break;
            default:
                // 默认处理
                break;
        }

        question.setOption(options);

        // 漏选分值（根据题型处理）
        List<Integer> missScore = buildMissScore(questionVO);
        question.setMissScore(missScore);

//        if ("多选".equals(questionVO.getTypeName())){
//            question.setScoreMode(questionVO.getScoreModeName().equals("是") ? 2 : 1);
//        } else {
//            question.setScoreMode(1);
//        }
        // 漏选计分模式
        if ("多选".equals(questionVO.getTypeName())){
            question.setScoreMode(1);
        }

        question.setPaperId(paperId);
        question.setNumber(number);


        return question;
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

        // Double.valueOf(questionVO.getScoreRatioA()) 转换为Double类型 getScoreRatioA() 为空时 会报错, 处理方法为自动填充0
        if (questionVO.getOptionA() != null) {
            addOptionIfNotEmpty(options, "A", questionVO.getOptionA(), Double.valueOf(questionVO.getScoreRatioA() != null ? questionVO.getScoreRatioA() : 0));
        }
        if (questionVO.getOptionB() != null) {
            addOptionIfNotEmpty(options, "B", questionVO.getOptionB(), Double.valueOf(questionVO.getScoreRatioB() != null ? questionVO.getScoreRatioB() : 0));
        }
        if (questionVO.getOptionC() != null) {
            addOptionIfNotEmpty(options, "C", questionVO.getOptionC(), Double.valueOf(questionVO.getScoreRatioC() != null ? questionVO.getScoreRatioC() : 0));
        }
        if (questionVO.getOptionD() != null) {
            addOptionIfNotEmpty(options, "D", questionVO.getOptionD(), Double.valueOf(questionVO.getScoreRatioD() != null ? questionVO.getScoreRatioD() : 0));
        }
        if (questionVO.getOptionE() != null) {
            addOptionIfNotEmpty(options, "E", questionVO.getOptionE(), Double.valueOf(questionVO.getScoreRatioE() != null ? questionVO.getScoreRatioE() : 0));
        }
        if (questionVO.getOptionF() != null) {
            addOptionIfNotEmpty(options, "F", questionVO.getOptionF(), Double.valueOf(questionVO.getScoreRatioF() != null ? questionVO.getScoreRatioF() : 0));
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
            addFillOptionIfNotEmpty(options, 1, questionVO.getOptionA(), Double.valueOf(questionVO.getScoreRatioA() != null ? questionVO.getScoreRatioA() : 0));
        }
        if (questionVO.getOptionB() != null) {
            addFillOptionIfNotEmpty(options, 2, questionVO.getOptionB(), Double.valueOf(questionVO.getScoreRatioB() != null ? questionVO.getScoreRatioB() : 0));
        }
        if (questionVO.getOptionC() != null) {
            addFillOptionIfNotEmpty(options, 3, questionVO.getOptionC(), Double.valueOf(questionVO.getScoreRatioC() != null ? questionVO.getScoreRatioC() : 0));
        }
        if (questionVO.getOptionD() != null) {
            addFillOptionIfNotEmpty(options, 4, questionVO.getOptionD(), Double.valueOf(questionVO.getScoreRatioD() != null ? questionVO.getScoreRatioD() : 0));
        }
        if (questionVO.getOptionE() != null) {
            addFillOptionIfNotEmpty(options, 5, questionVO.getOptionE(), Double.valueOf(questionVO.getScoreRatioE() != null ? questionVO.getScoreRatioE() : 0));
        }
        if (questionVO.getOptionF() != null) {
            addFillOptionIfNotEmpty(options, 6, questionVO.getOptionF(), Double.valueOf(questionVO.getScoreRatioF() != null ? questionVO.getScoreRatioF() : 0));
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
//            String answer = removeHtmlTags((String) option.getOrDefault("answer", ""));
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

            // 设置得分比
            Object scaleObj = option.get("scale");
            String scale = scaleObj != null ? scaleObj.toString() : "0";

            switch (i) {
                case 0: vo.setScoreRatioA(Double.valueOf(scale)); break;
                case 1: vo.setScoreRatioB(Double.valueOf(scale)); break;
                case 2: vo.setScoreRatioC(Double.valueOf(scale)); break;
                case 3: vo.setScoreRatioD(Double.valueOf(scale)); break;
                case 4: vo.setScoreRatioE(Double.valueOf(scale)); break;
                case 5: vo.setScoreRatioF(Double.valueOf(scale)); break;
            }
        }
    }

    /**
     * 处理填空题选项
     * @param vo 导出VO对象
     * @param question 试题对象
     */
    private void processFillBlankOptions(QuestionExportVO vo, YeeQuestion question) {
        if (question.getOption() == null || question.getOption().isEmpty()) {
            return;
        }

        List<Map<String, Object>> options = question.getOption();
        // 按照idx排序填空题答案
        options.sort((o1, o2) -> {
            Object idx1 = o1.get("idx");
            Object idx2 = o2.get("idx");
            if (idx1 instanceof Integer && idx2 instanceof Integer) {
                return ((Integer) idx1).compareTo((Integer) idx2);
            } else if (idx1 instanceof String && idx2 instanceof String) {
                return ((String) idx1).compareTo((String) idx2);
            }
            return 0;
        });

        // 填充答案到选项字段
        for (int i = 0; i < options.size() && i < 6; i++) {
            Map<String, Object> option = options.get(i);
//            String answer = removeHtmlTags((String) option.getOrDefault("answer", ""));
            String answer = (String) option.getOrDefault("answer", "");

            switch (i) {
                case 0: vo.setOptionA(answer); break;
                case 1: vo.setOptionB(answer); break;
                case 2: vo.setOptionC(answer); break;
                case 3: vo.setOptionD(answer); break;
                case 4: vo.setOptionE(answer); break;
                case 5: vo.setOptionF(answer); break;
            }

            // 设置得分比
            Object scaleObj = option.get("scale");
            String scale = scaleObj != null ? scaleObj.toString() : "0";

            switch (i) {
                case 0: vo.setScoreRatioA(Double.valueOf(scale)); break;
                case 1: vo.setScoreRatioB(Double.valueOf(scale)); break;
                case 2: vo.setScoreRatioC(Double.valueOf(scale)); break;
                case 3: vo.setScoreRatioD(Double.valueOf(scale)); break;
                case 4: vo.setScoreRatioE(Double.valueOf(scale)); break;
                case 5: vo.setScoreRatioF(Double.valueOf(scale)); break;
            }

        }
    }

    /**
     * 处理漏选分值
     * @param vo 导出VO对象
     * @param question 试题对象
     */
    private void processMissScores(QuestionExportVO vo, YeeQuestion question) {
        if (question.getMissScore() == null || question.getMissScore().isEmpty()) {
            return;
        }
        List<Integer> missScores = question.getMissScore();
        for (int i = 0; i < missScores.size() && i < 5; i++)
            switch (i) {
                case 0: vo.setMissScore1(missScores.get(i).toString()); break;
                case 1: vo.setMissScore2(missScores.get(i).toString()); break;
                case 2: vo.setMissScore3(missScores.get(i).toString()); break;
                case 3: vo.setMissScore4(missScores.get(i).toString()); break;
                case 4: vo.setMissScore5(missScores.get(i).toString()); break;
            }


        // 设置计分模式名称
        if (question.getScoreMode() != null) {
            vo.setScoreModeName(getScoreModeName(question.getScoreMode()));
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
}
