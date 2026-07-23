package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeAnnouncement;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeAnnouncementService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YeeAnnouncementServiceImpl implements YeeAnnouncementService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private YeeAnnouncement rsToYeeAnnouncement(ResultSet rs) throws SQLException {
        YeeAnnouncement announcement = new YeeAnnouncement();
        announcement.setId(rs.getLong("id"));
        announcement.setTitle(rs.getString("title"));
        announcement.setContent(rs.getString("content"));
        announcement.setAddTime(rs.getTimestamp("addTime"));
        announcement.setCourseId(rs.getLong("courseId"));
        announcement.setUserId(rs.getLong("userId"));
        announcement.setSchoolId(rs.getLong("schoolId"));
        return announcement;
    }
    @Override
    public Result selectAll(Integer schoolId, long courseId, int pageNum, int pageSize) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {

                int offset = (pageNum - 1) * pageSize;

                // ✅ 查询公告 + 发布人姓名（LEFT JOIN yee_manage）
                String sql = """
                SELECT 
                    a.id,
                    a.courseId,
                    a.title,
                    a.content,
                    a.addTime,
                    a.userId,
                    m.name AS publisherName
                FROM yee_announcement a
                LEFT JOIN yee_manage m ON a.userId = m.id
                WHERE a.courseId = ?
                ORDER BY a.addTime DESC
                LIMIT ? OFFSET ?
                """;

                String countSql = "SELECT COUNT(*) FROM yee_announcement WHERE courseId = ?";

                // 1. 查询总数
                int totalCount = 0;
                try (PreparedStatement countSt = connection.prepareStatement(countSql)) {
                    countSt.setLong(1, courseId);
                    try (ResultSet countRs = countSt.executeQuery()) {
                        if (countRs.next()) {
                            totalCount = countRs.getInt(1);
                        }
                    }
                }

                // 2. 查询列表并封装为 Map
                List<Map<String, Object>> resultList = new ArrayList<>();
                try (PreparedStatement st = connection.prepareStatement(sql)) {
                    st.setLong(1, courseId);
                    st.setInt(2, pageSize);
                    st.setInt(3, offset);

                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> row = new HashMap<>();
                            row.put("id", rs.getLong("id"));
                            row.put("courseId", rs.getLong("courseId"));
                            row.put("title", rs.getString("title"));
                            row.put("content", rs.getString("content"));
                            row.put("addTime", rs.getTimestamp("addTime"));
                            row.put("userId", rs.getObject("userId")); // 可能为 null
                            row.put("publisherName", rs.getString("publisherName")); // 来自 yee_manage
                            resultList.add(row);
                        }
                    }
                }

                return Result.success(resultList, (long) totalCount);

            } catch (Exception e) {
                e.printStackTrace();
                return Result.error("查询失败：" + e.getMessage());
            }
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result add(YeeAnnouncement announcement) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) announcement.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder columns = new StringBuilder("INSERT INTO yee_announcement (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();
            
            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(announcement.getSchoolId());
            
            if (announcement.getCourseId() > 0) {
                columns.append("`courseId`, ");
                values.append("?, ");
                parameters.add(announcement.getCourseId());
            }
            
            if (announcement.getTitle() != null && !announcement.getTitle().trim().isEmpty()) {
                columns.append("`title`, ");
                values.append("?, ");
                parameters.add(announcement.getTitle());
            }
            
            if (announcement.getContent() != null && !announcement.getContent().trim().isEmpty()) {
                columns.append("`content`, ");
                values.append("?, ");
                parameters.add(announcement.getContent());
            }
            
            if (announcement.getUserId() > 0) {
                columns.append("`userId`, ");
                values.append("?, ");
                parameters.add(announcement.getUserId());
            }
            
            columns.append("`addTime`, ");
            values.append("?, ");
            parameters.add(announcement.getAddTime() != null ? announcement.getAddTime() : new Timestamp(System.currentTimeMillis()));
            
            columns.delete(columns.length() - 2, columns.length());
            values.delete(values.length() - 2, values.length());
            
            columns.append(") ");
            values.append(")");
            String sql = columns.toString() + values.toString();
            
            PreparedStatement st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof String) {
                    st.setString(i + 1, (String) param);
                } else if (param instanceof Long) {
                    st.setLong(i + 1, (Long) param);
                } else if (param instanceof Integer) {
                    st.setInt(i + 1, (Integer) param);
                } else if (param instanceof Timestamp) {
                    st.setTimestamp(i + 1, (Timestamp) param);
                }
            }
            
            int rowsInserted = st.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet generatedKeys = st.getGeneratedKeys();
                if (generatedKeys.next()) {
                    announcement.setId(generatedKeys.getLong(1));
                }
                generatedKeys.close();
                
                st.close();
                connection.close();
                return Result.success("添加成功");
            } else {
                st.close();
                connection.close();
                return Result.error("添加失败");
            }
            
        } catch (Exception e) {
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    @Override
    public Result update(YeeAnnouncement announcement) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) announcement.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder sql = new StringBuilder("UPDATE yee_announcement SET ");
            List<Object> parameters = new ArrayList<>();
            
            if (announcement.getTitle() != null && !announcement.getTitle().trim().isEmpty()) {
                sql.append("`title` = ?, ");
                parameters.add(announcement.getTitle());
            }
            
            if (announcement.getContent() != null && !announcement.getContent().trim().isEmpty()) {
                sql.append("`content` = ?, ");
                parameters.add(announcement.getContent());
            }
            
            if (announcement.getCourseId() > 0) {
                sql.append("`courseId` = ?, ");
                parameters.add(announcement.getCourseId());
            }
            
            if (announcement.getUserId() > 0) {
                sql.append("`userId` = ?, ");
                parameters.add(announcement.getUserId());
            }
            
            if (announcement.getAddTime() != null) {
                sql.append("`addTime` = ?, ");
                parameters.add(announcement.getAddTime());
            }
            
            if (parameters.isEmpty()) {
                connection.close();
                return Result.error("没有可更新的字段");
            }
            
            sql.delete(sql.length() - 2, sql.length());
            sql.append(" WHERE id = ?");
            parameters.add(announcement.getId());
            
            PreparedStatement st = connection.prepareStatement(sql.toString());
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof String) {
                    st.setString(i + 1, (String) param);
                } else if (param instanceof Long) {
                    st.setLong(i + 1, (Long) param);
                } else if (param instanceof Integer) {
                    st.setInt(i + 1, (Integer) param);
                } else if (param instanceof Timestamp) {
                    st.setTimestamp(i + 1, (Timestamp) param);
                }
            }
            
            int rowsUpdated = st.executeUpdate();
            st.close();
            connection.close();
            
            if (rowsUpdated > 0) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败：未找到匹配的记录");
            }
            
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result delete(Integer schoolId, int id) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "DELETE FROM yee_announcement WHERE id = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, id);
            
            int rowsDeleted = st.executeUpdate();
            st.close();
            connection.close();
            
            if (rowsDeleted > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败：未找到匹配的记录");
            }
            
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Override
    public Result like(Integer schoolId, long courseId, String name) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "SELECT * FROM yee_announcement WHERE courseId = ? AND title LIKE ? ORDER BY addTime DESC";
            String countSql = "SELECT COUNT(*) FROM yee_announcement WHERE courseId = ? AND title LIKE ?";
            
            String searchTitle = "%" + name + "%";
            
            PreparedStatement countSt = connection.prepareStatement(countSql);
            countSt.setLong(1, courseId);
            countSt.setString(2, searchTitle);
            ResultSet countRs = countSt.executeQuery();
            
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, courseId);
            st.setString(2, searchTitle);
            ResultSet rs = st.executeQuery();
            
            List<YeeAnnouncement> announcements = new ArrayList<>();
            while (rs.next()) {
                YeeAnnouncement announcement = rsToYeeAnnouncement(rs);
                announcements.add(announcement);
            }
            
            countRs.close();
            countSt.close();
            rs.close();
            st.close();
            connection.close();
            
            return Result.success(announcements, (long) totalCount);
            
        } catch (Exception e) {
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
}
