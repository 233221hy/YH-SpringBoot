package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.dto.BatchDiscussScoreReq;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeDiscuss;
import cn.xfywz.guozespring.entity.vo.YeeDiscussVo;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeDiscussService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class YeeDiscussServiceImpl implements YeeDiscussService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private DatabaseUtil databaseUtil;

    @Override
    public Result list(int pageNum, int pageSize, int schoolId, long courseId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        Connection connection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        PreparedStatement countSt = null;
        ResultSet countRs = null;

        try {
            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 计算分页偏移
            int offset = (pageNum - 1) * pageSize;

            // 主查询：关联 yee_manage 获取老师姓名和头像
            // 优化后的极速查询（无join膨胀、无慢group、无count distinct）
            String sql = """
                SELECT 
                    d.id,
                    d.title,
                    d.teacherId,
                    d.addTime,
                    d.content,
                    d.images,
                    d.classId,
                    d.courseId,
                    d.top,
                    d.files,
                    d.isDelete,
                    d.changeTime,
                    d.schoolId,
                    d.addDate,
                    IFNULL((SELECT COUNT(*) FROM yee_discuss_reply r WHERE r.discussId = d.id AND r.isDelete=0), 0) AS replyCount,
                    IFNULL((SELECT COUNT(DISTINCT r.userId) FROM yee_discuss_reply r WHERE r.discussId = d.id AND r.isDelete=0), 0) AS participantCount,
                    m.name AS teacherName 
                FROM yee_discuss d
                LEFT JOIN yee_manage m ON d.teacherId = m.id AND d.schoolId = m.schoolId
                WHERE d.courseId = ? AND d.isDelete = 0
                ORDER BY d.top DESC, d.addTime DESC
                LIMIT ? OFFSET ?
                """;

            // 总数查询（无需改）
            String countSql = """
            SELECT COUNT(*) 
            FROM yee_discuss 
            WHERE courseId = ? AND isDelete = 0
            """;

            // 执行总数查询
            countSt = connection.prepareStatement(countSql);
            countSt.setLong(1, courseId);
            countRs = countSt.executeQuery();
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }

            // 执行主查询
            st = connection.prepareStatement(sql);
            st.setLong(1, courseId);
            st.setInt(2, pageSize);
            st.setInt(3, offset);
            rs = st.executeQuery();

            List<YeeDiscussVo> discusses = new ArrayList<>();
            while (rs.next()) {
                YeeDiscussVo discuss = new YeeDiscussVo();
                discuss.setId(rs.getLong("id"));
                discuss.setTitle(rs.getString("title"));
                discuss.setTeacherId(rs.getLong("teacherId"));
                discuss.setAddTime(rs.getTimestamp("addTime"));
                discuss.setContent(rs.getString("content"));
                discuss.setImages(rs.getString("images"));
                discuss.setClassId(rs.getLong("classId"));
                discuss.setCourseId(rs.getLong("courseId"));
                discuss.setTop(rs.getLong("top"));
                discuss.setFiles(rs.getString("files"));
                discuss.setIsDelete(rs.getLong("isDelete"));
                discuss.setChangeTime(rs.getLong("changeTime"));
                discuss.setSchoolId(rs.getLong("schoolId"));
                discuss.setAddDate(rs.getDate("addDate"));

                // 新增：老师信息
                discuss.setTeacherName(rs.getString("teacherName"));

                // 设置统计字段
                discuss.setReplyCount(rs.getInt("replyCount"));
                discuss.setParticipantCount(rs.getInt("participantCount"));

                discusses.add(discuss);
            }

            return Result.success(discusses, (long) totalCount);

        } catch (SQLException e) {
            e.printStackTrace();
            return Result.error("数据库查询失败：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        } finally {
            // 安全关闭资源
            if (countRs != null) try { countRs.close(); } catch (SQLException ignored) {}
            if (countSt != null) try { countSt.close(); } catch (SQLException ignored) {}
            if (rs != null) try { rs.close(); } catch (SQLException ignored) {}
            if (st != null) try { st.close(); } catch (SQLException ignored) {}
            if (connection != null) try { connection.close(); } catch (SQLException ignored) {}
        }
    }

    @Override
    public Result add(YeeDiscuss yeeDiscuss) throws Exception {
        Connection connection = null;
        PreparedStatement insertDiscussStmt = null;
        PreparedStatement updateCountStmt = null;

        try {
            if (yeeDiscuss == null || yeeDiscuss.getSchoolId() <= 0) {
                return Result.error("学校ID无效");
            }

            SlSchool slSchool = slSchoolMapper.selectById((int) yeeDiscuss.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            connection.setAutoCommit(false);

            StringBuilder columns = new StringBuilder("INSERT INTO yee_discuss (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();

            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(yeeDiscuss.getSchoolId());

            Long courseId = null;
            if (yeeDiscuss.getCourseId() > 0) {
                columns.append("`courseId`, ");
                values.append("?, ");
                courseId = yeeDiscuss.getCourseId();
                parameters.add(courseId);
            }

            if (yeeDiscuss.getTitle() != null) {
                columns.append("`title`, ");
                values.append("?, ");
                parameters.add(yeeDiscuss.getTitle().trim());
            }

            if (yeeDiscuss.getContent() != null) {
                columns.append("`content`, ");
                values.append("?, ");
                parameters.add(yeeDiscuss.getContent().trim());
            }

            if (yeeDiscuss.getTeacherId() > 0) {
                columns.append("`teacherId`, ");
                values.append("?, ");
                parameters.add(yeeDiscuss.getTeacherId());
            }

            if (yeeDiscuss.getClassId() > 0) {
                columns.append("`classId`, ");
                values.append("?, ");
                parameters.add(yeeDiscuss.getClassId());
            }

            if (yeeDiscuss.getImages() != null) {
                columns.append("`images`, ");
                values.append("?, ");
                parameters.add(yeeDiscuss.getImages());
            }

            // 🔥 强制生成 files JSON，不依赖 this.files 字段
            if (yeeDiscuss.getAttachFiles() != null) {
                String filesJson;
                if (yeeDiscuss.getAttachFiles().isEmpty()) {
                    filesJson = "[]";
                } else {
                    filesJson = new Gson().toJson(yeeDiscuss.getAttachFiles());
                }
                columns.append("`files`, ");
                values.append("?, ");
                parameters.add(filesJson);
            }

            if (yeeDiscuss.getTop() >= 0) {
                columns.append("`top`, ");
                values.append("?, ");
                parameters.add(yeeDiscuss.getTop());
            }

            columns.append("`isDelete`, ");
            values.append("?, ");
            parameters.add(0L);

            if (yeeDiscuss.getChangeTime() > 0) {
                columns.append("`changeTime`, ");
                values.append("?, ");
                parameters.add(yeeDiscuss.getChangeTime());
            }

            columns.append("`addTime`, ");
            values.append("?, ");
            Timestamp addTime = yeeDiscuss.getAddTime() != null
                    ? yeeDiscuss.getAddTime()
                    : new Timestamp(System.currentTimeMillis());
            parameters.add(addTime);

            columns.setLength(columns.length() - 2);
            values.setLength(values.length() - 2);

            String insertSql = columns.toString() + ") " + values.toString() + ")";


            insertDiscussStmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);

            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof String) {
                    insertDiscussStmt.setString(i + 1, (String) param);
                } else if (param instanceof Long) {
                    insertDiscussStmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Integer) {
                    insertDiscussStmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof java.sql.Timestamp) {
                    insertDiscussStmt.setTimestamp(i + 1, (java.sql.Timestamp) param);
                }
            }

            int rowsInserted = insertDiscussStmt.executeUpdate();
            if (rowsInserted <= 0) {
                connection.rollback();
                return Result.error("添加失败");
            }

            try (ResultSet generatedKeys = insertDiscussStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    yeeDiscuss.setId(generatedKeys.getLong(1));
                }
            }

            if (courseId != null && courseId > 0) {
                String updateSql = "UPDATE yee_course_student SET discussCount = discussCount + 1 WHERE schoolId = ? AND courseId = ?";
                updateCountStmt = connection.prepareStatement(updateSql);
                updateCountStmt.setLong(1, yeeDiscuss.getSchoolId());
                updateCountStmt.setLong(2, courseId);
                updateCountStmt.executeUpdate();
            }

            connection.commit();
            return Result.success("添加成功");

        } catch (Exception e) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ignored) {}
            }
            e.printStackTrace();
            return Result.error("添加失败：" + e.getMessage());
        } finally {
            try {
                if (insertDiscussStmt != null) insertDiscussStmt.close();
                if (updateCountStmt != null) updateCountStmt.close();
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public Result update(YeeDiscuss yeeDiscuss) throws Exception {
        if (yeeDiscuss == null || yeeDiscuss.getId() <= 0) {
            return Result.error("讨论ID无效");
        }

        try {
            // 校验学校是否存在且已审核
            SlSchool slSchool = slSchoolMapper.selectById((int) yeeDiscuss.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            // 🔥 关键修复：强制同步 attachFiles 到 files 字段
            if (yeeDiscuss.getAttachFiles() != null) {
                yeeDiscuss.setAttachFiles(yeeDiscuss.getAttachFiles());
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            StringBuilder sql = new StringBuilder("UPDATE yee_discuss SET ");
            List<Object> parameters = new ArrayList<>();

            // 标题（允许传空字符串表示清空）
            if (yeeDiscuss.getTitle() != null) {
                sql.append("`title` = ?, ");
                parameters.add(yeeDiscuss.getTitle().trim());
            }

            // 内容（允许传空字符串表示清空）
            if (yeeDiscuss.getContent() != null) {
                sql.append("`content` = ?, ");
                parameters.add(yeeDiscuss.getContent().trim());
            }

            // teacherId（>0 才更新）
            if (yeeDiscuss.getTeacherId() > 0) {
                sql.append("`teacherId` = ?, ");
                parameters.add(yeeDiscuss.getTeacherId());
            }

            // classId（>0 才更新）
            if (yeeDiscuss.getClassId() > 0) {
                sql.append("`classId` = ?, ");
                parameters.add(yeeDiscuss.getClassId());
            }

            // courseId（>0 才更新；注意：若业务不允许修改 courseId，应移除此段）
            if (yeeDiscuss.getCourseId() > 0) {
                sql.append("`courseId` = ?, ");
                parameters.add(yeeDiscuss.getCourseId());
            }

            // images：只要传了（包括 ""），就更新（"" 表示清空图片）
            if (yeeDiscuss.getImages() != null) {
                sql.append("`images` = ?, ");
                parameters.add(yeeDiscuss.getImages());
            }

            // 🔥 关键修复：使用已同步的 files 字段
            String filesJson = yeeDiscuss.getFiles();
            if (filesJson != null) {
                sql.append("`files` = ?, ");
                parameters.add(filesJson);
            }

            // 置顶状态（top >= 0 才更新）
            if (yeeDiscuss.getTop() >= 0) {
                sql.append("`top` = ?, ");
                parameters.add(yeeDiscuss.getTop());
            }

            // changeTime（>0 才更新）
            if (yeeDiscuss.getChangeTime() > 0) {
                sql.append("`changeTime` = ?, ");
                parameters.add(yeeDiscuss.getChangeTime());
            }

            // addTime（一般不建议更新，但保留逻辑）
            if (yeeDiscuss.getAddTime() != null) {
                sql.append("`addTime` = ?, ");
                parameters.add(yeeDiscuss.getAddTime());
            }

            // 如果没有字段需要更新
            if (parameters.isEmpty()) {
                connection.close();
                return Result.error("没有可更新的字段");
            }

            // 构建 WHERE 条件：防止越权（必须属于该学校 + 指定ID）
            sql.delete(sql.length() - 2, sql.length()); // 移除最后的 ", "
            sql.append(" WHERE id = ? AND schoolId = ?");
            parameters.add(yeeDiscuss.getId());
            parameters.add(yeeDiscuss.getSchoolId());

            // 执行更新
            PreparedStatement st = connection.prepareStatement(sql.toString());
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof String) {
                    st.setString(i + 1, (String) param);
                } else if (param instanceof Long) {
                    st.setLong(i + 1, (Long) param);
                } else if (param instanceof Integer) {
                    st.setInt(i + 1, (Integer) param);
                } else if (param instanceof java.sql.Timestamp) {
                    st.setTimestamp(i + 1, (java.sql.Timestamp) param);
                }
                // 注意：如果还有其他类型（如 Date），需补充
            }

            int rowsUpdated = st.executeUpdate();
            st.close();
            connection.close();

            if (rowsUpdated > 0) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败：记录不存在或无权修改");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result delete(long id, int schoolId) throws Exception {
        Connection connection = null;
        PreparedStatement selectDiscussStmt = null;
        PreparedStatement updateRepliesStmt = null;
        PreparedStatement deleteScoreStmt = null; // 新增：用于删除评分
        PreparedStatement updateCountStmt = null;
        PreparedStatement updateDiscussStmt = null;

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            connection.setAutoCommit(false); // 开启事务

            // 1. 查询讨论是否存在，并获取 courseId
            String selectSql = "SELECT courseId FROM yee_discuss WHERE id = ? AND schoolId = ?";
            selectDiscussStmt = connection.prepareStatement(selectSql);
            selectDiscussStmt.setLong(1, id);
            selectDiscussStmt.setInt(2, schoolId);

            Long courseId = null;
            boolean discussExists = false;
            try (ResultSet rs = selectDiscussStmt.executeQuery()) {
                if (rs.next()) {
                    discussExists = true;
                    courseId = rs.getLong("courseId");
                }
            }

            if (!discussExists) {
                connection.rollback();
                return Result.error("删除失败：讨论不存在或不属于该校");
            }

            // 2. 逻辑删除所有相关回复（yee_discuss_reply）
            String deleteRepliesSql = "UPDATE yee_discuss_reply SET isDelete = 1 WHERE discussId = ? AND schoolId = ?";
            updateRepliesStmt = connection.prepareStatement(deleteRepliesSql);
            updateRepliesStmt.setLong(1, id);
            updateRepliesStmt.setInt(2, schoolId);
            updateRepliesStmt.executeUpdate();

            // >>> 新增：3. 物理删除对应的讨论评分记录（yee_discuss_score） <<<
            String deleteScoreSql = "DELETE FROM yee_discuss_score WHERE discussId = ? AND schoolId = ?";
            deleteScoreStmt = connection.prepareStatement(deleteScoreSql);
            deleteScoreStmt.setLong(1, id);
            deleteScoreStmt.setInt(2, schoolId);
            deleteScoreStmt.executeUpdate();

            // 4. 如果讨论关联了课程，减少所有选课学生的 discussCount
            if (courseId != null && courseId > 0) {
                String updateCountSql = """
                UPDATE yee_course_student 
                SET discussCount = GREATEST(discussCount - 1, 0) 
                WHERE schoolId = ? AND courseId = ?
                """;
                updateCountStmt = connection.prepareStatement(updateCountSql);
                updateCountStmt.setInt(1, schoolId);
                updateCountStmt.setLong(2, courseId);
                updateCountStmt.executeUpdate();
            }

            // 5. 逻辑删除讨论本身
            String deleteDiscussSql = "UPDATE yee_discuss SET isDelete = 1 WHERE id = ? AND schoolId = ?";
            updateDiscussStmt = connection.prepareStatement(deleteDiscussSql);
            updateDiscussStmt.setLong(1, id);
            updateDiscussStmt.setInt(2, schoolId);
            int rowsUpdated = updateDiscussStmt.executeUpdate();

            if (rowsUpdated > 0) {
                connection.commit();
                return Result.success("删除成功");
            } else {
                connection.rollback();
                return Result.error("删除失败：讨论已被删除或权限不足");
            }

        } catch (Exception e) {
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ignored) {
            }
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        } finally {
            try {
                if (selectDiscussStmt != null) selectDiscussStmt.close();
                if (updateRepliesStmt != null) updateRepliesStmt.close();
                if (deleteScoreStmt != null) deleteScoreStmt.close(); // 关闭新增的 stmt
                if (updateCountStmt != null) updateCountStmt.close();
                if (updateDiscussStmt != null) updateDiscussStmt.close();
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public Result like(int schoolId, long courseId, String title) {
        Connection connection = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        PreparedStatement countSt = null;
        ResultSet countRs = null;

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 构造模糊查询关键字
            String searchTitle = "%" + (title != null ? title : "") + "%";

            // 主查询：关联老师信息 + 统计字段
            String sql = """
            SELECT 
                d.id,
                d.title,
                d.teacherId,
                d.addTime,
                d.content,
                d.images,
                d.classId,
                d.courseId,
                d.top,
                d.files,
                d.isDelete,
                d.changeTime,
                d.schoolId,
                d.addDate,
                COALESCE(COUNT(r.id), 0) AS replyCount,
                COALESCE(COUNT(DISTINCT r.userId), 0) AS participantCount,
                m.name AS teacherName 
            FROM yee_discuss d
            LEFT JOIN yee_manage m ON d.teacherId = m.id AND d.schoolId = m.schoolId
            LEFT JOIN yee_discuss_reply r ON d.id = r.discussId AND r.isDelete = 0
            WHERE d.courseId = ? 
              AND d.title LIKE ? 
              AND d.isDelete = 0
            GROUP BY d.id, d.teacherId, d.title, d.addTime, d.content, d.images, 
                     d.classId, d.courseId, d.top, d.files, d.changeTime, d.schoolId, d.addDate,
                     m.name, m.avatar
            ORDER BY d.top DESC, d.addTime DESC
            """;

            // 修正后的总数查询：必须与主查询的 GROUP BY 逻辑一致
            String countSql = """
            SELECT COUNT(*)
            FROM yee_discuss d
            WHERE d.courseId = ? 
              AND d.title LIKE ? 
              AND d.isDelete = 0
            """;

            // 执行总数查询
            countSt = connection.prepareStatement(countSql);
            countSt.setLong(1, courseId);
            countSt.setString(2, searchTitle);
            countRs = countSt.executeQuery();

            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }

            // 执行主查询
            st = connection.prepareStatement(sql);
            st.setLong(1, courseId);
            st.setString(2, searchTitle);
            rs = st.executeQuery();

            List<YeeDiscussVo> discusses = new ArrayList<>();
            while (rs.next()) {
                YeeDiscussVo discuss = new YeeDiscussVo();
                discuss.setId(rs.getLong("id"));
                discuss.setTitle(rs.getString("title"));
                discuss.setTeacherId(rs.getLong("teacherId"));
                discuss.setAddTime(rs.getTimestamp("addTime"));
                discuss.setContent(rs.getString("content"));
                discuss.setImages(rs.getString("images"));
                discuss.setClassId(rs.getLong("classId"));
                discuss.setCourseId(rs.getLong("courseId"));
                discuss.setTop(rs.getLong("top"));
                discuss.setFiles(rs.getString("files"));
                discuss.setIsDelete(rs.getLong("isDelete"));
                discuss.setChangeTime(rs.getLong("changeTime"));
                discuss.setSchoolId(rs.getLong("schoolId"));
                discuss.setAddDate(rs.getDate("addDate"));

                // 新增：老师信息
                discuss.setTeacherName(rs.getString("teacherName"));

                // 设置统计字段
                discuss.setReplyCount(rs.getInt("replyCount"));
                discuss.setParticipantCount(rs.getInt("participantCount"));

                discusses.add(discuss);
            }

            return Result.success(discusses, (long) totalCount);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("搜索失败：" + e.getMessage());
        } finally {
            // 安全关闭资源
            if (countRs != null) try { countRs.close(); } catch (Exception ignored) {}
            if (countSt != null) try { countSt.close(); } catch (Exception ignored) {}
            if (rs != null) try { rs.close(); } catch (Exception ignored) {}
            if (st != null) try { st.close(); } catch (Exception ignored) {}
            if (connection != null) try { connection.close(); } catch (Exception ignored) {}
        }
    }

    @Override
    public Result batchUpdateScore(Integer schoolId, Long discussId,
                                   List<BatchDiscussScoreReq.ScoreItem> scores) {
        // 1. 参数基础校验
        if (schoolId == null || discussId == null || scores == null || scores.isEmpty()) {
            return Result.error("参数不完整");
        }

        // 3. 逐项校验分数合法性和用户ID
        for (BatchDiscussScoreReq.ScoreItem item : scores) {
            if (item.getUserId() == null || item.getScore() == null) {
                return Result.error("userId 或 score 不能为空");
            }
            BigDecimal score = item.getScore();
            if (score.compareTo(BigDecimal.ZERO) < 0 ||
                    score.compareTo(new BigDecimal("100")) > 0) {
                return Result.error("分数必须在 0-100 之间");
            }
        }

        // 4. 构建批量插入/更新 SQL
        String sql = """
        INSERT INTO yee_discuss_score (schoolId, discussId, userId, score, scored)
        VALUES (?, ?, ?, ?, 1)
        ON DUPLICATE KEY UPDATE score = VALUES(score), scored = 1
        """;

        try {
            // 5. 使用链式调用执行批量操作
            int affected = databaseUtil.executeBatch(schoolId, sql, batch -> {
                for (BatchDiscussScoreReq.ScoreItem item : scores) {
                    batch.addBatch(paramsBuilder -> paramsBuilder
                            .param(schoolId)
                            .param(discussId)
                            .param(item.getUserId())
                            .param(item.getScore())
                    );
                }
            });

            return Result.success("评分成功");
        } catch (Exception e) {
            log.error("批量评分失败: schoolId={}, discussId={}", schoolId, discussId, e);
            return Result.error("评分失败：" + e.getMessage());
        }
    }
}
