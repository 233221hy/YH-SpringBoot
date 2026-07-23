package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeSignIn;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.YeeSignInService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import cn.xfywz.guozespring.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class YeeSignInServiceImpl implements YeeSignInService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result listSignIn(int schoolId, int courseId, int pageSize, int pageNum) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            int offset = (pageNum - 1) * pageSize;
            String countSql = "SELECT COUNT(*) FROM yee_sign_in WHERE schoolId = ? AND courseId = ?";
            String sql = "SELECT * FROM yee_sign_in WHERE schoolId = ? AND courseId = ? ORDER BY signInTime DESC LIMIT ? OFFSET ?";

            // 查询总数
            PreparedStatement countSt = connection.prepareStatement(countSql);
            countSt.setInt(1, schoolId);
            countSt.setInt(2, courseId);
            ResultSet countRs = countSt.executeQuery();

            int totalCount = 0;
            if (countRs.next()) {
                totalCount = countRs.getInt(1);
            }
            countRs.close();
            countSt.close();

            // 查询数据列表
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, schoolId);
            st.setInt(2, courseId);
            st.setInt(3, pageSize);
            st.setInt(4, offset);
            ResultSet rs = st.executeQuery();

            List<YeeSignIn> signInList = new ArrayList<>();
            while (rs.next()) {
                YeeSignIn signIn = rsToYeeSignIn(rs);
                signInList.add(signIn);
            }

            rs.close();
            st.close();
            connection.close();

            return Result.success(signInList, (long) totalCount);

        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result addSignIn(YeeSignIn yeeSignIn) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) yeeSignIn.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
                String sql = "INSERT INTO yee_sign_in (courseId, name, teacherId, classList, allow, finish, schoolId, signInTime, endTime, lateTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement st = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    st.setLong(1, yeeSignIn.getCourseId());
                    st.setString(2, yeeSignIn.getName());
                    st.setLong(3, yeeSignIn.getTeacherId());
                    st.setString(4, yeeSignIn.getClassList());
                    st.setLong(5, yeeSignIn.getAllow());
                    st.setLong(6, yeeSignIn.getFinish());
                    st.setLong(7, yeeSignIn.getSchoolId());
                    st.setTimestamp(8, yeeSignIn.getSignInTime());
                    st.setTimestamp(9, yeeSignIn.getEndTime());
                    st.setLong(10, yeeSignIn.getLateTime());

                    int rowsInserted = st.executeUpdate();
                    if (rowsInserted <= 0) {
                        return Result.error("添加失败");
                    }
                    try (ResultSet generatedKeys = st.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            yeeSignIn.setId(generatedKeys.getLong(1));
                        }
                    }
                }

                int totalSynced = 0;
                List<Long> classIds = new ArrayList<>();
                String rawClassList = yeeSignIn.getClassList();
                if (rawClassList != null && !rawClassList.trim().isEmpty()) {
                    String s = rawClassList.trim();
                    if (s.startsWith("[")) {
                        List<Long> nums = null;
                        try { nums = JsonUtil.parseList(s, Long.class); } catch (Exception ignore) {}
                        if (nums != null) classIds.addAll(nums);
                        else {
                            List<String> strs = null;
                            try { strs = JsonUtil.parseList(s, String.class); } catch (Exception ignore) {}
                            if (strs != null) {
                                for (String v : strs) {
                                    if (v == null) continue;
                                    String cleaned = v.replaceAll("[^0-9-]", "");
                                    if (!cleaned.isEmpty()) {
                                        try { classIds.add(Long.parseLong(cleaned)); } catch (Exception ignore) {}
                                    }
                                }
                            }
                        }
                    } else {
                        s = s.replaceAll("[\\[\\]\"]", "");
                        for (String part : s.split(",")) {
                            if (part == null) continue;
                            String cleaned = part.trim().replaceAll("[^0-9-]", "");
                            if (!cleaned.isEmpty()) {
                                try { classIds.add(Long.parseLong(cleaned)); } catch (Exception ignore) {}
                            }
                        }
                    }
                }
                String stuSql = "SELECT ycs.studentId FROM yee_course_student ycs WHERE ycs.schoolId = ? AND ycs.courseId = ? AND ycs.classId = ?";
                String insertSql = "INSERT INTO yee_sign_in_record (signId, userId, signTime, courseId, classId, schoolId, state) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement psQuery = connection.prepareStatement(stuSql);
                     PreparedStatement psInsert = connection.prepareStatement(insertSql)) {
                    for (Long cid : classIds) {
                        if (cid == null || cid <= 0) continue;

                        psQuery.setLong(1, yeeSignIn.getSchoolId());
                        psQuery.setLong(2, yeeSignIn.getCourseId());
                        psQuery.setLong(3, cid);
                        try (ResultSet rsStu = psQuery.executeQuery()) {
                            while (rsStu.next()) {
                                long stuId = rsStu.getLong(1);
                                psInsert.setLong(1, yeeSignIn.getId());
                                psInsert.setLong(2, stuId);
                                psInsert.setNull(3, Types.TIMESTAMP);
                                psInsert.setLong(4, yeeSignIn.getCourseId());
                                psInsert.setLong(5, cid);
                                psInsert.setLong(6, yeeSignIn.getSchoolId());
                                psInsert.setLong(7, 2L);
                                psInsert.addBatch();
                            }
                        }
                        int[] counts = psInsert.executeBatch();
                        for (int c : counts) if (c > 0) totalSynced += c;
                    }
                }

                return Result.success("添加成功，同步导入" + totalSynced + "条学生记录");
            }

        } catch (Exception e) {
            return Result.error("添加失败：" + e.getMessage());
        }
    }

    @Override
    public Result delSignIn(int id, int schoolId) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            String sql = "DELETE FROM yee_sign_in WHERE id = ? AND schoolId = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, id);
            st.setInt(2, schoolId);

            int rowsDeleted = st.executeUpdate();
            st.close();
            connection.close();

            if (rowsDeleted > 0) {
                return Result.success("删除成功");
            } else {
                return Result.error("删除失败，可能记录不存在或无权限");
            }

        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    @Override
    public Result likeSignIn(int schoolId, int courseId, String name) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(schoolId);
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }

            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            String sql = "SELECT * FROM yee_sign_in WHERE schoolId = ? AND courseId = ? AND name LIKE ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, schoolId);
            st.setInt(2, courseId);
            st.setString(3, "%" + name + "%");
            ResultSet rs = st.executeQuery();

            List<YeeSignIn> signInList = new ArrayList<>();
            while (rs.next()) {
                YeeSignIn signIn = rsToYeeSignIn(rs);
                signInList.add(signIn);
            }

            rs.close();
            st.close();
            connection.close();

            return Result.success(signInList);

        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @Override
    public Result updateSignIn(YeeSignIn yeeSignIn) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById((int) yeeSignIn.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            
            // 构建动态更新SQL
            StringBuilder sqlBuilder = new StringBuilder("UPDATE yee_sign_in SET ");
            List<Object> parameters = new ArrayList<>();
            
            // 动态添加需要更新的字段 (只更新显式设置的字段)
            // 通过反射检查字段是否为默认值来判断是否需要更新
            if (yeeSignIn.getCourseId() > 0) {
                sqlBuilder.append("courseId = ?, ");
                parameters.add(yeeSignIn.getCourseId());
            }
            if (yeeSignIn.getName() != null && !yeeSignIn.getName().isEmpty()) {
                sqlBuilder.append("name = ?, ");
                parameters.add(yeeSignIn.getName());
            }
            if (yeeSignIn.getTeacherId() > 0) {
                sqlBuilder.append("teacherId = ?, ");
                parameters.add(yeeSignIn.getTeacherId());
            }
            if (yeeSignIn.getClassList() != null) {
                sqlBuilder.append("classList = ?, ");
                parameters.add(yeeSignIn.getClassList());
            }
            if (yeeSignIn.getAllow() != null) { // allow可能为0，只有非默认值才更新
                sqlBuilder.append("allow = ?, ");
                parameters.add(yeeSignIn.getAllow());
            }
            if (yeeSignIn.getFinish() != 0) { // finish可能为0，只有非默认值才更新
                sqlBuilder.append("finish = ?, ");
                parameters.add(yeeSignIn.getFinish());
            }
            if (yeeSignIn.getSchoolId() > 0) {
                sqlBuilder.append("schoolId = ?, ");
                parameters.add(yeeSignIn.getSchoolId());
            }
            if (yeeSignIn.getSignInTime() != null) {
                sqlBuilder.append("signInTime = ?, ");
                parameters.add(yeeSignIn.getSignInTime());
            }
            if (yeeSignIn.getEndTime() != null) {
                sqlBuilder.append("endTime = ?, ");
                parameters.add(yeeSignIn.getEndTime());
            }
            if (yeeSignIn.getLateTime() != 0) { // lateTime可能为0，只有非默认值才更新
                sqlBuilder.append("lateTime = ?, ");
                parameters.add(yeeSignIn.getLateTime());
            }
            
            // 如果没有需要更新的字段，返回错误
            if (parameters.isEmpty()) {
                return Result.error("没有需要更新的字段");
            }
            
            // 移除最后的逗号和空格
            sqlBuilder.delete(sqlBuilder.length() - 2, sqlBuilder.length());
            
            // 添加WHERE条件
            sqlBuilder.append(" WHERE id = ? AND schoolId = ?");
            parameters.add(yeeSignIn.getId());
            parameters.add(yeeSignIn.getSchoolId());
            
            // 执行更新
            PreparedStatement st = connection.prepareStatement(sqlBuilder.toString());
            for (int i = 0; i < parameters.size(); i++) {
                st.setObject(i + 1, parameters.get(i));
            }
            
            int rowsUpdated = st.executeUpdate();
            st.close();
            connection.close();
            
            if (rowsUpdated > 0) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败，可能记录不存在或无权限");
            }
            
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }


    /**
     * 将ResultSet转换为YeeSignIn对象
     */
    private YeeSignIn rsToYeeSignIn(ResultSet rs) throws SQLException {
        YeeSignIn signIn = new YeeSignIn();
        signIn.setId(rs.getLong("id"));
        signIn.setCourseId(rs.getLong("courseId"));
        signIn.setName(rs.getString("name"));
        signIn.setTeacherId(rs.getLong("teacherId"));
        signIn.setClassList(rs.getString("classList"));
        signIn.setAllow(rs.getLong("allow"));
        signIn.setFinish(rs.getLong("finish"));
        signIn.setSchoolId(rs.getLong("schoolId"));
        signIn.setSignInTime(rs.getTimestamp("signInTime"));
        signIn.setEndTime(rs.getTimestamp("endTime"));
        signIn.setLateTime(rs.getLong("lateTime"));
        return signIn;
    }
}
