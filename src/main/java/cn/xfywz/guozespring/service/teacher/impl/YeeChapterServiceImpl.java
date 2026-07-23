package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeChapter;
import cn.xfywz.guozespring.entity.mhsch.YeeNode;
import cn.xfywz.guozespring.entity.vo.ChapterTreeNodeVo;
import cn.xfywz.guozespring.entity.vo.CourseTreeNodeVo;
import cn.xfywz.guozespring.entity.vo.CourseTreeVo;
import cn.xfywz.guozespring.service.admin.SlSchoolService;
import cn.xfywz.guozespring.service.teacher.YeeChapterService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class YeeChapterServiceImpl implements YeeChapterService {
    @Autowired
    private SlSchoolService slSchoolService;

    private YeeChapter rsToYeeChapter(ResultSet rs) throws SQLException {
        YeeChapter yeeChapter = new YeeChapter();
        yeeChapter.setId(rs.getLong("id"));
        yeeChapter.setName(rs.getString("name"));
        yeeChapter.setCourseId(rs.getLong("courseId"));
        yeeChapter.setSort(rs.getLong("sort"));
        yeeChapter.setSchoolId(rs.getLong("schoolId"));
        return yeeChapter;
    }

    @Override
    public Result selectCourse(YeeChapter yeeChapter) throws Exception {
        SlSchool slSchool = slSchoolService.selectById((int) yeeChapter.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        String sql = "SELECT * FROM yee_chapter WHERE courseId = ? ORDER BY sort ASC";
        PreparedStatement st = connection.prepareStatement(sql);
        st.setLong(1, yeeChapter.getCourseId());
        ResultSet rs = st.executeQuery();
        
        List<YeeChapter> chapters = new ArrayList<>();
        while (rs.next()) {
            YeeChapter chapter = rsToYeeChapter(rs);
            chapters.add(chapter);
        }
        rs.close();
        st.close();
        connection.close();
        return Result.success(chapters);
    }

    @Override
    public Result add(YeeChapter yeeChapter) {
        try {
            SlSchool slSchool = slSchoolService.selectById((int) yeeChapter.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder columns = new StringBuilder("INSERT INTO yee_chapter (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();
            
            // 必填字段：schoolId, courseId
            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(yeeChapter.getSchoolId());
            
            columns.append("`courseId`, ");
            values.append("?, ");
            parameters.add(yeeChapter.getCourseId());
            
            // 动态添加可选字段
            if (yeeChapter.getName() != null && !yeeChapter.getName().trim().isEmpty()) {
                columns.append("`name`, ");
                values.append("?, ");
                parameters.add(yeeChapter.getName());
            }
            
            if (yeeChapter.getSort() >= 0) {
                columns.append("`sort`, ");
                values.append("?, ");
                parameters.add(yeeChapter.getSort());
            }
            
            // 删除最后的逗号和空格
            columns.delete(columns.length() - 2, columns.length());
            values.delete(values.length() - 2, values.length());
            
            // 构建完整SQL
            columns.append(") ");
            values.append(")");
            String sql = columns.toString() + values.toString();
            
            PreparedStatement st = connection.prepareStatement(sql);
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
            st.close();
            connection.close();
            
            if (rowsInserted > 0) {
                return Result.success("添加成功");
            } else {
                return Result.error("添加失败");
            }
            
        } catch (Exception e) {
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    @Override
    public Result update(YeeChapter yeeChapter) {
        try {
            SlSchool slSchool = slSchoolService.selectById((int) yeeChapter.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder sql = new StringBuilder("UPDATE yee_chapter SET ");
            List<Object> parameters = new ArrayList<>();
            
            // 动态添加更新字段
            if (yeeChapter.getName() != null && !yeeChapter.getName().trim().isEmpty()) {
                sql.append("`name` = ?, ");
                parameters.add(yeeChapter.getName());
            }
            
            if (yeeChapter.getCourseId() > 0) {
                sql.append("`courseId` = ?, ");
                parameters.add(yeeChapter.getCourseId());
            }
            
            if (yeeChapter.getSort() >= 0) {
                sql.append("`sort` = ?, ");
                parameters.add(yeeChapter.getSort());
            }
            
            // 检查是否有可更新的字段
            if (parameters.isEmpty()) {
                connection.close();
                return Result.error("没有可更新的字段");
            }
            
            // 删除最后的逗号和空格
            sql.delete(sql.length() - 2, sql.length());
            
            // 添加WHERE条件
            sql.append(" WHERE id = ?");
            parameters.add(yeeChapter.getId());
            
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
    public Result delete(YeeChapter yeeChapter) {
        Connection connection = null;
        PreparedStatement checkStmt = null;
        PreparedStatement deleteStmt = null;

        try {
            SlSchool slSchool = slSchoolService.selectById((int) yeeChapter.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            connection.setAutoCommit(true);

            // 1. 检查该章节下是否存在节点
            String checkSql = "SELECT COUNT(1) FROM yee_node WHERE chapterId = ?";
            checkStmt = connection.prepareStatement(checkSql);
            checkStmt.setLong(1, yeeChapter.getId());

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    long nodeCount = rs.getLong(1);
                    if (nodeCount > 0) {
                        return Result.error("该章节下存在 " + nodeCount + " 个节点，无法删除");
                    }
                }
            }

            // 2. 若无节点，则删除章节
            String deleteSql = "DELETE FROM yee_chapter WHERE id = ?";
            deleteStmt = connection.prepareStatement(deleteSql);
            deleteStmt.setLong(1, yeeChapter.getId());

            int rowsDeleted = deleteStmt.executeUpdate();

            if (rowsDeleted > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败：未找到匹配的章节记录");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        } finally {
            // 安全关闭资源
            try {
                if (checkStmt != null) checkStmt.close();
                if (deleteStmt != null) deleteStmt.close();
                if (connection != null) connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * 获取课程的完整章节树（含节点与考试）
     *
     * @param schoolId  学校ID
     * @param courseId  课程ID
     * @return Result 包含树形结构
     */
    public Result getCourseChapterTreeWithExams(int schoolId, long courseId) {
        // 1. 参数校验
        if (schoolId <= 0 || courseId <= 0) {
            return Result.error("参数无效");
        }

        // 2. 校验学校是否存在且已审核
        SlSchool slSchool = slSchoolService.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        Connection conn = null;
        try {
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 3. 查询课程信息（同时校验课程是否存在）
            CourseTreeVo courseInfo = new CourseTreeVo();
            // ✅ 改造SQL：LEFT JOIN yee_manage 关联创建人姓名
            String courseSql = """
            SELECT 
                c.id, c.name, c.mode, c.collegeId, c.categoryId, c.lecturers,
                c.startDate, c.endDate, c.cover, c.content, c.credit, c.allow,
                c.intro, c.teacherIntro, c.code, c.stuCount, c.proclamation,
                c.clusterId, c.periodName, c.addTime, c.createId, c.schoolId,
                c.cateBid, c.cateMid, c.signStartTime, c.signEndTime, c.signScope,
                c.signClass, c.lecturerName, c.offline, c.mission, c.signLimit, c.lineLock, c.tplId,
                m.name AS createName ,c.isPractice
            FROM yee_course c
            LEFT JOIN yee_manage m ON c.createId = m.id
            WHERE c.id = ? AND c.schoolId = ?
            """;

            try (PreparedStatement ps = conn.prepareStatement(courseSql)) {
                ps.setLong(1, courseId);
                ps.setInt(2, schoolId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("课程不存在");
                    }

                    // 手动映射字段
                    courseInfo.setCourseId(rs.getLong("id"));
                    courseInfo.setCourseName(rs.getString("name"));
                    courseInfo.setMode(rs.getInt("mode"));
                    courseInfo.setCollegeId(rs.getInt("collegeId"));
                    courseInfo.setCategoryId(rs.getString("categoryId"));
                    courseInfo.setLecturers(rs.getString("lecturers"));
                    courseInfo.setStartDate(rs.getDate("startDate"));
                    courseInfo.setEndDate(rs.getDate("endDate"));
                    courseInfo.setCover(rs.getString("cover"));
                    courseInfo.setContent(rs.getString("content"));

                    BigDecimal credit = rs.getBigDecimal("credit");
                    if (credit != null) {
                        try {
                            courseInfo.setCredit(credit.setScale(2, RoundingMode.UNNECESSARY));
                        } catch (ArithmeticException e) {
                            courseInfo.setCredit(credit.setScale(2, RoundingMode.HALF_UP));
                        }
                    } else {
                        courseInfo.setCredit(null);
                    }

                    courseInfo.setAllow(rs.getInt("allow"));
                    courseInfo.setIntro(rs.getString("intro"));
                    courseInfo.setTeacherIntro(rs.getString("teacherIntro"));
                    courseInfo.setCode(rs.getString("code"));
                    courseInfo.setStuCount(rs.getInt("stuCount"));
                    courseInfo.setProclamation(rs.getString("proclamation"));
                    courseInfo.setClusterId(rs.getInt("clusterId"));
                    courseInfo.setPeriodName(rs.getString("periodName"));
                    courseInfo.setAddTime(rs.getTimestamp("addTime"));
                    courseInfo.setCreateId(rs.getInt("createId"));
                    courseInfo.setSchoolId(rs.getInt("schoolId"));
                    courseInfo.setCateBid(rs.getInt("cateBid"));
                    courseInfo.setCateMid(rs.getInt("cateMid"));
                    courseInfo.setSignStartTime(rs.getTimestamp("signStartTime"));
                    courseInfo.setSignEndTime(rs.getTimestamp("signEndTime"));
                    courseInfo.setSignScope(rs.getInt("signScope"));
                    courseInfo.setSignClass(rs.getString("signClass")); // 原始 JSON
                    courseInfo.setLecturerName(rs.getString("lecturerName"));
                    courseInfo.setOffline(rs.getInt("offline"));
                    courseInfo.setMission(rs.getInt("mission"));
                    courseInfo.setSignLimit(rs.getInt("signLimit"));
                    courseInfo.setLineLock(rs.getInt("lineLock"));
                    courseInfo.setTplId(rs.getInt("tplId"));

                    // ✅ 新增：映射创建人姓名（处理null值）
                    courseInfo.setCreateName(rs.getString("createName"));
                    courseInfo.setIsPractice(rs.getInt("isPractice"));
                }
            }

            // 4. 查询章节列表
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

            // 若无章节，直接返回
            if (chapterList.isEmpty()) {
                courseInfo.setChapterList(Collections.emptyList());
                courseInfo.setTotalVideoDuration(0L);
                courseInfo.setTotalVideoDurationText("0秒");
                return Result.success(courseInfo);
            }

            // 5. 查询所有“节”（nodes）—— 补全所有字段
            List<Long> chapterIds = chapterList.stream().map(ChapterTreeNodeVo::getId).collect(Collectors.toList());

            // 防御：理论上不会为空（前面已判断），但保险起见
            if (chapterIds.isEmpty()) {
                courseInfo.setChapterList(chapterList);
                courseInfo.setTotalVideoDuration(0L);
                courseInfo.setTotalVideoDurationText("0秒");
                return Result.success(courseInfo);
            }

            String placeholders = String.join(",", Collections.nCopies(chapterIds.size(), "?"));
            String nodeSql = String.format("""
            SELECT 
                id, name, type, chapterId, courseId, sort,
                tabVideo, tabFile, tabVote, tabWork, tabExam,
                videoFile, videoDuration, votingPath, videoMode,
                localFile, schoolId, `lock`, unlockTime
            FROM yee_node
            WHERE chapterId IN (%s) AND schoolId = ?
            ORDER BY chapterId, sort ASC, id ASC
            """, placeholders);
            List<YeeNode> nodes = new ArrayList<>();
            // ✅ 总视频时长（秒）
            long totalVideoDuration = 0L;

            try (PreparedStatement ps = conn.prepareStatement(nodeSql)) {
                int index = 1;
                for (Long id : chapterIds) {
                    ps.setLong(index++, id);
                }
                ps.setInt(index, schoolId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        YeeNode node = new YeeNode();
                        node.setId(rs.getLong("id"));
                        node.setName(rs.getString("name"));
                        node.setType(rs.getString("type"));
                        node.setChapterId(rs.getLong("chapterId"));
                        node.setCourseId(rs.getLong("courseId"));
                        node.setSort(rs.getLong("sort"));

                        // Tabs
                        node.setTabVideo(rs.getLong("tabVideo"));
                        node.setTabFile(rs.getLong("tabFile"));
                        node.setTabVote(rs.getLong("tabVote"));
                        node.setTabWork(rs.getLong("tabWork"));
                        node.setTabExam(rs.getLong("tabExam"));

                        // 视频 & 文件
                        node.setVideoFile(rs.getString("videoFile"));
                        long duration = rs.getLong("videoDuration");
                        node.setVideoDuration(duration);
                        // 累加
                        totalVideoDuration += duration;

                        node.setVideoMode(rs.getLong("videoMode"));
                        node.setLocalFile(rs.getString("localFile"));

                        // 投票
                        node.setVotingPath(rs.getString("votingPath"));

                        // 锁 & 学校
                        node.setSchoolId(rs.getLong("schoolId"));
                        node.setLock(rs.getLong("lock"));
                        node.setUnlockTime(rs.getLong("unlockTime"));

                        nodes.add(node);
                    }
                }
            }

            // 6. 按章节分组并组装树形结构（含完整节点信息）
            Map<Long, List<YeeNode>> nodeGroup = nodes.stream()
                    .collect(Collectors.groupingBy(YeeNode::getChapterId));

            for (ChapterTreeNodeVo chapter : chapterList) {
                List<YeeNode> nodeList = nodeGroup.getOrDefault(chapter.getId(), Collections.emptyList());
                List<CourseTreeNodeVo> children = new ArrayList<>();
                for (YeeNode node : nodeList) {
                    CourseTreeNodeVo child = new CourseTreeNodeVo();
                    child.setId(node.getId());
                    child.setName(node.getName());
                    child.setSort(node.getSort());

                    // Tabs
                    child.setTabVideo(node.getTabVideo());
                    child.setTabFile(node.getTabFile());
                    child.setTabVote(node.getTabVote());
                    child.setTabWork(node.getTabWork());
                    child.setTabExam(node.getTabExam());

                    // 完整节点信息（新增）
                    child.setType(node.getType());
                    child.setVideoFile(node.getVideoFile());
                    child.setVideoDuration(node.getVideoDuration());
                    child.setVotingPath(node.getVotingPath());
                    child.setVideoMode(node.getVideoMode());
                    child.setLocalFile(node.getLocalFile());
                    child.setLock(node.getLock());
                    child.setUnlockTime(node.getUnlockTime());

                    children.add(child);
                }
                children.sort(Comparator.comparing(CourseTreeNodeVo::getSort));
                chapter.setChildren(children);
            }

            // ====================== 核心：秒转 时分秒 ======================
            String durationText = formatSecondToHms(totalVideoDuration);

            courseInfo.setTotalVideoDuration(totalVideoDuration);
            courseInfo.setTotalVideoDurationText(durationText);
            courseInfo.setChapterList(chapterList);
            return Result.success(courseInfo);

        } catch (SQLException e) {
            return Result.error("系统繁忙，请稍后重试");
        } catch (Exception e) {
            return Result.error("系统错误，请稍后重试");
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    /**
     * 秒数转为 xx小时xx分xx秒 格式
     */
    private String formatSecondToHms(long totalSec) {
        if (totalSec <= 0) {
            return "0秒";
        }

        long hours = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("小时");
        }
        if (minutes > 0) {
            sb.append(minutes).append("分");
        }
        if (seconds > 0 || sb.length() == 0) {
            sb.append(seconds).append("秒");
        }
        return sb.toString();
    }

}
