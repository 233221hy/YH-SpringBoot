package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.dto.DiscussScoreDto;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeDiscussScore;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeDiscussScoreService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YeeDiscussScoreServiceImpl implements YeeDiscussScoreService {
    
    @Autowired
    private SlSchoolMapper slSchoolMapper;

    /**
     * ResultSet转换为YeeDiscussScore对象
     */
    private YeeDiscussScore rsToYeeDiscussScore(ResultSet rs) throws SQLException {
        YeeDiscussScore discussScore = new YeeDiscussScore();
        discussScore.setId(rs.getInt("id"));
        discussScore.setCourseId(rs.getInt("courseId"));
        discussScore.setDiscussId(rs.getInt("discussId"));
        discussScore.setUserId(rs.getInt("userId"));
        discussScore.setScore(rs.getBigDecimal("score"));
        discussScore.setClassId(rs.getInt("classId"));
        discussScore.setPostQty(rs.getInt("postQty"));
        discussScore.setReplyQty(rs.getInt("replyQty"));
        discussScore.setLikeQty(rs.getInt("likeQty"));
        discussScore.setScored(rs.getInt("scored"));
        discussScore.setRank(rs.getInt("rank"));
        discussScore.setUserType(rs.getInt("userType"));
        discussScore.setAllQty(rs.getInt("allQty"));
        discussScore.setSchoolId(rs.getInt("schoolId"));
        return discussScore;
    }

    /**
     * 获取讨论得分列表
     * 支持分页查询指定讨论的所有学生得分情况
     */
    @Override
    public Result list(Integer schoolId, Integer discussId, Integer pageSize, Integer pageNum) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            int offset = (pageNum - 1) * pageSize;
            
            // 构建查询SQL
            String sql;
            String countSql;
            if (discussId != null && discussId > 0) {
                sql = "SELECT ds.*, s.name as studentName, s.number as studentNumber " +
                      "FROM yee_discuss_score ds " +
                      "LEFT JOIN yee_student s ON ds.userId = s.id " +
                      "WHERE ds.discussId = ? " +
                      "ORDER BY ds.score DESC, ds.rank ASC " +
                      "LIMIT ? OFFSET ?";
                countSql = "SELECT COUNT(*) FROM yee_discuss_score WHERE discussId = ?";
            } else {
                sql = "SELECT ds.*, s.name as studentName, s.number as studentNumber " +
                      "FROM yee_discuss_score ds " +
                      "LEFT JOIN yee_student s ON ds.userId = s.id " +
                      "ORDER BY ds.score DESC, ds.rank ASC " +
                      "LIMIT ? OFFSET ?";
                countSql = "SELECT COUNT(*) FROM yee_discuss_score";
            }
            
            // 获取总数
            PreparedStatement countStmt = connection.prepareStatement(countSql);
            if (discussId != null && discussId > 0) {
                countStmt.setInt(1, discussId);
            }
            ResultSet countRs = countStmt.executeQuery();
            
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            countRs.close();
            countStmt.close();
            
            // 查询列表数据
            PreparedStatement stmt = connection.prepareStatement(sql);
            int paramIndex = 1;
            if (discussId != null && discussId > 0) {
                stmt.setInt(paramIndex++, discussId);
            }
            stmt.setInt(paramIndex++, pageSize);
            stmt.setInt(paramIndex, offset);
            
            ResultSet rs = stmt.executeQuery();
            
            List<YeeDiscussScore> discussScoreList = new ArrayList<>();
            while (rs.next()) {
                YeeDiscussScore discussScore = rsToYeeDiscussScore(rs);
                discussScoreList.add(discussScore);
            }
            
            rs.close();
            stmt.close();
            connection.close();
            
            return Result.success(discussScoreList, (long) totalCount);
            
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 更新讨论得分
     * 支持更新学生的讨论参与得分和相关统计信息
     */
    @Override
    public Result update(YeeDiscussScore yeeDiscussScore) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(yeeDiscussScore.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            // 检查记录是否存在
            String checkSql = "SELECT COUNT(*) FROM yee_discuss_score WHERE id = ?";
            PreparedStatement checkStmt = connection.prepareStatement(checkSql);
            checkStmt.setInt(1, yeeDiscussScore.getId());
            ResultSet checkRs = checkStmt.executeQuery();
            
            boolean recordExists = false;
            if (checkRs.next()) {
                recordExists = checkRs.getInt(1) > 0;
            }
            checkRs.close();
            checkStmt.close();
            
            if (!recordExists) {
                connection.close();
                return Result.error("记录不存在");
            }
            
            // 构建更新SQL
            StringBuilder sql = new StringBuilder("UPDATE yee_discuss_score SET ");
            List<Object> parameters = new ArrayList<>();
            
            if (yeeDiscussScore.getCourseId() != null) {
                sql.append("courseId = ?, ");
                parameters.add(yeeDiscussScore.getCourseId());
            }
            
            if (yeeDiscussScore.getDiscussId() != null) {
                sql.append("discussId = ?, ");
                parameters.add(yeeDiscussScore.getDiscussId());
            }
            
            if (yeeDiscussScore.getUserId() != null) {
                sql.append("userId = ?, ");
                parameters.add(yeeDiscussScore.getUserId());
            }
            
            if (yeeDiscussScore.getScore() != null) {
                sql.append("score = ?, ");
                parameters.add(yeeDiscussScore.getScore());
            }
            
            if (yeeDiscussScore.getClassId() != null) {
                sql.append("classId = ?, ");
                parameters.add(yeeDiscussScore.getClassId());
            }
            
            if (yeeDiscussScore.getPostQty() != null) {
                sql.append("postQty = ?, ");
                parameters.add(yeeDiscussScore.getPostQty());
            }
            
            if (yeeDiscussScore.getReplyQty() != null) {
                sql.append("replyQty = ?, ");
                parameters.add(yeeDiscussScore.getReplyQty());
            }
            
            if (yeeDiscussScore.getLikeQty() != null) {
                sql.append("likeQty = ?, ");
                parameters.add(yeeDiscussScore.getLikeQty());
            }
            
            if (yeeDiscussScore.getScored() != null) {
                sql.append("scored = ?, ");
                parameters.add(yeeDiscussScore.getScored());
            }
            
            if (yeeDiscussScore.getRank() != null) {
                sql.append("rank = ?, ");
                parameters.add(yeeDiscussScore.getRank());
            }
            
            if (yeeDiscussScore.getUserType() != null) {
                sql.append("userType = ?, ");
                parameters.add(yeeDiscussScore.getUserType());
            }
            
            if (yeeDiscussScore.getAllQty() != null) {
                sql.append("allQty = ?, ");
                parameters.add(yeeDiscussScore.getAllQty());
            }
            
            if (parameters.isEmpty()) {
                connection.close();
                return Result.error("没有可更新的字段");
            }
            
            // 移除最后的逗号和空格
            sql.delete(sql.length() - 2, sql.length());
            sql.append(" WHERE id = ?");
            parameters.add(yeeDiscussScore.getId());
            
            PreparedStatement stmt = connection.prepareStatement(sql.toString());
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof Integer) {
                    stmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof BigDecimal) {
                    stmt.setBigDecimal(i + 1, (BigDecimal) param);
                } else if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                }
            }
            
            int rowsUpdated = stmt.executeUpdate();
            stmt.close();
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


    /**
     * 查询学生讨论得分统计列表（支持分页和条件查询）
     */
    @Override
    public Result listStudentDiscussScore(
            Integer schoolId,
            Integer courseId,
            Integer discussId,
            String studentKeyword,
            Integer classId,
            Integer totalPostsMin,
            Integer postCountMin,
            Integer replyCountMin,
            Integer likeCountMin,
            Integer scoredStatus,
            Integer page,
            Integer pageSize
    ) {
        if (schoolId == null || courseId == null || discussId == null) {
            return Result.error("schoolId、courseId、discussId 不能为空");
        }
        if (page == null || page < 1) page = 1;
        if (pageSize == null || pageSize < 1) pageSize = 10;

        int offset = (page - 1) * pageSize;

        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() != 1) {
            return Result.error("学校不存在或未审核通过");
        }

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {

            // === 1. 查询总条数 ===
            String countSql = """
            SELECT COUNT(*) AS total
            FROM yee_discuss_score ds
            INNER JOIN yee_student s ON ds.userId = s.id AND s.schoolId = ds.schoolId
            LEFT JOIN yee_course_class cc ON ds.classId = cc.id
            WHERE ds.userType = 1
              AND ds.schoolId = ?
              AND ds.courseId = ?
              AND ds.discussId = ?
              AND (? IS NULL OR s.number LIKE CONCAT('%', ?, '%') OR s.name LIKE CONCAT('%', ?, '%'))
              AND (? IS NULL OR ds.classId = ?)
              AND (? IS NULL OR ds.allQty >= ?)
              AND (? IS NULL OR ds.postQty >= ?)
              AND (? IS NULL OR ds.replyQty >= ?)
              AND (? IS NULL OR ds.likeQty >= ?)
              AND (? IS NULL OR ds.scored = ?)
            """;

            int total = 0;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                int idx = 1;
                ps.setInt(idx++, schoolId);
                ps.setInt(idx++, courseId);
                ps.setInt(idx++, discussId);

                // 学生关键词（传3次：用于 IS NULL 判断 + number + name）
                ps.setString(idx++, studentKeyword);
                ps.setString(idx++, studentKeyword);
                ps.setString(idx++, studentKeyword);

                // 班级ID
                ps.setObject(idx++, classId);
                ps.setObject(idx++, classId);

                // 数量条件（每个条件传2次：用于 IS NULL + 值）
                ps.setObject(idx++, totalPostsMin);
                ps.setObject(idx++, totalPostsMin);
                ps.setObject(idx++, postCountMin);
                ps.setObject(idx++, postCountMin);
                ps.setObject(idx++, replyCountMin);
                ps.setObject(idx++, replyCountMin);
                ps.setObject(idx++, likeCountMin);
                ps.setObject(idx++, likeCountMin);

                // 打分状态
                ps.setObject(idx++, scoredStatus);
                ps.setObject(idx++, scoredStatus);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getInt("total");
                    }
                }
            }

            // === 2. 查询分页数据 ===
            String dataSql = """
            SELECT 
                ds.id,
                ds.rank,
                s.id AS studentId,
                s.name AS studentName,
                s.number AS studentNumber,
                COALESCE(cc.name, '未分配班级') AS className,
                ds.allQty,
                ds.postQty,
                ds.replyQty,
                ds.likeQty,
                CASE WHEN ds.scored = 1 THEN '已打分' ELSE '未打分' END AS status,
                ds.score
            FROM yee_discuss_score ds
            INNER JOIN yee_student s ON ds.userId = s.id AND s.schoolId = ds.schoolId
            LEFT JOIN yee_course_class cc ON ds.classId = cc.id
            WHERE ds.userType = 1
              AND ds.schoolId = ?
              AND ds.courseId = ?
              AND ds.discussId = ?
              AND (? IS NULL OR s.number LIKE CONCAT('%', ?, '%') OR s.name LIKE CONCAT('%', ?, '%'))
              AND (? IS NULL OR ds.classId = ?)
              AND (? IS NULL OR ds.allQty >= ?)
              AND (? IS NULL OR ds.postQty >= ?)
              AND (? IS NULL OR ds.replyQty >= ?)
              AND (? IS NULL OR ds.likeQty >= ?)
              AND (? IS NULL OR ds.scored = ?)
            ORDER BY ds.rank ASC
            LIMIT ?, ?
            """;

            List<DiscussScoreDto> list = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(dataSql)) {
                int idx = 1;
                ps.setInt(idx++, schoolId);
                ps.setInt(idx++, courseId);
                ps.setInt(idx++, discussId);

                ps.setString(idx++, studentKeyword);
                ps.setString(idx++, studentKeyword);
                ps.setString(idx++, studentKeyword);

                ps.setObject(idx++, classId);
                ps.setObject(idx++, classId);

                ps.setObject(idx++, totalPostsMin);
                ps.setObject(idx++, totalPostsMin);
                ps.setObject(idx++, postCountMin);
                ps.setObject(idx++, postCountMin);
                ps.setObject(idx++, replyCountMin);
                ps.setObject(idx++, replyCountMin);
                ps.setObject(idx++, likeCountMin);
                ps.setObject(idx++, likeCountMin);

                ps.setObject(idx++, scoredStatus);
                ps.setObject(idx++, scoredStatus);

                ps.setInt(idx++, offset);
                ps.setInt(idx++, pageSize);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        DiscussScoreDto dto = new DiscussScoreDto();
                        dto.setId(rs.getInt("id"));
                        dto.setRank(rs.getInt("rank"));
                        dto.setStudentId(rs.getInt("studentId"));
                        dto.setStudentName(rs.getString("studentName"));
                        dto.setStudentNumber(rs.getString("studentNumber"));
                        dto.setClassName(rs.getString("className"));
                        dto.setAllQty(rs.getInt("allQty"));
                        dto.setPostQty(rs.getInt("postQty"));
                        dto.setReplyQty(rs.getInt("replyQty"));
                        dto.setLikeQty(rs.getInt("likeQty"));
                        dto.setStatus(rs.getString("status"));
                        dto.setScore(rs.getBigDecimal("score"));
                        list.add(dto);
                    }
                }
            }

            // === 3. 构造分页结果 ===
            Map<String, Object> result = new HashMap<>();
            result.put("pageNum", page);
            result.put("pageSize", pageSize);
            result.put("size", list.size());
            result.put("total", total);
            result.put("pages", total > 0 ? (int) Math.ceil((double) total / pageSize) : 0);
            result.put("list", list);

            return Result.success(result);

        } catch (SQLException e) {
            e.printStackTrace();
            return Result.error("数据库查询失败：" + e.getMessage());
        }
    }
}
