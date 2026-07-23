package cn.xfywz.guozespring.service.student.serviceImpl;


import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeExamTopic;
import cn.xfywz.guozespring.entity.vo.CollectedWorkGroup;
import cn.xfywz.guozespring.entity.vo.StudentStats;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.student.YeeQuestionBankService;
import cn.xfywz.guozespring.service.student.YeeStudentMangerService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class YeeQuestionBankServiceImpl implements YeeQuestionBankService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private YeeStudentMangerService yeeStudentMangerService;

    @Override
    public Result selectAll(int schoolId, long studentId, int pageSize, int pageNum) throws Exception {
        // 1. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 获取学生统计信息
        StudentStats studentStats = yeeStudentMangerService.getStudentStats(schoolId, studentId);

        int offset = (pageNum - 1) * pageSize;
        List<CollectedWorkGroup> resultList = new ArrayList<>();
        long total = 0;

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {

            // Step 1: 分页查询 workId 列表（含课程、章节、作业标题等）
            String groupSql = """
            SELECT 
                ct.workId,
                MAX(ct.addTime) AS lastAddTime,
                COUNT(ct.topicId) AS topicCount,
                w.title AS workTitle,
                c.name AS courseName,
                ch.name AS chapterName
            FROM 
                yee_collection_topic ct
                LEFT JOIN yee_work w ON w.id = ct.workId
                LEFT JOIN yee_course c ON c.id = ct.courseId
                LEFT JOIN yee_node yn ON yn.id = w.nodeId
                LEFT JOIN yee_chapter ch ON ch.id = yn.chapterId
            WHERE 
                ct.userId = ?
            GROUP BY 
                ct.workId, w.title, c.name, ch.name
            ORDER BY 
                lastAddTime DESC
            LIMIT ? OFFSET ?
            """;

            List<Long> workIds = new ArrayList<>();
            Map<Long, CollectedWorkGroup> workGroupMap = new HashMap<>();

            try (PreparedStatement stmt = conn.prepareStatement(groupSql)) {
                stmt.setLong(1, studentId);
                stmt.setInt(2, pageSize);
                stmt.setInt(3, offset);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Long workId = rs.getLong("workId");
                        CollectedWorkGroup group = new CollectedWorkGroup();
                        group.setWorkId(workId);
                        group.setWorkTitle(rs.getString("workTitle"));
                        group.setCourseName(rs.getString("courseName"));
                        group.setChapterName(rs.getString("chapterName"));
                        group.setLastAddTime(rs.getTimestamp("lastAddTime"));
                        group.setTopicCount(rs.getInt("topicCount"));
                        group.setTopics(new ArrayList<>());

                        workGroupMap.put(workId, group);
                        workIds.add(workId);
                    }
                }
            }

            // 如果没有收藏
            if (workIds.isEmpty()) {
                return Result.success("未查询到信息");
            }

            // Step 2: 查询总分组数
            String countSql = """
            SELECT COUNT(DISTINCT workId) 
            FROM yee_collection_topic 
            WHERE userId = ?
            """;
            try (PreparedStatement countSt = conn.prepareStatement(countSql)) {
                countSt.setLong(1, studentId);
                try (ResultSet rs = countSt.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getLong(1);
                    }
                }
            }

            // Step 3: 查询这些 workId 下的所有 topicId，并按 workId 分组
            String topicIdSql = """
            SELECT workId, topicId 
            FROM yee_collection_topic 
            WHERE userId = ? AND workId IN (""" +
                    String.join(",", Collections.nCopies(workIds.size(), "?")) +
                    ") ORDER BY id DESC";

            Map<Long, List<Long>> workToTopicIds = new HashMap<>();
            try (PreparedStatement stmt = conn.prepareStatement(topicIdSql)) {
                stmt.setLong(1, studentId);
                for (int i = 0; i < workIds.size(); i++) {
                    stmt.setLong(i + 2, workIds.get(i));
                }
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Long workId = rs.getLong("workId");
                        Long topicId = rs.getLong("topicId");
                        workToTopicIds.computeIfAbsent(workId, k -> new ArrayList<>()).add(topicId);
                    }
                }
            }

            // 提取所有 topicId（去重）
            List<Long> allTopicIds = workToTopicIds.values().stream()
                    .flatMap(List::stream)
                    .distinct()
                    .collect(Collectors.toList());

            if (allTopicIds.isEmpty()) {
                return Result.success(new ArrayList<>(), total).extra("studentStats", studentStats);
            }

            // Step 4: 批量查询题目详情
            String inPlaceholders = String.join(",", Collections.nCopies(allTopicIds.size(), "?"));
            String examSql = "SELECT * FROM yee_work_topic WHERE id IN (" + inPlaceholders + ")";

            Map<Long, YeeExamTopic> topicMap = new HashMap<>();
            try (PreparedStatement st = conn.prepareStatement(examSql)) {
                for (int i = 0; i < allTopicIds.size(); i++) {
                    st.setLong(i + 1, allTopicIds.get(i));
                }
                try (ResultSet rs = st.executeQuery()) {
                    List<YeeExamTopic> topics = mapExams(rs);
                    topicMap.putAll(topics.stream()
                            .collect(Collectors.toMap(YeeExamTopic::getId, Function.identity())));
                }
            }

            // Step 5: 将题目填充到对应的 workId 组中
            for (Map.Entry<Long, List<Long>> entry : workToTopicIds.entrySet()) {
                Long workId = entry.getKey();
                CollectedWorkGroup group = workGroupMap.get(workId);
                if (group != null) {
                    List<YeeExamTopic> topics = entry.getValue().stream()
                            .map(topicMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    group.setTopics(topics);
                }
            }

            resultList = new ArrayList<>(workGroupMap.values());

            return Result.success(resultList, total)
                    .extra("studentStats", studentStats);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询收藏试题失败，请稍后重试");
        }
    }

    /**
     * 查询某章节下，当前用户收藏的所有试题
     */
    @Override
    public Result selectById(int schoolId, int chapterId,long studentId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }


        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {

            // 🔍 核心SQL：直接查询该章节下，用户收藏的所有题目信息
            String sql = """
            SELECT 
                et.*,
                MAX(ct.addTime) AS lastAddTime
            FROM yee_collection_topic ct
            JOIN yee_work_topic et ON et.id = ct.topicId
            JOIN yee_work w ON w.id = ct.workId
            JOIN yee_node yn ON yn.id = w.nodeId
            WHERE 
                ct.userId = ?
                AND yn.chapterId = ?
            GROUP BY et.id
            ORDER BY lastAddTime DESC
            """;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setLong(1, studentId);
                stmt.setInt(2, chapterId);

                try (ResultSet rs = stmt.executeQuery()) {
                    List<YeeExamTopic> topics = mapExams(rs); // 复用你的 mapExams 映射方法

                    if (topics.isEmpty()) {
                        return Result.success("未查询到信息");
                    }

                    return Result.success(topics);
                }
            }

        } catch (SQLException e) {
            return Result.error("查询失败，请稍后重试");
        } catch (Exception e) {
            return Result.error("系统异常");
        }
    }
    /**
         * 将 ResultSet 映射为 YeeExamTopic 列表
         */
        private List<YeeExamTopic> mapExams(ResultSet rs) throws Exception {
            List<YeeExamTopic> list = new ArrayList<>();
            while (rs.next()) {
                YeeExamTopic e = new YeeExamTopic();
                e.setId(rs.getLong("id"));
                e.setTopic(rs.getString("topic"));
                e.setType(rs.getLong("type"));
                e.setLevel(rs.getLong("level"));
                e.setScore(rs.getLong("score"));
                e.setAnalysis(rs.getString("analysis"));
                e.setTitle(rs.getString("title"));
                e.setNumber(rs.getLong("number"));
                e.setScoreMode(rs.getLong("scoreMode"));
                e.setSchoolId(rs.getLong("schoolId"));
                String categoryId = rs.getString("categoryId");
                e.setCategoryId(Arrays.asList(categoryId.split(",")).stream().map(Integer::parseInt).collect(Collectors.toList()));
                e.setCateBid(rs.getLong("cateBid"));
                e.setCateMid(rs.getLong("cateMid"));
                list.add(e);
            }
            return list;
        }

    }


