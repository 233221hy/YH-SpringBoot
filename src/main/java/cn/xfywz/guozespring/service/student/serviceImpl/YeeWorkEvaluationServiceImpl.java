package cn.xfywz.guozespring.service.student.serviceImpl;


import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeWork;
import cn.xfywz.guozespring.entity.vo.StudentStats;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.student.YeeStudentMangerService;
import cn.xfywz.guozespring.service.student.YeeWorkEvaluationService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class YeeWorkEvaluationServiceImpl implements YeeWorkEvaluationService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private YeeStudentMangerService yeeStudentMangerService;

    @Override
    public Result selectList(int schoolId, long studentId,int type, int pageSize, int pageNum) throws  Exception{
        // 1. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 获取学生统计信息
        StudentStats studentStats = yeeStudentMangerService.getStudentStats(schoolId, studentId);

        if (pageNum <= 0) pageNum = 1;
        int offset = (pageNum - 1) * pageSize;

        List<YeeWork> resultList = new ArrayList<>();
        long total = 0;

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {

            // 构建 SQL 查询 workId 列表
            String sql;
            String countSql;

            if (type == 1) {
                // 已完成：addTime 和 subScore 都不为空
                sql = """
                SELECT workId 
                FROM yee_work_evaluation 
                WHERE markId = ? 
                  AND addTime IS NOT NULL 
                  AND subScore IS NOT NULL
                ORDER BY id DESC 
                LIMIT ? OFFSET ?
                """;
                countSql = """
                SELECT COUNT(*) 
                FROM yee_work_evaluation 
                WHERE markId = ? 
                  AND addTime IS NOT NULL 
                  AND subScore IS NOT NULL
                """;
            } else if (type == 0) {
                // 进行中：addTime 或 subScore 为空
                sql = """
                SELECT workId 
                FROM yee_work_evaluation 
                WHERE markId = ? 
                  AND (addTime IS NULL OR subScore IS NULL)
                ORDER BY id DESC 
                LIMIT ? OFFSET ?
                """;
                countSql = """
                SELECT COUNT(*) 
                FROM yee_work_evaluation 
                WHERE markId = ? 
                  AND (addTime IS NULL OR subScore IS NULL)
                """;
            } else {
                return Result.error("type 不合法，0=进行中，1=已完成");
            }

            // 查询 workId 列表
            List<Long> workIds = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, studentId);
                stmt.setInt(2, pageSize);
                stmt.setInt(3, offset);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        workIds.add(rs.getLong("workId"));
                    }
                }
            }

            // 如果没有符合条件的记录
            if (workIds.isEmpty()) {
                return Result.success("未查询到信息");
            }

            //  查询总数量
            try (PreparedStatement ct = conn.prepareStatement(countSql)) {
                ct.setLong(1, studentId);
                try (ResultSet rs = ct.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getLong(1);
                    }
                }
            }

            //  批量查询作业详情
            String inPlaceholders = String.join(",", Collections.nCopies(workIds.size(), "?"));
            String workSql = "SELECT * FROM yee_work WHERE id IN (" + inPlaceholders + ") ORDER BY FIELD(id, " + inPlaceholders + ")";

            try (PreparedStatement st = conn.prepareStatement(workSql)) {
                int index = 1;
                // 设置 IN 参数
                for (Long workId : workIds) {
                    st.setLong(index++, workId);
                }
                // 设置 FIELD 排序参数
                for (Long workId : workIds) {
                    st.setLong(index++, workId);
                }

                try (ResultSet rs = st.executeQuery()) {
                    List<YeeWork> works = rsWorkEvaluation(rs); // 假设你有这个方法解析 ResultSet
                    Map<Long, YeeWork> workMap = new LinkedHashMap<>();
                    for (YeeWork w : works) {
                        workMap.putIfAbsent(w.getId().longValue(), w);
                    }
                    // 按 workIds 顺序重组
                    resultList = workIds.stream()
                            .map(workMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                }
            }

            return Result.success(resultList, total)
                    .extra("studentStats", studentStats);

        } catch (Exception e) {
            return Result.error("查询失败，请稍后重试");
        }
    }

    @Override
    public Result selectById(int schoolId, int workId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        String sql = "SELECT * FROM yee_work WHERE id = ?";
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement st = conn.prepareStatement(sql)) {

            st.setInt(1, workId);
            try (ResultSet rs = st.executeQuery()) {
                List<YeeWork> list = rsWorkEvaluation(rs);
                if (list.isEmpty()) {
                    return Result.error("没有此作业");
                }
                return Result.success(list.get(0));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询作业详情失败，请稍后重试");
        }
    }


    /**
     * 将 ResultSet 映射为 YeeWork 列表
     */
    private List<YeeWork> rsWorkEvaluation(ResultSet rs) throws SQLException {
        List<YeeWork> workEvaluations = new ArrayList<>();
        while (rs.next()) {
            YeeWork w = new YeeWork();
            w.setId(rs.getInt("id"));
            w.setUserId(rs.getInt("userId"));
            w.setTitle(rs.getString("title"));
            w.setTopicNumber(rs.getInt("topicNumber"));
            w.setScore(rs.getInt("score"));
            w.setType(rs.getInt("type"));
            w.setRemarks(rs.getString("remarks"));
            w.setAddTime(rs.getTimestamp("addTime"));
            w.setNodeId(rs.getInt("nodeId"));
            w.setSequence(rs.getInt("sequence"));
            w.setCourseId(rs.getInt("courseId"));
            w.setStartTime(rs.getInt("startTime"));
            w.setStartTime(rs.getInt("startTime"));
            w.setCreateUserId(rs.getInt("createUserId"));
            w.setTeacherType(rs.getInt("teacherType"));
            w.setFrequency(rs.getInt("frequency"));

            workEvaluations.add(w);
        }
        return workEvaluations;
    }
}
