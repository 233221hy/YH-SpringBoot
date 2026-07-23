package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeCourseStudent;
import cn.xfywz.guozespring.entity.vo.SelectedCoursesStudents;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeCourseResultsService;
import cn.xfywz.guozespring.service.teacher.YeeCourseStudentService;
import cn.xfywz.guozespring.service.teacher.YeeStudentService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class YeeCourseStudentServiceImpl implements YeeCourseStudentService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;

    // ====== 内部 DTO：学生基本信息 ======
    private static class StudentInfo {
        final String name;
        final String number;
        StudentInfo(String name, String number) {
            this.name = name == null ? "" : name;
            this.number = number == null ? "" : number;
        }
    }

    private static final String INSERT_COURSE_RESULTS_SQL = """
        INSERT INTO yee_course_results (
            schoolId, courseId, userId, classId,
            score, videoScore, examScore, workScore, discussScore, extraScore,
            ranking, stuName, stuNumber, videoResult, examResult, workResult, discussResult,
            calcDate
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    // ====== 批量查询学生信息 ======
    private Map<Long, StudentInfo> queryStudentInfo(Connection conn, List<Long> studentIds) throws SQLException {
        if (studentIds == null || studentIds.isEmpty()) {
            return new HashMap<>();
        }

        String placeholders = studentIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = "SELECT id, name, number FROM yee_student WHERE id IN (" + placeholders + ")";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < studentIds.size(); i++) {
                stmt.setLong(i + 1, studentIds.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            Map<Long, StudentInfo> map = new HashMap<>();
            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");
                String number = rs.getString("number");
                map.put(id, new StudentInfo(name, number));
            }
            rs.close();
            return map;
        }
    }
    @Override
    public Result selectAll(Integer schoolId, long courseId, long classId, int pageNum, int pageSize) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            int offset = (pageNum - 1) * pageSize;
            String sql = "SELECT * FROM yee_course_student WHERE courseId = ? AND classId = ? ORDER BY addTime DESC LIMIT ? OFFSET ?";
            String countSql = "SELECT COUNT(*) FROM yee_course_student WHERE courseId = ? AND classId = ?";
            
            PreparedStatement countSt = connection.prepareStatement(countSql);
            countSt.setLong(1, courseId);
            countSt.setLong(2, classId);
            ResultSet countRs = countSt.executeQuery();
            
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, courseId);
            st.setLong(2, classId);
            st.setInt(3, pageSize);
            st.setInt(4, offset);
            ResultSet rs = st.executeQuery();
            
            List<YeeCourseStudent> courseStudents = new ArrayList<>();
            while (rs.next()) {
                YeeCourseStudent courseStudent = rsToYeeCourseStudent(rs);
                courseStudents.add(courseStudent);
            }
            
            countRs.close();
            countSt.close();
            rs.close();
            st.close();
            connection.close();
            
            return Result.success(courseStudents, (long) totalCount);
            
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result add(YeeCourseStudent courseStudent) {
        Connection connection = null;
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;

        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) courseStudent.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            connection.setAutoCommit(false);

            long studentId = courseStudent.getStudentId();
            long courseId = courseStudent.getCourseId();
            long classId = courseStudent.getClassId();

            // >>>>>>>>>>>>>> 查重逻辑 <<<<<<<<<<<<<<
            String checkSql = "SELECT COUNT(1) FROM yee_course_student WHERE studentId = ? AND courseId = ? AND classId = ?";
            checkStmt = connection.prepareStatement(checkSql);
            checkStmt.setLong(1, studentId);
            checkStmt.setLong(2, courseId);
            checkStmt.setLong(3, classId);

            ResultSet rs = checkStmt.executeQuery();
            boolean exists = rs.next() && rs.getInt(1) > 0;
            rs.close();
            checkStmt.close();

            if (exists) {
                connection.rollback();
                return Result.error("该学生已选此课程，不可重复添加");
            }

            int videoCountFromNode = 0;
            int workCountFromNode = 0;
            int examCountFromNode = 0;

            if (courseId > 0) {
                String nodeCountSql = "SELECT " +
                        "COALESCE(SUM(tabVideo), 0) AS videoCount, " +
                        "COALESCE(SUM(tabWork), 0) AS workCount, " +
                        "COALESCE(SUM(tabExam), 0) AS examCount " +
                        "FROM yee_node WHERE courseId = ?";
                try (PreparedStatement nodeStmt = connection.prepareStatement(nodeCountSql)) {
                    nodeStmt.setLong(1, courseId);
                    ResultSet nodeRs = nodeStmt.executeQuery();
                    if (nodeRs.next()) {
                        videoCountFromNode = nodeRs.getInt("videoCount");
                        workCountFromNode = nodeRs.getInt("workCount");
                        examCountFromNode = nodeRs.getInt("examCount");
                    }
                    nodeRs.close();
                }
            }
            courseStudent.setVideoCount(videoCountFromNode);
            courseStudent.setWorkCount(workCountFromNode);
            courseStudent.setExamCount(examCountFromNode);

            // >>>>>>>>>>>>>> 构建 INSERT SQL <<<<<<<<<<<<<<
            StringBuilder columns = new StringBuilder("INSERT INTO yee_course_student (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();

            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(courseStudent.getSchoolId());

            if (courseStudent.getClassId() > 0) {
                columns.append("`classId`, ");
                values.append("?, ");
                parameters.add(courseStudent.getClassId());
            }

            if (courseStudent.getCourseId() > 0) {
                columns.append("`courseId`, ");
                values.append("?, ");
                parameters.add(courseStudent.getCourseId());
            }

            if (courseStudent.getStudentId() > 0) {
                columns.append("`studentId`, ");
                values.append("?, ");
                parameters.add(courseStudent.getStudentId());
            }

            if (courseStudent.getVideoLearned() >= 0) {
                columns.append("`videoLearned`, ");
                values.append("?, ");
                parameters.add(courseStudent.getVideoLearned());
            }

            columns.append("`videoCount`, ");
            values.append("?, ");
            parameters.add(courseStudent.getVideoCount());

            if (courseStudent.getWorkLearned() >= 0) {
                columns.append("`workLearned`, ");
                values.append("?, ");
                parameters.add(courseStudent.getWorkLearned());
            }

            columns.append("`workCount`, ");
            values.append("?, ");
            parameters.add(courseStudent.getWorkCount());

            if (courseStudent.getExamLearned() >= 0) {
                columns.append("`examLearned`, ");
                values.append("?, ");
                parameters.add(courseStudent.getExamLearned());
            }

            columns.append("`examCount`, ");
            values.append("?, ");
            parameters.add(courseStudent.getExamCount());

            if (courseStudent.getLastNodeId() >= 0) {
                columns.append("`lastNodeId`, ");
                values.append("?, ");
                parameters.add(courseStudent.getLastNodeId());
            }

            if (courseStudent.getDiscussJoin() >= 0) {
                columns.append("`discussJoin`, ");
                values.append("?, ");
                parameters.add(courseStudent.getDiscussJoin());
            }

            if (courseStudent.getDiscussCount() >= 0) {
                columns.append("`discussCount`, ");
                values.append("?, ");
                parameters.add(courseStudent.getDiscussCount());
            }

            if (courseStudent.getStudyTime() >= 0) {
                columns.append("`studyTime`, ");
                values.append("?, ");
                parameters.add(courseStudent.getStudyTime());
            }

            if (courseStudent.getChange() >= 0) {
                columns.append("`change`, ");
                values.append("?, ");
                parameters.add(courseStudent.getChange());
            }

            if (courseStudent.getCalculate() >= 0) {
                columns.append("`calculate`, ");
                values.append("?, ");
                parameters.add(courseStudent.getCalculate());
            }

            columns.append("`addTime`, ");
            values.append("?, ");
            parameters.add(courseStudent.getAddTime() != null ? courseStudent.getAddTime() : new Timestamp(System.currentTimeMillis()));

            columns.delete(columns.length() - 2, columns.length());
            values.delete(values.length() - 2, values.length());
            columns.append(") ");
            values.append(")");
            String sql = columns.toString() + values.toString();

            insertStmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof Long) {
                    insertStmt.setLong(i + 1, (Long) param);
                } else if (param instanceof Integer) {
                    insertStmt.setInt(i + 1, (Integer) param);
                } else if (param instanceof Timestamp) {
                    insertStmt.setTimestamp(i + 1, (Timestamp) param);
                } else if (param instanceof String) {
                    insertStmt.setString(i + 1, (String) param);
                } else {
                    insertStmt.setObject(i + 1, param);
                }
            }

            int rowsInserted = insertStmt.executeUpdate();
            if (rowsInserted > 0) {
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    courseStudent.setId(generatedKeys.getLong(1));
                }
                generatedKeys.close();

                // 更新课程学生数
                if (courseId > 0) {
                    String updateSql = "UPDATE yee_course SET stuCount = stuCount + 1 WHERE id = ?";
                    try (PreparedStatement updateSt = connection.prepareStatement(updateSql)) {
                        updateSt.setLong(1, courseId);
                        updateSt.executeUpdate();
                    }
                }

                // >>>>>>>>>>>>>> 查询学生信息并创建成绩记录 <<<<<<<<<<<<<<
                Map<Long, StudentInfo> studentInfoMap = queryStudentInfo(connection, Collections.singletonList(studentId));
                StudentInfo info = studentInfoMap.getOrDefault(studentId, new StudentInfo("", ""));

                try (PreparedStatement stmt = connection.prepareStatement(INSERT_COURSE_RESULTS_SQL)) {
                    stmt.setLong(1, courseStudent.getSchoolId());
                    stmt.setLong(2, courseId);
                    stmt.setLong(3, studentId);
                    stmt.setLong(4, classId);
                    stmt.setBigDecimal(5, ZERO);
                    stmt.setBigDecimal(6, ZERO);
                    stmt.setBigDecimal(7, ZERO);
                    stmt.setBigDecimal(8, ZERO);
                    stmt.setBigDecimal(9, ZERO);
                    stmt.setBigDecimal(10, ZERO);
                    stmt.setInt(11, 1);
                    stmt.setString(12, info.name);
                    stmt.setString(13, info.number);
                    stmt.setBigDecimal(14, ZERO);
                    stmt.setBigDecimal(15, ZERO);
                    stmt.setBigDecimal(16, ZERO);
                    stmt.setBigDecimal(17, ZERO);
                    stmt.setNull(18, Types.DATE);
                    stmt.executeUpdate();
                }

                connection.commit();
                return Result.success("添加成功");
            } else {
                connection.rollback();
                return Result.error("插入失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ignored) {}
            return Result.error("添加失败：" + e.getMessage());
        } finally {
            try {
                if (checkStmt != null) checkStmt.close();
                if (insertStmt != null) insertStmt.close();
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public Result batchAdd(List<Long> studentIds, long courseId, long classId, long schoolId) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Result.error("学生ID列表为空");
        }

        if (studentIds.size() > 5000) {
            return Result.error("单次最多支持5000名学生");
        }

        Connection connection = null;
        PreparedStatement existsStmt = null;
        PreparedStatement insertCourseStudentStmt = null;

        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            connection.setAutoCommit(false);

            // >>>>>>>>>>>>>> 查询 yee_node 统计数量 <<<<<<<<<<<<<<
            int videoCount = 0;
            int workCount = 0;
            int examCount = 0;

            if (courseId > 0) {
                String nodeCountSql = """
            SELECT 
                COALESCE(SUM(tabVideo), 0) AS videoCount,
                COALESCE(SUM(tabWork), 0) AS workCount,
                COALESCE(SUM(tabExam), 0) AS examCount
            FROM yee_node 
            WHERE courseId = ?
            """;
                try (PreparedStatement nodeStmt = connection.prepareStatement(nodeCountSql)) {
                    nodeStmt.setLong(1, courseId);
                    ResultSet rs = nodeStmt.executeQuery();
                    if (rs.next()) {
                        videoCount = rs.getInt("videoCount");
                        workCount = rs.getInt("workCount");
                        examCount = rs.getInt("examCount");
                    }
                    rs.close();
                }
            }

            // 1. 查询已存在的选课记录（避免重复）
            Set<Long> existingStudentIds = new HashSet<>();
            String placeholders = studentIds.stream().map(id -> "?").collect(Collectors.joining(", "));
            String existsSql = "SELECT studentId FROM yee_course_student WHERE courseId = ? AND classId = ? AND studentId IN (" + placeholders + ")";
            existsStmt = connection.prepareStatement(existsSql);
            existsStmt.setLong(1, courseId);
            existsStmt.setLong(2, classId);
            for (int i = 0; i < studentIds.size(); i++) {
                existsStmt.setLong(3 + i, studentIds.get(i));
            }

            try (ResultSet rs = existsStmt.executeQuery()) {
                while (rs.next()) {
                    existingStudentIds.add(rs.getLong("studentId"));
                }
            }

            // 2. 准备插入 yee_course_student
            String insertCourseStudentSql = """
        INSERT INTO yee_course_student (
            schoolId, classId, courseId, studentId,
            videoLearned, videoCount, lastNodeId,
            workLearned, workCount,
            examLearned, examCount,
            discussJoin, discussCount,
            studyTime, `change`, calculate, addTime
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

            insertCourseStudentStmt = connection.prepareStatement(insertCourseStudentSql);
            Timestamp now = new Timestamp(System.currentTimeMillis());
            List<Long> newStudentIds = new ArrayList<>();

            for (Long studentId : studentIds) {
                if (!existingStudentIds.contains(studentId)) {
                    insertCourseStudentStmt.setLong(1, schoolId);
                    insertCourseStudentStmt.setLong(2, classId);
                    insertCourseStudentStmt.setLong(3, courseId);
                    insertCourseStudentStmt.setLong(4, studentId);
                    insertCourseStudentStmt.setInt(5, 0); // videoLearned
                    insertCourseStudentStmt.setInt(6, videoCount);
                    insertCourseStudentStmt.setInt(7, 0); // lastNodeId
                    insertCourseStudentStmt.setInt(8, 0); // workLearned
                    insertCourseStudentStmt.setInt(9, workCount);
                    insertCourseStudentStmt.setInt(10, 0); // examLearned
                    insertCourseStudentStmt.setInt(11, examCount);
                    insertCourseStudentStmt.setInt(12, 0); // discussJoin
                    insertCourseStudentStmt.setInt(13, 0); // discussCount
                    insertCourseStudentStmt.setInt(14, 0); // studyTime
                    insertCourseStudentStmt.setInt(15, 0); // change
                    insertCourseStudentStmt.setInt(16, 0); // calculate
                    insertCourseStudentStmt.setTimestamp(17, now);
                    insertCourseStudentStmt.addBatch();
                    newStudentIds.add(studentId);
                }
            }

            int insertedCount = 0;
            if (!newStudentIds.isEmpty()) {
                insertCourseStudentStmt.executeBatch();
                insertedCount = newStudentIds.size();

                // 3. 更新课程人数
                String updateStuCountSql = "UPDATE yee_course SET stuCount = stuCount + ? WHERE id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateStuCountSql)) {
                    updateStmt.setLong(1, insertedCount);
                    updateStmt.setLong(2, courseId);
                    updateStmt.executeUpdate();
                }

                // 4. 查询新增学生的姓名和学号
                Map<Long, StudentInfo> studentInfoMap = queryStudentInfo(connection, newStudentIds);

                // 5. 批量插入 yee_course_results（带 stuName/stuNumber）
                try (PreparedStatement stmt = connection.prepareStatement(INSERT_COURSE_RESULTS_SQL)) {
                    for (Long studentId : newStudentIds) {
                        StudentInfo info = studentInfoMap.getOrDefault(studentId, new StudentInfo("", ""));
                        stmt.setLong(1, schoolId);
                        stmt.setLong(2, courseId);
                        stmt.setLong(3, studentId);
                        stmt.setLong(4, classId);
                        stmt.setBigDecimal(5, ZERO);
                        stmt.setBigDecimal(6, ZERO);
                        stmt.setBigDecimal(7, ZERO);
                        stmt.setBigDecimal(8, ZERO);
                        stmt.setBigDecimal(9, ZERO);
                        stmt.setBigDecimal(10, ZERO);
                        stmt.setInt(11, 1);
                        stmt.setString(12, info.name);
                        stmt.setString(13, info.number);
                        stmt.setBigDecimal(14, ZERO);
                        stmt.setBigDecimal(15, ZERO);
                        stmt.setBigDecimal(16, ZERO);
                        stmt.setBigDecimal(17, ZERO);
                        stmt.setNull(18, Types.DATE);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
            }

            connection.commit();
            return Result.success("成功添加了 " + insertedCount + " 条记录");

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (connection != null) {
                    connection.rollback();
                }
            } catch (SQLException ignored) {
            }
            return Result.error("批量导入失败：" + e.getMessage());
        } finally {
            try {
                if (insertCourseStudentStmt != null) insertCourseStudentStmt.close();
                if (existsStmt != null) existsStmt.close();
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public Result update(YeeCourseStudent courseStudent) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) courseStudent.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            StringBuilder sql = new StringBuilder("UPDATE yee_course_student SET ");
            List<Object> parameters = new ArrayList<>();

            if (courseStudent.getClassId() > 0) {
                sql.append("`classId` = ?, ");
                parameters.add(courseStudent.getClassId());
            }

            if (courseStudent.getCourseId() > 0) {
                sql.append("`courseId` = ?, ");
                parameters.add(courseStudent.getCourseId());
            }

            if (courseStudent.getStudentId() > 0) {
                sql.append("`studentId` = ?, ");
                parameters.add(courseStudent.getStudentId());
            }

            if (courseStudent.getVideoLearned() >= 0) {
                sql.append("`videoLearned` = ?, ");
                parameters.add(courseStudent.getVideoLearned());
            }

            if (courseStudent.getVideoCount() >= 0) {
                sql.append("`videoCount` = ?, ");
                parameters.add(courseStudent.getVideoCount());
            }

            if (courseStudent.getLastNodeId() >= 0) {
                sql.append("`lastNodeId` = ?, ");
                parameters.add(courseStudent.getLastNodeId());
            }

            if (courseStudent.getWorkLearned() >= 0) {
                sql.append("`workLearned` = ?, ");
                parameters.add(courseStudent.getWorkLearned());
            }

            if (courseStudent.getWorkCount() >= 0) {
                sql.append("`workCount` = ?, ");
                parameters.add(courseStudent.getWorkCount());
            }

            if (courseStudent.getExamLearned() >= 0) {
                sql.append("`examLearned` = ?, ");
                parameters.add(courseStudent.getExamLearned());
            }

            if (courseStudent.getExamCount() >= 0) {
                sql.append("`examCount` = ?, ");
                parameters.add(courseStudent.getExamCount());
            }

            if (courseStudent.getDiscussJoin() >= 0) {
                sql.append("`discussJoin` = ?, ");
                parameters.add(courseStudent.getDiscussJoin());
            }

            if (courseStudent.getDiscussCount() >= 0) {
                sql.append("`discussCount` = ?, ");
                parameters.add(courseStudent.getDiscussCount());
            }

            if (courseStudent.getStudyTime() >= 0) {
                sql.append("`studyTime` = ?, ");
                parameters.add(courseStudent.getStudyTime());
            }

            if (courseStudent.getChange() >= 0) {
                sql.append("`change` = ?, ");
                parameters.add(courseStudent.getChange());
            }

            if (courseStudent.getCalculate() >= 0) {
                sql.append("`calculate` = ?, ");
                parameters.add(courseStudent.getCalculate());
            }

            if (courseStudent.getAddTime() != null) {
                sql.append("`addTime` = ?, ");
                parameters.add(courseStudent.getAddTime());
            }

            if (parameters.isEmpty()) {
                connection.close();
                return Result.error("没有可更新的字段");
            }

            sql.delete(sql.length() - 2, sql.length());
            sql.append(" WHERE id = ?");
            parameters.add(courseStudent.getId());

            PreparedStatement st = connection.prepareStatement(sql.toString());
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof Long) {
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
    public Result delete(Integer schoolId, long courseId, long classId, List<Long> studentIds) {
        Connection connection = null;
        PreparedStatement deleteStudentStmt = null;
        PreparedStatement deleteResultsStmt = null;
        PreparedStatement updateStuCountStmt = null;

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            if (studentIds == null || studentIds.isEmpty()) {
                return Result.error("请选择要删除的学生");
            }

            // 【关键修复】把 List<Object> 安全转成 List<Long>，解决类型转换异常
            List<Long> ids = new ArrayList<>();
            for (Object o : studentIds) {
                if (o instanceof Number) {
                    ids.add(((Number) o).longValue());
                }
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            connection.setAutoCommit(false);

            // 批量删除学生课程关系
            String deleteStudentSql = "DELETE FROM yee_course_student WHERE courseId = ? AND classId = ? AND studentId = ?";
            deleteStudentStmt = connection.prepareStatement(deleteStudentSql);
            int deleteCount = 0;
            for (long studentId : ids) {
                deleteStudentStmt.setLong(1, courseId);
                deleteStudentStmt.setLong(2, classId);
                deleteStudentStmt.setLong(3, studentId);
                deleteStudentStmt.addBatch();
            }
            int[] results = deleteStudentStmt.executeBatch();
            for (int r : results) {
                if (r > 0) deleteCount++;
            }

            if (deleteCount > 0) {
                // 删除成绩记录
                String deleteResultsSql = "DELETE FROM yee_course_results WHERE courseId = ? AND classId = ? AND userId = ? AND schoolId = ?";
                deleteResultsStmt = connection.prepareStatement(deleteResultsSql);
                for (long studentId : ids) {
                    deleteResultsStmt.setLong(1, courseId);
                    deleteResultsStmt.setLong(2, classId);
                    deleteResultsStmt.setLong(3, studentId);
                    deleteResultsStmt.setLong(4, schoolId);
                    deleteResultsStmt.addBatch();
                }
                deleteResultsStmt.executeBatch();

                // 更新人数
                String updateStuCountSql = "UPDATE yee_course SET stuCount = GREATEST(stuCount - ?, 0) WHERE id = ?";
                updateStuCountStmt = connection.prepareStatement(updateStuCountSql);
                updateStuCountStmt.setInt(1, deleteCount);
                updateStuCountStmt.setLong(2, courseId);
                updateStuCountStmt.executeUpdate();
            }

            connection.commit();
            return deleteCount > 0 ? Result.success("批量删除成功，共删除 " + deleteCount + " 名学生")
                    : Result.error("删除失败：未找到匹配记录");

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (connection != null) connection.rollback();
            } catch (SQLException ignored) {}
            return Result.error("批量删除失败：" + e.getMessage());
        } finally {
            try {
                if (deleteStudentStmt != null) deleteStudentStmt.close();
                if (deleteResultsStmt != null) deleteResultsStmt.close();
                if (updateStuCountStmt != null) updateStuCountStmt.close();
                if (connection != null) {
                    connection.setAutoCommit(true);
                    connection.close();
                }
            } catch (SQLException ignored) {}
        }
    }

    @Override
    public Result courseStudentLike(int schoolId, long courseId, long classId, String name, String number, String idCard, int pageNum, int pageSize) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 构建动态 WHERE 条件
            StringBuilder whereClause = new StringBuilder();
            List<Object> params = new ArrayList<>();
            List<Object> countParams = new ArrayList<>();

            whereClause.append(" yes.courseId = ? AND yes.classId = ?");
            params.add(courseId);
            params.add(classId);
            countParams.add(courseId);
            countParams.add(classId);

            if (name != null && !name.trim().isEmpty()) {
                whereClause.append(" AND ys.name LIKE ?");
                String searchName = "%" + name.trim() + "%";
                params.add(searchName);
                countParams.add(searchName);
            }
            if (number != null && !number.trim().isEmpty()) {
                whereClause.append(" AND ys.number LIKE ?");
                String searchNumber = "%" + number.trim() + "%";
                params.add(searchNumber);
                countParams.add(searchNumber);
            }
            if (idCard != null && !idCard.trim().isEmpty()) {
                whereClause.append(" AND ys.idCard LIKE ?");
                String searchIdCard = "%" + idCard.trim() + "%";
                params.add(searchIdCard);
                countParams.add(searchIdCard);
            }

            // 分页计算
            int offset = (pageNum - 1) * pageSize;

            // 主查询 SQL
            String sql = "SELECT ys.id, ys.name, ys.number, ys.idCard,ys.point,ys.gender, yc.name AS className " +
                    "FROM yee_course_student yes " +
                    "INNER JOIN yee_student ys ON yes.studentId = ys.id " +
                    "LEFT JOIN yee_classes yc ON ys.classId = yc.id " +
                    "WHERE " + whereClause +
                    " ORDER BY yes.addTime DESC " +
                    "LIMIT ?, ?";

            // Count SQL
            String countSql = "SELECT COUNT(*) " +
                    "FROM yee_course_student yes " +
                    "INNER JOIN yee_student ys ON yes.studentId = ys.id " +
                    "WHERE " + whereClause.toString();

            // 执行 count 查询
            PreparedStatement countSt = connection.prepareStatement(countSql);
            for (int i = 0; i < countParams.size(); i++) {
                countSt.setObject(i + 1, countParams.get(i));
            }
            ResultSet countRs = countSt.executeQuery();
            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }

            // 执行主查询
            PreparedStatement st = connection.prepareStatement(sql);
            for (int i = 0; i < params.size(); i++) {
                st.setObject(i + 1, params.get(i));
            }
            st.setInt(params.size() + 1, offset);
            st.setInt(params.size() + 2, pageSize);

            ResultSet rs = st.executeQuery();
            List<SelectedCoursesStudents> resultList = new ArrayList<>();
            while (rs.next()) {
                SelectedCoursesStudents vo = new SelectedCoursesStudents();
                vo.setId(rs.getLong("id"));
                vo.setName(rs.getString("name"));
                vo.setNumber(rs.getString("number"));
                vo.setIdCard(rs.getString("idCard"));
                vo.setGender(rs.getString("gender"));
                vo.setPoint(rs.getString("point"));
                vo.setClassName(rs.getString("className"));
                resultList.add(vo);
            }

            // 关闭资源
            rs.close();
            st.close();
            countRs.close();
            countSt.close();
            connection.close();

            return Result.success(resultList, (long) totalCount);

        } catch (Exception e) {
            e.printStackTrace(); // 建议记录日志
            return Result.error("搜索失败：" + e.getMessage());
        }
    }

//    @Override
//    public Result getAllStudentsWithCourseType(
//            int schoolId,
//            long courseId,
//            long classId,
//            long teaClassId,
//            String name,
//            String number,
//            String idCard,
//            String join,
//            int pageNum,
//            int pageSize) {
//
//        try {
//            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//            if (slSchool == null || slSchool.getAllow() == 0) {
//                return Result.error("学校不存在或未审核");
//            }
//
//            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
//            if (teaClassId <= 0) {
//                return Result.error("教学班级ID无效");
//            }
//            // 构建 WHERE 条件（注意：cs 表在 WHERE 中使用时需确保 LEFT JOIN 生效）
//            StringBuilder whereClause = new StringBuilder(" WHERE s.schoolId = ? ");
//            List<Object> params = new ArrayList<>();
//            params.add(schoolId);
//
//            if (name != null && !name.trim().isEmpty()) {
//                whereClause.append(" AND s.name LIKE ?");
//                params.add("%" + name.trim() + "%");
//            }
//            if (number != null && !number.trim().isEmpty()) {
//                whereClause.append(" AND s.number LIKE ?");
//                params.add("%" + number.trim() + "%");
//            }
//            if (idCard != null && !idCard.trim().isEmpty()) {
//                whereClause.append(" AND s.idCard LIKE ?");
//                params.add("%" + idCard.trim() + "%");
//            }
//            // 按行政班级筛选
//            if (classId > 0) {
//                whereClause.append(" AND s.classId = ?");
//                params.add(classId);
//            }
//
//            // 根据 join 参数过滤（注意：此时 cs 已在 FROM 中 LEFT JOIN）
//            if ("1".equals(join)) {
//                whereClause.append(" AND cs.studentId IS NOT NULL");
//            } else if ("0".equals(join)) {
//                whereClause.append(" AND cs.studentId IS NULL");
//            }
//
//            // ===== Count SQL =====
//            String countSql = "SELECT COUNT(DISTINCT s.id) " +
//                    "FROM yee_student s " +
//                    "LEFT JOIN yee_classes ys ON s.classId = ys.id " +
//                    "LEFT JOIN yee_college yc ON s.collegeId = yc.id " +
//                    "LEFT JOIN yee_course_student cs ON s.id = cs.studentId AND cs.courseId = ? " +
//                    whereClause;
//
//            PreparedStatement countPs = connection.prepareStatement(countSql);
//            countPs.setLong(1, courseId); // cs.courseId
//
//            int idx = 2;
//            for (Object p : params) {
//                countPs.setObject(idx++, p);
//            }
//
//            ResultSet countRs = countPs.executeQuery();
//            int totalCount = countRs.next() ? countRs.getInt(1) : 0;
//
//            // ===== 主查询 SQL =====
//            String sql = "SELECT " +
//                    "s.id, " +
//                    "s.name, " +
//                    "s.number, " +
//                    "s.idCard, " +
//                    "s.gender, " +
//                    "ys.name AS className, " +
//                    "yc.name AS collegeName, " +
//                    "CASE " +
//                    "  WHEN cs.studentId IS NULL THEN 0 " +
//                    "  WHEN cs.classId = ? THEN 1 " +
//                    "  ELSE 2 " +
//                    "END AS type " +
//                    "FROM yee_student s " +
//                    "LEFT JOIN yee_classes ys ON s.classId = ys.id " +
//                    "LEFT JOIN yee_college yc ON s.collegeId = yc.id " +
//                    "LEFT JOIN yee_course_student cs ON s.id = cs.studentId AND cs.courseId = ? " +
//                    whereClause +
//                    " ORDER BY type DESC, s.addTime DESC " +
//                    "LIMIT ?, ?";
//
//            PreparedStatement ps = connection.prepareStatement(sql);
//
//            // 设置 CASE 中的 teaClassId（判断是否本教学班）
//            ps.setLong(1, teaClassId);     // 对应 cs.classId = ?
//            // 设置 JOIN 中的 courseId
//            ps.setLong(2, courseId);       // 对应 cs.courseId = ?
//
//            // 设置 WHERE 条件参数（schoolId, classId, name...）
//            int index = 3;
//            for (Object param : params) {
//                ps.setObject(index++, param);
//            }
//
//            // 设置分页
//            int offset = (pageNum - 1) * pageSize;
//            ps.setInt(index++, offset);
//            ps.setInt(index, pageSize);
//
//            // 执行查询
//            ResultSet rs = ps.executeQuery();
//            List<SelectedCoursesStudents> resultList = new ArrayList<>();
//            while (rs.next()) {
//                SelectedCoursesStudents vo = new SelectedCoursesStudents();
//                vo.setId(rs.getLong("id"));
//                vo.setName(rs.getString("name"));
//                vo.setNumber(rs.getString("number"));
//                vo.setIdCard(rs.getString("idCard"));
//                vo.setGender(rs.getString("gender"));
//                vo.setClassName(rs.getString("className"));
//                vo.setCollegeName(rs.getString("collegeName"));
//                vo.setType(rs.getInt("type"));
//                resultList.add(vo);
//            }
//
//            // 关闭资源
//            rs.close();
//            ps.close();
//            countRs.close();
//            countPs.close();
//            connection.close();
//
//            return Result.success(resultList, (long) totalCount);
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            return Result.error("查询失败：" + e.getMessage());
//        }
//    }
@Override
public Result getAllStudentsWithCourseType(
        int schoolId,
        long courseId,
        long classId,
        long teaClassId,
        String name,
        String number,
        String idCard,
        String join,
        int pageNum,
        int pageSize) {

    SlSchool slSchool = slSchoolMapper.selectById(schoolId);
    if (slSchool == null || slSchool.getAllow() == 0) {
        return Result.error("学校不存在或未审核");
    }
    if (teaClassId <= 0) {
        return Result.error("教学班级ID无效");
    }

    // 使用try-with-resources自动关闭连接、ps、rs
    try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
        // 通用WHERE片段与参数
        StringBuilder whereBase = new StringBuilder("s.schoolId = ? ");
        List<Object> baseParams = new ArrayList<>();
        baseParams.add(schoolId);

        if (name != null && !name.trim().isEmpty()) {
            whereBase.append(" AND s.name LIKE ?");
            baseParams.add("%" + name.trim() + "%");
        }
        if (number != null && !number.trim().isEmpty()) {
            whereBase.append(" AND s.number LIKE ?");
            baseParams.add("%" + number.trim() + "%");
        }
        if (idCard != null && !idCard.trim().isEmpty()) {
            whereBase.append(" AND s.idCard LIKE ?");
            baseParams.add("%" + idCard.trim() + "%");
        }
        if (classId > 0) {
            whereBase.append(" AND s.classId = ?");
            baseParams.add(classId);
        }

        // ========== 1. 优化计数SQL：无多余JOIN，EXISTS替代左连接去重 ==========
        StringBuilder countSqlSb = new StringBuilder("SELECT COUNT(s.id) FROM yee_student s WHERE ").append(whereBase);
        List<Object> countParams = new ArrayList<>(baseParams);

        if ("1".equals(join)) {
            countSqlSb.append(" AND EXISTS (SELECT 1 FROM yee_course_student cs WHERE cs.studentId = s.id AND cs.courseId = ?)");
            countParams.add(courseId);
        } else if ("0".equals(join)) {
            countSqlSb.append(" AND NOT EXISTS (SELECT 1 FROM yee_course_student cs WHERE cs.studentId = s.id AND cs.courseId = ?)");
            countParams.add(courseId);
        }

        long totalCount = 0;
        try (PreparedStatement countPs = connection.prepareStatement(countSqlSb.toString())) {
            for (int i = 0; i < countParams.size(); i++) {
                countPs.setObject(i + 1, countParams.get(i));
            }
            try (ResultSet countRs = countPs.executeQuery()) {
                if (countRs.next()) {
                    totalCount = countRs.getLong(1);
                }
            }
        }

        // ========== 2. 列表查询SQL（保留班级学院关联展示名称） ==========
        StringBuilder listWhere = new StringBuilder(" WHERE ").append(whereBase);
        List<Object> listParams = new ArrayList<>(baseParams);
        // join过滤条件依旧走左连接匹配
        if ("1".equals(join)) {
            listWhere.append(" AND cs.studentId IS NOT NULL");
        } else if ("0".equals(join)) {
            listWhere.append(" AND cs.studentId IS NULL");
        }

        String listSql = "SELECT " +
                "s.id, s.name, s.number, s.idCard, s.gender, " +
                "ys.name AS className, yc.name AS collegeName, " +
                "CASE WHEN cs.studentId IS NULL THEN 0 WHEN cs.classId = ? THEN 1 ELSE 2 END AS type " +
                "FROM yee_student s " +
                "LEFT JOIN yee_classes ys ON s.classId = ys.id " +
                "LEFT JOIN yee_college yc ON s.collegeId = yc.id " +
                "LEFT JOIN yee_course_student cs ON s.id = cs.studentId AND cs.courseId = ? "
                + listWhere + " ORDER BY type DESC, s.addTime DESC LIMIT ?, ?";

        List<SelectedCoursesStudents> resultList = new ArrayList<>();
        int offset = (pageNum - 1) * pageSize;
        try (PreparedStatement ps = connection.prepareStatement(listSql)) {
            int idx = 1;
            ps.setLong(idx++, teaClassId);    // case里教学班
            ps.setLong(idx++, courseId);       // cs关联课程
            // 基础条件参数
            for (Object p : listParams) {
                ps.setObject(idx++, p);
            }
            ps.setInt(idx++, offset);
            ps.setInt(idx, pageSize);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SelectedCoursesStudents vo = new SelectedCoursesStudents();
                    vo.setId(rs.getLong("id"));
                    vo.setName(rs.getString("name"));
                    vo.setNumber(rs.getString("number"));
                    vo.setIdCard(rs.getString("idCard"));
                    vo.setGender(rs.getString("gender"));
                    vo.setClassName(rs.getString("className"));
                    vo.setCollegeName(rs.getString("collegeName"));
                    vo.setType(rs.getInt("type"));
                    resultList.add(vo);
                }
            }
        }

        return Result.success(resultList, totalCount);
    } catch (Exception e) {
        e.printStackTrace();
        return Result.error("查询失败：" + e.getMessage());
    }
}

    // 新增内部类（放在 importCourseStudent 方法所在类中任意位置，建议靠近该方法）
    private static class StudentRow {
        final int fileLineNumber;
        final String studentNo;
        final String name;

        StudentRow(int line, String no, String name) {
            this.fileLineNumber = line;
            this.studentNo = no;
            this.name = name;
        }
    }
    @Override
    public Result importCourseStudent(int schoolId, long courseId, long classId, MultipartFile file) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            if (file == null || file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                return Result.error("文件名无效");
            }

            List<String> studentNoKeys = Arrays.asList("学号", "studentno", "number", "id");
            List<String> nameKeys = Arrays.asList("姓名", "name", "full_name");

            // 修改：List<String[]> → List<StudentRow>
            List<StudentRow> validRows = new ArrayList<>();
            List<String> errors = new ArrayList<>();

            // 解析文件
            if (originalFilename.toLowerCase().endsWith(".csv")) {
                parseCsvFile(file, validRows, errors, studentNoKeys, nameKeys);
            } else if (originalFilename.toLowerCase().endsWith(".xls") || originalFilename.toLowerCase().endsWith(".xlsx")) {
                parseExcelFile(file, validRows, errors, studentNoKeys, nameKeys);
            } else {
                return Result.error("仅支持 .csv、.xls、.xlsx 格式的文件");
            }

            if (validRows.isEmpty()) {
                return Result.error("没有有效的学生数据");
            }

            Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            conn.setAutoCommit(false);

            int videoCount = 0, workCount = 0, examCount = 0;
            try (PreparedStatement nodeStmt = conn.prepareStatement(
                    "SELECT COALESCE(SUM(tabVideo),0), COALESCE(SUM(tabWork),0), COALESCE(SUM(tabExam),0) " +
                            "FROM yee_node WHERE courseId = ?")) {
                nodeStmt.setLong(1, courseId);
                try (ResultSet rs = nodeStmt.executeQuery()) {
                    if (rs.next()) {
                        videoCount = rs.getInt(1);
                        workCount = rs.getInt(2);
                        examCount = rs.getInt(3);
                    }
                }
            }

            int totalRows = validRows.size();
            int totalSuccess = 0;
            int batchSize = 500;

            try {
                for (int start = 0; start < validRows.size(); start += batchSize) {
                    int end = Math.min(start + batchSize, validRows.size());
                    // 修改：subList 类型为 StudentRow
                    List<StudentRow> batch = validRows.subList(start, end);

                    StringBuilder sql = new StringBuilder();
                    sql.append("SELECT s.id, s.number, s.name FROM yee_student s WHERE s.schoolId = ? AND (");
                    for (int i = 0; i < batch.size(); i++) {
                        if (i > 0) sql.append(" OR ");
                        sql.append("(s.number = ? AND s.name = ?)");
                    }
                    sql.append(") AND NOT EXISTS (SELECT 1 FROM yee_course_student cs WHERE cs.courseId = ? AND cs.studentId = s.id)");

                    Map<String, Long> validStudentMap = new HashMap<>();
                    try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
                        int idx = 1;
                        stmt.setLong(idx++, schoolId);
                        for (StudentRow row : batch) { // ← 使用 StudentRow
                            stmt.setString(idx++, row.studentNo);
                            stmt.setString(idx++, row.name);
                        }
                        stmt.setLong(idx++, courseId);

                        try (ResultSet rs = stmt.executeQuery()) {
                            while (rs.next()) {
                                String key = rs.getString("number") + "|" + rs.getString("name");
                                validStudentMap.put(key, rs.getLong("id"));
                            }
                        }
                    }

                    // 构建 studentId -> (name, number) 映射
                    Map<Long, StudentInfo> toInsertMap = new HashMap<>();
                    for (StudentRow row : batch) {
                        String key = row.studentNo + "|" + row.name;
                        Long studentId = validStudentMap.get(key);
                        if (studentId != null) {
                            toInsertMap.put(studentId, new StudentInfo(row.name, row.studentNo));
                        } else {
                            if (true) {
                                errors.add("第 " + row.fileLineNumber + " 行: 学号[" + row.studentNo + "]姓名[" + row.name + "]的学生不存在或已选该课程");
                            }
                        }
                    }

                    if (!toInsertMap.isEmpty()) {
                        List<Long> toInsert = new ArrayList<>(toInsertMap.keySet());
                        insertCourseStudents(conn, toInsert, schoolId, courseId, classId, videoCount, workCount, examCount);
                        insertCourseResults(conn, toInsertMap, schoolId, courseId, classId);
                        totalSuccess += toInsertMap.size();
                    }
                }

                if (totalSuccess > 0) {
                    try (PreparedStatement updateStmt = conn.prepareStatement(
                            "UPDATE yee_course SET stuCount = stuCount + ? WHERE id = ?")) {
                        updateStmt.setInt(1, totalSuccess);
                        updateStmt.setLong(2, courseId);
                        updateStmt.executeUpdate();
                    }
                }

                conn.commit();

                Map<String, Object> data = new HashMap<>();
                data.put("success", totalSuccess);
                data.put("failed", totalRows - totalSuccess);
                data.put("total", totalRows);
                data.put("errors", errors);
                return Result.success(data);

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return Result.error("导入过程中发生错误: " + e.getMessage());
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (SQLException ignored) {
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("导入失败: " + e.getMessage());
        }
    }

    // --- 辅助方法：批量插入 yee_course_student ---
    private void insertCourseStudents(Connection conn, List<Long> studentIds, long schoolId, long courseId, long classId,
                                      int videoCount, int workCount, int examCount) throws SQLException {
        String sql = """
    INSERT INTO yee_course_student (
        studentId, schoolId, courseId, classId,
        videoLearned, videoCount, workLearned, workCount,
        examLearned, examCount, discussJoin, discussCount,
        studyTime, `change`, calculate, addTime
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
    """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Long id : studentIds) {
                stmt.setLong(1, id);
                stmt.setLong(2, schoolId);
                stmt.setLong(3, courseId);
                stmt.setLong(4, classId);
                stmt.setInt(5, 0);
                stmt.setInt(6, videoCount);
                stmt.setInt(7, 0);
                stmt.setInt(8, workCount);
                stmt.setInt(9, 0);
                stmt.setInt(10, examCount);
                stmt.setInt(11, 0);
                stmt.setInt(12, 0);
                stmt.setInt(13, 0);
                stmt.setInt(14, 0);
                stmt.setInt(15, 0);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    // --- 辅助方法：批量插入 yee_course_results ---
    private void insertCourseResults(Connection conn,
                                     Map<Long, StudentInfo> studentInfoMap,
                                     long schoolId, long courseId, long classId) throws SQLException {
        String sql = """
    INSERT INTO yee_course_results (
        schoolId, courseId, userId, classId,
        score, videoScore, examScore, workScore, discussScore, extraScore,
        ranking, stuName, stuNumber, videoResult, examResult, workResult, discussResult,
        calcDate
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """;
        BigDecimal ZERO = BigDecimal.ZERO;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map.Entry<Long, StudentInfo> entry : studentInfoMap.entrySet()) {
                Long studentId = entry.getKey();
                StudentInfo info = entry.getValue();

                stmt.setLong(1, schoolId);
                stmt.setLong(2, courseId);
                stmt.setLong(3, studentId);
                stmt.setLong(4, classId);
                stmt.setBigDecimal(5, ZERO);
                stmt.setBigDecimal(6, ZERO);
                stmt.setBigDecimal(7, ZERO);
                stmt.setBigDecimal(8, ZERO);
                stmt.setBigDecimal(9, ZERO);
                stmt.setBigDecimal(10, ZERO);
                stmt.setInt(11, 1);
                stmt.setString(12, info.name);      // ✅ 正确姓名
                stmt.setString(13, info.number);    // ✅ 正确学号
                stmt.setBigDecimal(14, ZERO);
                stmt.setBigDecimal(15, ZERO);
                stmt.setBigDecimal(16, ZERO);
                stmt.setBigDecimal(17, ZERO);
                stmt.setNull(18, Types.DATE);
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    // 解析 CSV 文件 —— 修改参数类型
    private void parseCsvFile(MultipartFile file,
                              List<StudentRow> validRows, // ← 改为 StudentRow
                              List<String> errors,
                              List<String> studentNoKeys,
                              List<String> nameKeys) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = br.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                throw new IllegalArgumentException("CSV 文件为空");
            }

            List<String> headers = parseCsvLine(headerLine);
            int studentNoIndex = -1;
            int nameIndex = -1;

            for (int i = 0; i < headers.size(); i++) {
                String h = cleanHeader(headers.get(i));
                if (studentNoKeys.contains(h)) {
                    studentNoIndex = i;
                } else if (nameKeys.contains(h)) {
                    nameIndex = i;
                }
            }

            if (studentNoIndex == -1 || nameIndex == -1) {
                throw new IllegalArgumentException("CSV缺少必要列：'学号' 或 '姓名'");
            }

            String line;
            int lineNumber = 2; // 数据从第2行开始
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    lineNumber++;
                    continue;
                }

                try {
                    List<String> values = parseCsvLine(line);
                    if (values.size() <= Math.max(studentNoIndex, nameIndex)) {
                        throw new IllegalArgumentException("列数不足");
                    }

                    String number = values.get(studentNoIndex).trim();
                    String name = values.get(nameIndex).trim();

                    if (number.isEmpty() || name.isEmpty()) {
                        throw new IllegalArgumentException("学号或姓名为空");
                    }

                    validRows.add(new StudentRow(lineNumber, number, name)); // ← 保存行号
                } catch (Exception ex) {
                    if (errors.size() < 10) {
                        errors.add("第 " + lineNumber + " 行: " + ex.getMessage());
                    }
                }
                lineNumber++;
            }
        }
    }

    private void parseExcelFile(MultipartFile file,
                                List<StudentRow> validRows,
                                List<String> errors,
                                List<String> studentNoKeys,
                                List<String> nameKeys) throws IOException {
        try (InputStream is = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalArgumentException("Excel 文件为空");
            }

            // 1. 读取表头，匹配学号/姓名列
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel 缺少表头行");
            }

            int studentNoIndex = -1;
            int nameIndex = -1;
            int lastCellNum = headerRow.getLastCellNum();

            for (int i = 0; i < lastCellNum; i++) {
                Cell cell = headerRow.getCell(i);
                String headerValue = getCellValueAsString(cell);
                String cleaned = cleanHeader(headerValue);

                if (studentNoKeys.contains(cleaned)) {
                    studentNoIndex = i;
                } else if (nameKeys.contains(cleaned)) {
                    nameIndex = i;
                }
            }

            if (studentNoIndex == -1 || nameIndex == -1) {
                throw new IllegalArgumentException("Excel缺少必要列：'学号' 或 '姓名'，请检查表头");
            }

            // 2. 遍历行，增加3层空行过滤
            int lineNumber = 2; // 数据从第2行（Excel物理行）开始计数
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);

                // 🔴 第一层：跳过null行
                if (row == null) {
                    lineNumber++;
                    continue;
                }

                // 🔴 第二层：跳过完全空白的行（WPS视觉空行）
                if (isRowEmpty(row)) {
                    lineNumber++;
                    continue;
                }

                try {
                    // 3. 读取学号/姓名，做非空校验
                    String number = getCellValueAsString(row.getCell(studentNoIndex)).trim();
                    String name = getCellValueAsString(row.getCell(nameIndex)).trim();

                    // 🔴 第三层：跳过学号/姓名都为空的行
                    if (number.isEmpty() && name.isEmpty()) {
                        lineNumber++;
                        continue;
                    }

                    // 🔴 校验：学号/姓名不能单独为空
                    if (number.isEmpty() || name.isEmpty()) {
                        throw new IllegalArgumentException("学号或姓名为空");
                    }

                    // 4. 校验通过，加入有效列表
                    validRows.add(new StudentRow(lineNumber, number, name));

                } catch (Exception ex) {
                    if (true) {
                        errors.add("第 " + lineNumber + " 行: " + ex.getMessage());
                    }
                }

                lineNumber++;
            }

        }
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            String cellValue = getCellValueAsString(cell).trim();
            if (!cellValue.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // 以下辅助方法保持完全不变（无需修改）
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    return String.format("%.0f", val);
                } else {
                    return String.valueOf(val);
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        result.add(cur.toString());
        return result;
    }

    private String cleanHeader(String header) {
        if (header == null) return "";
        return header.trim()
                .replaceAll("[\\s\\uFEFF\\u200B\\x00-\\x1F]", "")
                .replaceAll("^\"|\"$", "");
    }

    private YeeCourseStudent rsToYeeCourseStudent(ResultSet rs) throws SQLException {
        YeeCourseStudent courseStudent = new YeeCourseStudent();
        courseStudent.setId(rs.getLong("id"));
        courseStudent.setClassId(rs.getLong("classId"));
        courseStudent.setCourseId(rs.getLong("courseId"));
        courseStudent.setStudentId(rs.getLong("studentId"));
        courseStudent.setVideoLearned(rs.getLong("videoLearned"));
        courseStudent.setVideoCount(rs.getLong("videoCount"));
        courseStudent.setLastNodeId(rs.getLong("lastNodeId"));
        courseStudent.setWorkLearned(rs.getLong("workLearned"));
        courseStudent.setWorkCount(rs.getLong("workCount"));
        courseStudent.setExamLearned(rs.getLong("examLearned"));
        courseStudent.setExamCount(rs.getLong("examCount"));
        courseStudent.setDiscussJoin(rs.getLong("discussJoin"));
        courseStudent.setDiscussCount(rs.getLong("discussCount"));
        courseStudent.setSchoolId(rs.getLong("schoolId"));
        courseStudent.setStudyTime(rs.getLong("studyTime"));
        courseStudent.setChange(rs.getLong("change"));
        courseStudent.setCalculate(rs.getLong("calculate"));
        courseStudent.setAddTime(rs.getTimestamp("addTime"));
        courseStudent.setAddDate(rs.getDate("addDate"));
        return courseStudent;
    }
}
