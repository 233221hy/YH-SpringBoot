package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeNodeDiscuss;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeNodeDiscussService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Slf4j
@Service
public class YeeNodeDiscussServiceImpl implements YeeNodeDiscussService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result add(YeeNodeDiscuss discuss) throws Exception {
        // 1. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(discuss.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 必填字段校验
        if (discuss.getSchoolId() == null) return Result.error("学校ID不能为空");
        if (discuss.getNodeId() == null) return Result.error("节点ID不能为空");

        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            Timestamp now = new Timestamp(System.currentTimeMillis());

            // 构建插入参数
            InsertParams insertParams = buildInsertParams(discuss, now);

            // 执行插入操作
            return executeInsert(connection, discuss, insertParams);
        } catch (SQLException e) {
            log.error("添加评论-数据库错误：", e);
            return Result.error("数据库错误：" + e.getMessage());
        } catch (Exception e) {
            log.error("添加评论-失败：", e);
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    /**
     * 构建插入参数
     */
    private InsertParams buildInsertParams(YeeNodeDiscuss discuss, Timestamp now) {
        List<String> columns = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();

        columns.add("`schoolId`"); parameters.add(discuss.getSchoolId());
        columns.add("`nodeId`"); parameters.add(discuss.getNodeId());

        if (discuss.getCourseId() != null) {
            columns.add("`courseId`");
            parameters.add(discuss.getCourseId());
        }

        if (discuss.getUserId() != null && discuss.getUserId() > 0) {
            columns.add("`userId`");
            parameters.add(discuss.getUserId());
        }

        if (discuss.getContent() != null && !discuss.getContent().trim().isEmpty()) {
            columns.add("`content`");
            parameters.add(discuss.getContent());
        }

        // 处理 images：支持字符串或列表，转为合法 JSON 数组
        String jsonImages = convertToJsonArray(discuss.getImages());
        columns.add("`images`");
        parameters.add(jsonImages);

        // 处理 files：同上
        String jsonFiles = convertToJsonArray(discuss.getFiles());
        columns.add("`files`");
        parameters.add(jsonFiles);

        if (discuss.getReplyId() != null) {
            columns.add("`replyId`");
            parameters.add(discuss.getReplyId());
        }

        if (discuss.getReUserId() != null) {
            columns.add("`reUserId`");
            parameters.add(discuss.getReUserId());
        }

        if (discuss.getPlatform() != null && !discuss.getPlatform().trim().isEmpty()) {
            columns.add("`platform`");
            parameters.add(discuss.getPlatform());
        }

        columns.add("`isDelete`");
        parameters.add(0);

        columns.add("`addTime`");
        parameters.add(now);

        return new InsertParams(columns, parameters);
    }

    /**
     * 执行插入操作
     */
    private Result executeInsert(Connection connection, YeeNodeDiscuss discuss, InsertParams insertParams) throws SQLException {
        // 构建 SQL
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO yee_node_discuss (");
        sqlBuilder.append(String.join(", ", insertParams.columns));
        sqlBuilder.append(") VALUES (");
        for (int i = 0; i < insertParams.columns.size(); i++) {
            sqlBuilder.append("?, ");
        }
        sqlBuilder.setLength(sqlBuilder.length() - 2);
        sqlBuilder.append(")");

        try (PreparedStatement st = connection.prepareStatement(sqlBuilder.toString(), Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < insertParams.parameters.size(); i++) {
                st.setObject(i + 1, insertParams.parameters.get(i));
            }

            int rows = st.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = st.getGeneratedKeys()) {
                    if (rs.next()) {
                        discuss.setId(rs.getInt(1));
                    }
                }
                return Result.success("添加成功", discuss);
            } else {
                return Result.error("添加失败：未插入数据");
            }
        }
    }

    @Override
    public Result delete(int id, int schoolId) throws Exception {

        if (id <= 0 || schoolId <= 0) {
            return Result.error("参数无效");
        }

        // 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 执行删除
        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement st = connection.prepareStatement(
                     "UPDATE yee_node_discuss SET isDelete = 1 WHERE id = ? AND schoolId = ?")) {

            st.setInt(1, id);
            st.setInt(2, schoolId);

            int rowsUpdated = st.executeUpdate();

            if (rowsUpdated > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败：未找到匹配的记录或无权操作");
            }

        } catch (SQLException e) {
            log.error("删除评论-数据库错误：", e);
            return Result.error("数据库错误：" + e.getMessage());
        } catch (Exception e) {
            log.error("删除评论-系统异常：", e);
            return Result.error("系统异常：" + e.getMessage());
        }
    }

    @Override
    public Result update(int id, int schoolId, String content) throws Exception {
        // 参数校验
        if (id <= 0 || schoolId <= 0) {
            return Result.error("参数无效");
        }
        if (content == null || content.trim().isEmpty()) {
            return Result.error("评论内容不能为空");
        }

        // 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 执行更新
        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement st = connection.prepareStatement(
                     "UPDATE yee_node_discuss SET content = ? WHERE id = ? AND schoolId = ?")) {

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
            log.error("修改评论-数据库错误：", e);
            return Result.error("数据库错误");
        } catch (Exception e) {
            log.error("修改评论-系统异常：", e);
            return Result.error("系统异常");
        }
    }

    @Override
    public Result yeeNodeReplyLike(int id, int schoolId, int userId) {
        // 1. 参数校验
        if (id <= 0 || schoolId <= 0 || userId <= 0) {
            return Result.error("参数无效");
        }

        // 2. 校验学校是否存在且已启用
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() != 1) {
            return Result.error("学校不存在或未审核通过");
        }

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {

            conn.setAutoCommit(false); // 开启事务

            try {
                // 3. 验证评论是否存在且属于该校
                String checkReplySql = """
                SELECT COUNT(*) 
                FROM yee_node_discuss 
                WHERE id = ? AND schoolId = ? AND isDelete = 0
                """;
                try (PreparedStatement ps = conn.prepareStatement(checkReplySql)) {
                    ps.setInt(1, id);
                    ps.setInt(2, schoolId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next() || rs.getLong(1) == 0) {
                            conn.rollback();
                            return Result.error("评论不存在或已被删除");
                        }
                    }
                }

                // 4. 查询用户是否已经点赞
                String selectSql = "SELECT id FROM yee_node_reply_like WHERE replyId = ? AND userId = ? AND schoolId = ?";
                boolean hasLiked = false;
                Integer existingId = null;

                try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                    ps.setInt(1, id);
                    ps.setInt(2, userId);
                    ps.setInt(3, schoolId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            hasLiked = true;
                            existingId = rs.getInt("id");
                        }
                    }
                }

                long likeCount = 0;

                if (hasLiked) {
                    // 已点赞 → 不允许重复点赞
                    conn.rollback();
                    return Result.error("不能重复点赞");
                } else {
                    // 未点赞 → 执行点赞插入
                    String insertSql = "INSERT INTO yee_node_reply_like (replyId, userId, schoolId) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setInt(1, id);
                        ps.setInt(2, userId);
                        ps.setInt(3, schoolId);

                        int rows = ps.executeUpdate();
                        if (rows == 0) {
                            conn.rollback();
                            return Result.error("点赞失败，请重试");
                        }
                    }
                }

                // 5. 查询最新的点赞总数
                String countSql = "SELECT COUNT(*) FROM yee_node_reply_like WHERE replyId = ? AND schoolId = ?";
                try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                    ps.setInt(1, id);
                    ps.setInt(2, schoolId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            likeCount = rs.getLong(1);
                        }
                    }
                }

                conn.commit();

                // 6. 返回成功结果
                Map<String, Object> result = new HashMap<>();
                result.put("liked", true);
                result.put("likeCount", likeCount);

                return Result.success(result);

            } catch (SQLException e) {
                conn.rollback();
                log.error("评论点赞-事务异常：", e);
                throw e;
            }

        } catch (SQLException e) {
            log.error("评论点赞-数据库错误：", e);
            return Result.error("数据库操作失败，请稍后重试");
        } catch (Exception e) {
            log.error("评论点赞-系统异常：", e);
            return Result.error("系统异常");
        }
    }

    @Override
    public Result  discussList(int pageNum, int pageSize, int schoolId, long userId,int nodeId) {
        // 校验学校是否存在
        SlSchool school = slSchoolMapper.selectById(schoolId);
        if (school == null || school.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> result = new ArrayList<>();
        long totalCount = 0;

        String mainSql = """
    SELECT
        h.id,
        h.addTime,
        h.content,
        h.images, 
        h.nodeId,
        h.courseId,
        h.userId,
        h.replyId,
        h.reUserId,
        h.files,
        h.isDelete,
        h.platform,
        h.schoolId,
        COALESCE(s.name, m.name, '') AS userName,
        COALESCE(s.avatar, m.avatar, '') AS avatar
    FROM yee_node_discuss h
    LEFT JOIN yee_student s ON h.userId = s.id AND h.schoolId = s.schoolId
    LEFT JOIN yee_manage m ON h.userId = m.id AND h.schoolId = m.schoolId
    WHERE h.nodeId = ?
      AND h.replyId = 0
      AND h.isDelete = 0
      AND h.schoolId = ?
    ORDER BY h.addTime DESC
    LIMIT ? OFFSET ?
    """;
        String countSql = """
           SELECT COUNT(*)
           FROM yee_node_discuss
           WHERE nodeId = ?
             AND replyId = 0
             AND isDelete = 0
             AND schoolId = ?
           """;
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(school)) {

            // 查询总数
            try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                countPs.setLong(1, nodeId);
                countPs.setInt(2, schoolId);
                try (ResultSet countRs = countPs.executeQuery()) {
                    if (countRs.next()) {
                        totalCount = countRs.getLong(1);
                    }
                }
            }

            // 查询主评论
            List<Long> mainIds = new ArrayList<>();
            List<Map<String, Object>> mainRows = new ArrayList<>();
            try (PreparedStatement mainPs = conn.prepareStatement(mainSql)) {
                mainPs.setLong(1, nodeId);
                mainPs.setInt(2, schoolId);
                mainPs.setInt(3, pageSize);
                mainPs.setInt(4, offset);
                try (ResultSet mainRs = mainPs.executeQuery()) {
                    while (mainRs.next()) {
                        long mid = mainRs.getLong("id");
                        mainIds.add(mid);
                        Map<String, Object> mainData = new HashMap<>();
                        mainData.put("id", mid);
                        mainData.put("addTime", mainRs.getTimestamp("addTime"));
                        mainData.put("content", mainRs.getString("content"));
                        mainData.put("images", mainRs.getString("images"));
                        mainData.put("nodeId", mainRs.getLong("nodeId"));
                        mainData.put("courseId", mainRs.getLong("courseId"));
                        mainData.put("userId", mainRs.getLong("userId"));
                        mainData.put("replyId", mainRs.getLong("replyId"));
                        mainData.put("reUserId", mainRs.getLong("reUserId"));
                        mainData.put("files", mainRs.getString("files"));
                        mainData.put("isDelete", mainRs.getInt("isDelete"));
                        mainData.put("platform", mainRs.getString("platform"));
                        mainData.put("schoolId", mainRs.getInt("schoolId"));
                        mainData.put("userName", mainRs.getString("userName"));
                        mainData.put("avatar", mainRs.getString("avatar"));
                        mainRows.add(mainData);
                    }
                }
            }

            if (mainIds.isEmpty()) {
                return Result.success(result, totalCount);
            }

            // 批量统计：点赞数、回复数、当前用户是否点赞
            Map<Long, Long> likeCountByCommentId = new HashMap<>();
            Map<Long, Long> replyCountByParentId = new HashMap<>();
            Set<Long> likedCommentIds = new HashSet<>();
            fillLikeAndReplyAggregates(conn, mainIds, schoolId, userId,
                    likeCountByCommentId, replyCountByParentId, likedCommentIds);

            // 查询每条评论的回复（代码内控制每个主评论最多5条）
            String replyListSql = """
    SELECT
        r.id,
        r.addTime,
        r.content,
        r.images,
        r.nodeId,
        r.courseId,
        r.userId,
        r.replyId,
        r.reUserId,
        r.files,
        r.isDelete,
        r.platform,
        r.schoolId,
        COALESCE(s.name, m.name, '') AS userName,
        COALESCE(s.avatar, m.avatar, '') AS avatar
    FROM yee_node_discuss r
    LEFT JOIN yee_student s ON r.userId = s.id AND r.schoolId = s.schoolId
    LEFT JOIN yee_manage m ON r.userId = m.id AND r.schoolId = m.schoolId
    WHERE r.replyId IN (__IN__)
      AND r.isDelete = 0
      AND r.schoolId = ?
    ORDER BY r.replyId, r.addTime ASC
    """;
            Map<Long, List<ResultRow>> repliesByParent = fetchRepliesGrouped(conn, replyListSql, mainIds, schoolId);

            // 批量查询回复的点赞状态
            List<Long> replyIdsAll = new ArrayList<>();
            for (List<ResultRow> rows : repliesByParent.values()) {
                for (ResultRow row : rows) {
                    replyIdsAll.add(row.id);
                }
            }
            Map<Long, Long> likeCountReply = new HashMap<>();
            Set<Long> likedReplyIds = new HashSet<>();
            if (!replyIdsAll.isEmpty()) {
                fillLikeAndReplyAggregates(conn, replyIdsAll, schoolId, userId,
                        likeCountReply, null, likedReplyIds);
            }

            // 组装最终返回结构
            for (Map<String, Object> mainData : mainRows) {
                long mid = (Long) mainData.get("id");
                Map<String, Object> item = new HashMap<>();
                boolean isLiked = likedCommentIds.contains(mid);
                long likeCount = likeCountByCommentId.getOrDefault(mid, 0L);
                long replyCount = replyCountByParentId.getOrDefault(mid, 0L);

                List<Map<String, Object>> replies = new ArrayList<>();
                List<ResultRow> rows = repliesByParent.getOrDefault(mid, Collections.emptyList());
                for (ResultRow rr : rows) {
                    Map<String, Object> reply = new HashMap<>();
                    reply.put("id", rr.id);
                    reply.put("addTime", rr.addTime);
                    reply.put("content", rr.content);
                    reply.put("images", rr.images);
                    reply.put("nodeId", rr.nodeId);
                    reply.put("courseId", rr.courseId);
                    reply.put("userId", rr.userId);
                    reply.put("replyId", rr.replyId);
                    reply.put("reUserId", rr.reUserId);
                    reply.put("files", rr.files);
                    reply.put("isDelete", rr.isDelete);
                    reply.put("platform", rr.platform);
                    reply.put("schoolId", rr.schoolId);
                    reply.put("userName", rr.userName);
                    reply.put("avatar", rr.avatar);
                    reply.put("isLiked", likedReplyIds.contains(rr.id));
                    reply.put("likeCount", likeCountReply.getOrDefault(rr.id, 0L));
                    replies.add(reply);
                }

                item.put("main", mainData);
                item.put("replies", replies);
                item.put("replyCount", replyCount);
                item.put("isLiked", isLiked);
                item.put("likeCount", likeCount);
                result.add(item);
            }

            return Result.success(result, totalCount);
        } catch (Exception e) {
            log.error("评论列表查询失败：", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    // 通用方法：将字符串或列表转为 JSON 数组字符串
    private String convertToJsonArray(Object input) {
        if (input == null) {
            return "[]";
        }

        List<String> result = new ArrayList<>();

        if (input instanceof String str && !str.trim().isEmpty()) {
            Arrays.stream(str.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(result::add);
        } else if (input instanceof List<?> list) {
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
        } else if (input instanceof Object[]) {
            for (Object item : (Object[]) input) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
        }

        return JSON.toJSONString(result);
    }

    /**
     * 插入参数封装类
     */
    private static class InsertParams {
        final List<String> columns;
        final List<Object> parameters;

        InsertParams(List<String> columns, List<Object> parameters) {
            this.columns = columns;
            this.parameters = parameters;
        }
    }

    /**
     * 批量统计：点赞数、回复数、用户是否点赞
     */
    private static void fillLikeAndReplyAggregates(Connection conn, List<Long> commentIds, int schoolId, long userId,
                                                   Map<Long, Long> likeCountByCommentId,
                                                   Map<Long, Long> replyCountByParentId,
                                                   Set<Long> likedCommentIds) throws SQLException {
        if (commentIds.isEmpty()) {
            return;
        }
        String inList = buildInPlaceholders(commentIds.size());

        // 统计每个评论的点赞总数
        String sqlLikes = "SELECT replyId, COUNT(*) AS c FROM yee_node_reply_like WHERE replyId IN (" + inList + ") GROUP BY replyId";
        try (PreparedStatement ps = conn.prepareStatement(sqlLikes)) {
            bindIds(ps, commentIds);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    likeCountByCommentId.put(rs.getLong("replyId"), rs.getLong("c"));
                }
            }
        }

        // 当前用户是否点赞
        String sqlLiked = "SELECT replyId FROM yee_node_reply_like WHERE userId = ? AND replyId IN (" + inList + ")";
        try (PreparedStatement ps = conn.prepareStatement(sqlLiked)) {
            ps.setLong(1, userId);
            bindIds(ps, commentIds, 2);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    likedCommentIds.add(rs.getLong("replyId"));
                }
            }
        }

        // 统计每个主评论的回复总数
        if (replyCountByParentId != null) {
            String sqlReplies = "SELECT replyId, COUNT(*) AS c FROM yee_node_discuss WHERE isDelete = 0 AND schoolId = ? AND replyId IN ("
                    + inList + ") GROUP BY replyId";
            try (PreparedStatement ps = conn.prepareStatement(sqlReplies)) {
                ps.setInt(1, schoolId);
                bindIds(ps, commentIds, 2);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        replyCountByParentId.put(rs.getLong("replyId"), rs.getLong("c"));
                    }
                }
            }
        }
    }

    /**
     * 构建 IN 占位符
     */
    private static String buildInPlaceholders(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    /**
     * 绑定 ID 参数
     */
    private static void bindIds(PreparedStatement ps, List<Long> ids) throws SQLException {
        for (int i = 0; i < ids.size(); i++) {
            ps.setLong(i + 1, ids.get(i));
        }
    }

    private static void bindIds(PreparedStatement ps, List<Long> ids, int startIndex) throws SQLException {
        for (int i = 0; i < ids.size(); i++) {
            ps.setLong(startIndex + i, ids.get(i));
        }
    }

    /**
     * 批量查询回复并按父ID分组，每个父ID最多返回5条
     */
    private static Map<Long, List<ResultRow>> fetchRepliesGrouped(Connection conn, String sqlTemplate,
                                                                  List<Long> mainIds, int schoolId) throws SQLException {
        String inList = buildInPlaceholders(mainIds.size());
        String sql = sqlTemplate.replace("__IN__", inList);
        Map<Long, List<ResultRow>> byParent = new LinkedHashMap<>();
        for (Long id : mainIds) {
            byParent.put(id, new ArrayList<>());
        }
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            for (Long id : mainIds) {
                ps.setLong(idx++, id);
            }
            ps.setInt(idx, schoolId);
            try (ResultSet rs = ps.executeQuery()) {
                Map<Long, Integer> taken = new HashMap<>();
                while (rs.next()) {
                    long parentId = rs.getLong("replyId");
                    int n = taken.getOrDefault(parentId, 0);
                    if (n >= 5) {
                        continue;
                    }
                    taken.put(parentId, n + 1);
                    ResultRow rr = ResultRow.fromReply(rs);
                    byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(rr);
                }
            }
        }
        return byParent;
    }

    /**
     * 结果行封装（内部专用）
     */
    private static final class ResultRow {
        long id;
        Timestamp addTime;
        String content;
        String images;
        long nodeId;
        long courseId;
        long userId;
        long replyId;
        long reUserId;
        String files;
        int isDelete;
        String platform;
        int schoolId;
        String userName;
        String avatar;

        ResultRow(long id, Timestamp addTime, String content, String images, long nodeId, long courseId,
                  long userId, long replyId, long reUserId, String files, int isDelete, String platform,
                  int schoolId, String userName, String avatar) {
            this.id = id;
            this.addTime = addTime;
            this.content = content;
            this.images = images;
            this.nodeId = nodeId;
            this.courseId = courseId;
            this.userId = userId;
            this.replyId = replyId;
            this.reUserId = reUserId;
            this.files = files;
            this.isDelete = isDelete;
            this.platform = platform;
            this.schoolId = schoolId;
            this.userName = userName;
            this.avatar = avatar;
        }

        static ResultRow fromReply(ResultSet rs) throws SQLException {
            return new ResultRow(
                    rs.getLong("id"),
                    rs.getTimestamp("addTime"),
                    rs.getString("content"),
                    rs.getString("images"),
                    rs.getLong("nodeId"),
                    rs.getLong("courseId"),
                    rs.getLong("userId"),
                    rs.getLong("replyId"),
                    rs.getLong("reUserId"),
                    rs.getString("files"),
                    rs.getInt("isDelete"),
                    rs.getString("platform"),
                    rs.getInt("schoolId"),
                    rs.getString("userName"),
                    rs.getString("avatar")
            );
        }
    }
}