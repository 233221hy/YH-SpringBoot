package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeDefaultScoreRule;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeDefaultScoreRuleService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 评分规则
 * - 列表查询
 * - 新增评分规则
 * - 修改评分规则
 * - 删除评分规则
 * - 模糊查询
 */
@Service
public class YeeDefaultScoreRuleServiceImpl implements YeeDefaultScoreRuleService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private YeeDefaultScoreRule rsToYeeDefaultScoreRule(ResultSet rs) throws SQLException {
        YeeDefaultScoreRule rule = new YeeDefaultScoreRule();
        rule.setId(rs.getLong("id"));
        rule.setCourseId(rs.getLong("courseId"));
        rule.setUseVideo(rs.getLong("useVideo"));
        rule.setVideoRatio(rs.getLong("videoRatio"));
        rule.setUseDiscuss(rs.getLong("useDiscuss"));
        rule.setDiscussRatio(rs.getLong("discussRatio"));
        rule.setDiscussItems(rs.getString("discussItems"));
        rule.setUseWork(rs.getLong("useWork"));
        rule.setWorkRatio(rs.getLong("workRatio"));
        rule.setWorkItems(rs.getString("workItems"));
        rule.setUseExam(rs.getLong("useExam"));
        rule.setExamRatio(rs.getLong("examRatio"));
        rule.setExamItems(rs.getString("examItems"));
        rule.setUseExtra(rs.getLong("useExtra"));
        rule.setExtraRatio(rs.getLong("extraRatio"));
        rule.setUseReport(rs.getLong("useReport"));
        rule.setReportRatio(rs.getLong("reportRatio"));
        rule.setAddTime(rs.getTimestamp("addTime"));
        rule.setCalcNumber(rs.getLong("calcNumber"));
        rule.setName(rs.getString("name"));
        rule.setVideoItems(rs.getString("videoItems"));
        rule.setUpdateTime(rs.getTimestamp("updateTime"));
        rule.setVideoMode(rs.getLong("videoMode"));
        rule.setDescription(rs.getString("description"));
        rule.setSchoolId(rs.getLong("schoolId"));
        return rule;
    }

    @Override
    public Result list(int schoolId, long courseId, int pageNum, int pageSize) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            int offset = (pageNum - 1) * pageSize;
            String sql = "SELECT * FROM yee_default_score_rule WHERE courseId = ? ORDER BY updateTime DESC LIMIT ? OFFSET ?";
            String countSql = "SELECT COUNT(*) FROM yee_default_score_rule WHERE courseId = ?";
            
            PreparedStatement countSt = connection.prepareStatement(countSql);
            countSt.setLong(1, courseId);
            ResultSet countRs = countSt.executeQuery();
            
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, courseId);
            st.setInt(2, pageSize);
            st.setInt(3, offset);
            ResultSet rs = st.executeQuery();
            
            List<YeeDefaultScoreRule> ruleList = new ArrayList<>();
            while (rs.next()) {
                YeeDefaultScoreRule rule = rsToYeeDefaultScoreRule(rs);
                ruleList.add(rule);
            }
            
            countRs.close();
            countSt.close();
            rs.close();
            st.close();
            connection.close();
            
            return Result.success(ruleList, (long) totalCount);
            
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result add(YeeDefaultScoreRule yeeDefaultScoreRule) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) yeeDefaultScoreRule.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder columns = new StringBuilder("INSERT INTO yee_default_score_rule (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();
            
            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getSchoolId());
            
            if (yeeDefaultScoreRule.getCourseId() > 0) {
                columns.append("`courseId`, ");
                values.append("?, ");
                parameters.add(yeeDefaultScoreRule.getCourseId());
            }
            
            if (yeeDefaultScoreRule.getName() != null && !yeeDefaultScoreRule.getName().trim().isEmpty()) {
                columns.append("`name`, ");
                values.append("?, ");
                parameters.add(yeeDefaultScoreRule.getName());
            }
            
            columns.append("`useVideo`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getUseVideo());
            
            columns.append("`videoRatio`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getVideoRatio());
            
            columns.append("`useWork`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getUseWork());
            
            columns.append("`workRatio`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getWorkRatio());
            
            columns.append("`useExam`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getUseExam());
            
            columns.append("`examRatio`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getExamRatio());
            
            columns.append("`useDiscuss`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getUseDiscuss());
            
            columns.append("`discussRatio`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getDiscussRatio());
            
            columns.append("`useExtra`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getUseExtra());

            columns.append("`extraRatio`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getExtraRatio());

            columns.append("`useReport`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getUseReport());

            columns.append("`reportRatio`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getReportRatio());

            if (yeeDefaultScoreRule.getVideoItems() != null && !yeeDefaultScoreRule.getVideoItems().trim().isEmpty()) {
                columns.append("`videoItems`, ");
                values.append("?, ");
                parameters.add(yeeDefaultScoreRule.getVideoItems());
            }
            
            if (yeeDefaultScoreRule.getWorkItems() != null && !yeeDefaultScoreRule.getWorkItems().trim().isEmpty()) {
                columns.append("`workItems`, ");
                values.append("?, ");
                parameters.add(yeeDefaultScoreRule.getWorkItems());
            }
            
            if (yeeDefaultScoreRule.getExamItems() != null && !yeeDefaultScoreRule.getExamItems().trim().isEmpty()) {
                columns.append("`examItems`, ");
                values.append("?, ");
                parameters.add(yeeDefaultScoreRule.getExamItems());
            }
            
            if (yeeDefaultScoreRule.getDiscussItems() != null && !yeeDefaultScoreRule.getDiscussItems().trim().isEmpty()) {
                columns.append("`discussItems`, ");
                values.append("?, ");
                parameters.add(yeeDefaultScoreRule.getDiscussItems());
            }
            
            if (yeeDefaultScoreRule.getDescription() != null && !yeeDefaultScoreRule.getDescription().trim().isEmpty()) {
                columns.append("`description`, ");
                values.append("?, ");
                parameters.add(yeeDefaultScoreRule.getDescription());
            }
            
            columns.append("`videoMode`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getVideoMode());
            
            columns.append("`calcNumber`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getCalcNumber());
            
            columns.append("`addTime`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getAddTime() != null ? yeeDefaultScoreRule.getAddTime() : new Timestamp(System.currentTimeMillis()));
            
            columns.append("`updateTime`, ");
            values.append("?, ");
            parameters.add(yeeDefaultScoreRule.getUpdateTime() != null ? yeeDefaultScoreRule.getUpdateTime() : new Timestamp(System.currentTimeMillis()));
            
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
                    yeeDefaultScoreRule.setId(generatedKeys.getLong(1));
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
    public Result update(YeeDefaultScoreRule yeeDefaultScoreRule) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) yeeDefaultScoreRule.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder sql = new StringBuilder("UPDATE yee_default_score_rule SET ");
            List<Object> parameters = new ArrayList<>();
            
            if (yeeDefaultScoreRule.getName() != null && !yeeDefaultScoreRule.getName().trim().isEmpty()) {
                sql.append("`name` = ?, ");
                parameters.add(yeeDefaultScoreRule.getName());
            }
            
            sql.append("`useVideo` = ?, ");
            parameters.add(yeeDefaultScoreRule.getUseVideo());
            
            sql.append("`videoRatio` = ?, ");
            parameters.add(yeeDefaultScoreRule.getVideoRatio());
            
            sql.append("`useWork` = ?, ");
            parameters.add(yeeDefaultScoreRule.getUseWork());
            
            sql.append("`workRatio` = ?, ");
            parameters.add(yeeDefaultScoreRule.getWorkRatio());
            
            sql.append("`useExam` = ?, ");
            parameters.add(yeeDefaultScoreRule.getUseExam());
            
            sql.append("`examRatio` = ?, ");
            parameters.add(yeeDefaultScoreRule.getExamRatio());
            
            sql.append("`useDiscuss` = ?, ");
            parameters.add(yeeDefaultScoreRule.getUseDiscuss());
            
            sql.append("`discussRatio` = ?, ");
            parameters.add(yeeDefaultScoreRule.getDiscussRatio());
            
            sql.append("`useExtra` = ?, ");
            parameters.add(yeeDefaultScoreRule.getUseExtra());

            sql.append("`extraRatio` = ?, ");
            parameters.add(yeeDefaultScoreRule.getExtraRatio());

            sql.append("`useReport` = ?, ");
            parameters.add(yeeDefaultScoreRule.getUseReport());

            sql.append("`reportRatio` = ?, ");
            parameters.add(yeeDefaultScoreRule.getReportRatio());

            if (yeeDefaultScoreRule.getVideoItems() != null) {
                sql.append("`videoItems` = ?, ");
                parameters.add(yeeDefaultScoreRule.getVideoItems());
            }
            
            if (yeeDefaultScoreRule.getWorkItems() != null) {
                sql.append("`workItems` = ?, ");
                parameters.add(yeeDefaultScoreRule.getWorkItems());
            }
            
            if (yeeDefaultScoreRule.getExamItems() != null) {
                sql.append("`examItems` = ?, ");
                parameters.add(yeeDefaultScoreRule.getExamItems());
            }
            
            if (yeeDefaultScoreRule.getDiscussItems() != null) {
                sql.append("`discussItems` = ?, ");
                parameters.add(yeeDefaultScoreRule.getDiscussItems());
            }
            
            if (yeeDefaultScoreRule.getDescription() != null) {
                sql.append("`description` = ?, ");
                parameters.add(yeeDefaultScoreRule.getDescription());
            }
            
            sql.append("`videoMode` = ?, ");
            parameters.add(yeeDefaultScoreRule.getVideoMode());
            
            sql.append("`calcNumber` = ?, ");
            parameters.add(yeeDefaultScoreRule.getCalcNumber());
            
            sql.append("`updateTime` = ?, ");
            parameters.add(new Timestamp(System.currentTimeMillis()));
            
            sql.delete(sql.length() - 2, sql.length());
            sql.append(" WHERE id = ? AND courseId = ?");
            parameters.add(yeeDefaultScoreRule.getId());
            parameters.add(yeeDefaultScoreRule.getCourseId());
            
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
    public Result delete(int schoolId, long courseId, int id) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "DELETE FROM yee_default_score_rule WHERE id = ? AND courseId = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, id);
            st.setLong(2, courseId);
            
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
    public Result like(int schoolId, long courseId, String name) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "SELECT * FROM yee_default_score_rule WHERE courseId = ? AND name LIKE ? ORDER BY updateTime DESC";
            String countSql = "SELECT COUNT(*) FROM yee_default_score_rule WHERE courseId = ? AND name LIKE ?";
            
            String searchName = "%" + name + "%";
            
            PreparedStatement countSt = connection.prepareStatement(countSql);
            countSt.setLong(1, courseId);
            countSt.setString(2, searchName);
            ResultSet countRs = countSt.executeQuery();
            
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, courseId);
            st.setString(2, searchName);
            ResultSet rs = st.executeQuery();
            
            List<YeeDefaultScoreRule> ruleList = new ArrayList<>();
            while (rs.next()) {
                YeeDefaultScoreRule rule = rsToYeeDefaultScoreRule(rs);
                ruleList.add(rule);
            }
            
            countRs.close();
            countSt.close();
            rs.close();
            st.close();
            connection.close();
            
            return Result.success(ruleList, (long) totalCount);
            
        } catch (Exception e) {
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
}
