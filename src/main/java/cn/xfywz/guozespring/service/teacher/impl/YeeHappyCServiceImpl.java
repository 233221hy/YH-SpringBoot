package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeHappyCircle;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeHappyCService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import cn.xfywz.guozespring.util.db.BuiltSql;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YeeHappyCServiceImpl implements YeeHappyCService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private YeeHappyCircle rsToCircle(ResultSet rs) throws SQLException {
        YeeHappyCircle c = new YeeHappyCircle();
        c.setId(rs.getLong("id"));
        c.setAddTime(rs.getTimestamp("addTime"));
        c.setContent(rs.getString("content"));
        c.setImages(rs.getString("images"));
        c.setFiles(rs.getString("files"));
        c.setUserId(rs.getLong("userId"));
        c.setReplyId(rs.getLong("replyId"));
        c.setReUserId(rs.getLong("reUserId"));
        c.setIsDelete(rs.getInt("isDelete"));
        c.setSchoolId(rs.getLong("schoolId"));
        c.setAddDate(rs.getDate("addDate"));
        c.setUserName(rs.getString("userName"));
        c.setUserAvatar(rs.getString("avatar"));
        return c;
    }

    /**
     * 评论区列表
     */
    @Override
    public Result list(int pageNum, int pageSize, int schoolId, long userId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        int offset = (pageNum - 1) * pageSize;

        // 主评论 SQL：关联用户信息
        String mainSql = """
        SELECT
            h.*,
            u.name AS userName,
            u.avatar
        FROM yee_happy_circle h
        LEFT JOIN yee_student u ON h.userId = u.id AND h.schoolId = u.schoolId
        WHERE h.replyId = 0 AND h.isDelete = 0
        ORDER BY h.addTime DESC
        LIMIT ? OFFSET ?
        """;

        String countSql = "SELECT COUNT(*) FROM yee_happy_circle WHERE replyId = 0 AND isDelete = 0";

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement mainPs = conn.prepareStatement(mainSql);
             PreparedStatement countPs = conn.prepareStatement(countSql)) {

            mainPs.setInt(1, pageSize);
            mainPs.setInt(2, offset);
            ResultSet mainRs = mainPs.executeQuery();
            List<Map<String, Object>> mainList = new ArrayList<>();

            while (mainRs.next()) {
                YeeHappyCircle main = rsToCircle(mainRs); // 已包含 userName 和 avatar
                Map<String, Object> mainData = new HashMap<>();
                mainData.put("main", main);

                // 查询回复（也关联用户信息）
                String replySql = """
                SELECT
                    h.*,
                    u.name AS userName,
                    u.avatar
                FROM yee_happy_circle h
                LEFT JOIN yee_student u ON h.userId = u.id AND h.schoolId = u.schoolId
                WHERE h.replyId = ? AND h.isDelete = 0
                ORDER BY h.addTime
                LIMIT 3
                """;
                try (PreparedStatement replyPs = conn.prepareStatement(replySql)) {
                    replyPs.setLong(1, main.getId());
                    ResultSet replyRs = replyPs.executeQuery();
                    List<YeeHappyCircle> replies = new ArrayList<>();
                    while (replyRs.next()) {
                        replies.add(rsToCircle(replyRs)); // 自动带用户名和头像
                    }
                    replyRs.close();
                    mainData.put("replies", replies);
                }

                // 查询回复总数
                String replyCountSql = "SELECT COUNT(*) FROM yee_happy_circle WHERE replyId = ? AND isDelete = 0";
                try (PreparedStatement rcPs = conn.prepareStatement(replyCountSql)) {
                    rcPs.setLong(1, main.getId());
                    ResultSet rcRs = rcPs.executeQuery();
                    long replyCount = 0;
                    if (rcRs.next()) replyCount = rcRs.getLong(1);
                    rcRs.close();
                    mainData.put("replyCount", replyCount);
                }

                // 是否点赞
                if (userId > 0) {
                    String likeSql = "SELECT COUNT(*) FROM yee_happy_reply_like WHERE replyId = ? AND userId = ?";
                    try (PreparedStatement likePs = conn.prepareStatement(likeSql)) {
                        likePs.setLong(1, main.getId());
                        likePs.setLong(2, userId);
                        ResultSet likeRs = likePs.executeQuery();
                        boolean isLiked = false;
                        if (likeRs.next()) isLiked = likeRs.getInt(1) > 0;
                        likeRs.close();
                        mainData.put("isLiked", isLiked);
                    }
                } else {
                    mainData.put("isLiked", false);
                }

                // 点赞总数
                String likeCountSql = "SELECT COUNT(*) FROM yee_happy_reply_like WHERE replyId = ?";
                try (PreparedStatement lcPs = conn.prepareStatement(likeCountSql)) {
                    lcPs.setLong(1, main.getId());
                    ResultSet lcRs = lcPs.executeQuery();
                    long likeCount = 0;
                    if (lcRs.next()) likeCount = lcRs.getLong(1);
                    lcRs.close();
                    mainData.put("likeCount", likeCount);
                }

                mainList.add(mainData);
            }
            mainRs.close();

            // 总数
            ResultSet countRs = countPs.executeQuery();
            long total = 0;
            if (countRs.next()) total = countRs.getLong(1);
            countRs.close();

            return Result.success(mainList, total);
        }
    }

    @Override
    public Result detail(int schoolId, long id) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        String sql = "SELECT * FROM yee_happy_circle WHERE id = ?";
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                YeeHappyCircle c = rsToCircle(rs);
                rs.close();
                return Result.success(c);
            }
            rs.close();
            return Result.error("不存在");
        }
    }

    @Override
    public Result add(YeeHappyCircle c) throws Exception {
        // 基础业务校验
        if (c == null) return Result.error("参数不能为空");
        if (c.getSchoolId() < 0) return Result.error("schoolId必填");
        SlSchool slSchool = slSchoolMapper.selectById((int) c.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
    
        // 准备默认值（避免在 SQL 构造处混入业务判断）
        Timestamp addTime = c.getAddTime() != null ? c.getAddTime() : new Timestamp(System.currentTimeMillis());
    
        // 方法内本地类：承载 SQL 和参数
        // 使用通用 SQL 载体
        // class BuiltSql { ... } 已移除，改用 util.BuiltSql
    
        // 方法内本地类：负责 SQL 构造
        class SqlBuilder {
            BuiltSql buildInsert(YeeHappyCircle circle, Timestamp ts) {
                // 先收集字段与值（保持插入顺序）
                java.util.LinkedHashMap<String, Object> fields = new java.util.LinkedHashMap<>();
                fields.put("schoolId", circle.getSchoolId());
                fields.put("isDelete", 0);
                if (circle.getContent() != null) fields.put("content", circle.getContent());
                if (circle.getImages() != null) fields.put("images", circle.getImages());
                if (circle.getFiles() != null) fields.put("files", circle.getFiles());
                if (circle.getUserId() > 0) fields.put("userId", circle.getUserId());
                fields.put("replyId", circle.getReplyId());
                fields.put("reUserId", circle.getReUserId());
                fields.put("addTime", ts);
                //        fields.put("addDate", circle.getAddDate() != null ? circle.getAddDate() : new java.sql.Date(System.currentTimeMillis()));

                // 拼接列名与占位符
                String columns = String.join(", ", fields.keySet());
                String placeholders = fields.keySet().stream().map(k -> "?")
                        .collect(java.util.stream.Collectors.joining(", "));
                String sql = "INSERT INTO yee_happy_circle (" + columns + ") VALUES (" + placeholders + ")";

                // 参数按列顺序
                List<Object> params = new ArrayList<>(fields.values());
                return BuiltSql.of(sql, params);
            }
        }
    
        // 方法内本地类：负责 SQL 执行
        class DbExecutor {
            Result executeInsert(SlSchool school, BuiltSql built, YeeHappyCircle circle) throws Exception {
                try (Connection conn = SlaveMysqlConnectionUtil.getConnection(school);
                     PreparedStatement ps = conn.prepareStatement(built.sql(), Statement.RETURN_GENERATED_KEYS)) {
                    for (int i = 0; i < built.params().size(); i++) ps.setObject(i+1, built.params().get(i));
                    int n = ps.executeUpdate();
                    if (n > 0) {
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next()) circle.setId(keys.getLong(1));
                        }
                        return Result.success("添加成功");
                    }
                    return Result.error("添加失败");
                }
            }
        }
    
        // 调用拆分后的方法，清晰表达业务流程
        BuiltSql built = new SqlBuilder().buildInsert(c, addTime);
        return new DbExecutor().executeInsert(slSchool, built, c);
    }


    @Override
    public Result delete(long id, int schoolId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) return Result.error("学校不存在或未审核");
        String sql = "UPDATE yee_happy_circle SET isDelete = 1 WHERE id = ?";
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            int n = ps.executeUpdate();
            return n > 0 ? Result.success("删除成功") : Result.error("删除失败");
        }
    }


    @Override
    public Result likeToggle(int schoolId, long replyId, long userId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) return Result.error("学校不存在或未审核");
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            // 查是否存在
            String check = "SELECT id FROM yee_happy_reply_like WHERE replyId = ? AND userId = ?";
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setLong(1, replyId);
                ps.setLong(2, userId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long likeId = rs.getLong(1);
                    rs.close();
                    try (PreparedStatement del = conn.prepareStatement("DELETE FROM yee_happy_reply_like WHERE id = ?")) {
                        del.setLong(1, likeId);
                        del.executeUpdate();
                    }
                    
                    // 返回取消点赞后的状态和总数
                    String countSql = "SELECT COUNT(*) FROM yee_happy_reply_like WHERE replyId = ?";
                    try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                        countPs.setLong(1, replyId);
                        ResultSet countRs = countPs.executeQuery();
                        long likeCount = 0;
                        if (countRs.next()) likeCount = countRs.getLong(1);
                        countRs.close();
                        Map<String, Object> result = new HashMap<>();
                        result.put("isLiked", false);
                        result.put("likeCount", likeCount);
                        result.put("message", "取消点赞");
                        return Result.success(result);
                    }
                }
                rs.close();
            }
            
            // 插入点赞
            String ins = "INSERT INTO yee_happy_reply_like (replyId, userId, schoolId) VALUES (?, ?, ?)";
            try (PreparedStatement ip = conn.prepareStatement(ins)) {
                ip.setLong(1, replyId);
                ip.setLong(2, userId);
                ip.setInt(3, schoolId);
                ip.executeUpdate();
            }
            
            // 返回点赞后的状态和总数
            String countSql = "SELECT COUNT(*) FROM yee_happy_reply_like WHERE replyId = ?";
            try (PreparedStatement countPs = conn.prepareStatement(countSql)) {
                countPs.setLong(1, replyId);
                ResultSet countRs = countPs.executeQuery();
                long likeCount = 0;
                if (countRs.next()) likeCount = countRs.getLong(1);
                countRs.close();
                Map<String, Object> result = new HashMap<>();
                result.put("isLiked", true);
                result.put("likeCount", likeCount);
                result.put("message", "点赞成功");
                return Result.success(result);
            }
        }
    }

    @Override
    public Result addReply(YeeHappyCircle reply) throws Exception {
        // replyId > 0 代表回复
        if (reply.getSchoolId() <= 0 || reply.getReplyId() <= 0) return Result.error("参数错误");
        return add(reply);
    }


}