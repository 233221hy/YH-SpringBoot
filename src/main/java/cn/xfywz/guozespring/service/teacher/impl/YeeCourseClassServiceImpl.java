package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.constant.DataAuth;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeCourseClass;
import cn.xfywz.guozespring.entity.vo.YeeCourseClassVo;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeCourseClassService;
import cn.xfywz.guozespring.util.AuthDataPermissionUtil;
import cn.xfywz.guozespring.util.CurrentUserUtil;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static cn.xfywz.guozespring.entity.vo.YeeCourseClassVo.rsToCourseClassWithStats;

@Service
public class YeeCourseClassServiceImpl implements YeeCourseClassService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;

    private YeeCourseClass rsToYeeCourseClass(ResultSet rs) throws SQLException {
        YeeCourseClass yeeCourseClass = new YeeCourseClass();
        yeeCourseClass.setId(rs.getLong("id"));
        yeeCourseClass.setName(rs.getString("name"));
        yeeCourseClass.setCourseId(rs.getLong("courseId"));
        yeeCourseClass.setTeacherId(rs.getLong("teacherId"));
        yeeCourseClass.setSchoolId(rs.getLong("schoolId"));
        yeeCourseClass.setAllow(rs.getLong("allow"));
        yeeCourseClass.setAddTime(rs.getTimestamp("addTime"));
        yeeCourseClass.setCreateId(rs.getLong("createId"));
        yeeCourseClass.setChange(rs.getLong("change"));
        yeeCourseClass.setCalculate(rs.getLong("calculate"));
        yeeCourseClass.setAddDate(rs.getDate("addDate"));
        return yeeCourseClass;
    }
    @Override
    public Result selectAll(int schoolId, long courseId, int pageNum, int pageSize) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 从 AOP 鉴权存入的请求上下文获取用户身份
        JSONObject subjectJson = CurrentUserUtil.getCurrentUserSubject();
        Long currentUserId = CurrentUserUtil.getCurrentUserId();

        // 判断是否为教师（subject 中有 yeeManage）
        boolean isTeacher = subjectJson != null && subjectJson.containsKey("yeeManage");

        if (isTeacher) {
            StringBuilder sqlBuilder = new StringBuilder();
            List<Object> params = new ArrayList<>();

            sqlBuilder.append("""
            SELECT
                c.id,
                c.name,
                c.courseId,
                c.teacherId,
                c.schoolId,
                c.allow,
                c.addTime,
                c.createId,
                c.change,
                c.calculate,
                ycsr.announce,
                COALESCE(s.studentNum, 0) AS studentNum,
                m.name AS teacherName
            FROM yee_course_class c
            LEFT JOIN (
                SELECT classId, COUNT(*) AS studentNum
                FROM yee_course_student
                WHERE courseId = ?
                GROUP BY classId
            ) s ON c.id = s.classId
            LEFT JOIN yee_manage m ON c.teacherId = m.id
            LEFT JOIN (
                SELECT r.classId, r.courseId, r.schoolId, r.announce
                FROM yee_course_score_rules r
                INNER JOIN (
                    SELECT classId, courseId, schoolId, MAX(updateTime) AS maxUpdate
                    FROM yee_course_score_rules
                    GROUP BY classId, courseId, schoolId
                ) latest ON latest.classId = r.classId AND latest.courseId = r.courseId AND latest.schoolId = r.schoolId AND r.updateTime = latest.maxUpdate
            ) ycsr ON ycsr.classId = c.id AND ycsr.schoolId = c.schoolId AND ycsr.courseId = c.courseId
            WHERE c.courseId = ? AND c.schoolId = ?
        """);

            params.add(courseId);
            params.add(courseId);
            params.add(schoolId);

            try {
                AuthDataPermissionUtil.buildDataPermission(sqlBuilder, params, "c.courseId", "c.id");
            } catch (Exception e) {}

            sqlBuilder.append(" ORDER BY c.addTime DESC ");
            String countSql = "SELECT COUNT(*) FROM (" + sqlBuilder + ") AS temp";
            int offset = (pageNum - 1) * pageSize;
            sqlBuilder.append(" LIMIT ? OFFSET ? ");
            List<Object> pageParams = new ArrayList<>(params);
            pageParams.add(pageSize);
            pageParams.add(offset);

            int totalCount = 0;
            List<YeeCourseClassVo> list = new ArrayList<>();

            try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
                try (PreparedStatement countSt = connection.prepareStatement(countSql)) {
                    for (int i = 0; i < params.size(); i++) {
                        countSt.setObject(i + 1, params.get(i));
                    }
                    try (ResultSet countRs = countSt.executeQuery()) {
                        if (countRs.next()) {
                            totalCount = countRs.getInt(1);
                        }
                    }
                }
                try (PreparedStatement st = connection.prepareStatement(sqlBuilder.toString())) {
                    for (int i = 0; i < pageParams.size(); i++) {
                        st.setObject(i + 1, pageParams.get(i));
                    }
                    try (ResultSet rs = st.executeQuery()) {
                        while (rs.next()) {
                            list.add(rsToCourseClassWithStats(rs));
                        }
                    }
                }
            }
            return Result.success(list, (long) totalCount);
        }

        // 学生分支
        String sql = """
            SELECT DISTINCT
                c.id,
                c.name,
                c.courseId,
                c.teacherId,
                c.schoolId,
                c.allow,
                c.addTime
            FROM yee_course_class c
            JOIN yee_course_student cs ON cs.classId = c.id
            WHERE c.courseId = ?
              AND c.schoolId = ?
              AND cs.studentId = ?
            ORDER BY c.addTime DESC
            LIMIT ? OFFSET ?
        """;

        List<YeeCourseClassVo> list = new ArrayList<>();
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, courseId);
            ps.setObject(2, schoolId);
            ps.setObject(3, currentUserId);
            ps.setObject(4, pageSize);
            ps.setObject(5, (pageNum - 1) * pageSize);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                YeeCourseClassVo vo = new YeeCourseClassVo();
                vo.setId(rs.getInt("id"));
                vo.setName(rs.getString("name"));
                vo.setCourseId(rs.getInt("courseId"));
                vo.setTeacherId(rs.getInt("teacherId"));
                vo.setSchoolId(rs.getInt("schoolId"));
                vo.setAllow(rs.getByte("allow"));
                vo.setAddTime(rs.getTimestamp("addTime"));
                list.add(vo);
            }
        }
        return Result.success(list, (long) list.size());
    }

    @Override
    public Result add(YeeCourseClass yeeCourseClass) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) yeeCourseClass.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            // ===================== ✅ 权限校验：只有超管 / 课程创建者可添加 =====================
            Long currentUserId = AuthDataPermissionUtil.getCurrentUserId();
            DataAuth currentAuth = AuthDataPermissionUtil.getCurrentDataAuth();
            Long courseId = yeeCourseClass.getCourseId();

            // 1. 不是超管 → 必须校验是否是课程创建者
            if (!DataAuth.ALL.equals(currentAuth)) {
                // 查询课程是否是当前用户创建
                Connection connCheck = SlaveMysqlConnectionUtil.getConnection(slSchool);
                String checkSql = "SELECT createId FROM yee_course WHERE id = ?";
                PreparedStatement stCheck = connCheck.prepareStatement(checkSql);
                stCheck.setLong(1, courseId);
                ResultSet rsCheck = stCheck.executeQuery();

                boolean isCourseCreator = false;
                if (rsCheck.next()) {
                    Long createId = rsCheck.getLong("createId");
                    if (createId != null && createId.equals(currentUserId)) {
                        isCourseCreator = true;
                    }
                }

                rsCheck.close();
                stCheck.close();
                connCheck.close();

                // 既不是超管，也不是课程创建者 → 直接拦截
                if (!isCourseCreator) {
                    return Result.error("你无权限添加教学班级！");
                }
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            StringBuilder columns = new StringBuilder("INSERT INTO yee_course_class (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();

            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(yeeCourseClass.getSchoolId());

            if (yeeCourseClass.getName() != null && !yeeCourseClass.getName().trim().isEmpty()) {
                columns.append("`name`, ");
                values.append("?, ");
                parameters.add(yeeCourseClass.getName());
            }

            if (yeeCourseClass.getCourseId() > 0) {
                columns.append("`courseId`, ");
                values.append("?, ");
                parameters.add(yeeCourseClass.getCourseId());
            }

            if (yeeCourseClass.getTeacherId() > 0) {
                columns.append("`teacherId`, ");
                values.append("?, ");
                parameters.add(yeeCourseClass.getTeacherId());
            }

            if (yeeCourseClass.getAllow() >= 0) {
                columns.append("`allow`, ");
                values.append("?, ");
                parameters.add(yeeCourseClass.getAllow());
            }

            if (yeeCourseClass.getCreateId() > 0) {
                columns.append("`createId`, ");
                values.append("?, ");
                parameters.add(yeeCourseClass.getCreateId());
            }

            if (yeeCourseClass.getChange() >= 0) {
                columns.append("`change`, ");
                values.append("?, ");
                parameters.add(yeeCourseClass.getChange());
            }

            if (yeeCourseClass.getCalculate() >= 0) {
                columns.append("`calculate`, ");
                values.append("?, ");
                parameters.add(yeeCourseClass.getCalculate());
            }

            columns.append("`addTime`, ");
            values.append("?, ");
            parameters.add(yeeCourseClass.getAddTime() != null ? yeeCourseClass.getAddTime() : new Timestamp(System.currentTimeMillis()));

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
                    yeeCourseClass.setId(generatedKeys.getLong(1));
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
    public Result update(YeeCourseClass yeeCourseClass) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) yeeCourseClass.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            StringBuilder sql = new StringBuilder("UPDATE yee_course_class SET ");
            List<Object> parameters = new ArrayList<>();
            
            if (yeeCourseClass.getName() != null && !yeeCourseClass.getName().trim().isEmpty()) {
                sql.append("`name` = ?, ");
                parameters.add(yeeCourseClass.getName());
            }
            
            if (yeeCourseClass.getCourseId() > 0) {
                sql.append("`courseId` = ?, ");
                parameters.add(yeeCourseClass.getCourseId());
            }
            
            if (yeeCourseClass.getTeacherId() > 0) {
                sql.append("`teacherId` = ?, ");
                parameters.add(yeeCourseClass.getTeacherId());
            }
            
            if (yeeCourseClass.getAllow() >= 0) {
                sql.append("`allow` = ?, ");
                parameters.add(yeeCourseClass.getAllow());
            }
            
            if (yeeCourseClass.getCreateId() > 0) {
                sql.append("`createId` = ?, ");
                parameters.add(yeeCourseClass.getCreateId());
            }
            
            if (yeeCourseClass.getChange() >= 0) {
                sql.append("`change` = ?, ");
                parameters.add(yeeCourseClass.getChange());
            }
            
            if (yeeCourseClass.getCalculate() >= 0) {
                sql.append("`calculate` = ?, ");
                parameters.add(yeeCourseClass.getCalculate());
            }
            
            if (yeeCourseClass.getAddTime() != null) {
                sql.append("`addTime` = ?, ");
                parameters.add(yeeCourseClass.getAddTime());
            }
            
            if (parameters.isEmpty()) {
                connection.close();
                return Result.error("没有可更新的字段");
            }
            
            sql.delete(sql.length() - 2, sql.length());
            sql.append(" WHERE id = ?");
            parameters.add(yeeCourseClass.getId());
            
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
    public Result delete(int schoolId, int id) {
        Connection connection = null;
        PreparedStatement checkStmt = null;
        PreparedStatement deleteStmt = null;

        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 1. 检查该班级是否有已选课的学生
            String checkSql = "SELECT 1 FROM yee_course_student WHERE classId = ? LIMIT 1";
            checkStmt = connection.prepareStatement(checkSql);
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // 存在至少一个学生，禁止删除
                return Result.error("该班级已有学生选课，无法删除");
            }
            rs.close();
            checkStmt.close();

            // 2. 执行删除班级
            String deleteSql = "DELETE FROM yee_course_class WHERE id = ?";
            deleteStmt = connection.prepareStatement(deleteSql);
            deleteStmt.setInt(1, id);
            int rowsDeleted = deleteStmt.executeUpdate();

            if (rowsDeleted > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败：班级不存在");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败：" + e.getMessage());
        } finally {
            try {
                if (checkStmt != null) checkStmt.close();
                if (deleteStmt != null) deleteStmt.close();
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public Result like(int schoolId, long courseId,String name) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "SELECT * FROM yee_course_class WHERE courseId = ? AND name LIKE ? ORDER BY addTime DESC";
            String countSql = "SELECT COUNT(*) FROM yee_course_class WHERE courseId = ? AND name LIKE ?";
            
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
            
            List<YeeCourseClass> courseClasses = new ArrayList<>();
            while (rs.next()) {
                YeeCourseClass courseClass = rsToYeeCourseClass(rs);
                courseClasses.add(courseClass);
            }
            
            countRs.close();
            countSt.close();
            rs.close();
            st.close();
            connection.close();
            
            return Result.success(courseClasses, (long) totalCount);
            
        } catch (Exception e) {
            return Result.error("搜索失败：" + e.getMessage());
        }
    }
}
