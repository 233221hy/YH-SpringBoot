package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeCourseScoreRules;
import cn.xfywz.guozespring.exception.BusinessException;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.cache.CacheService;
import cn.xfywz.guozespring.service.teacher.YeeCourseScoreRulesService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 课程计分规则
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YeeCourseScoreRulesServiceImpl implements YeeCourseScoreRulesService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;
    private final DatabaseUtil databaseUtil;
    private final CacheService cacheService;

    private static final String CACHE_KEY_PREFIX = "score_rules:";
    private static final long CACHE_TTL = 1;
    private static final TimeUnit CACHE_TTL_UNIT = TimeUnit.DAYS;

    private static String cacheKey(int schoolId, long courseId, long classId) {
        return CACHE_KEY_PREFIX + schoolId + ":" + courseId + ":" + classId;
    }

    private void evictCache(YeeCourseScoreRules rules) {
        cacheService.evict(cacheKey(rules.getSchoolId(), rules.getCourseId(), rules.getClassId()));
    }

    private void evictCache(int schoolId, long courseId, long classId) {
        cacheService.evict(cacheKey(schoolId, courseId, classId));
    }

    static YeeCourseScoreRules rsToYeeCourseScoreRules(ResultSet rs) throws SQLException {
        YeeCourseScoreRules rules = new YeeCourseScoreRules();
        rules.setId(rs.getLong("id"));
        rules.setCourseId(rs.getLong("courseId"));
        rules.setClassId(rs.getLong("classId"));
        rules.setUseVideo(rs.getLong("useVideo"));
        rules.setVideoRatio(rs.getLong("videoRatio"));
        rules.setUseDiscuss(rs.getLong("useDiscuss"));
        rules.setDiscussRatio(rs.getLong("discussRatio"));
        rules.setDiscussItems(rs.getString("discussItems"));
        rules.setUseWork(rs.getLong("useWork"));
        rules.setWorkRatio(rs.getLong("workRatio"));
        rules.setWorkItems(rs.getString("workItems"));
        rules.setUseExam(rs.getLong("useExam"));
        rules.setExamRatio(rs.getLong("examRatio"));
        rules.setExamItems(rs.getString("examItems"));
        rules.setUseExtra(rs.getLong("useExtra"));
        rules.setExtraRatio(rs.getLong("extraRatio"));
        rules.setUseReport(rs.getLong("useReport"));
        rules.setReportRatio(rs.getLong("reportRatio"));
        rules.setAddTime(rs.getTimestamp("addTime"));
        rules.setCalcNumber(rs.getLong("calcNumber"));
        rules.setUpdateTime(rs.getTimestamp("updateTime"));
        rules.setVideoItems(rs.getString("videoItems"));
        rules.setRealTime(rs.getLong("realTime"));
        rules.setVideoMode(rs.getLong("videoMode"));
        rules.setDescription(rs.getString("description"));
        rules.setSchoolId(rs.getInt("schoolId"));
        rules.setAnnounce(rs.getLong("announce"));
        return rules;
    }


    @Override
    public Result info(int schoolId, long courseId, long classId) {
        try {
            YeeCourseScoreRules rules = cacheService.getOrLoad(
                    cacheKey(schoolId, courseId, classId),
                    () -> Objects.requireNonNull(databaseUtil.query(schoolId)
                            .sql("SELECT * FROM yee_course_score_rules WHERE courseId = ? AND classId = ? ORDER BY updateTime DESC")
                            .params(courseId, classId)
                            .single(YeeCourseScoreRulesServiceImpl::rsToYeeCourseScoreRules)
                            .orElse(null)),
                    CACHE_TTL, CACHE_TTL_UNIT);

            return Result.success(rules);

        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result add(YeeCourseScoreRules yeeCourseScoreRules) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) yeeCourseScoreRules.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder columns = new StringBuilder("INSERT INTO yee_course_score_rules (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();
            
            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getSchoolId());
            
            if (yeeCourseScoreRules.getCourseId() > 0) {
                columns.append("`courseId`, ");
                values.append("?, ");
                parameters.add(yeeCourseScoreRules.getCourseId());
            }
            
            if (yeeCourseScoreRules.getClassId() > 0) {
                columns.append("`classId`, ");
                values.append("?, ");
                parameters.add(yeeCourseScoreRules.getClassId());
            }
            
            columns.append("`useVideo`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getUseVideo());
            
            columns.append("`videoRatio`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getVideoRatio());
            
            columns.append("`useWork`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getUseWork());
            
            columns.append("`workRatio`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getWorkRatio());
            
            columns.append("`useExam`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getUseExam());
            
            columns.append("`examRatio`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getExamRatio());
            
            columns.append("`useDiscuss`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getUseDiscuss());
            
            columns.append("`discussRatio`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getDiscussRatio());
            
            columns.append("`useExtra`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getUseExtra());

            columns.append("`extraRatio`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getExtraRatio());

            columns.append("`useReport`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getUseReport());

            columns.append("`reportRatio`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getReportRatio());

            if (yeeCourseScoreRules.getVideoItems() != null && !yeeCourseScoreRules.getVideoItems().trim().isEmpty()) {
                columns.append("`videoItems`, ");
                values.append("?, ");
                parameters.add(yeeCourseScoreRules.getVideoItems());
            }
            
            if (yeeCourseScoreRules.getWorkItems() != null && !yeeCourseScoreRules.getWorkItems().trim().isEmpty()) {
                columns.append("`workItems`, ");
                values.append("?, ");
                parameters.add(yeeCourseScoreRules.getWorkItems());
            }
            
            if (yeeCourseScoreRules.getExamItems() != null && !yeeCourseScoreRules.getExamItems().trim().isEmpty()) {
                columns.append("`examItems`, ");
                values.append("?, ");
                parameters.add(yeeCourseScoreRules.getExamItems());
            }
            
            if (yeeCourseScoreRules.getDiscussItems() != null && !yeeCourseScoreRules.getDiscussItems().trim().isEmpty()) {
                columns.append("`discussItems`, ");
                values.append("?, ");
                parameters.add(yeeCourseScoreRules.getDiscussItems());
            }
            
            if (yeeCourseScoreRules.getDescription() != null && !yeeCourseScoreRules.getDescription().trim().isEmpty()) {
                columns.append("`description`, ");
                values.append("?, ");
                parameters.add(yeeCourseScoreRules.getDescription());
            }
            
            columns.append("`videoMode`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getVideoMode());
            
            columns.append("`realTime`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getRealTime());
            
            columns.append("`announce`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getAnnounce());
            
            columns.append("`calcNumber`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getCalcNumber());
            
            columns.append("`addTime`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getAddTime() != null ? yeeCourseScoreRules.getAddTime() : new Timestamp(System.currentTimeMillis()));
            
            columns.append("`updateTime`, ");
            values.append("?, ");
            parameters.add(yeeCourseScoreRules.getUpdateTime() != null ? yeeCourseScoreRules.getUpdateTime() : new Timestamp(System.currentTimeMillis()));
            
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
                    yeeCourseScoreRules.setId(generatedKeys.getLong(1));
                }
                generatedKeys.close();

                st.close();
                connection.close();
                evictCache(yeeCourseScoreRules);
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
    public Result update(YeeCourseScoreRules yeeCourseScoreRules) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) yeeCourseScoreRules.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder sql = new StringBuilder("UPDATE yee_course_score_rules SET ");
            List<Object> parameters = new ArrayList<>();
            
            sql.append("`useVideo` = ?, ");
            parameters.add(yeeCourseScoreRules.getUseVideo());
            
            sql.append("`videoRatio` = ?, ");
            parameters.add(yeeCourseScoreRules.getVideoRatio());
            
            sql.append("`useWork` = ?, ");
            parameters.add(yeeCourseScoreRules.getUseWork());
            
            sql.append("`workRatio` = ?, ");
            parameters.add(yeeCourseScoreRules.getWorkRatio());
            
            sql.append("`useExam` = ?, ");
            parameters.add(yeeCourseScoreRules.getUseExam());
            
            sql.append("`examRatio` = ?, ");
            parameters.add(yeeCourseScoreRules.getExamRatio());
            
            sql.append("`useDiscuss` = ?, ");
            parameters.add(yeeCourseScoreRules.getUseDiscuss());
            
            sql.append("`discussRatio` = ?, ");
            parameters.add(yeeCourseScoreRules.getDiscussRatio());
            
            sql.append("`useExtra` = ?, ");
            parameters.add(yeeCourseScoreRules.getUseExtra());

            sql.append("`extraRatio` = ?, ");
            parameters.add(yeeCourseScoreRules.getExtraRatio());

            sql.append("`useReport` = ?, ");
            parameters.add(yeeCourseScoreRules.getUseReport());

            sql.append("`reportRatio` = ?, ");
            parameters.add(yeeCourseScoreRules.getReportRatio());

            if (yeeCourseScoreRules.getVideoItems() != null) {
                sql.append("`videoItems` = ?, ");
                parameters.add(yeeCourseScoreRules.getVideoItems());
            }
            
            if (yeeCourseScoreRules.getWorkItems() != null) {
                sql.append("`workItems` = ?, ");
                parameters.add(yeeCourseScoreRules.getWorkItems());
            }
            
            if (yeeCourseScoreRules.getExamItems() != null) {
                sql.append("`examItems` = ?, ");
                parameters.add(yeeCourseScoreRules.getExamItems());
            }
            
            if (yeeCourseScoreRules.getDiscussItems() != null) {
                sql.append("`discussItems` = ?, ");
                parameters.add(yeeCourseScoreRules.getDiscussItems());
            }
            
            if (yeeCourseScoreRules.getDescription() != null) {
                sql.append("`description` = ?, ");
                parameters.add(yeeCourseScoreRules.getDescription());
            }
            
            sql.append("`videoMode` = ?, ");
            parameters.add(yeeCourseScoreRules.getVideoMode());
            
            sql.append("`realTime` = ?, ");
            parameters.add(yeeCourseScoreRules.getRealTime());
            
            sql.append("`announce` = ?, ");
            parameters.add(yeeCourseScoreRules.getAnnounce());
            
            sql.append("`calcNumber` = ?, ");
            parameters.add(yeeCourseScoreRules.getCalcNumber());
            
            sql.append("`updateTime` = ?, ");
            parameters.add(new Timestamp(System.currentTimeMillis()));
            
            sql.delete(sql.length() - 2, sql.length());
            sql.append(" WHERE id = ? AND courseId = ? AND classId = ?");
            parameters.add(yeeCourseScoreRules.getId());
            parameters.add(yeeCourseScoreRules.getCourseId());
            parameters.add(yeeCourseScoreRules.getClassId());
            
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
                evictCache(yeeCourseScoreRules);
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败：没有修改记录");
            }
            
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result delete(int schoolId, long courseId, long classId, int id) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "DELETE FROM yee_course_score_rules WHERE id = ? AND courseId = ? AND classId = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, id);
            st.setLong(2, courseId);
            st.setLong(3, classId);
            
            int rowsDeleted = st.executeUpdate();
            st.close();
            connection.close();
            
            if (rowsDeleted > 0) {
                evictCache(schoolId, courseId, classId);
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败：未找到匹配的记录");
            }
            
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Override
    public Result publish(int schoolId, Integer id, Integer announce) {
        // 先查出 courseId + classId，用于精准清除缓存
        YeeCourseScoreRules rules = databaseUtil.query(schoolId)
                .sql("SELECT courseId, classId FROM yee_course_score_rules WHERE id = ?")
                .param(id)
                .single(rs -> {
                    YeeCourseScoreRules r = new YeeCourseScoreRules();
                    r.setCourseId(rs.getLong("courseId"));
                    r.setClassId(rs.getLong("classId"));
                    return r;
                })
                .orElse(null);

        if (rules == null) {
            throw new BusinessException("评分规则不存在，无法发布");
        }

        int rows = databaseUtil.update(schoolId)
                .table("yee_course_score_rules")
                .set("announce", announce)
                .eq("id", id)
                .update();

        if (rows == 0) {
            throw new BusinessException("评分规则不存在，无法发布");
        }
        evictCache(schoolId, rules.getCourseId(), rules.getClassId());
        return Result.success("发布成功");
    }
}
