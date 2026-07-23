package cn.xfywz.guozespring.service.student.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeDiscussReply;
import cn.xfywz.guozespring.entity.vo.StudentStats;
import cn.xfywz.guozespring.entity.vo.YeeDiscussReplyVo;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.student.YeeDiscussReplyService;
import cn.xfywz.guozespring.service.student.YeeStudentMangerService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/**
 * 课程主题讨论评论回复
 * @TableName yee_discuss_reply
 */
@Service
public class YeeDiscussReplyServiceImpl implements YeeDiscussReplyService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private static final Gson gson = new Gson();

    @Override
    public Result selectAll(int schoolId, int studentId, int type, int pageSize, int pageNum) throws Exception {
        // 1. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 2. 构建 WHERE 条件
        String baseWhere;
        if (type == 0) {
            baseWhere = "ydr.userId = ? AND ydr.reUserId = 0 AND ydr.isDelete = 0 AND ydr.schoolId = ?";
        } else if (type == 1) {
            baseWhere = "ydr.userId = ? AND ydr.reUserId != 0 AND ydr.isDelete = 0 AND ydr.schoolId = ?";
        } else {
            return Result.error("无效的 type 参数");
        }

        long total = 0;
        List<YeeDiscussReplyVo> result = new ArrayList<>();

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {

            // Step 1: 查总数
            String countSql = "SELECT COUNT(*) FROM yee_discuss_reply ydr WHERE " + baseWhere;
            try (PreparedStatement countSt = conn.prepareStatement(countSql)) {
                countSt.setInt(1, studentId);
                countSt.setInt(2, schoolId);
                try (ResultSet rs = countSt.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getLong(1);
                    }
                }
            }
            if (total == 0) {
                return Result.success(new ArrayList<>(), total);
            }

            // ====================== 原版正常代码 ======================
            String sql = """
            SELECT
                ydr.id,
                ydr.content,
                ydr.images,
                ydr.files,
                ydr.addTime,
                ydr.courseId,
                yc.name AS courseName,
                ydr.discussId,
                yd.title AS discussName,
                IFNULL(likeInfo.like_count, 0) AS like_count,
                IFNULL(replyInfo.reply_count, 0) AS reply_count
            FROM yee_discuss_reply ydr
            LEFT JOIN yee_course yc ON ydr.courseId = yc.id
            LEFT JOIN yee_discuss yd ON ydr.discussId = yd.id
            
            LEFT JOIN (
                SELECT replyId, COUNT(*) AS like_count
                FROM yee_reply_like
                WHERE schoolId = ?
                GROUP BY replyId
            ) likeInfo ON ydr.id = likeInfo.replyId
            
            LEFT JOIN (
                SELECT replyId, COUNT(*) AS reply_count
                FROM yee_discuss_reply
                WHERE schoolId = ? AND isDelete = 0
                GROUP BY replyId
            ) replyInfo ON ydr.id = replyInfo.replyId
            
            WHERE %s
            ORDER BY ydr.addTime DESC
            LIMIT ? OFFSET ?
        """.formatted(baseWhere);

            int offset = (pageNum - 1) * pageSize;
            try (PreparedStatement st = conn.prepareStatement(sql)) {
                int idx = 1;

                st.setInt(idx++, schoolId);
                st.setInt(idx++, schoolId);

                st.setInt(idx++, studentId);
                st.setInt(idx++, schoolId);

                st.setInt(idx++, pageSize);
                st.setInt(idx++, offset);

                try (ResultSet rs = st.executeQuery()) {
                    result = rsToYeeQuestion(rs);
                }
            }

            return Result.success(result, total);

        } catch (SQLException e) {
            e.printStackTrace();
            return Result.error("查询失败，请稍后重试");
        }
    }


//    @Override
//    public Result add(YeeDiscussReply yeeDiscussReply, Integer userType) {
//        // 1. 参数校验
//        if (yeeDiscussReply == null) {
//            return Result.error("评论内容不能为空");
//        }
//        if (yeeDiscussReply.getSchoolId() == null) {
//            return Result.error("schoolId 不能为空");
//        }
//        if (yeeDiscussReply.getDiscussId() == null) {
//            return Result.error("discussId 不能为空");
//        }
//        if (yeeDiscussReply.getCourseId() == null) {
//            return Result.error("courseId 不能为空");
//        }
//
//        // 2. 校验学校
//        SlSchool slSchool = slSchoolMapper.selectById(yeeDiscussReply.getSchoolId());
//        if (slSchool == null || slSchool.getAllow() != 1) {
//            return Result.error("学校不存在或未审核通过");
//        }
//
//        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
//
//            Timestamp now = new Timestamp(System.currentTimeMillis());
//
//            Integer realClassId = 0;
//            if (userType != null && userType == 1 &&
//                    yeeDiscussReply.getUserId() != null &&
//                    yeeDiscussReply.getUserId() > 0) {
//
//                realClassId = getStudentCourseClassIdFromRelation(
//                        connection,
//                        yeeDiscussReply.getSchoolId(),
//                        yeeDiscussReply.getUserId(),
//                        yeeDiscussReply.getCourseId()
//                );
//            }
//            // 非学生（如老师）或 userId 无效 → classId = 0
//
//            // 3. 构建动态 INSERT
//            List<String> columns = new ArrayList<>();
//            List<Object> parameters = new ArrayList<>();
//
//            // 必填字段
//            columns.add("`schoolId`"); parameters.add(yeeDiscussReply.getSchoolId());
//            columns.add("`discussId`"); parameters.add(yeeDiscussReply.getDiscussId());
//            columns.add("`courseId`"); parameters.add(yeeDiscussReply.getCourseId());
//            columns.add("`classId`"); parameters.add(realClassId);
//
//            // 可选字段
//            if (yeeDiscussReply.getUserId() != null && yeeDiscussReply.getUserId() > 0) {
//                columns.add("`userId`"); parameters.add(yeeDiscussReply.getUserId());
//            }
//            if (hasText(yeeDiscussReply.getContent())) {
//                columns.add("`content`"); parameters.add(yeeDiscussReply.getContent());
//            }
//            if (yeeDiscussReply.getImages() != null && !yeeDiscussReply.getImages().isEmpty()) {
//                columns.add("`images`");
//                parameters.add(gson.toJson(yeeDiscussReply.getImages())); // 仍是字符串数组
//            }
//            if (yeeDiscussReply.getAttachFiles() != null && !yeeDiscussReply.getAttachFiles().isEmpty()) {
//                columns.add("`files`");
//                parameters.add(yeeDiscussReply.getFilesJson()); // 已经是 JSON 字符串
//            }
//            if (yeeDiscussReply.getPid() != null) {
//                columns.add("`pid`"); parameters.add(yeeDiscussReply.getPid());
//            }
//            if (yeeDiscussReply.getReUserId() != null) {
//                columns.add("`reUserId`"); parameters.add(yeeDiscussReply.getReUserId());
//            }
//            if (yeeDiscussReply.getReplyId() != null) {
//                columns.add("`replyId`"); parameters.add(yeeDiscussReply.getReplyId());
//            }
//            if (yeeDiscussReply.getPlatform() != null) {
//                columns.add("`platform`"); parameters.add(yeeDiscussReply.getPlatform());
//            }
//
//            columns.add("`isDelete`"); parameters.add(0);
//            columns.add("`addTime`"); parameters.add(yeeDiscussReply.getAddTime() != null ? yeeDiscussReply.getAddTime() : now);
//
//            // 4. 执行插入
//            String sql = "INSERT INTO yee_discuss_reply (" +
//                    String.join(", ", columns) +
//                    ") VALUES (" +
//                    String.join(",", Collections.nCopies(columns.size(), "?")) +
//                    ")";
//
//            Integer generatedId = null;
//            try (PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//                for (int i = 0; i < parameters.size(); i++) {
//                    st.setObject(i + 1, parameters.get(i));
//                }
//
//                int rows = st.executeUpdate();
//                if (rows > 0) {
//                    try (ResultSet rs = st.getGeneratedKeys()) {
//                        if (rs.next()) {
//                            generatedId = rs.getInt(1);
//                            yeeDiscussReply.setId(generatedId);
//                        }
//                    }
//                } else {
//                    return Result.error("评论添加失败");
//                }
//            }
//
//            // 5. 更新讨论评分统计（仅当 userId 有效）
//            if (yeeDiscussReply.getUserId() != null && yeeDiscussReply.getUserId() > 0) {
//                boolean isTopLevel = (yeeDiscussReply.getPid() == null || yeeDiscussReply.getPid() == 0);
//
//                // 初始化记录（如果不存在）
//                insertDiscussScoreIfNotExists(
//                        connection,
//                        yeeDiscussReply.getSchoolId(),
//                        yeeDiscussReply.getCourseId(),
//                        yeeDiscussReply.getUserId(),
//                        realClassId,
//                        yeeDiscussReply.getDiscussId(),
//                        userType
//                );
//
//                // 增量更新数量
//                updateDiscussScoreCount(
//                        connection,
//                        yeeDiscussReply.getSchoolId(),
//                        yeeDiscussReply.getCourseId(),
//                        yeeDiscussReply.getUserId(),
//                        realClassId,
//                        yeeDiscussReply.getDiscussId(),
//                        isTopLevel,
//                        +1
//                );
//            }
//
//            return Result.success("评论成功", yeeDiscussReply);
//
//        } catch (SQLException e) {
//            e.printStackTrace();
//            return Result.error("数据库错误：" + e.getMessage());
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Result.error("评论失败：" + e.getMessage());
//        }
//    }

    @Override
    public Result add(YeeDiscussReply yeeDiscussReply, Integer userType) {
        // 1. 参数校验
        if (yeeDiscussReply == null) {
            return Result.error("评论内容不能为空");
        }
        if (yeeDiscussReply.getSchoolId() == null) {
            return Result.error("schoolId 不能为空");
        }
        if (yeeDiscussReply.getDiscussId() == null) {
            return Result.error("discussId 不能为空");
        }
        if (yeeDiscussReply.getCourseId() == null) {
            return Result.error("courseId 不能为空");
        }

        // 2. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(yeeDiscussReply.getSchoolId());
        if (slSchool == null || slSchool.getAllow() != 1) {
            return Result.error("学校不存在或未审核通过");
        }

        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {

            Timestamp now = new Timestamp(System.currentTimeMillis());

            Integer realClassId = 0;
            if (userType != null && userType == 1 &&
                    yeeDiscussReply.getUserId() != null &&
                    yeeDiscussReply.getUserId() > 0) {

                realClassId = getStudentCourseClassIdFromRelation(
                        connection,
                        yeeDiscussReply.getSchoolId(),
                        yeeDiscussReply.getUserId(),
                        yeeDiscussReply.getCourseId()
                );
            }
            // 非学生（如老师）或 userId 无效 → classId = 0

            // 3. 构建动态 INSERT
            List<String> columns = new ArrayList<>();
            List<Object> parameters = new ArrayList<>();

            // 必填字段
            columns.add("`schoolId`"); parameters.add(yeeDiscussReply.getSchoolId());
            columns.add("`discussId`"); parameters.add(yeeDiscussReply.getDiscussId());
            columns.add("`courseId`"); parameters.add(yeeDiscussReply.getCourseId());
            columns.add("`classId`"); parameters.add(realClassId);

            // 可选字段
            if (yeeDiscussReply.getUserId() != null && yeeDiscussReply.getUserId() > 0) {
                columns.add("`userId`"); parameters.add(yeeDiscussReply.getUserId());
            }
            if (hasText(yeeDiscussReply.getContent())) {
                columns.add("`content`"); parameters.add(yeeDiscussReply.getContent());
            }
            if (yeeDiscussReply.getImages() != null && !yeeDiscussReply.getImages().isEmpty()) {
                columns.add("`images`");
                parameters.add(gson.toJson(yeeDiscussReply.getImages()));
            }
            if (yeeDiscussReply.getAttachFiles() != null && !yeeDiscussReply.getAttachFiles().isEmpty()) {
                columns.add("`files`");
                parameters.add(yeeDiscussReply.getFilesJson());
            }
            if (yeeDiscussReply.getPid() != null) {
                columns.add("`pid`"); parameters.add(yeeDiscussReply.getPid());
            }
            if (yeeDiscussReply.getReUserId() != null) {
                columns.add("`reUserId`"); parameters.add(yeeDiscussReply.getReUserId());
            }
            if (yeeDiscussReply.getReplyId() != null) {
                columns.add("`replyId`"); parameters.add(yeeDiscussReply.getReplyId());
            }
            if (yeeDiscussReply.getPlatform() != null) {
                columns.add("`platform`"); parameters.add(yeeDiscussReply.getPlatform());
            }

            columns.add("`isDelete`"); parameters.add(0);
            columns.add("`addTime`"); parameters.add(yeeDiscussReply.getAddTime() != null ? yeeDiscussReply.getAddTime() : now);

            // 4. 执行插入
            String sql = "INSERT INTO yee_discuss_reply (" +
                    String.join(", ", columns) +
                    ") VALUES (" +
                    String.join(",", Collections.nCopies(columns.size(), "?")) +
                    ")";

            Integer generatedId = null;
            try (PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 0; i < parameters.size(); i++) {
                    st.setObject(i + 1, parameters.get(i));
                }

                int rows = st.executeUpdate();
                if (rows > 0) {
                    try (ResultSet rs = st.getGeneratedKeys()) {
                        if (rs.next()) {
                            generatedId = rs.getInt(1);
                            yeeDiscussReply.setId(generatedId);
                        }
                    }
                } else {
                    return Result.error("评论添加失败");
                }
            }

            // 5. 仅学生才做讨论统计 + discussJoin更新
            if (yeeDiscussReply.getUserId() != null && yeeDiscussReply.getUserId() > 0
                    && userType != null && userType == 1) {

                boolean isTopLevel = (yeeDiscussReply.getPid() == null || yeeDiscussReply.getPid() == 0);

                // 初始化讨论统计记录
                insertDiscussScoreIfNotExists(
                        connection,
                        yeeDiscussReply.getSchoolId(),
                        yeeDiscussReply.getCourseId(),
                        yeeDiscussReply.getUserId(),
                        realClassId,
                        yeeDiscussReply.getDiscussId(),
                        userType
                );

                // 增量更新讨论数量
                updateDiscussScoreCount(
                        connection,
                        yeeDiscussReply.getSchoolId(),
                        yeeDiscussReply.getCourseId(),
                        yeeDiscussReply.getUserId(),
                        realClassId,
                        yeeDiscussReply.getDiscussId(),
                        isTopLevel,
                        +1
                );

                // 判断是否是该讨论下第一条发言，是则更新 yee_course_student discussJoin+1
                boolean isFirstReplyInDiscuss = checkIsFirstReplyInDiscuss(
                        connection,
                        yeeDiscussReply.getSchoolId(),
                        yeeDiscussReply.getUserId(),
                        yeeDiscussReply.getCourseId(),
                        yeeDiscussReply.getDiscussId()
                );

                if (isFirstReplyInDiscuss) {
                    updateCourseStudentDiscussJoin(
                            connection,
                            yeeDiscussReply.getSchoolId(),
                            yeeDiscussReply.getUserId(),
                            yeeDiscussReply.getCourseId()
                    );
                }
            }

            // 老师、管理员等非学生身份：直接跳过 discussJoin 逻辑，不更新 yee_course_student
            return Result.success("评论成功", yeeDiscussReply);

        } catch (SQLException e) {
            e.printStackTrace();
            return Result.error("数据库错误：" + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("评论失败：" + e.getMessage());
        }
    }

    /**
     * 判断：该学生在【该课程 + 该讨论】下是否是第一条评论/回复
     */
    private boolean checkIsFirstReplyInDiscuss(Connection connection, Integer schoolId, Integer userId,
                                               Integer courseId, Integer discussId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM yee_discuss_reply " +
                "WHERE schoolId = ? AND userId = ? AND courseId = ? AND discussId = ? ";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, schoolId);
            ps.setInt(2, userId);
            ps.setInt(3, courseId);
            ps.setInt(4, discussId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // 数量为1 代表当前这条是首次发言
                    return rs.getInt(1) == 1;
                }
            }
        }
        return false;
    }

    /**
     * 更新学生选课表 discussJoin +1
     */
    private void updateCourseStudentDiscussJoin(Connection connection, Integer schoolId, Integer studentId, Integer courseId) throws SQLException {
        String sql = "UPDATE yee_course_student " +
                "SET discussJoin = discussJoin + 1 " +
                "WHERE schoolId = ? AND studentId = ? AND courseId = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, schoolId);
            ps.setInt(2, studentId);
            ps.setInt(3, courseId);
            ps.executeUpdate();
        }
    }

    // 辅助方法：判断字符串非空
    private boolean hasText(String str) {
        return str != null && !str.trim().isEmpty();
    }

    @Override
    public Result delete(int id, int schoolId, int operatorUserId, int operatorUserType) {
        // 1. 参数校验
        if (id <= 0 || schoolId <= 0 || operatorUserId <= 0) {
            return Result.error("参数无效");
        }
        if (operatorUserType != 1 && operatorUserType != 2) {
            return Result.error("用户类型无效");
        }

        // 2. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            conn.setAutoCommit(false);

            try {
                // 3. 查询被删除的评论（只查必要字段）
                String selectSql = """
                SELECT userId, courseId, discussId, pid
                FROM yee_discuss_reply
                WHERE id = ? AND schoolId = ? AND isDelete = 0
                """;

                YeeDiscussReply reply = null;
                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, id);
                    ps.setInt(2, schoolId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            reply = new YeeDiscussReply();
                            reply.setUserId(rs.getInt("userId"));
                            reply.setCourseId(rs.getInt("courseId"));
                            reply.setDiscussId(rs.getInt("discussId"));
                            reply.setPid(rs.getObject("pid") != null ? rs.getInt("pid") : null);
                        }
                    }
                }

                if (reply == null) {
                    return Result.error("评论不存在或已被删除");
                }

                // 4. 权限校验：学生只能删自己的
                if (operatorUserType == 1 && reply.getUserId() != operatorUserId) {
                    return Result.error("无权删除他人评论");
                }

                String updateReplySql = "UPDATE yee_discuss_reply SET isDelete = 1 WHERE id = ? AND schoolId = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateReplySql)) {
                    ps.setInt(1, id);
                    ps.setInt(2, schoolId);
                    if (ps.executeUpdate() == 0) {
                        conn.rollback();
                        return Result.error("删除失败");
                    }
                }

                boolean isTopLevel = (reply.getPid() == null || reply.getPid() == 0);

                String updateScoreSql;
                if (isTopLevel) {
                    // 主贴（post）减少
                    updateScoreSql = """
                    UPDATE yee_discuss_score
                    SET postQty = GREATEST(postQty - 1, 0),
                        allQty = GREATEST(allQty - 1, 0)
                    WHERE schoolId = ? AND courseId = ? AND userId = ? AND discussId = ?
                    """;
                } else {
                    // 回复（reply）减少
                    updateScoreSql = """
                    UPDATE yee_discuss_score
                    SET replyQty = GREATEST(replyQty - 1, 0),
                        allQty = GREATEST(allQty - 1, 0)
                    WHERE schoolId = ? AND courseId = ? AND userId = ? AND discussId = ?
                    """;
                }

                try (PreparedStatement ps = conn.prepareStatement(updateScoreSql)) {
                    ps.setInt(1, schoolId);
                    ps.setInt(2, reply.getCourseId());
                    ps.setInt(3, reply.getUserId());      // 被删评论的作者 ID
                    ps.setLong(4, reply.getDiscussId());
                    int updated = ps.executeUpdate();
                }

                conn.commit();
                return Result.success("删除成功");

            } catch (Exception e) {
                conn.rollback();
                throw e;
            }

        } catch (SQLException e) {
            return Result.error("数据库错误：" + e.getMessage());
        } catch (Exception e) {
            return Result.error("系统异常：" + e.getMessage());
        }
    }

    @Override
    public Result update(int id, int schoolId, String content) {
        // 参数校验
        if (id <= 0 || schoolId <= 0) {
            return Result.error("参数无效");
        }

        // 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 执行更新
        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement st = connection.prepareStatement(
                     "UPDATE yee_discuss_reply SET content = ? WHERE id = ? AND schoolId = ?")) {

            st.setString(1, content);
            st.setInt(2, id);
            st.setInt(3, schoolId);

            int rowsUpdated = st.executeUpdate();

            if (rowsUpdated > 0) {
                return Result.success("修改成功");
            } else {
                return Result.error("修改失败：未找到记录或无权操作");
            }

        } catch (SQLException e) {
            return Result.error("数据库错误");
        } catch (Exception e) {
            return Result.error("系统异常");
        }
    }

    @Override
    public Result discussReplyLike(int replyId, int schoolId, int userId) {
        if (replyId <= 0 || schoolId <= 0 || userId <= 0) {
            return Result.error("参数无效");
        }

        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() != 1) {
            return Result.error("学校不存在或未审核通过");
        }

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            conn.setAutoCommit(false);

            try {
                // 1. 查询被点赞的回复详情（关键：获取被点赞者的 userId、discussId、courseId）
                String replyInfoSql = """
                SELECT 
                    r.userId AS targetUserId,
                    r.discussId,
                    d.courseId
                FROM yee_discuss_reply r
                INNER JOIN yee_discuss d ON r.discussId = d.id
                WHERE r.id = ? AND r.schoolId = ? AND r.isDelete = 0
                """;

                Integer targetUserId = null;
                Long discussId = null;
                Long courseId = null;

                try (PreparedStatement ps = conn.prepareStatement(replyInfoSql)) {
                    ps.setInt(1, replyId);
                    ps.setInt(2, schoolId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            conn.rollback();
                            return Result.error("评论不存在或已被删除");
                        }
                        targetUserId = rs.getInt("targetUserId");
                        discussId = rs.getLong("discussId");
                        courseId = rs.getLong("courseId");
                    }
                }

                // 2. 检查用户是否已点赞
                String checkLikeSql = "SELECT 1 FROM yee_reply_like WHERE replyId = ? AND userId = ? AND schoolId = ?";
                boolean hasLiked = false;
                try (PreparedStatement ps = conn.prepareStatement(checkLikeSql)) {
                    ps.setInt(1, replyId);
                    ps.setInt(2, userId);
                    ps.setInt(3, schoolId);
                    try (ResultSet rs = ps.executeQuery()) {
                        hasLiked = rs.next();
                    }
                }

                int delta = 0;
                if (hasLiked) {
                    // 取消点赞
                    String deleteSql = "DELETE FROM yee_reply_like WHERE replyId = ? AND userId = ? AND schoolId = ?";
                    try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
                        ps.setInt(1, replyId);
                        ps.setInt(2, userId);
                        ps.setInt(3, schoolId);
                        int rows = ps.executeUpdate();
                        if (rows == 0) {
                            conn.rollback();
                            return Result.error("取消点赞失败");
                        }
                    }
                    delta = -1;
                } else {
                    // 点赞
                    String insertSql = "INSERT INTO yee_reply_like (replyId, userId, schoolId) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setInt(1, replyId);
                        ps.setInt(2, userId);
                        ps.setInt(3, schoolId);
                        int rows = ps.executeUpdate();
                        if (rows == 0) {
                            conn.rollback();
                            return Result.error("点赞失败");
                        }
                    }
                    delta = +1;
                }

                // >>> 新增：3. 更新 yee_discuss_score 的 likeQty <<<
                if (targetUserId != null && discussId != null && courseId != null) {
                    String updateScoreSql = """
                    UPDATE yee_discuss_score 
                    SET likeQty = GREATEST(likeQty + ?, 0)
                    WHERE schoolId = ?
                      AND courseId = ?
                      AND discussId = ?
                      AND userId = ?
                    """;
                    try (PreparedStatement ps = conn.prepareStatement(updateScoreSql)) {
                        ps.setInt(1, delta);
                        ps.setInt(2, schoolId);
                        ps.setLong(3, courseId);
                        ps.setLong(4, discussId);
                        ps.setInt(5, targetUserId);
                        int updatedRows = ps.executeUpdate();

                        // 如果记录不存在（理论上不应发生），可考虑插入？但通常已有记录
                        if (updatedRows == 0) {
                            // 可选：记录日志，或忽略（因为可能非学生回复，无评分记录）
                            // 此处暂不报错，因为老师回复可能无 score 记录
                        }
                    }
                }

                // 4. 获取最新点赞数
                long likeCount = 0;
                String countSql = "SELECT COUNT(*) FROM yee_reply_like WHERE replyId = ? AND schoolId = ?";
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    ps.setInt(1, replyId);
                    ps.setInt(2, schoolId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            likeCount = rs.getLong(1);
                        }
                    }
                }

                conn.commit();

                Map<String, Object> result = new HashMap<>();
                result.put("liked", !hasLiked);
                result.put("likeCount", likeCount);
                return Result.success(result);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            return Result.error("数据库操作失败，请稍后重试");
        } catch (Exception e) {
            return Result.error("系统异常");
        }
    }


    /**
     * 将 ResultSet 转换为 YeeDiscussReply 列表 (VO 包含点赞数、回复数）
     */
    private List<YeeDiscussReplyVo> rsToYeeQuestion(ResultSet rs) throws SQLException {
        List<YeeDiscussReplyVo> list = new ArrayList<>();
        while (rs.next()) {
            YeeDiscussReplyVo vo = new YeeDiscussReplyVo();
            vo.setId(rs.getLong("id"));
            vo.setContent(rs.getString("content"));
            vo.setImages(rs.getString("images"));
            vo.setFiles(rs.getString("files"));
            vo.setAddTime(rs.getTimestamp("addTime"));
            vo.setCourseId(rs.getInt("courseId"));
            vo.setCourseName(rs.getString("courseName"));
            vo.setDiscussId(rs.getLong("discussId"));
            vo.setDiscussName(rs.getString("discussName"));
            vo.setLikeCount(rs.getLong("like_count"));
            vo.setReplyCount(rs.getLong("reply_count"));
            list.add(vo);
        }
        return list;
    }

    /**
     * 初始化评分记录（如果不存在）
     */
    /**
     * 初始化评分记录（如果不存在）—— 使用 SELECT + INSERT 逻辑，不依赖唯一索引
     */
    private void insertDiscussScoreIfNotExists(Connection conn,
                                               int schoolId, int courseId, int userId,
                                               Integer classId,
                                               int discussId, Integer userType) throws SQLException {
        int safeClassId = (classId == null) ? 0 : classId;
        int safeUserType = (userType == null) ? 1 : userType;

        // 1. 先查询是否存在（使用 FOR UPDATE 加行锁，防止并发重复插入）
        String selectSql = """
        SELECT id FROM yee_discuss_score 
        WHERE schoolId = ? 
          AND courseId = ? 
          AND userId = ? 
          AND discussId = ?
        FOR UPDATE
        """;

        try (PreparedStatement selectPs = conn.prepareStatement(selectSql)) {
            selectPs.setInt(1, schoolId);
            selectPs.setInt(2, courseId);
            selectPs.setInt(3, userId);
            selectPs.setInt(4, discussId);

            try (ResultSet rs = selectPs.executeQuery()) {
                if (rs.next()) {
                    // 已存在，无需插入
                    return;
                }
            }
        }

        // 2. 不存在，执行插入
        String insertSql = """
        INSERT INTO yee_discuss_score 
        (schoolId, courseId, userId, classId, discussId, userType, scored, score, 
         allQty, postQty, replyQty, likeQty)
        VALUES (?, ?, ?, ?, ?, ?, 0, 0.0, 0, 0, 0, 0)
        """;

        try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
            insertPs.setInt(1, schoolId);
            insertPs.setInt(2, courseId);
            insertPs.setInt(3, userId);
            insertPs.setInt(4, safeClassId);
            insertPs.setInt(5, discussId);
            insertPs.setInt(6, safeUserType);
            insertPs.executeUpdate();
        }
    }

    /**
     * 增量更新讨论评分数量
     */
    private void updateDiscussScoreCount(
            Connection conn,
            int schoolId,
            int courseId,
            int userId,
            Integer classId,
            long discussId,
            boolean isTopLevel,
            int delta
    ) throws SQLException {
        int safeClassId = (classId == null) ? 0 : classId;

        String sql = """
        UPDATE yee_discuss_score 
        SET 
            postQty = GREATEST(0, postQty + ?),
            replyQty = GREATEST(0, replyQty + ?),
            allQty = GREATEST(0, allQty + ?)
        WHERE 
            schoolId = ? 
            AND courseId = ? 
            AND userId = ? 
            AND discussId = ?
            AND classId = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setInt(idx++, isTopLevel ? delta : 0);
            ps.setInt(idx++, isTopLevel ? 0 : delta);
            ps.setInt(idx++, delta);
            ps.setInt(idx++, schoolId);
            ps.setInt(idx++, courseId);
            ps.setInt(idx++, userId);
            ps.setLong(idx++, discussId);
            ps.setInt(idx++, safeClassId);
            ps.executeUpdate();
        }
    }

    /**
     * 根据 courseId + studentId + schoolId 查询该学生在本课程中的教学班级ID
     * 如果未加入班级，返回 0
     */
    private Integer getStudentCourseClassIdFromRelation(Connection conn,
                                                        int schoolId,
                                                        int studentId,
                                                        int courseId) throws SQLException {
        String sql = """
        SELECT classId
        FROM yee_course_student
        WHERE courseId = ?
          AND studentId = ?
          AND schoolId = ?
        LIMIT 1
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, courseId);
            ps.setInt(2, studentId);
            ps.setInt(3, schoolId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int classId = rs.getInt("classId");
                    return classId > 0 ? classId : 0;
                }
            }
        }
        return 0;
    }
}