package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeePaper;
import cn.xfywz.guozespring.entity.mhsch.YeePaperTopic;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeePaperService;
import cn.xfywz.guozespring.util.JsonUtil;
import cn.xfywz.guozespring.util.PageResult;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @Author: ChengLin
 */
@Service
public class YeePaperServiceImpl implements YeePaperService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    // 共享 Gson 实例
    private static final Gson GSON = new Gson();

    @Override
    public Result selectAll(int schoolId, Integer userId, String title, Integer type, Integer allow, Integer cateBid, Integer cateMid, Integer pageNo, Integer pageSize) throws Exception {
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
            // 3. 构建查询 SQL（count + list）
            StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) FROM yee_paper WHERE 1=1");
            StringBuilder listSqlBuilder = new StringBuilder("SELECT * FROM yee_paper WHERE 1=1");
            List<Object> parameters = new ArrayList<>();

            // 添加查询条件
            if (userId != null) {
                countSqlBuilder.append(" AND userId = ?");
                listSqlBuilder.append(" AND userId = ?");
                parameters.add(userId);
            }
            if (title != null && !title.trim().isEmpty()) {
                countSqlBuilder.append(" AND title LIKE ?");
                listSqlBuilder.append(" AND title LIKE ?");
                parameters.add("%" + title + "%");
            }
            if (type != null) {
                countSqlBuilder.append(" AND type = ?");
                listSqlBuilder.append(" AND type = ?");
                parameters.add(type);
            }
            if (allow != null) {
                countSqlBuilder.append(" AND allow = ?");
                listSqlBuilder.append(" AND allow = ?");
                parameters.add(allow);
            }
            if (cateBid != null) {
                countSqlBuilder.append(" AND cateBid = ?");
                listSqlBuilder.append(" AND cateBid = ?");
                parameters.add(cateBid);
            }
            if (cateMid != null) {
                countSqlBuilder.append(" AND cateMid = ?");
                listSqlBuilder.append(" AND cateMid = ?");
                parameters.add(cateMid);
            }

            // 排序
            listSqlBuilder.append(" ORDER BY addTime DESC");

            // 分页
            int offset = (pageNo - 1) * pageSize;
            listSqlBuilder.append(" LIMIT ? OFFSET ?");
            parameters.add(pageSize);
            parameters.add(offset);

            // 4. 查询总数
            PreparedStatement countSt = connection.prepareStatement(countSqlBuilder.toString());
            for (int i = 0; i < parameters.size() - 2; i++) { // 总数不需要分页参数
                countSt.setObject(i + 1, parameters.get(i));
            }
            ResultSet countRs = countSt.executeQuery();
            long total = 0;
            if (countRs.next()) {
                total = countRs.getLong(1);
            }
            countRs.close();
            countSt.close();

            // 5. 查询列表
            PreparedStatement listSt = connection.prepareStatement(listSqlBuilder.toString());
            for (int i = 0; i < parameters.size(); i++) {
                listSt.setObject(i + 1, parameters.get(i));
            }
            ResultSet listRs = listSt.executeQuery();
            List<YeePaper> list = new ArrayList<>();

            Gson gson = new Gson(); // 用于解析 JSON 字段
            while (listRs.next()) {
                YeePaper paper = new YeePaper();
                paper.setId(listRs.getInt("id"));
                paper.setUserId(listRs.getInt("userId"));
                paper.setTitle(listRs.getString("title"));
                paper.setTopicNumber(listRs.getInt("topicNumber"));
                paper.setScore(listRs.getInt("score"));
                paper.setType(listRs.getInt("type"));
                paper.setScope(listRs.getString("scope"));
                paper.setRemarks(listRs.getString("remarks"));
                paper.setAllow(listRs.getByte("allow"));
                paper.setAddTime(listRs.getObject("addTime", LocalDateTime.class));
                paper.setSchoolId(listRs.getInt("schoolId"));

                paper.setCateBid(listRs.getInt("cateBid"));
                paper.setCateMid(listRs.getInt("cateMid"));
                list.add(paper);
            }
            listRs.close();
            listSt.close();

            // 6. 返回分页结果
            PageResult<YeePaper> pageResult = new PageResult<>();
            pageResult.setTotal(total);
            pageResult.setRows(list);

            return Result.success(pageResult);

        } catch (Exception e) {
            throw new Exception("查询试卷列表失败: " + e.getMessage(), e);
        } finally {
            if (connection != null && !connection.isClosed()) {
                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    @Override
    public Result add(YeePaper yeePaper) throws Exception {

        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(yeePaper.getSchoolId());
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

            // 3. 检查试卷标题是否已存在（防重复）
//            String checkSql = "SELECT COUNT(*) FROM yee_paper WHERE userId = ? AND title = ? AND schoolId = ?";
//            try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
//                checkSt.setObject(1, yeePaper.getUserId());
//                checkSt.setObject(2, yeePaper.getTitle());
//                checkSt.setObject(3, yeePaper.getSchoolId());
//                try (ResultSet checkRs = checkSt.executeQuery()) {
//                    if (checkRs.next() && checkRs.getLong(1) > 0) {
//                        return Result.error("该试卷标题已存在");
//                    }
//                }
//            }

            // 4. 插入试卷主表
            String insertSql = """
                    INSERT INTO yee_paper 
                    (userId, title, topicNumber, score, type, scope, remarks, allow, addTime, schoolId, categoryId, cateBid, cateMid)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            Integer paperId;
            try (PreparedStatement insertSt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertSt.setObject(1, yeePaper.getUserId());
                insertSt.setObject(2, yeePaper.getTitle());
                insertSt.setObject(3, yeePaper.getTopicNumber() != null ? yeePaper.getTopicNumber() : 0);
                insertSt.setObject(4, yeePaper.getScore() != null ? yeePaper.getScore() : 0);
                insertSt.setObject(5, yeePaper.getType());
                insertSt.setObject(6, yeePaper.getScope());
                insertSt.setObject(7, yeePaper.getRemarks());
                insertSt.setObject(8, yeePaper.getAllow() != null ? yeePaper.getAllow() : 0);
                insertSt.setObject(9, yeePaper.getAddTime() != null ? yeePaper.getAddTime() : new Timestamp(System.currentTimeMillis()));
                insertSt.setObject(10, yeePaper.getSchoolId());
                insertSt.setObject(11, yeePaper.getCategoryId() != null ? GSON.toJson(yeePaper.getCategoryId()) : null);
                insertSt.setObject(12, yeePaper.getCateBid() != null ? yeePaper.getCateBid() : 0);
                insertSt.setObject(13, yeePaper.getCateMid() != null ? yeePaper.getCateMid() : 0);

                int rows = insertSt.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("添加试卷失败");
                }

                try (ResultSet generatedKeys = insertSt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        paperId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("创建试卷失败，未获取到主键");
                    }
                }
            }

            // 5. 查询题篮中的题目 ID 列表
            String basketSql = "SELECT exId FROM yee_basket WHERE userId = ?";
            List<Integer> exIdList = new ArrayList<>();
            try (PreparedStatement basketSt = connection.prepareStatement(basketSql)) {
                basketSt.setObject(1, yeePaper.getUserId());
                try (ResultSet rs = basketSt.executeQuery()) {
                    while (rs.next()) {
                        exIdList.add(rs.getInt("exId"));
                    }
                }
            }

            if (exIdList.isEmpty()) {
                // 提交事务（即使无题目，也算成功）
                connection.commit();
                return Result.success("试卷创建成功，但无题目", paperId);
            }

            // 6. 动态生成 IN 查询
            String placeholders = String.join(",", Collections.nCopies(exIdList.size(), "?"));
            String questionSql = "SELECT * FROM yee_question WHERE id IN (" + placeholders + ")";

            List<YeePaperTopic> topicList = new ArrayList<>();
            try (PreparedStatement questionSt = connection.prepareStatement(questionSql)) {
                for (int i = 0; i < exIdList.size(); i++) {
                    questionSt.setInt(i + 1, exIdList.get(i));
                }
                int i = 1;
                try (ResultSet rs = questionSt.executeQuery()) {
                    while (rs.next()) {
                        YeePaperTopic topic = new YeePaperTopic();
                        topic.setOid(rs.getInt("oid"));
                        topic.setTopic(rs.getString("topic"));
                        topic.setType(rs.getInt("type"));
                        topic.setLevel(rs.getInt("level"));
                        topic.setScore(rs.getInt("score"));
                        topic.setAnalysis(rs.getString("analysis"));
                        topic.setPid(rs.getInt("pid"));
                        topic.setPaperId(paperId);
                        topic.setTitle(rs.getString("title"));
                        topic.setUpload(rs.getString("upload"));
                        topic.setScoreMode(rs.getInt("scoreMode"));
                        topic.setSchoolId(yeePaper.getSchoolId());
                        topic.setCateBid(rs.getInt("cateBid"));
                        topic.setCateMid(rs.getInt("cateMid"));
                        topic.setNumber(i++);

                        topic.setOption(JsonUtil.parseOption(rs.getString("option")));
                        topic.setCategoryId(JsonUtil.parseList(rs.getString("categoryId"), Integer.class));
                        topic.setMissScore(null);
                        topic.setOption1(null);
                        topic.setOption2(null);
                        topic.setOption3(null);

                        topicList.add(topic);
                    }
                }
            }

            // 7. 批量插入题目
            String insertTopicSql = """
                    INSERT INTO yee_paper_topic 
                    (oid, topic, type, level, score, missScore, option1, option2, option3, analysis, pid, paperId, title, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid, number)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement insertTopicSt = connection.prepareStatement(insertTopicSql)) {
                for (YeePaperTopic topic : topicList) {
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
                    insertTopicSt.setObject(12, topic.getPaperId());
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
            int totalScore = topicList.stream().mapToInt(YeePaperTopic::getScore).sum();
            
            String updatePaperSql = "UPDATE yee_paper SET topicNumber = ?, score = ? WHERE id = ?";
            try (PreparedStatement updatePaperSt = connection.prepareStatement(updatePaperSql)) {
                updatePaperSt.setInt(1, topicNumber);
                updatePaperSt.setInt(2, totalScore);
                updatePaperSt.setInt(3, paperId);
                
                int updateRows = updatePaperSt.executeUpdate();
                if (updateRows != 1) {
                    throw new SQLException("更新试卷信息失败");
                }
            }

            // 提交事务
            connection.commit();

            return Result.success("试卷创建成功", paperId);

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
    public Result addBlank(YeePaper yeePaper) throws Exception {

        // 1. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(yeePaper.getSchoolId());
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

            // 3. 检查试卷标题是否已存在（防重复）
            String checkSql = "SELECT COUNT(*) FROM yee_paper WHERE userId = ? AND title = ? AND schoolId = ?";
            try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
                checkSt.setObject(1, yeePaper.getUserId());
                checkSt.setObject(2, yeePaper.getTitle());
                checkSt.setObject(3, yeePaper.getSchoolId());
                try (ResultSet checkRs = checkSt.executeQuery()) {
                    if (checkRs.next() && checkRs.getLong(1) > 0) {
                        return Result.error("该试卷标题已存在");
                    }
                }
            }

            // 4. 插入试卷主表（创建空白试卷）
            String insertSql = """
                INSERT INTO yee_paper 
                (userId, title, topicNumber, score, type, scope, remarks, allow, addTime, schoolId, categoryId, cateBid, cateMid)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            Integer paperId;
            try (PreparedStatement insertSt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertSt.setObject(1, yeePaper.getUserId());
                insertSt.setObject(2, yeePaper.getTitle());
                insertSt.setObject(3, yeePaper.getTopicNumber() != null ? yeePaper.getTopicNumber() : 0);
                insertSt.setObject(4, yeePaper.getScore() != null ? yeePaper.getScore() : 0);
                insertSt.setObject(5, yeePaper.getType());
                insertSt.setObject(6, yeePaper.getScope());
                insertSt.setObject(7, yeePaper.getRemarks());
                insertSt.setObject(8, yeePaper.getAllow() != null ? yeePaper.getAllow() : 0);
                insertSt.setObject(9, yeePaper.getAddTime() != null ? yeePaper.getAddTime() : new Timestamp(System.currentTimeMillis()));
                insertSt.setObject(10, yeePaper.getSchoolId());
                insertSt.setObject(11, yeePaper.getCategoryId() != null ? GSON.toJson(yeePaper.getCategoryId()) : null);
                insertSt.setObject(12, yeePaper.getCateBid() != null ? yeePaper.getCateBid() : 0);
                insertSt.setObject(13, yeePaper.getCateMid() != null ? yeePaper.getCateMid() : 0);

                int rows = insertSt.executeUpdate();
                if (rows == 0) {
                    throw new SQLException("添加试卷失败");
                }

                try (ResultSet generatedKeys = insertSt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        paperId = generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("创建试卷失败，未获取到主键");
                    }
                }
            }

            // 提交事务
            connection.commit();

            return Result.success("空白试卷创建成功", paperId);

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
    public Result update(YeePaper yeePaper) throws Exception {
        Integer paperId = yeePaper.getId();
        Integer userId = yeePaper.getUserId();
        Integer schoolId = yeePaper.getSchoolId();

        if (paperId == null || userId == null || schoolId == null) {
            return Result.error("试卷ID、用户ID、学校ID不能为空");
        }

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
            // 开启事务
            connection.setAutoCommit(false);

            // 3. 检查试卷是否存在（且属于该用户）→ 加 FOR UPDATE
            String checkSql = "SELECT id FROM yee_paper WHERE id = ? AND userId = ? AND schoolId = ? FOR UPDATE";
            try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
                checkSt.setObject(1, paperId);
                checkSt.setObject(2, userId);
                checkSt.setObject(3, schoolId);
                try (ResultSet rs = checkSt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("试卷不存在或无权限修改");
                    }
                }
            }

            // 4. 更新数据
            String updateSql = """
                    UPDATE yee_paper SET 
                        title = ?, topicNumber = ?, score = ?, type = ?, scope = ?, 
                        remarks = ?, allow = ?, cateBid = ?, cateMid = ?, categoryId = ?
                    WHERE id = ?
                    """;

            try (PreparedStatement updateSt = connection.prepareStatement(updateSql)) {
                updateSt.setObject(1, yeePaper.getTitle());
                updateSt.setObject(2, yeePaper.getTopicNumber());
                updateSt.setObject(3, yeePaper.getScore()); // ✅ 允许 null，数据库决定默认值
                updateSt.setObject(4, yeePaper.getType());
                updateSt.setObject(5, yeePaper.getScope());
                updateSt.setObject(6, yeePaper.getRemarks());
                updateSt.setObject(7, yeePaper.getAllow());
                updateSt.setObject(8, yeePaper.getCateBid());
                updateSt.setObject(9, yeePaper.getCateMid());
                updateSt.setObject(10, yeePaper.getCategoryId() != null ? JsonUtil.toJson(yeePaper.getCategoryId()) : null);
                updateSt.setObject(11, paperId);

                int rows = updateSt.executeUpdate();
                if (rows != 1) {
                    return Result.error("更新失败，可能数据异常");
                }
            }

            // 提交事务
            connection.commit();
            return Result.success("修改成功");

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
            return Result.error("系统异常，请稍后重试");
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
    public Result delete(int schoolId, int id, Integer userId) throws Exception {
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
            // 开启事务
            connection.setAutoCommit(false);

            // 3. 检查试卷是否存在（且属于该用户）→ 加 FOR UPDATE 锁
            String checkSql = "SELECT COUNT(*) FROM yee_paper WHERE id = ? AND userId = ? AND schoolId = ? FOR UPDATE";
            try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
                checkSt.setObject(1, id);
                checkSt.setObject(2, userId);
                checkSt.setObject(3, schoolId);
                try (ResultSet rs = checkSt.executeQuery()) {
                    if (!rs.next() || rs.getLong(1) == 0) {
                        return Result.error("试卷不存在或无权限删除");
                    }
                }
            }

            // 4. 先删除子表：yee_paper_topic
            String deleteTopicSql = "DELETE FROM yee_paper_topic WHERE paperId = ?";
            try (PreparedStatement deleteTopicSt = connection.prepareStatement(deleteTopicSql)) {
                deleteTopicSt.setObject(1, id);
                deleteTopicSt.executeUpdate(); // 删除所有关联题目
            }

            // 5. 再删除主表：yee_paper
            String deletePaperSql = "DELETE FROM yee_paper WHERE id = ?";
            try (PreparedStatement deletePaperSt = connection.prepareStatement(deletePaperSql)) {
                deletePaperSt.setObject(1, id);
                int rows = deletePaperSt.executeUpdate();
                if (rows == 0) {
                    return Result.error("删除失败，试卷可能已被删除");
                }
            }

            // 提交事务
            connection.commit();
            return Result.success("试卷及关联题目删除成功");

        } catch (Exception e) {
            // 回滚事务
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }
            throw new Exception("删除试卷失败: " + e.getMessage(), e);
        } finally {
            // 恢复自动提交并关闭连接
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
    public Result allow(int schoolId, int id, Integer userId, Byte allow) throws Exception {
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
            // 开启事务（显式控制，更安全）
            connection.setAutoCommit(false);

            // 先检查试卷是否存在且属于该用户（加 FOR UPDATE 防并发）
            String checkSql = "SELECT id FROM yee_paper WHERE id = ? AND userId = ? AND schoolId = ? FOR UPDATE";
            try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
                checkSt.setObject(1, id);
                checkSt.setObject(2, userId);
                checkSt.setObject(3, schoolId);
                try (ResultSet rs = checkSt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("试卷不存在或无权限操作");
                    }
                }
            }

            // 执行更新
            String updateSql = "UPDATE yee_paper SET allow = ? WHERE id = ? AND userId = ? AND schoolId = ?";
            try (PreparedStatement updateSt = connection.prepareStatement(updateSql)) {
                updateSt.setObject(1, allow);
                updateSt.setObject(2, id);
                updateSt.setObject(3, userId);
                updateSt.setObject(4, schoolId);

                int rows = updateSt.executeUpdate();
                // 因为前面已检查存在性，这里 rows 应该为 1
                if (rows != 1) {
                    return Result.error("更新失败，可能数据异常");
                }
            }

            // 提交事务
            connection.commit();
            return Result.success("审核状态修改成功");

        } catch (SQLException e) {
            // 回滚事务
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }
            // 记录日志
            e.printStackTrace();
            return Result.error("数据库操作失败，请稍后重试");
        } catch (Exception e) {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.rollback();
                }
            } catch (SQLException rollbackEx) {
                System.err.println("事务回滚失败: " + rollbackEx.getMessage());
            }
            e.printStackTrace();
            return Result.error("系统异常: " + e.getMessage());
        } finally {
            // 恢复自动提交并关闭连接
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("关闭数据库连接失败: " + e.getMessage());
            }
        }
    }

    @Override
    public Result getById(int schoolId, int id, Integer userId) throws Exception {
        // 1. 参数校验
        if (schoolId <= 0) return Result.error("学校ID无效");
        if (id <= 0) return Result.error("试卷ID无效");
        if (userId == null) return Result.error("用户ID不能为空");

        // 2. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        String sql = "SELECT id, userId, title, topicNumber, score, type, scope, " +
                "remarks, allow, addTime, schoolId, cateBid, cateMid " +
                "FROM yee_paper " +
                "WHERE id = ? AND userId = ? AND schoolId = ?";

        try (connection; PreparedStatement st = connection.prepareStatement(sql)) {
            st.setObject(1, id);
            st.setObject(2, userId);
            st.setObject(3, schoolId);

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    YeePaper yeePaper = new YeePaper();
                    yeePaper.setId(rs.getInt("id"));
                    yeePaper.setUserId(rs.getInt("userId"));
                    yeePaper.setTitle(rs.getString("title"));
                    yeePaper.setTopicNumber(rs.getObject("topicNumber", Integer.class));
                    yeePaper.setScore(rs.getObject("score", Integer.class));
                    yeePaper.setType(rs.getObject("type", Integer.class));
                    yeePaper.setScope(rs.getString("scope"));
                    yeePaper.setRemarks(rs.getString("remarks"));
                    yeePaper.setAllow(rs.getByte("allow"));
                    Timestamp ts = rs.getTimestamp("addTime");
                    yeePaper.setAddTime(ts != null ? ts.toLocalDateTime() : null);
                    yeePaper.setSchoolId(rs.getInt("schoolId"));
                    yeePaper.setCateBid(rs.getInt("cateBid"));
                    yeePaper.setCateMid(rs.getInt("cateMid"));

                    return Result.success(yeePaper);
                } else {
                    return Result.error("试卷不存在或无权限访问");
                }
            }
        } catch (SQLException e) {
            return Result.error("数据库查询失败，请稍后重试");
        } catch (Exception e) {
            return Result.error("系统异常，请联系管理员");
        } finally {
            // 安全关闭连接
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    @Override
    public Result changeTeacher(int schoolId, int id, Integer userId, Integer teacherId) throws Exception {
        // 1. 参数校验
        if (schoolId <= 0) {
            return Result.error("学校ID无效");
        }
        if (id <= 0) {
            return Result.error("试卷ID无效");
        }
        if (userId == null) {
            return Result.error("用户ID不能为空");
        }

        if (teacherId == null) {
            return Result.error("新教师ID不能为空");
        }

        // 2. 验证学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 3. 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        // 使用 try-with-resources 自动关闭 PreparedStatement 和 ResultSet（如果有）
        // 注意：connection 不在 try-with-resources 中，需手动 close
        PreparedStatement st = null;
        try {
            String sql = "UPDATE yee_paper SET userId = ? WHERE id = ? AND userId = ? AND schoolId = ?";
            st = connection.prepareStatement(sql);

            // 设置参数（使用类型安全方法）
            st.setObject(1, teacherId); // 允许 null
            st.setInt(2, id);
            st.setInt(3, userId);
            st.setInt(4, schoolId);

            int rows = st.executeUpdate();

            if (rows == 0) {
                // 可选：查询记录是否存在，给出更友好提示
                String checkSql = "SELECT COUNT(*) FROM yee_paper WHERE id = ? AND schoolId = ? AND userId = ?";
                try (PreparedStatement checkSt = connection.prepareStatement(checkSql)) {
                    checkSt.setInt(1, id);
                    checkSt.setInt(2, schoolId);
                    checkSt.setInt(3, userId);
                    try (ResultSet rs = checkSt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            return Result.error("更新失败：原用户ID不匹配，可能已被修改");
                        } else {
                            return Result.error("更新失败：试卷不存在或不属于该学校");
                        }
                    }
                }
            }

            return Result.success("更换教师成功");

        } catch (SQLException e) {
            e.printStackTrace();
            return Result.error("数据库更新失败：" + e.getMessage());
        } finally {
            // 关闭 PreparedStatement
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            // 关闭 Connection
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
