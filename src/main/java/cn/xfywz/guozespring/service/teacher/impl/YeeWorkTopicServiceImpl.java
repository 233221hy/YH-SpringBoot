package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeWorkTopic;
import cn.xfywz.guozespring.entity.vo.QuestionExportVO;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeWorkTopicService;
import cn.xfywz.guozespring.excel.ExcelExportStyles;
import cn.xfywz.guozespring.util.JsonUtil;
import cn.xfywz.guozespring.util.ParseJsonUtil;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.builder.ExcelWriterBuilder;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: ChengLin
 * 作业题目 yee_work_topic
 */
@Service
public class YeeWorkTopicServiceImpl implements YeeWorkTopicService {

    private static final Logger logger = LoggerFactory.getLogger(YeeWorkTopicServiceImpl.class);
    private static final ParseJsonUtil parseJsonUtil = new ParseJsonUtil();
    private static final JsonUtil jsonUtil = new JsonUtil();

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result selectAll(int schoolId, Integer pageSize, Integer pageNum, Integer workId, String topic, Integer type, Integer level, Integer cateBid, Integer cateMid) throws Exception {

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
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_work_topic WHERE schoolId = ?");
        StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) FROM yee_work_topic WHERE schoolId = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(schoolId);

        if (topic != null && !topic.trim().isEmpty()) {
            String likeValue = "%" + topic.trim() + "%";
            sqlBuilder.append(" AND (topic LIKE ? OR title LIKE ?)");
            countSqlBuilder.append(" AND (topic LIKE ? OR title LIKE ?)");
            parameters.add(likeValue);
            parameters.add(likeValue);
        }
        if (workId != null) {
            sqlBuilder.append(" AND workId = ?");
            countSqlBuilder.append(" AND workId = ?");
            parameters.add(workId);
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
                List<YeeWorkTopic> list = mapWorkTopics(rs);
                return Result.success(list, total);
            }

        } catch (SQLException e) {
            logger.error("查询作业题目列表失败", e);
            throw new Exception("查询作业题目列表失败：" + e.getMessage(), e);
        }
    }

    @Override
    public Result add(YeeWorkTopic topic) throws Exception {
        Integer schoolId = topic.getSchoolId();
        Integer workId = topic.getWorkId();
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

            // 3. 获取当前作业最大 number，用于自增
            String maxSql = "SELECT MAX(number) FROM yee_work_topic WHERE workId = ?";
            Integer newNumber = 1;
            try (PreparedStatement maxSt = connection.prepareStatement(maxSql)) {
                maxSt.setLong(1, workId);
                try (ResultSet rs = maxSt.executeQuery()) {
                    if (rs.next()) {
                        Integer maxNum = rs.getInt(1);
                        if (rs.wasNull()) maxNum = 0;
                        newNumber = maxNum + 1;
                    }
                }
            }
            topic.setNumber(newNumber);

            // 4. 插入题目
            String insertSql = """
            INSERT INTO yee_work_topic 
            (oid, topic, type, level, score, missScore, option1, option2, option3, analysis, 
             pid, workId, title, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid, number)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

            try (PreparedStatement st = connection.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
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
                st.setObject(12, workId);
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

                // 5. 更新 yee_work 表：topicNumber + 1, score += 当前题分数
                String updateWorkSql = """
                UPDATE yee_work 
                SET topicNumber = COALESCE(topicNumber, 0) + 1,
                    score = COALESCE(score, 0) + ?
                WHERE id = ? AND schoolId = ?
                """;

                try (PreparedStatement updateSt = connection.prepareStatement(updateWorkSql)) {
                    updateSt.setObject(1, score != null ? score : 0);
                    updateSt.setObject(2, workId);
                    updateSt.setObject(3, schoolId);

                    int workRows = updateSt.executeUpdate();
                    if (workRows == 0) {
                        throw new SQLException("更新作业统计失败：作业不存在或不属于该学校");
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
            logger.error("新增作业题目失败", e);
            return Result.error("新增题目失败：" + e.getMessage());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close(); // 归还连接
                }
            } catch (SQLException e) {
                logger.error("关闭数据库连接失败", e);
            }
        }
    }

    @Override
    public Result update(YeeWorkTopic topic) throws Exception {
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

            // 3. 检查题目是否存在，并获取旧分值和 workId
            String checkSql = "SELECT score, workId FROM yee_work_topic WHERE id = ? AND schoolId = ?";
            Long oldScore = null;
            Long workId = null;

            try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
                checkSt.setLong(1, topicId);
                checkSt.setLong(2, schoolId);
                try (ResultSet rs = checkSt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("题目不存在");
                    }
                    oldScore = rs.getObject("score", Long.class);
                    workId = rs.getObject("workId", Long.class);
                }
            }

            // 如果 workId 为空，无法更新作业，报错
            if (workId == null) {
                return Result.error("题目所属作业信息缺失");
            }

            // 4. 执行题目更新
            String updateSql = """
            UPDATE yee_work_topic SET 
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

            // 5. 更新作业总分（仅当分数变化时）
            if (newScore != null && oldScore != null && !newScore.equals(oldScore)) {
                String updateWorkSql = """
                UPDATE yee_work 
                SET score = COALESCE(score, 0) + ?
                WHERE id = ? AND schoolId = ?
                """;

                try (PreparedStatement updateSt = connection.prepareStatement(updateWorkSql)) {
                    updateSt.setObject(1, newScore - oldScore); // 增量更新
                    updateSt.setObject(2, workId);
                    updateSt.setObject(3, schoolId);

                    int workRows = updateSt.executeUpdate();
                    if (workRows == 0) {
                        throw new SQLException("更新作业总分失败：作业不存在或不属于该学校");
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
            logger.error("更新作业题目失败", e);
            return Result.error("更新题目失败：" + e.getMessage());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.error("关闭数据库连接失败", e);
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

            // 3. 查询题目信息（获取 score 和 workId）
            String querySql = "SELECT score, workId FROM yee_work_topic WHERE id = ? AND schoolId = ?";
            Long topicScore = null;
            Long workId = null;

            try (PreparedStatement querySt = connection.prepareStatement(querySql)) {
                querySt.setLong(1, id);
                querySt.setLong(2, schoolId);
                try (ResultSet rs = querySt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("题目不存在");
                    }
                    topicScore = rs.getObject("score", Long.class);
                    workId = rs.getObject("workId", Long.class);
                }
            }

            if (workId == null) {
                return Result.error("题目所属作业信息缺失");
            }

            // 4. 删除题目
            String deleteSql = "DELETE FROM yee_work_topic WHERE id = ? AND schoolId = ?";
            try (PreparedStatement st = connection.prepareStatement(deleteSql)) {
                st.setLong(1, id);
                st.setLong(2, schoolId);

                int rows = st.executeUpdate();
                if (rows == 0) {
                    connection.rollback();
                    return Result.error("删除失败");
                }
            }

            // 5. 更新作业：题目数 -1，总分减去该题分数
            String updateWorkSql = """
            UPDATE yee_work 
            SET topicNumber = COALESCE(topicNumber, 0) - 1,
                score = COALESCE(score, 0) - ?
            WHERE id = ? AND schoolId = ?
            """;

            try (PreparedStatement updateSt = connection.prepareStatement(updateWorkSql)) {
                updateSt.setObject(1, topicScore != null ? topicScore : 0);
                updateSt.setObject(2, workId);
                updateSt.setObject(3, schoolId);

                int workRows = updateSt.executeUpdate();
                if (workRows == 0) {
                    throw new SQLException("更新作业统计失败：作业不存在或不属于该学校");
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
            logger.error("删除作业题目失败", e);
            return Result.error("删除题目失败：" + e.getMessage());
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.error("关闭数据库连接失败", e);
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
            String sql = "SELECT * FROM yee_work_topic WHERE id = ? AND schoolId = ?";
            st = connection.prepareStatement(sql);
            st.setLong(1, id);
            st.setLong(2, schoolId);

            rs = st.executeQuery();
            if (rs.next()) {
                YeeWorkTopic topic = new YeeWorkTopic();

                topic.setId(rs.getInt("id"));
                topic.setOid(rs.getInt("oid"));
                topic.setTopic(rs.getString("topic"));
                topic.setType(rs.getInt("type"));
                topic.setLevel(rs.getInt("level"));
                topic.setScore(rs.getInt("score"));

                // JSON 字段：使用 JsonUtil 反序列化
                topic.setOption(jsonUtil.parseOption(rs.getString("option")));
                topic.setMissScore(jsonUtil.parseList(rs.getString("missScore"), Integer.class));
                topic.setCategoryId(jsonUtil.parseList(rs.getString("categoryId"), Integer.class));

                topic.setAnalysis(rs.getString("analysis"));
                topic.setPid(rs.getInt("pid"));
                topic.setWorkId(rs.getInt("workId"));
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
            logger.error("查询作业题目详情失败", e);
            return Result.error("数据库查询失败：" + e.getMessage());
        } finally {
            // 安全关闭资源
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("关闭 ResultSet 失败", e);
                }
            }
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    logger.error("关闭 PreparedStatement 失败", e);
                }
            }
            if (connection != null) {
                try {
                    connection.close(); // 归还连接
                } catch (SQLException e) {
                    logger.error("关闭数据库连接失败", e);
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
            UPDATE yee_work_topic 
            SET number = ? 
            WHERE id = ? AND schoolId = ?
            """;

            st = connection.prepareStatement(updateSql);

            // 第一次更新：id1 的 number 改为 number2
            st.setLong(1, number2);
            st.setLong(2, id1);
            st.setLong(3, schoolId);
            int rows1 = st.executeUpdate();

            if (rows1 == 0) {
                connection.rollback();
                return Result.error("更新失败：题目 " + id1 + " 不存在或不属于该学校");
            }

            // 第二次更新：id2 的 number 改为 number1
            st.setLong(1, number1);
            st.setLong(2, id2);
            st.setLong(3, schoolId);
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
                logger.error("事务回滚失败", rollbackEx);
            }
            logger.error("排序更新失败", e);
            return Result.error("数据库操作失败：" + e.getMessage());
        } finally {
            // 清理资源
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    logger.error("关闭 PreparedStatement 失败", e);
                }
            }
            if (connection != null) {
                try {
                    connection.setAutoCommit(true); // 恢复默认
                    connection.close(); // 归还连接
                } catch (SQLException e) {
                    logger.error("关闭数据库连接失败", e);
                }
            }
        }
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


            // 3. 创建 ExcelWriter 并写入数据（流式写入）
            ExcelWriterBuilder writerBuilder = EasyExcel.write(outputStream, QuestionExportVO.class)
                    .registerWriteHandler(ExcelExportStyles.defaultStyleStrategy());
            excelWriter = writerBuilder.build();
            WriteSheet writeSheet = EasyExcel.writerSheet("试题数据").build();

            // 4. 分页查询并流式写入数据
            int pageSize = 10000; // 每页 10000 条数据
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
                    List<YeeWorkTopic> questions = new ArrayList<>();
                    if (result instanceof Result) {
                        Object data = ((Result) result).getData();
                        if (data instanceof List) {
                            questions = (List<YeeWorkTopic>) data;
                        }
                    } else if (result instanceof Map && ((Map<?, ?>) result).containsKey("data")) {
                        Object data = ((Map<?, ?>) result).get("data");
                        if (data instanceof List) {
                            questions = (List<YeeWorkTopic>) data;
                        }
                    }

                    // 如果没有数据了，跳出循环
                    if (questions.isEmpty()) {
                        break;
                    }

                    // 转换为导出 VO 对象
                    List<QuestionExportVO> exportList = convertToExportVO(questions);

                    // 写入数据到 Excel（流式写入）
                    excelWriter.write(exportList, writeSheet);

                    // 测试执行时间
                    long endTime = System.currentTimeMillis();
                    totalExported += exportList.size();

                    // 如果当前页数据少于 pageSize，说明已经是最后一页，跳出循环
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
            // 确保 ExcelWriter 正确关闭，这会自动刷新和关闭输出流
            if (excelWriter != null) {
                try {
                    excelWriter.finish();
                } catch (Exception e) {
                    logger.error("关闭 ExcelWriter 时发生异常", e);
                    // 检查异常是否与流已关闭有关
                    if (e.getMessage() != null && e.getMessage().contains("Stream closed")) {
                        logger.warn("检测到流已关闭，可能是客户端已断开连接，这是正常现象");
                    } else {
                        // 对于其他异常，记录但不抛出，避免影响主流程
                        logger.warn("ExcelWriter 关闭时发生非流关闭异常，但不会影响导出文件的完整性");
                    }
                }
            }
        }
    }

    /**
     * 将 YeeWorkTopic 列表转换为 QuestionExportVO 列表
     * @param questions 试题列表
     * @return 导出 VO 列表
     */
    private List<QuestionExportVO> convertToExportVO(List<YeeWorkTopic> questions) {
        List<QuestionExportVO> exportList = new ArrayList<>();

        for (YeeWorkTopic question : questions) {
            QuestionExportVO vo = new QuestionExportVO();

            // 基础字段
            vo.setTitle(question.getTitle());
            vo.setTopic(removeHtmlTags(question.getTopic()));
            vo.setType((int) question.getType());
            vo.setLevel((int) question.getLevel());
            vo.setScore((int) question.getScore());
            vo.setAnalysis(removeHtmlTags(question.getAnalysis()));
            vo.setScoreMode((int) question.getScoreMode());

            // 类型名称转换
            vo.setTypeName(getTypeName((int) question.getType()));

            // 难度等级转换
            vo.setLevelName(getLevelName((int) question.getLevel()));

            // 处理选项和得分比
            processOptionsAndScores(vo, question);

            // 处理漏选分值（仅对多选题）
            if (question.getType() == 2 && question.getScoreMode() == 2) {
                processMissScores(vo, question);
            }

            // 设置计分模式名称（对多选题）
            if (question.getType() == 2) {
                vo.setScoreModeName(getScoreModeName((int) question.getScoreMode()));
            }

            exportList.add(vo);
        }

        return exportList;
    }

    /**
     * 移除 HTML 标签
     */
    private String removeHtmlTags(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return html.replaceAll("<[^>]+>", "");
    }

    /**
     * 获取题型名称
     */
    private String getTypeName(int type) {
        switch (type) {
            case 1: return "单选题";
            case 2: return "多选题";
            case 3: return "判断题";
            case 4: return "填空题";
            case 5: return "简答题";
            default: return "未知";
        }
    }

    /**
     * 获取难度等级名称
     */
    private String getLevelName(int level) {
        switch (level) {
            case 1: return "简单";
            case 2: return "较易";
            case 3: return "中等";
            case 4: return "较难";
            case 5: return "困难";
            default: return "未知";
        }
    }

    /**
     * 获取计分模式名称
     */
    private String getScoreModeName(int scoreMode) {
        switch (scoreMode) {
            case 1: return "全对才给分";
            case 2: return "漏选给分";
            default: return "未知";
        }
    }

    /**
     * 处理选项和分数比例
     */
    private void processOptionsAndScores(QuestionExportVO vo, YeeWorkTopic topic) {
        try {
            List<Map<String, Object>> options = topic.getOption();
            if (options != null && !options.isEmpty()) {
                for (Map<String, Object> option : options) {
                    String key = (String) option.get("key");
                    String value = (String) option.get("value");
                    Double scoreRatio = option.get("scoreRatio") != null ? 
                        ((Number) option.get("scoreRatio")).doubleValue() : 0.0;

                    if ("A".equals(key)) {
                        vo.setOptionA(value);
                        vo.setScoreRatioA(scoreRatio);
                    } else if ("B".equals(key)) {
                        vo.setOptionB(value);
                        vo.setScoreRatioB(scoreRatio);
                    } else if ("C".equals(key)) {
                        vo.setOptionC(value);
                        vo.setScoreRatioC(scoreRatio);
                    } else if ("D".equals(key)) {
                        vo.setOptionD(value);
                        vo.setScoreRatioD(scoreRatio);
                    } else if ("E".equals(key)) {
                        vo.setOptionE(value);
                        vo.setScoreRatioE(scoreRatio);
                    } else if ("F".equals(key)) {
                        vo.setOptionF(value);
                        vo.setScoreRatioF(scoreRatio);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("处理选项失败", e);
        }
    }

    /**
     * 处理漏选分值
     */
    private void processMissScores(QuestionExportVO vo, YeeWorkTopic question) {
        try {
            List<Integer> missScoreList = question.getMissScore();
            if (missScoreList != null && !missScoreList.isEmpty()) {
                for (int i = 0; i < missScoreList.size() && i < 5; i++) {
                    switch (i) {
                        case 0:
                            vo.setMissScore1(missScoreList.get(i).toString());
                            break;
                        case 1:
                            vo.setMissScore2(missScoreList.get(i).toString());
                            break;
                        case 2:
                            vo.setMissScore3(missScoreList.get(i).toString());
                            break;
                        case 3:
                            vo.setMissScore4(missScoreList.get(i).toString());
                            break;
                        case 4:
                            vo.setMissScore5(missScoreList.get(i).toString());
                            break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("处理漏选分值失败", e);
        }
    }

    /**
     * 分页查询试题数据（复用数据库连接）
     */
    public Result exportAllWithPagination(Connection connection, int schoolId, Integer pageSize, Integer pageNum,
                                          String topic, Integer createId, Integer type,
                                          Integer level, Integer cateBid, Integer cateMid, Integer workId) throws Exception {
        // 验证参数
        if (connection == null) {
            throw new Exception("数据库连接不能为空");
        }

        // 调用 selectAllForExport 方法获取分页数据
        return selectAllForExport(connection, schoolId, pageSize, pageNum, topic, createId, type, level, cateBid, cateMid, workId);
    }

    /**
     * 分页查询试题数据（用于导出）
     */
    public Result selectAllForExport(Connection connection, int schoolId, Integer pageSize, Integer pageNum,
                                     String topic, Integer createId, Integer type,
                                     Integer level, Integer cateBid, Integer cateMid, Integer workId) throws Exception {
        try {
            // 3. 基础 SQL
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_work_topic WHERE schoolId = ?");
            StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) FROM yee_work_topic WHERE schoolId = ?");

            List<Object> parameters = new ArrayList<>();
            parameters.add(schoolId);

            // 特别处理：topic 同时在 topic 和 title 字段模糊匹配
            if (topic != null && !topic.trim().isEmpty()) {
                sqlBuilder.append(" AND (topic LIKE ? OR title LIKE ?)");
                countSqlBuilder.append(" AND (topic LIKE ? OR title LIKE ?)");
                String likeValue = "%" + topic.trim() + "%";
                parameters.add(likeValue);
                parameters.add(likeValue);
            }

            // 其他条件：只有非 null 才加入（这是标准做法）
            if (workId != null) {
                sqlBuilder.append(" AND workId = ?");
                countSqlBuilder.append(" AND workId = ?");
                parameters.add(workId);
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
            Object result = mapWorkTopicsToList(rs);
            rs.close();
            st.close();

            return Result.success(result, total);

        } catch (Exception e) {
            throw new Exception("查询试题列表失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将 ResultSet 映射为 List<YeeWorkTopic>
     */
    private List<YeeWorkTopic> mapWorkTopicsToList(ResultSet rs) throws SQLException {
        List<YeeWorkTopic> list = new ArrayList<>();

        while (rs.next()) {
            YeeWorkTopic topic = new YeeWorkTopic();

            topic.setId(rs.getInt("id"));
            topic.setOid(rs.getInt("oid"));
            topic.setTopic(rs.getString("topic"));
            topic.setType(rs.getInt("type"));
            topic.setLevel(rs.getInt("level"));
            topic.setScore(rs.getInt("score"));

            // JSON 字段：使用 JsonUtil 反序列化
            topic.setOption(jsonUtil.parseOption(rs.getString("option")));

            topic.setAnalysis(rs.getString("analysis"));
            topic.setPid(rs.getInt("pid"));
            topic.setWorkId(rs.getInt("workId"));
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
     * 将 ResultSet 映射为 YeeWorkTopic 对象列表
     */
    private List<YeeWorkTopic> mapWorkTopics(ResultSet rs) throws SQLException {
        List<YeeWorkTopic> list = new ArrayList<>();
        while (rs.next()) {
            YeeWorkTopic topic = new YeeWorkTopic();
            topic.setId(rs.getInt("id"));
            topic.setTopic(rs.getString("topic"));
            topic.setType(rs.getInt("type"));
            topic.setLevel(rs.getInt("level"));
            topic.setScore(rs.getInt("score"));
            topic.setMissScore(JSON.parseObject(rs.getString("missScore"), List.class));
            topic.setOption1(JSON.parseObject(rs.getString("option1"), List.class));
            topic.setOption2(JSON.parseObject(rs.getString("option2"), List.class));
            topic.setOption3(JSON.parseObject(rs.getString("option3"), List.class));
            topic.setAnalysis(rs.getString("analysis"));
            topic.setPid(rs.getInt("pid"));
            topic.setWorkId(rs.getInt("workId"));
            topic.setTitle(rs.getString("title"));
            topic.setOid(rs.getInt("oid"));
            topic.setNumber(rs.getInt("number"));
            topic.setOption(jsonUtil.parseOption(rs.getString("option")));
            topic.setUpload(rs.getString("upload"));
            topic.setScoreMode(rs.getInt("scoreMode"));
            topic.setSchoolId(rs.getInt("schoolId"));
            topic.setCategoryId(jsonUtil.parseList(rs.getString("categoryId"), Integer.class));
            topic.setCateBid(rs.getInt("cateBid"));
            topic.setCateMid(rs.getInt("cateMid"));
            list.add(topic);
        }
        return list;
    }
}
