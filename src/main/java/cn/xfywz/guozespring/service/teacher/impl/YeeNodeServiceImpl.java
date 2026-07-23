package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeNode;
import cn.xfywz.guozespring.entity.vo.ChapterTreeNodeVo;
import cn.xfywz.guozespring.entity.vo.CourseTreeNodeVo;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeNodeService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class YeeNodeServiceImpl implements YeeNodeService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private YeeNode rsToYeeNode(ResultSet rs) throws SQLException {
        YeeNode yeeNode = new YeeNode();
        yeeNode.setId(rs.getLong("id"));
        yeeNode.setName(rs.getString("name"));
        yeeNode.setType(rs.getString("type"));
        yeeNode.setChapterId(rs.getLong("chapterId"));
        yeeNode.setCourseId(rs.getLong("courseId"));
        yeeNode.setVideoFile(rs.getString("videoFile"));
        yeeNode.setVideoDuration(rs.getLong("videoDuration"));
        yeeNode.setVotingPath(rs.getString("votingPath"));
        yeeNode.setTabVideo(rs.getLong("tabVideo"));
        yeeNode.setTabFile(rs.getLong("tabFile"));
        yeeNode.setTabVote(rs.getLong("tabVote"));
        yeeNode.setTabWork(rs.getLong("tabWork"));
        yeeNode.setTabExam(rs.getLong("tabExam"));
        yeeNode.setSort(rs.getLong("sort"));
        yeeNode.setVideoMode(rs.getLong("videoMode"));
        yeeNode.setLocalFile(rs.getString("localFile"));
        yeeNode.setSchoolId(rs.getLong("schoolId"));
        yeeNode.setLock(rs.getLong("lock"));
        yeeNode.setUnlockTime(rs.getLong("unlockTime"));
        return yeeNode;
    }


    @Override
    public Result selectByCourseId(Integer schoolId, long chapterId) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "SELECT * FROM yee_node WHERE chapterId = ? ORDER BY sort ASC";
            String countSql = "SELECT COUNT(*) FROM yee_node WHERE chapterId = ?";
            
            PreparedStatement countSt = connection.prepareStatement(countSql);
            countSt.setLong(1, chapterId);
            ResultSet countRs = countSt.executeQuery();
            
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, chapterId);
            ResultSet rs = st.executeQuery();

            List<YeeNode> nodes = new ArrayList<>();
            while (rs.next()) {
                YeeNode node = rsToYeeNode(rs);
                nodes.add(node);
            }

            countRs.close();
            countSt.close();
            rs.close();
            st.close();
            connection.close();
            
            return Result.success(nodes, (long) totalCount);
            
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result add(YeeNode node) {
        Connection connection = null;
        PreparedStatement st = null;

        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) node.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            connection.setAutoCommit(false); // 开启事务

            // === 第一步：插入 yee_node ===
            StringBuilder columns = new StringBuilder("INSERT INTO yee_node (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();

            // 必填字段
            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(node.getSchoolId());

            columns.append("`courseId`, ");
            values.append("?, ");
            parameters.add(node.getCourseId());

            // 可选字段（略，与原逻辑一致）
            if (node.getName() != null && !node.getName().trim().isEmpty()) {
                columns.append("`name`, ");
                values.append("?, ");
                parameters.add(node.getName());
            }
            if (node.getType() != null && !node.getType().trim().isEmpty()) {
                columns.append("`type`, ");
                values.append("?, ");
                parameters.add(node.getType());
            }
            if (node.getChapterId() > 0) {
                columns.append("`chapterId`, ");
                values.append("?, ");
                parameters.add(node.getChapterId());
            }
            if (node.getVideoFile() != null && !node.getVideoFile().trim().isEmpty()) {
                columns.append("`videoFile`, ");
                values.append("?, ");
                parameters.add(node.getVideoFile());
            }
            if (node.getVideoDuration() >= 0) {
                columns.append("`videoDuration`, ");
                values.append("?, ");
                parameters.add(node.getVideoDuration());
            }
            if (node.getVotingPath() != null && !node.getVotingPath().trim().isEmpty()) {
                columns.append("`votingPath`, ");
                values.append("?, ");
                parameters.add(node.getVotingPath());
            }
            if (node.getTabVideo() >= 0) {
                columns.append("`tabVideo`, ");
                values.append("?, ");
                parameters.add(node.getTabVideo());
            }
            if (node.getTabFile() >= 0) {
                columns.append("`tabFile`, ");
                values.append("?, ");
                parameters.add(node.getTabFile());
            }
            if (node.getTabVote() >= 0) {
                columns.append("`tabVote`, ");
                values.append("?, ");
                parameters.add(node.getTabVote());
            }
            if (node.getTabWork() >= 0) {
                columns.append("`tabWork`, ");
                values.append("?, ");
                parameters.add(node.getTabWork());
            }
            if (node.getTabExam() >= 0) {
                columns.append("`tabExam`, ");
                values.append("?, ");
                parameters.add(node.getTabExam());
            }
            if (node.getSort() >= 0) {
                columns.append("`sort`, ");
                values.append("?, ");
                parameters.add(node.getSort());
            }
            if (node.getVideoMode() >= 0) {
                columns.append("`videoMode`, ");
                values.append("?, ");
                parameters.add(node.getVideoMode());
            }
            if (node.getLocalFile() != null && !node.getLocalFile().trim().isEmpty()) {
                columns.append("`localFile`, ");
                values.append("?, ");
                parameters.add(node.getLocalFile());
            }
            if (node.getLock() >= 0) {
                columns.append("`lock`, ");
                values.append("?, ");
                parameters.add(node.getLock());
            }
            if (node.getUnlockTime() >= 0) {
                columns.append("`unlockTime`, ");
                values.append("?, ");
                parameters.add(node.getUnlockTime());
            }

            // 移除末尾逗号
            columns.delete(columns.length() - 2, columns.length());
            values.delete(values.length() - 2, values.length());
            String sql = columns.toString() + ") " + values.toString() + ")";

            st = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof String) {
                    st.setString(i + 1, (String) param);
                } else if (param instanceof Long) {
                    st.setLong(i + 1, (Long) param);
                } else if (param instanceof Integer) {
                    st.setInt(i + 1, (Integer) param);
                }
            }

            int rowsInserted = st.executeUpdate();
            if (rowsInserted <= 0) {
                connection.rollback();
                return Result.error("添加失败");
            }

            // 获取生成的ID
            ResultSet generatedKeys = st.getGeneratedKeys();
            if (generatedKeys.next()) {
                node.setId(generatedKeys.getLong(1));
            }
            generatedKeys.close();
            st.close();

            // === 第二步：更新 yee_course_student 的 count 字段 ===
            StringBuilder updateSql = new StringBuilder("UPDATE yee_course_student SET ");
            List<Object> updateParams = new ArrayList<>();
            boolean hasUpdate = false;

            if (node.getTabVideo() == 1) {
                updateSql.append("videoCount = videoCount + 1");
                hasUpdate = true;
            }
//            if (node.getTabWork() == 1) {
//                if (hasUpdate) updateSql.append(", ");
//                updateSql.append("workCount = workCount + 1");
//                hasUpdate = true;
//            }
//            if (node.getTabExam() == 1) {
//                if (hasUpdate) updateSql.append(", ");
//                updateSql.append("examCount = examCount + 1");
//                hasUpdate = true;
//            }

            if (hasUpdate) {
                updateSql.append(" WHERE schoolId = ? AND courseId = ?");
                updateParams.add(node.getSchoolId());
                updateParams.add(node.getCourseId());

                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql.toString())) {
                    for (int i = 0; i < updateParams.size(); i++) {
                        updateStmt.setObject(i + 1, updateParams.get(i));
                    }
                    updateStmt.executeUpdate();
                }
            }

            // 提交事务
            connection.commit();
            return Result.success("添加成功");

        } catch (Exception e) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {}
            }
            e.printStackTrace();
            return Result.error("添加失败：" + e.getMessage());
        } finally {
            try {
                if (st != null) st.close();
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public Result update(YeeNode node) {
        if (node == null || node.getId() <= 0) {
            return Result.error("节点ID无效");
        }

        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) node.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 如果本次更新涉及 tabVideo，先查询旧值用于后续同步 videoCount
            long oldTabVideo = -1;
            long courseIdForSync = -1;
            if (node.getTabVideo() > -1) {
                try (PreparedStatement qs = connection.prepareStatement(
                        "SELECT tabVideo, courseId FROM yee_node WHERE id = ?")) {
                    qs.setLong(1, node.getId());
                    try (ResultSet rs = qs.executeQuery()) {
                        if (rs.next()) {
                            oldTabVideo = rs.getLong("tabVideo");
                            courseIdForSync = rs.getLong("courseId");
                        }
                    }
                }
            }

            StringBuilder sql = new StringBuilder("UPDATE yee_node SET ");
            List<Object> parameters = new ArrayList<>();

            // 字符串字段：非空才更新
            if (node.getName() != null && !node.getName().trim().isEmpty()) {
                sql.append("`name` = ?, ");
                parameters.add(node.getName());
            }
            if (node.getType() != null && !node.getType().trim().isEmpty()) {
                sql.append("`type` = ?, ");
                parameters.add(node.getType());
            }
            if (node.getVideoFile() != null && !node.getVideoFile().trim().isEmpty()) {
                sql.append("`videoFile` = ?, ");
                parameters.add(node.getVideoFile());
            }
            if (node.getVotingPath() != null && !node.getVotingPath().trim().isEmpty()) {
                sql.append("`votingPath` = ?, ");
                parameters.add(node.getVotingPath());
            }
            if (node.getLocalFile() != null && !node.getLocalFile().trim().isEmpty()) {
                sql.append("`localFile` = ?, ");
                parameters.add(node.getLocalFile());
            }

            // 数值字段：> -1 才更新（约定 -1 表示不更新）
            if (node.getChapterId() > -1) {
                sql.append("`chapterId` = ?, ");
                parameters.add(node.getChapterId());
            }
            if (node.getCourseId() > -1) {
                sql.append("`courseId` = ?, ");
                parameters.add(node.getCourseId());
            }
            if (node.getVideoDuration() > -1) {
                sql.append("`videoDuration` = ?, ");
                parameters.add(node.getVideoDuration());
            }
            if (node.getTabVideo() > -1) {
                sql.append("`tabVideo` = ?, ");
                parameters.add(node.getTabVideo());
            }
            if (node.getTabFile() > -1) {
                sql.append("`tabFile` = ?, ");
                parameters.add(node.getTabFile());
            }
            if (node.getTabVote() > -1) {
                sql.append("`tabVote` = ?, ");
                parameters.add(node.getTabVote());
            }
            if (node.getTabWork() > -1) {
                sql.append("`tabWork` = ?, ");
                parameters.add(node.getTabWork());
            }
            if (node.getTabExam() > -1) {
                sql.append("`tabExam` = ?, ");
                parameters.add(node.getTabExam());
            }
            if (node.getSort() > -1) {
                sql.append("`sort` = ?, ");
                parameters.add(node.getSort());
            }
            if (node.getVideoMode() > -1) {
                sql.append("`videoMode` = ?, ");
                parameters.add(node.getVideoMode());
            }
            if (node.getLock() > -1) {
                sql.append("`lock` = ?, ");
                parameters.add(node.getLock());
            }
            if (node.getUnlockTime() > -1) {
                sql.append("`unlockTime` = ?, ");
                parameters.add(node.getUnlockTime());
            }

            if (parameters.isEmpty()) {
                connection.close();
                return Result.error("没有可更新的字段");
            }

            // 移除最后的 ", "
            sql.setLength(sql.length() - 2);
            sql.append(" WHERE id = ?");
            parameters.add(node.getId());

            connection.setAutoCommit(false);
            try {
                PreparedStatement st = connection.prepareStatement(sql.toString());
                for (int i = 0; i < parameters.size(); i++) {
                    Object param = parameters.get(i);
                    if (param instanceof String) {
                        st.setString(i + 1, (String) param);
                    } else if (param instanceof Long) {
                        st.setLong(i + 1, (Long) param);
                    } else if (param instanceof Integer) {
                        st.setInt(i + 1, (Integer) param);
                    }
                }

                int rowsUpdated = st.executeUpdate();
                st.close();

                // 同步 yee_course_student.videoCount：tabVideo 0↔1 切换时修正计数
                if (rowsUpdated > 0 && oldTabVideo != -1 && oldTabVideo != node.getTabVideo()) {
                    String syncSql = null;
                    if (oldTabVideo == 0 && node.getTabVideo() == 1) {
                        syncSql = "UPDATE yee_course_student SET videoCount = videoCount + 1 WHERE schoolId = ? AND courseId = ?";
                    } else if (oldTabVideo == 1 && node.getTabVideo() == 0) {
                        syncSql = "UPDATE yee_course_student SET videoCount = GREATEST(videoCount - 1, 0) WHERE schoolId = ? AND courseId = ?";
                    }
                    if (syncSql != null) {
                        try (PreparedStatement syncStmt = connection.prepareStatement(syncSql)) {
                            syncStmt.setLong(1, node.getSchoolId());
                            syncStmt.setLong(2, courseIdForSync);
                            syncStmt.executeUpdate();
                        }
                    }
                }

                connection.commit();
                return rowsUpdated > 0 ? Result.success("更新成功") : Result.error("未找到匹配记录");
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
                connection.close();
            }

        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    @Override
    public Result delete(Integer schoolId, long id) {
        Connection connection = null;
        PreparedStatement nodeInfoStmt = null;
        PreparedStatement examCheckStmt = null;
        PreparedStatement workCheckStmt = null;
        PreparedStatement updateCountStmt = null;
        PreparedStatement deleteNodeStmt = null;

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            connection.setAutoCommit(false); // 开启事务

            // 1. 先获取节点信息：courseId 和 tab 类型
            String nodeInfoSql = "SELECT courseId, tabVideo, tabWork, tabExam FROM yee_node WHERE id = ?";
            nodeInfoStmt = connection.prepareStatement(nodeInfoSql);
            nodeInfoStmt.setLong(1, id);
            Long courseId = null;
            int tabVideo = 0, tabWork = 0, tabExam = 0;
            boolean nodeExists = false;

            try (ResultSet rs = nodeInfoStmt.executeQuery()) {
                if (rs.next()) {
                    nodeExists = true;
                    courseId = rs.getLong("courseId");
                    tabVideo = rs.getInt("tabVideo");
                    tabWork = rs.getInt("tabWork");
                    tabExam = rs.getInt("tabExam");
                }
            }

            if (!nodeExists) {
                connection.rollback();
                return Result.error("删除失败：节点不存在");
            }

            // 2. 检查是否关联考试或作业（防止删除有内容的节点）
            String checkExamSql = "SELECT COUNT(1) FROM yee_exam WHERE nodeId = ?";
            examCheckStmt = connection.prepareStatement(checkExamSql);
            examCheckStmt.setLong(1, id);
            long examCount = 0;
            try (ResultSet rs = examCheckStmt.executeQuery()) {
                if (rs.next()) examCount = rs.getLong(1);
            }

            String checkWorkSql = "SELECT COUNT(1) FROM yee_work WHERE nodeId = ?";
            workCheckStmt = connection.prepareStatement(checkWorkSql);
            workCheckStmt.setLong(1, id);
            long workCount = 0;
            try (ResultSet rs = workCheckStmt.executeQuery()) {
                if (rs.next()) workCount = rs.getLong(1);
            }

            if (examCount > 0 || workCount > 0) {
                StringBuilder msg = new StringBuilder("该节点下存在");
                if (examCount > 0) msg.append(" ").append(examCount).append(" 个考试");
                if (workCount > 0) {
                    if (examCount > 0) msg.append(" 和");
                    msg.append(" ").append(workCount).append(" 个作业");
                }
                msg.append("，无法删除");
                connection.rollback();
                return Result.error(msg.toString());
            }

            // 3. 如果是视频/作业/考试节点，则更新 yee_course_student 的对应计数（-1）
            if (tabVideo == 1) {
//                if (tabVideo == 1 || tabWork == 1 || tabExam == 1) {
                StringBuilder updateSql = new StringBuilder("UPDATE yee_course_student SET ");
                List<Object> params = new ArrayList<>();

                boolean hasUpdate = false;
                if (tabVideo == 1) {
                    updateSql.append("videoCount = GREATEST(videoCount - 1, 0)");
                    hasUpdate = true;
                }
//                if (tabWork == 1) {
//                    if (hasUpdate) updateSql.append(", ");
//                    updateSql.append("workCount = GREATEST(workCount - 1, 0)");
//                    hasUpdate = true;
//                }
//                if (tabExam == 1) {
//                    if (hasUpdate) updateSql.append(", ");
//                    updateSql.append("examCount = GREATEST(examCount - 1, 0)");
//                    hasUpdate = true;
//                }

                if (hasUpdate) {
                    updateSql.append(" WHERE courseId = ?");
                    params.add(courseId);

                    updateCountStmt = connection.prepareStatement(updateSql.toString());
                    for (int i = 0; i < params.size(); i++) {
                        updateCountStmt.setObject(i + 1, params.get(i));
                    }
                    updateCountStmt.executeUpdate();
                }
            }

            // 4. 删除节点
            String deleteSql = "DELETE FROM yee_node WHERE id = ?";
            deleteNodeStmt = connection.prepareStatement(deleteSql);
            deleteNodeStmt.setLong(1, id);
            int rowsDeleted = deleteNodeStmt.executeUpdate();

            if (rowsDeleted > 0) {
                connection.commit();
                return Result.success("删除成功");
            } else {
                connection.rollback();
                return Result.error("删除失败：节点已被其他操作删除");
            }

        } catch (Exception e) {
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ignored) {}
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        } finally {
            try {
                if (nodeInfoStmt != null) nodeInfoStmt.close();
                if (examCheckStmt != null) examCheckStmt.close();
                if (workCheckStmt != null) workCheckStmt.close();
                if (updateCountStmt != null) updateCountStmt.close();
                if (deleteNodeStmt != null) deleteNodeStmt.close();
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException ignored) {}
        }
    }


    /**
     * 学生端获取课程章节树（自动判断作业/考试可见范围：全部班级 / 指定班级）
     */
    public Result getCourseChapterTreeForStudent(int schoolId, long courseId, int studentId) {

        // 1. 参数校验
        if (schoolId <= 0 || courseId <= 0 || studentId <= 0) {
            return Result.error("参数无效");
        }

        // 2. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        Connection conn = null;
        try {
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // ====================== 自动查询：学生在这门课里的所有班级 ID ======================
            Set<Integer> studentClassIds = getStudentClassIds(conn, courseId, studentId);
            if (studentClassIds.isEmpty()) {
                // 学生未选课/无班级 → 返回空树
                return Result.success(Collections.emptyList());
            }

            // ====================== 3. 查询章节 ======================
            List<ChapterTreeNodeVo> chapterList = new ArrayList<>();
            String chapterSql = """
            SELECT id, name, courseId, sort
            FROM yee_chapter
            WHERE courseId = ? AND schoolId = ?
            ORDER BY sort ASC, id ASC
        """;
            try (PreparedStatement ps = conn.prepareStatement(chapterSql)) {
                ps.setLong(1, courseId);
                ps.setInt(2, schoolId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        ChapterTreeNodeVo dto = new ChapterTreeNodeVo();
                        dto.setId(rs.getLong("id"));
                        dto.setName(rs.getString("name"));
                        dto.setSort(rs.getLong("sort"));
                        dto.setChildren(new ArrayList<>());
                        chapterList.add(dto);
                    }
                }
            }

            if (chapterList.isEmpty()) {
                return Result.success(Collections.emptyList());
            }

            // ====================== 4. 查询所有节点 ======================
            List<Long> chapterIds = chapterList.stream()
                    .map(ChapterTreeNodeVo::getId)
                    .collect(Collectors.toList());

            String placeholders = String.join(",", Collections.nCopies(chapterIds.size(), "?"));
            String nodeSql = String.format("""
                        SELECT 
                            id, name, type, chapterId, courseId, sort,
                            tabVideo, tabFile, tabVote, tabWork, tabExam,
                            videoFile, videoDuration, localFile, `lock`, unlockTime
                        FROM yee_node
                        WHERE chapterId IN (%s) AND schoolId = ?
                        ORDER BY chapterId, sort ASC, id ASC
                    """, placeholders);

            List<YeeNode> allNodes = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(nodeSql)) {
                int idx = 1;
                for (Long cid : chapterIds) ps.setLong(idx++, cid);
                ps.setInt(idx, schoolId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        YeeNode node = new YeeNode();
                        node.setId(rs.getLong("id"));
                        node.setName(rs.getString("name"));
                        node.setChapterId(rs.getLong("chapterId"));
                        node.setTabVideo(rs.getLong("tabVideo"));
                        node.setTabFile(rs.getLong("tabFile"));
                        node.setTabVote(rs.getLong("tabVote"));
                        node.setTabWork(rs.getLong("tabWork"));
                        node.setTabExam(rs.getLong("tabExam"));
                        node.setSort(rs.getLong("sort"));
                        node.setLocalFile(rs.getString("localFile"));
                        node.setVideoFile(rs.getString("videoFile"));
                        node.setVideoDuration(rs.getLong("videoDuration"));
                        node.setLock(rs.getLong("lock"));
                        node.setUnlockTime(rs.getLong("unlockTime"));
                        allNodes.add(node);
                    }
                }
            }

            if (allNodes.isEmpty()) {
                return Result.success(chapterList);
            }

            // ====================== 5. 核心：查询学生能看见的作业/考试节点 ======================
            Set<Long> canSeeWorkNodes = getStudentVisibleWorkNodes(conn, courseId, schoolId, studentClassIds);
            Set<Long> canSeeExamNodes = getStudentVisibleExamNodes(conn, courseId, schoolId, studentClassIds);

            // ====================== 6. 组装树 ======================
            Map<Long, List<YeeNode>> nodeGroup = allNodes.stream()
                    .collect(Collectors.groupingBy(YeeNode::getChapterId));

            for (ChapterTreeNodeVo chapter : chapterList) {
                List<YeeNode> nodeList = nodeGroup.getOrDefault(chapter.getId(), Collections.emptyList());
                List<CourseTreeNodeVo> children = new ArrayList<>();

                for (YeeNode node : nodeList) {
                    long nodeId = node.getId();
                    boolean isWorkNode = node.getTabWork() == 1;
                    boolean isExamNode = node.getTabExam() == 1;
                    boolean show = true;

                    if (isWorkNode) show = canSeeWorkNodes.contains(nodeId);
                    if (isExamNode) show = canSeeExamNodes.contains(nodeId);
                    if (!show) continue;

                    CourseTreeNodeVo vo = new CourseTreeNodeVo();
                    vo.setId(node.getId());
                    vo.setName(node.getName());
                    vo.setSort(node.getSort());
                    vo.setTabVideo(node.getTabVideo());
                    vo.setTabFile(node.getTabFile());
                    vo.setTabVote(node.getTabVote());
                    vo.setTabWork(node.getTabWork());
                    vo.setTabExam(node.getTabExam());
                    vo.setLocalFile(node.getLocalFile());
                    vo.setVideoFile(node.getVideoFile());
                    vo.setVideoDuration(node.getVideoDuration());
                    vo.setLock(node.getLock());
                    vo.setUnlockTime(node.getUnlockTime());
                    children.add(vo);
                }

                children.sort(Comparator.comparing(CourseTreeNodeVo::getSort));
                chapter.setChildren(children);
            }

            return Result.success(chapterList);

        } catch (SQLException e) {
            return Result.error("系统繁忙，请稍后重试");
        } catch (Exception e) {
            return Result.error("系统错误，请稍后重试");
        } finally {
            if (conn != null) {
                try { conn.close(); } catch (SQLException ignored) {}
            }
        }
    }
    /**
     * 1. 查询学生在这门课里的所有班级ID
     */
    private Set<Integer> getStudentClassIds(Connection conn, long courseId, int studentId) throws SQLException {
        Set<Integer> classIds = new HashSet<>();
        String sql = """
        SELECT DISTINCT classId
        FROM yee_course_student
        WHERE courseId = ? AND studentId = ? AND classId > 0
    """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            ps.setInt(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    classIds.add(rs.getInt("classId"));
                }
            }
        }
        return classIds;
    }

    /**
     * 2. 查询学生能看见的作业节点（满足任一班级即可）
     */
    private Set<Long> getStudentVisibleWorkNodes(Connection conn, long courseId, int schoolId, Set<Integer> classIds) throws SQLException {
        Set<Long> nodeIds = new HashSet<>();
        String classPlaceholders = String.join(",", Collections.nCopies(classIds.size(), "?"));
        String sql = """
        SELECT DISTINCT nodeId
        FROM yee_work
        WHERE courseId = ? AND schoolId = ? AND allow = 1
        AND (
            classList IS NULL OR classList = '' OR JSON_LENGTH(classList) = 0
            OR JSON_CONTAINS(classList, ?, '$')
        )
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            ps.setInt(2, schoolId);
            for (Integer cid : classIds) {
                ps.setString(3, String.valueOf(cid));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    nodeIds.add(rs.getLong("nodeId"));
                }
            }
        }
        return nodeIds;
    }

    /**
     * 3. 查询学生能看见的考试节点
     */
    private Set<Long> getStudentVisibleExamNodes(Connection conn, long courseId, int schoolId, Set<Integer> classIds) throws SQLException {
        Set<Long> nodeIds = new HashSet<>();
        String classPlaceholders = String.join(",", Collections.nCopies(classIds.size(), "?"));
        String sql = """
        SELECT DISTINCT nodeId
        FROM yee_exam
        WHERE courseId = ? AND schoolId = ? AND allow = 1
        AND (
            classList IS NULL OR classList = '' OR JSON_LENGTH(classList) = 0
            OR JSON_CONTAINS(classList, ?, '$')
        )
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            ps.setInt(2, schoolId);
            for (Integer cid : classIds) {
                ps.setString(3, String.valueOf(cid));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    nodeIds.add(rs.getLong("nodeId"));
                }
            }
        }
        return nodeIds;
    }
}
