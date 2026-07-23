package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.entity.vo.StuLike;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.admin.StudentService;
import cn.xfywz.guozespring.util.PageResult;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Service
public class StudentServiceImpl implements StudentService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private DatabaseUtil databaseUtil;


    /**
     * 构建学生查询SQL（所有字段）
     * @return SQL
     */
    private String buildStudentQuerySql() {
        return """
        SELECT id, number, name, idCard, gender, entryYear, mobile, weChat, email,
               intro, classId, collegeId, avatar, password, point, area, province,
               city, region, address, schoolId, signature, studyCourse, discJoin,
               discReply, studyDuration, completeCourse, circleCount
        FROM yee_student
        """;
    }

    /**
     * 学生查询条件（当前无条件，但保留扩展能力）
     * @param queryBuilder 查询构建器
     */
    private void applyStudentQueryConditions(QueryBuilder queryBuilder) {
        // 目前无额外条件，可根据业务需要后续添加
        log.debug("学生查询无条件限制");
    }

// ================ 执行方法 ================

    @Override
    public Result selectAll(int PageSize, int PageNum, int SchoolId) throws Exception {
        // 1. 校验学校存在且有效
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("allow", 1).eq("id", SchoolId);
        List<SlSchool> slSchool = slSchoolMapper.selectList(queryWrapper);
        if (slSchool.isEmpty()) {
            return Result.error("没有此学校");
        }

        // 2. 使用DatabaseUtil进行分页查询（内部处理连接和资源）
        PageResult<YeeStudent> pageResult = databaseUtil.query(SchoolId)
                .sql(buildStudentQuerySql())
                .apply(this::applyStudentQueryConditions)
                .orderBy("id DESC")
                .page(rs -> {
                    // 结果集映射为YeeStudent对象
                    YeeStudent yeeStudent = new YeeStudent();
                    try {
                        yeeStudent.setId(rs.getLong("id"));
                        yeeStudent.setNumber(rs.getString("number"));
                        yeeStudent.setName(rs.getString("name"));
                        yeeStudent.setIdCard(rs.getString("idCard"));
                        yeeStudent.setGender(rs.getString("gender"));
                        yeeStudent.setEntryYear(rs.getLong("entryYear"));
                        yeeStudent.setMobile(rs.getString("mobile"));
                        yeeStudent.setWeChat(rs.getString("weChat"));
                        yeeStudent.setEmail(rs.getString("email"));
                        yeeStudent.setIntro(rs.getString("intro"));
                        yeeStudent.setClassId(rs.getLong("classId"));
                        yeeStudent.setCollegeId(rs.getLong("collegeId"));
                        yeeStudent.setAvatar(rs.getString("avatar"));
                        yeeStudent.setPassword(rs.getString("password"));
                        yeeStudent.setPoint(rs.getLong("point"));
                        yeeStudent.setArea(rs.getString("area"));
                        yeeStudent.setProvince(rs.getLong("province"));
                        yeeStudent.setCity(rs.getLong("city"));
                        yeeStudent.setRegion(rs.getLong("region"));
                        yeeStudent.setAddress(rs.getString("address"));
                        yeeStudent.setSchoolId(rs.getLong("schoolId"));
                        yeeStudent.setSignature(rs.getString("signature"));
                        // studyCourse 特殊处理
                        String studyCourseStr = rs.getString("studyCourse");
                        if (studyCourseStr != null && !studyCourseStr.isEmpty()) {
                            yeeStudent.setStudyCourse(Long.parseLong(studyCourseStr));
                        } else {
                            yeeStudent.setStudyCourse(0L);
                        }
                        yeeStudent.setDiscJoin(rs.getLong("discJoin"));
                        yeeStudent.setDiscReply(rs.getLong("discReply"));
                        yeeStudent.setStudyDuration(rs.getLong("studyDuration"));
                        yeeStudent.setCompleteCourse(rs.getLong("completeCourse"));
                        yeeStudent.setCircleCount(rs.getLong("circleCount"));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    return yeeStudent;
                }, PageNum, PageSize);

        // 3. 返回结果
        return Result.success(pageResult.getRows(), pageResult.getTotal());
    }

//    @Override
//    public Result selectAll(int PageSize, int PageNum,int SchoolId) throws Exception {
//        List<YeeStudent> students = new ArrayList<>();
//        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("allow",1).eq("id",SchoolId);
//        List<SlSchool> slSchool =slSchoolMapper.selectList(queryWrapper);
//        if (slSchool.isEmpty()){
//            return Result.error("没有此学校");
//        }else {
//            SlSchool slSchool1 = slSchool.get(0);
//            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool1);
//            int offset = (PageNum - 1) * PageSize;
//            String sql = "SELECT * FROM yee_student LIMIT ? OFFSET ?";
//            String countSql = "SELECT count(*) FROM yee_student";
//            PreparedStatement countSt = connection.prepareStatement(countSql);
//            ResultSet countRs = countSt.executeQuery();
//            int totalCount = 0;
//            if (countRs.next()) { // 将指针移动到第一行（结果集一定有数据）
//                totalCount = countRs.getInt(1); // 获取第一列的整数值（即count(*)结果）
//            }
//            Long totalCount1 = (long) totalCount;
//            PreparedStatement st = connection.prepareStatement(sql);
//            st.setInt(1, PageSize);
//            st.setInt(2, offset);
//            ResultSet rs = st.executeQuery();
//            List<YeeStudent> yeeStudents = new java.util.ArrayList<>();
//            while (rs.next()) {
//                YeeStudent yeeStudent = new YeeStudent();
//                yeeStudent.setId(rs.getLong("id"));
//                yeeStudent.setNumber(rs.getString("number"));
//                yeeStudent.setName(rs.getString("name"));
//                yeeStudent.setIdCard(rs.getString("idCard"));
//                yeeStudent.setGender(rs.getString("gender"));
//                yeeStudent.setEntryYear(rs.getLong("entryYear"));
//                yeeStudent.setMobile(rs.getString("mobile"));
//                yeeStudent.setWeChat(rs.getString("weChat"));
//                yeeStudent.setEmail(rs.getString("email"));
//                yeeStudent.setIntro(rs.getString("intro"));
//                yeeStudent.setClassId(rs.getLong("classId"));
//                yeeStudent.setCollegeId(rs.getLong("collegeId"));
//                yeeStudent.setAvatar(rs.getString("avatar"));
//                yeeStudent.setPassword(rs.getString("password"));
//                yeeStudent.setPoint(rs.getLong("point"));
//                yeeStudent.setArea(rs.getString("area"));
//                yeeStudent.setProvince(rs.getLong("province"));
//                yeeStudent.setCity(rs.getLong("city"));
//                yeeStudent.setRegion(rs.getLong("region"));
//                yeeStudent.setAddress(rs.getString("address"));
//                yeeStudent.setSchoolId(rs.getLong("schoolId"));
//                yeeStudent.setSignature(rs.getString("signature"));
//                String studyCourseStr = rs.getString("studyCourse");
//                if (studyCourseStr != null && !studyCourseStr.isEmpty()) {
//                    yeeStudent.setStudyCourse(Long.parseLong(studyCourseStr));
//                } else {
//                    yeeStudent.setStudyCourse(0L); // 或者其他默认值或处理逻辑
//                }
//                yeeStudent.setDiscJoin(rs.getLong("discJoin"));
//                yeeStudent.setDiscReply(rs.getLong("discReply"));
//                yeeStudent.setStudyDuration(rs.getLong("studyDuration"));
//                yeeStudent.setCompleteCourse(rs.getLong("completeCourse"));
//                yeeStudent.setCircleCount(rs.getLong("circleCount"));
//                yeeStudents.add(yeeStudent);
//            }
//            st.close();
//            rs.close();
//            connection.close();
//            return Result.success((Object) yeeStudents,totalCount1);
//        }
//    }

    @Override
    public Result selectById(int schoolId ,int id) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("allow",1).eq("id",schoolId);
        List<SlSchool> slSchool =slSchoolMapper.selectList(queryWrapper);
        if (slSchool.isEmpty()){
            return Result.error("没有此学校");
        }else {
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool.get(0));
            String sql = "SELECT * FROM yee_student WHERE id = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            YeeStudent yeeStudent = new YeeStudent();
            while (rs.next()) {
                yeeStudent.setId(rs.getLong("id"));
                yeeStudent.setNumber(rs.getString("number"));
                yeeStudent.setName(rs.getString("name"));
                yeeStudent.setIdCard(rs.getString("idCard"));
                yeeStudent.setGender(rs.getString("gender"));
                yeeStudent.setEntryYear(rs.getLong("entryYear"));
                yeeStudent.setMobile(rs.getString("mobile"));
                yeeStudent.setWeChat(rs.getString("weChat"));
                yeeStudent.setEmail(rs.getString("email"));
                yeeStudent.setIntro(rs.getString("intro"));
                yeeStudent.setClassId(rs.getLong("classId"));
                yeeStudent.setCollegeId(rs.getLong("collegeId"));
                yeeStudent.setAvatar(rs.getString("avatar"));
                yeeStudent.setArea(rs.getString("area"));
                yeeStudent.setProvince(rs.getLong("province"));
                yeeStudent.setCity(rs.getLong("city"));
                yeeStudent.setRegion(rs.getLong("region"));
                yeeStudent.setAddress(rs.getString("address"));
                yeeStudent.setSchoolId(rs.getLong("schoolId"));
                yeeStudent.setSignature(rs.getString("signature"));
            }
            st.close();
            rs.close();
            connection.close();
            if (yeeStudent.getName() == null) {
                return Result.error("没有此学生");
            }else {
                return Result.success(yeeStudent);
            }
        }
    }

    @Override
    public Result passwordRandom(int schoolId , int id) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("allow",1).eq("id",schoolId);
        List<SlSchool> slSchool =slSchoolMapper.selectList(queryWrapper);
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        Random random = new Random();
        String password = String.valueOf(random.nextInt(1000000));
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool.get(0));
        String sql = "UPDATE yee_student SET password = ? WHERE id = ?";
        PreparedStatement st = connection.prepareStatement(sql);
        st.setString(1, bCryptPasswordEncoder.encode(password));
        st.setInt(2, id);
        int update = st.executeUpdate();
        st.close();
        connection.close();
        if (update == 0) {
            return Result.error("修改失败");
        }else {
            return Result.success("修改成功", password);
        }
    }

    @Override
    public Result passwordReset(int schoolId, List<Integer> id) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("allow",1).eq("id",schoolId);
        List<SlSchool> slSchool =slSchoolMapper.selectList(queryWrapper);
        for (Integer integer : id) {
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            String password = "a123456";
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool.get(0));
            String sql = "UPDATE yee_student SET password = ? WHERE id = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setString(1, bCryptPasswordEncoder.encode(password));
            st.setInt(2, integer);
            int status=st.executeUpdate();
            st.close();
            connection.close();
            if (status == 0) {
                return Result.error("修改失败");
            }
        }
        return Result.success("更新成功", "a123456");
    }

    @Override
    public Result selectLikeName(StuLike like) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("allow", 1).eq("id", like.getSchoolId());
        List<SlSchool> slSchool = slSchoolMapper.selectList(queryWrapper);
        if (slSchool.isEmpty()) {
            return Result.error("没有此学校");
        } else {
            SlSchool slSchool1 = slSchool.get(0);
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool1);

            StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_student WHERE 1=1");

            List<Object> params = new ArrayList<>();

            if (like != null) {
                // 动态添加 number, name, email, mobile 的模糊查询条件（OR 关系）
                if (like.getLike() != null && !like.getLike().isEmpty()) {
                    sqlBuilder.append(" AND (number LIKE ? OR name LIKE ? OR email LIKE ? OR mobile LIKE ?)");
                    String likePattern = "%" + like.getLike() + "%";
                    params.add(likePattern);
                    params.add(likePattern);
                    params.add(likePattern);
                    params.add(likePattern);
                }

                // 动态添加 idCard 查询条件
                if (like.getIdCard() != null && !like.getIdCard().isEmpty()) {
                    sqlBuilder.append(" AND idCard LIKE ?");
                    params.add("%" + like.getIdCard() + "%");
                }

                // 动态添加 gender 查询条件
                if (like.getGender() != null && !like.getGender().isEmpty()) {
                    sqlBuilder.append(" AND gender LIKE ?");
                    params.add("%" + like.getGender() + "%");
                }

                // 动态添加 classId 查询条件
                if (like.getClassId() != null && !like.getClassId().isEmpty()) {
                    sqlBuilder.append(" AND classId LIKE ?");
                    params.add("%" + like.getClassId() + "%");
                }

                // 动态添加 collegeId 查询条件
                if (like.getCollegeId() != null && !like.getCollegeId().isEmpty()) {
                    sqlBuilder.append(" AND collegeId LIKE ?");
                    params.add("%" + like.getCollegeId() + "%");
                }

                // 动态添加 entryYear 查询条件
                if (like.getEntryYear() != null && !like.getEntryYear().isEmpty()) {
                    sqlBuilder.append(" AND entryYear = ?");
                    params.add(like.getEntryYear());
                }
            }

            PreparedStatement st = connection.prepareStatement(sqlBuilder.toString());

            for (int i = 0; i < params.size(); i++) {
                st.setObject(i + 1, params.get(i));
            }

            ResultSet rs = st.executeQuery();

            List<YeeStudent> students = new ArrayList<>();
            while (rs.next()) {
                YeeStudent student = new YeeStudent();
                student.setId(rs.getLong("id"));
                student.setNumber(rs.getString("number"));
                student.setName(rs.getString("name"));
                student.setIdCard(rs.getString("idCard"));
                student.setGender(rs.getString("gender"));
                student.setEntryYear(rs.getLong("entryYear"));
                student.setMobile(rs.getString("mobile"));
                student.setWeChat(rs.getString("weChat"));
                student.setEmail(rs.getString("email"));
                student.setIntro(rs.getString("intro"));
                student.setClassId(rs.getLong("classId"));
                student.setCollegeId(rs.getLong("collegeId"));
                student.setAvatar(rs.getString("avatar"));
                student.setPassword(rs.getString("password"));
                student.setPoint(rs.getLong("point"));
                student.setArea(rs.getString("area"));
                student.setProvince(rs.getLong("province"));
                student.setCity(rs.getLong("city"));
                student.setRegion(rs.getLong("region"));
                student.setAddress(rs.getString("address"));
                student.setSchoolId(rs.getLong("schoolId"));
                student.setSignature(rs.getString("signature"));

                String studyCourseStr = rs.getString("studyCourse");
                if (studyCourseStr != null && !studyCourseStr.isEmpty()) {
                    student.setStudyCourse(Long.parseLong(studyCourseStr));
                } else {
                    student.setStudyCourse(0L); // 默认值
                }

                student.setDiscJoin(rs.getLong("discJoin"));
                student.setDiscReply(rs.getLong("discReply"));
                student.setStudyDuration(rs.getLong("studyDuration"));
                student.setCompleteCourse(rs.getLong("completeCourse"));
                student.setCircleCount(rs.getLong("circleCount"));
                students.add(student);
            }

            rs.close();
            st.close();
            connection.close();

            return Result.success(students, (long) students.size());
        }
    }
}
