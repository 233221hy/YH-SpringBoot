package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import cn.xfywz.guozespring.entity.vo.QuestionExportVO;
import cn.xfywz.guozespring.entity.vo.YeeQuestionWithCreatorVO;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeQuestionService;
import cn.xfywz.guozespring.util.JsonUtil;
import cn.xfywz.guozespring.util.ParseJsonUtil;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @Author: ChengLin
 */
@Service
public class YeeQuestionServiceImpl implements YeeQuestionService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final ParseJsonUtil parseJsonUtil = new ParseJsonUtil();

    private static final JsonUtil jsonUtil = new JsonUtil();

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
            yeeQuestion.setCreateId(rs.getInt("createId"));
            yeeQuestion.setLevel(rs.getInt("level"));
            yeeQuestion.setScore(rs.getInt("score"));
            yeeQuestion.setTitle(rs.getString("title"));
            yeeQuestion.setTopic(rs.getString("topic"));
            // 新增返回 9.4
            yeeQuestion.setScoreMode(rs.getInt("scoreMode"));
            yeeQuestion.setSchoolId(rs.getInt("schoolId"));
            yeeQuestion.setAddTime(rs.getTimestamp("addTime").toLocalDateTime());
            yeeQuestion.setCateMid(rs.getInt("cateMid"));
            yeeQuestion.setCateBid(rs.getInt("cateBid"));
            JSONArray missScoreJson = JSON.parseArray(rs.getString("missScore"));
            if (missScoreJson != null){
                yeeQuestion.setMissScore(missScoreJson.toList(Integer.class));

            }

            yeeQuestions.add(yeeQuestion);
        }
        return yeeQuestions;
    }

    /**
     * 将 ResultSet 映射为 List<YeeQuestionWithCreatorVO>
     */
    private Object rsToYeeQuestionWithCreator(ResultSet rs) throws SQLException {
        ArrayList<YeeQuestionWithCreatorVO> yeeQuestions = new ArrayList<>();

        while (rs.next()) {
            YeeQuestionWithCreatorVO yeeQuestion = new YeeQuestionWithCreatorVO();
            yeeQuestion.setId(rs.getInt("id"));

            // option -> List<Map<String, Object>>
            String optionJson = rs.getString("option");
            List<Map<String, Object>> option = parseJsonUtil.parseJsonToList(optionJson, "option", yeeQuestion.getId());
            yeeQuestion.setOption(option);
            yeeQuestion.setPid(rs.getInt("pid"));
            yeeQuestion.setType(rs.getInt("type"));
            yeeQuestion.setUpload(rs.getString("upload"));
            yeeQuestion.setAnalysis(rs.getString("analysis"));
            yeeQuestion.setCreateId(rs.getInt("createId"));
            yeeQuestion.setLevel(rs.getInt("level"));
            yeeQuestion.setScore(rs.getInt("score"));
            yeeQuestion.setTitle(rs.getString("title"));
            yeeQuestion.setTopic(rs.getString("topic"));
            // 新增返回 9.4
            yeeQuestion.setScoreMode(rs.getInt("scoreMode"));
            yeeQuestion.setSchoolId(rs.getInt("schoolId"));
            yeeQuestion.setAddTime(rs.getTimestamp("addTime").toLocalDateTime());
            yeeQuestion.setCateMid(rs.getInt("cateMid"));
            yeeQuestion.setCateBid(rs.getInt("cateBid"));
//            yeeQuestion.setDeleted(rs.getInt("deleted"));

            // 设置创建者姓名
            yeeQuestion.setCreatorName(rs.getString("creatorName"));

            JSONArray missScoreJson = JSON.parseArray(rs.getString("missScore"));
            if (missScoreJson != null){
                yeeQuestion.setMissScore(missScoreJson.toList(Integer.class));
            }

            yeeQuestions.add(yeeQuestion);
        }
        return yeeQuestions;
    }

//    public Result selectAll(Connection connection, int schoolId, String topic, Integer createId, Integer type,
//                            Integer level, Integer cateBid, Integer cateMid) throws Exception {
//        return selectAll(connection, schoolId, null, null, topic, createId, type, level, cateBid, cateMid);
//    }


    @Override
    public Result selectAll(int schoolId, Integer pageSize, Integer pageNum,
                               String topic, Integer createId, String creatorName, Integer type,
                               Integer level, Integer cateBid, Integer cateMid) throws Exception {

        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            throw new Exception("无法获取数据库连接");
        }

        try {
            // 调用新的selectAllNew方法
            return selectAllNew(connection, schoolId, pageSize, pageNum, topic, createId, creatorName, type, level, cateBid, cateMid);
        } finally {
            // 确保连接关闭
            if (connection != null && !connection.isClosed()) {
                try {
                    connection.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * 新的分页查询试题数据（支持根据创建者姓名查询）
     * @param connection 数据库连接
     * @param schoolId 学校ID
     * @param pageSize 每页大小
     * @param pageNum 页码
     * @param topic 题目内容搜索关键词
     * @param createId 创建人ID
     * @param creatorName 创建者姓名
     * @param type 题型
     * @param level 难度等级
     * @param cateBid 分类大类ID
     * @param cateMid 分类中类ID
     * @return 分页查询结果
     * @throws Exception 数据库查询异常
     */
    public Result selectAllNew(Connection connection, int schoolId, Integer pageSize, Integer pageNum,
                               String topic, Integer createId, String creatorName, Integer type,
                               Integer level, Integer cateBid, Integer cateMid) throws Exception {
        try {
            // 3. 基础 SQL - 添加逻辑删除过滤条件，同时连表查询创建者姓名
            StringBuilder sqlBuilder = new StringBuilder("""
                SELECT q.*, m.name as creatorName 
                FROM yee_question q 
                LEFT JOIN yee_manage m ON q.createId = m.id 
                WHERE q.schoolId = ?
                """);
            StringBuilder countSqlBuilder = new StringBuilder("""
                SELECT COUNT(*) 
                FROM yee_question q 
                LEFT JOIN yee_manage m ON q.createId = m.id 
                WHERE q.schoolId = ?
                """);

            List<Object> parameters = new ArrayList<>();
            parameters.add(schoolId);

            // 特别处理：topic 同时在 topic 和 title 字段模糊匹配
            if (topic != null && !topic.trim().isEmpty()) {
                sqlBuilder.append(" AND (q.topic LIKE ? OR q.title LIKE ?)");
                countSqlBuilder.append(" AND (q.topic LIKE ? OR q.title LIKE ?)");
                String likeValue = "%" + topic.trim() + "%";
                parameters.add(likeValue);
                parameters.add(likeValue);
            }

            // 根据创建者ID查询
            if (createId != null) {
                sqlBuilder.append(" AND q.createId = ?");
                countSqlBuilder.append(" AND q.createId = ?");
                parameters.add(createId);
            }

            // 根据创建者姓名查询（支持模糊匹配）
            if (creatorName != null && !creatorName.trim().isEmpty()) {
                sqlBuilder.append(" AND m.name LIKE ?");
                countSqlBuilder.append(" AND m.name LIKE ?");
                String likeValue = "%" + creatorName.trim() + "%";
                parameters.add(likeValue);
            }

            // 其他条件：只有非 null 才加入（这是标准做法）
            if (type != null) {
                sqlBuilder.append(" AND q.type = ?");
                countSqlBuilder.append(" AND q.type = ?");
                parameters.add(type);
            }
            if (level != null) {
                sqlBuilder.append(" AND q.level = ?");
                countSqlBuilder.append(" AND q.level = ?");
                parameters.add(level);
            }
            if (cateBid != null) {
                sqlBuilder.append(" AND q.cateBid = ?");
                countSqlBuilder.append(" AND q.cateBid = ?");
                parameters.add(cateBid);
            }
            if (cateMid != null) {
                sqlBuilder.append(" AND q.cateMid = ?");
                countSqlBuilder.append(" AND q.cateMid = ?");
                parameters.add(cateMid);
            }

            // 分页参数：使用 pageSize
            int size = pageSize != null && pageSize > 0 ? pageSize : 10;
            int page = pageNum != null && pageNum >= 1 ? pageNum : 1;
            int offset = (page - 1) * size;

            // 排序 + 分页
            sqlBuilder.append(" ORDER BY q.id DESC LIMIT ? OFFSET ?");

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
            Object result = rsToYeeQuestionWithCreator(rs);
            rs.close();
            st.close();

            return Result.success(result, total);

        } catch (Exception e) {
            throw new Exception("查询试题列表失败: " + e.getMessage(), e);
        }
    }




    // 需使用此功能: 导出excel
    public Result selectAll(Connection connection, int schoolId, Integer pageSize, Integer pageNum,
                            String topic, Integer createId, Integer type,
                            Integer level, Integer cateBid, Integer cateMid) throws Exception {
        try {
            // 3. 基础 SQL - 添加逻辑删除过滤条件
            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_question where 1=1");
            StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) FROM yee_question where 1=1");

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
    public boolean add(Connection connection, YeeQuestion yeeQuestion) throws Exception {
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
            INSERT INTO yee_question (
                topic, type, level, score, missScore, analysis, 
                pid, title, oid, upload, `option`, scoreMode, 
                schoolId, categoryId, cateBid, cateMid, createId, addTime
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
            st.setObject(index++, yeeQuestion.getTitle());
            st.setObject(index++, yeeQuestion.getOid());
            st.setObject(index++, yeeQuestion.getUpload());
            st.setObject(index++, toJsonSafe(yeeQuestion.getOption()));
            st.setObject(index++, yeeQuestion.getScoreMode());
            st.setObject(index++, yeeQuestion.getSchoolId());
            st.setObject(index++, toJsonSafe(yeeQuestion.getCategoryId()));
            st.setObject(index++, yeeQuestion.getCateBid());
            st.setObject(index++, yeeQuestion.getCateMid());
            st.setObject(index++, yeeQuestion.getCreateId());

            // addTime 处理
            LocalDateTime addTime = Optional.ofNullable(yeeQuestion.getAddTime())
                    .orElse(LocalDateTime.now());
            st.setObject(index++, Timestamp.valueOf(addTime));

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

    @Override
    public Result add(YeeQuestion yeeQuestion) throws  Exception{
        // 1. 参数校验
        if (yeeQuestion == null) {
            return Result.error("试题数据不能为空");
        }

        Integer schoolId = yeeQuestion.getSchoolId();
        if (schoolId == null) {
            return Result.error("缺少学校ID");
        }

        // 可选：更细校验
        if (StringUtils.isEmpty(yeeQuestion.getTopic())) {
            return Result.error("题目内容不能为空");
        }
        if (yeeQuestion.getType() == null) {
            return Result.error("题型不能为空");
        }
        if (yeeQuestion.getScore() == null || yeeQuestion.getScore() < 0) {
            return Result.error("分数必须大于等于0");
        }

        // 2. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 3. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取学校数据库连接");
        }

        try {
            // 开启事务
            connection.setAutoCommit(false);

            // 4. 插入 SQL（addDate 是生成列，不插入）
            String sql = """
            INSERT INTO yee_question (
                topic, type, level, score, missScore, analysis, 
                pid, title, oid, upload, `option`, scoreMode, 
                schoolId, categoryId, cateBid, cateMid, createId, addTime
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
                st.setObject(index++, yeeQuestion.getTitle());
                st.setObject(index++, yeeQuestion.getOid());
                st.setObject(index++, yeeQuestion.getUpload());
                st.setObject(index++, toJsonSafe(yeeQuestion.getOption()));
                st.setObject(index++, yeeQuestion.getScoreMode());
                st.setObject(index++, yeeQuestion.getSchoolId());
                st.setObject(index++, toJsonSafe(yeeQuestion.getCategoryId()));
                st.setObject(index++, yeeQuestion.getCateBid());
                st.setObject(index++, yeeQuestion.getCateMid());
                st.setObject(index++, yeeQuestion.getCreateId());

                // addTime 处理
                LocalDateTime addTime = Optional.ofNullable(yeeQuestion.getAddTime())
                        .orElse(LocalDateTime.now());
                st.setObject(index++, Timestamp.valueOf(addTime));

                // 执行插入
                int rows = st.executeUpdate();
                if (rows == 0) {
                    return Result.error("插入试题失败，影响行数为0");
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

                // 提交事务
                connection.commit();
                return Result.success("试题添加成功", yeeQuestion);

            }

        } catch (SQLException e) {
            // 回滚
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }
            return Result.error("数据库操作失败，请稍后重试");
        } catch (Exception e) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }
            return Result.error("系统异常，请联系管理员");
        } finally {
            // 恢复并关闭
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


    @Override
    public Result update(YeeQuestion yeeQuestion) throws  Exception{
        // 1. 参数校验
        if (yeeQuestion == null) {
            return Result.error("试题数据不能为空");
        }

        Integer id = yeeQuestion.getId();
        if (id == null || id <= 0) {
            return Result.error("试题ID不能为空");
        }

        Integer schoolId = yeeQuestion.getSchoolId();
        if (schoolId == null) {
            return Result.error("缺少学校ID");
        }

        // 可选：更细校验
        if (StringUtils.isEmpty(yeeQuestion.getTopic())) {
            return Result.error("题目内容不能为空");
        }
        if (yeeQuestion.getType() == null) {
            return Result.error("题型不能为空");
        }
        if (yeeQuestion.getScore() == null || yeeQuestion.getScore() < 0) {
            return Result.error("分数必须大于等于0");
        }

        // 2. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 3. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取学校数据库连接");
        }

        try {
            // 开启事务
            connection.setAutoCommit(false);

            // 4. 更新 SQL（不更新 addDate，它是生成列，同时确保不更新已删除的记录）
            String sql = """
            UPDATE yee_question SET
                topic = ?,
                type = ?,
                level = ?,
                score = ?,
                missScore = ?,
                analysis = ?,
                pid = ?,
                title = ?,
                oid = ?,
                upload = ?,
                `option` = ?,
                scoreMode = ?,
                categoryId = ?,
                cateBid = ?,
                cateMid = ?
            WHERE id = ? AND schoolId = ? 
            """;

            try (PreparedStatement st = connection.prepareStatement(sql)) {
                st.setObject(1, yeeQuestion.getTopic());
                st.setObject(2, yeeQuestion.getType());
                st.setObject(3, yeeQuestion.getLevel());
                st.setObject(4, yeeQuestion.getScore());
                st.setObject(5, toJsonSafe(yeeQuestion.getMissScore()));
                st.setObject(6, yeeQuestion.getAnalysis());
                st.setObject(7, yeeQuestion.getPid());
                st.setObject(8, yeeQuestion.getTitle());
                st.setObject(9, yeeQuestion.getOid());
                st.setObject(10, yeeQuestion.getUpload());
                st.setObject(11, toJsonSafe(yeeQuestion.getOption()));
                st.setObject(12, yeeQuestion.getScoreMode());
                st.setObject(13, toJsonSafe(yeeQuestion.getCategoryId()));
                st.setObject(14, yeeQuestion.getCateBid());
                st.setObject(15, yeeQuestion.getCateMid());

                // WHERE 条件
                st.setObject(16, id);
                st.setObject(17, schoolId);

                int rows = st.executeUpdate();
                if (rows == 0) {
                    return Result.error("更新失败，可能试题不存在、已被删除或无权限");
                }

                // 提交事务
                connection.commit();
                return Result.success("试题更新成功", yeeQuestion);
            }

        } catch (SQLException e) {
            // 回滚
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }
            return Result.error("数据库操作失败，请稍后重试");
        } catch (Exception e) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }
            return Result.error("系统异常，请联系管理员");
        } finally {
            // 恢复并关闭
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

    // 安全 JSON 序列化
    private String toJsonSafe(Object obj) {
        try {
            return obj != null ? jsonUtil.toJson(obj) : null;
        } catch (Exception e) {
            throw new RuntimeException("数据格式异常", e);
        }
    }

    @Override
    public Result delete(int schoolId, int id) throws  Exception{
        // 1. 参数校验
        if (id <= 0) {
            return Result.error("试题ID无效");
        }
        if (schoolId <= 0) {
            return Result.error("学校ID无效");
        }

        // 2. 验证学校是否存在且已审核
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 3. 获取对应学校的数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取学校数据库连接");
        }

        try {
            // 开启事务
            connection.setAutoCommit(false);

            // 4. 删除 SQL（带 schoolId 防越权）
            String sql = "DELETE FROM yee_question WHERE id = ? AND schoolId = ?";

            try (PreparedStatement st = connection.prepareStatement(sql)) {
                st.setObject(1, id);
                st.setObject(2, schoolId);

                // 5. 执行删除
                int rows = st.executeUpdate();
                if (rows == 0) {
                    return Result.error("删除失败，可能试题不存在、已被删除或无权限");
                }

                // 提交事务
                connection.commit();
                return Result.success("试题删除成功");

            }

        } catch (SQLException e) {
            // 回滚
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }

            return Result.error("数据库操作失败，请稍后重试");
        } catch (Exception e) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());

            }
            return Result.error("系统异常，请联系管理员");
        } finally {
            // 恢复并关闭
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

    @Override
    public Result getById(int schoolId, int id) throws Exception {
        // 1. 参数校验
        if (id <= 0) {
            return Result.error("试题ID无效");
        }
        if (schoolId <= 0) {
            return Result.error("学校ID无效");
        }

        // 2. 验证学校是否存在且已审核
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 3. 获取对应学校的数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            throw new Exception("无法获取学校数据库连接");
        }

        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            // 4. 准备查询 SQL（带 schoolId 防越权，并添加逻辑删除过滤）
            String sql = """
            SELECT 
                id, topic, type, level, score, missScore, analysis,
                pid, title, oid, upload, `option`, scoreMode,
                schoolId, categoryId, cateBid, cateMid, createId, addTime, addDate
            FROM yee_question 
            WHERE id = ? AND schoolId = ? 
            """;

            st = connection.prepareStatement(sql);
            st.setObject(1, id);
            st.setObject(2, schoolId);

            // 5. 执行查询
            rs = st.executeQuery();
            if (!rs.next()) {
                return Result.error("试题不存在或已被删除");
            }

            // 6. 映射结果到 YeeQuestion 对象
            YeeQuestion question = new YeeQuestion();

            question.setId(rs.getInt("id"));
            question.setTopic(rs.getString("topic"));
            question.setType(rs.getInt("type"));
            question.setLevel(rs.getInt("level"));
            question.setScore(rs.getInt("score"));
            question.setAnalysis(rs.getString("analysis"));
            question.setPid(rs.getInt("pid"));
            question.setTitle(rs.getString("title"));
            question.setUpload(rs.getString("upload"));
            question.setSchoolId(rs.getInt("schoolId"));
            question.setCateBid(rs.getInt("cateBid"));
            question.setCateMid(rs.getInt("cateMid"));
            question.setCreateId(rs.getInt("createId"));
//            question.setDeleted(rs.getInt("deleted"));

            // JSON 反序列化
            String optionJson = rs.getString("option");
            if (optionJson != null && !optionJson.isEmpty()) {
                List<Map<String, Object>> optionList = jsonUtil.fromJsonToListOfMap(optionJson);
                question.setOption(optionList);
            }

            String categoryIdJson = rs.getString("categoryId");
            if (categoryIdJson != null && !categoryIdJson.isEmpty()) {
                question.setCategoryId(jsonUtil.fromJsonToList(categoryIdJson, Integer.class)); // List<Integer>
            }

            return Result.success(question);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("数据库查询错误: " + e.getMessage(), e);
        } finally {
            // 7. 安全关闭资源
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignored) {}
            }
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ignored) {}
            }
            if (connection != null && !connection.isClosed()) {
                try {
                    connection.close();
                } catch (SQLException ignored) {}
            }
        }
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
                                          Integer level, Integer cateBid, Integer cateMid) throws Exception {
        // 验证参数
        if (connection == null) {
            throw new Exception("数据库连接不能为空");
        }

        // 调用新的selectAll方法获取分页数据
        return selectAll(connection, schoolId, pageSize, pageNum, topic, createId, type, level, cateBid, cateMid);
    }

    @Override
    public Result importQuestions(int schoolId, int createId, MultipartFile file, Integer cateBid, Integer cateMid) throws Exception {
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
                    YeeQuestion question = convertToYeeQuestion(questionVO, schoolId, createId, cateBid, cateMid);

                    // 插入数据库
                    if (add(connection, question)) {
                        successCount++;
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

            // 返回结果
            if (failCount == 0) {
                return Result.success("成功导入" + successCount + "条试题数据");
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
    private YeeQuestion convertToYeeQuestion(QuestionExportVO questionVO, int schoolId, int createId, Integer cateBid, Integer cateMid) {
        YeeQuestion question = new YeeQuestion();

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
        question.setCreateId(createId);
        question.setAddTime(LocalDateTime.now());
//        question.setAddDate(LocalDate.now().toString()); // 数据库自行生成

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

        // 漏选计分模式
        if ("多选".equals(questionVO.getTypeName())){
                question.setScoreMode(1);
        }

        return question;
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

    // 批量删除方法实现
    @Override
    public Result batchDelete(int schoolId, List<Integer> ids) throws Exception {
        // 1. 参数校验
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的试题");
        }
        if (schoolId <= 0) {
            return Result.error("学校ID无效");
        }

        // 2. 验证学校是否存在且已审核
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 3. 获取对应学校的数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取学校数据库连接");
        }

        try {
            // 开启事务
            connection.setAutoCommit(false);

            // 4. 构建批量删除 SQL（带 schoolId 防越权）
//            StringBuilder sqlBuilder = new StringBuilder("UPDATE yee_question SET deleted = 1 WHERE schoolId = ? AND id IN (");
            StringBuilder sqlBuilder = new StringBuilder("DELETE FROM yee_question WHERE schoolId = ? AND id IN (");
            for (int i = 0; i < ids.size(); i++) {
                sqlBuilder.append("?");
                if (i < ids.size() - 1) {
                    sqlBuilder.append(",");
                }
            }
            sqlBuilder.append(")");

            try (PreparedStatement st = connection.prepareStatement(sqlBuilder.toString())) {
                // 设置参数
                st.setObject(1, schoolId);
                for (int i = 0; i < ids.size(); i++) {
                    st.setObject(i + 2, ids.get(i));
                }

                // 5. 执行批量删除
                int rows = st.executeUpdate();
                if (rows == 0) {
                    return Result.error("删除失败，可能试题不存在、已被删除或无权限");
                }

                // 提交事务
                connection.commit();
                return Result.success("成功删除" + rows + "条试题记录");
            }

        } catch (SQLException e) {
            // 回滚
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }

            return Result.error("数据库操作失败，请稍后重试");
        } catch (Exception e) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }
            return Result.error("系统异常，请联系管理员");
        } finally {
            // 恢复并关闭
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
}
