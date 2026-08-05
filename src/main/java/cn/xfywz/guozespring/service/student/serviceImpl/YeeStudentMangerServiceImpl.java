package cn.xfywz.guozespring.service.student.serviceImpl;

import cn.xfywz.guozespring.util.RandomPwdUtil;
import cn.xfywz.guozespring.entity.dto.ResetPasswordDTO;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.entity.vo.StudentStats;
import cn.xfywz.guozespring.exception.BusinessException;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.file.MailService;
import cn.xfywz.guozespring.service.student.YeeStudentMangerService;
import cn.xfywz.guozespring.util.JwtTokenUtil;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.jsonwebtoken.Claims;
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
import java.util.Optional;

import static cn.xfywz.guozespring.util.EncodePasswordUtil.encodePassword;

@Slf4j
@Service
public class YeeStudentMangerServiceImpl implements YeeStudentMangerService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private DatabaseUtil databaseUtil;
    @Autowired
    private MailService mailService;


    @Override
    public StudentStats getStudentStats(int schoolId, long studentId) {
        // 1. 校验学校
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return null;
        }

        Connection conn = null;
        PreparedStatement st = null;
        ResultSet rs = null;

        try {
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 查询基础统计
            String baseSql = """
            SELECT
                s.id,
                s.NAME,
                s.number,
                s.signature,
                COUNT(DISTINCT sc.id) AS course_count,
                COUNT(DISTINCT ep.id) AS evaluation_count,
                COUNT(DISTINCT sq.id) AS circle_count,
                COUNT(DISTINCT dt.id) AS discussion_count
            FROM
                yee_student s
                LEFT JOIN yee_course_student sc ON s.id = sc.studentId
                LEFT JOIN yee_work_evaluation ep ON s.id = ep.markId
                LEFT JOIN yee_happy_circle sq ON s.id = sq.userId
                LEFT JOIN yee_discuss_reply dt ON s.id = dt.userId
            WHERE
                s.id = ?
            GROUP BY
                s.id, s.NAME
            """;

            st = conn.prepareStatement(baseSql);
            st.setLong(1, studentId);
            rs = st.executeQuery();

            if (!rs.next()) {
                return null; // 未找到学生
            }

            StudentStats stats = new StudentStats();
            stats.setId(rs.getLong("id"));
            stats.setNumber(rs.getString("number"));
            stats.setSignature(rs.getString("signature"));
            stats.setName(rs.getString("NAME"));
            stats.setCourseCount(rs.getLong("course_count"));
            stats.setEvaluationCount(rs.getLong("evaluation_count"));
            stats.setCircleCount(rs.getLong("circle_count"));
            stats.setDiscussionCount(rs.getLong("discussion_count"));

            // 关闭第一轮资源
            rs.close();
            st.close();

            // 单独查询学习时长
            String durationSql = """
            SELECT COALESCE(SUM(duration), 0) AS total_study_duration
            FROM yee_study_total
            WHERE userId = ?
            """;

            st = conn.prepareStatement(durationSql);
            st.setLong(1, studentId);
            rs = st.executeQuery();

            if (rs.next()) {
                stats.setTotalStudyDuration(rs.getInt("total_study_duration"));
            } else {
                stats.setTotalStudyDuration(0);
            }

            return stats;

        } catch (Exception e) {
            return null; // 出错也返回 null
        } finally {
            // 手动关闭资源
            if (rs != null) {
                try {rs.close();} catch (SQLException e) {}
            }
            if (st != null) {
                try {st.close();} catch (SQLException e) {}
            }
            if (conn != null) {
                try {conn.close();} catch (SQLException e) {}
            }
        }
    }

    @Override
    public Result studentInfoUpdate(String Authorization, YeeStudent yeeStudent) throws Exception {
        // 基本参数校验
        if (yeeStudent.getSchoolId() < 0) {
            return Result.error("缺少或非法的schoolId");
        }
        if (yeeStudent.getId() < 0) {
            return Result.error("缺少或非法的id");
        }
        //根据令牌获取数据库的用户信息
        YeeStudent currentYee = getInfo(Authorization);

        SlSchool slSchool = slSchoolMapper.selectById((int) currentYee.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 构建Sql语句与参数
        StringBuilder sql = new StringBuilder("UPDATE yee_student SET ");
        List<Object> parameters = new ArrayList<>();
        if (yeeStudent.getAvatar() != null) {
            sql.append("`avatar` = ?, ");
            parameters.add(yeeStudent.getAvatar());
        }
        if (yeeStudent.getGender() != null) {
            sql.append("`gender` = ?, ");
            parameters.add(yeeStudent.getGender());
        }
        if (yeeStudent.getWeChat() != null) {
            sql.append("`weChat` = ?, ");
            parameters.add(yeeStudent.getWeChat());
        }
        if (yeeStudent.getProvince() != 0) {
            sql.append("`province` = ?, ");
            parameters.add(yeeStudent.getProvince());
        }
        if (yeeStudent.getCity() != 0) {
            sql.append("`city` = ?, ");
            parameters.add(yeeStudent.getCity());
        }
        if (yeeStudent.getRegion() != 0) {
            sql.append("`region` = ?, ");
            parameters.add(yeeStudent.getRegion());
        }
        if (yeeStudent.getAddress() != null) {
            sql.append("`address` = ?, ");
            parameters.add(yeeStudent.getAddress());
        }
        if (yeeStudent.getSignature() != null) {
            sql.append("`signature` = ?, ");
            parameters.add(yeeStudent.getSignature());
        }
        if (yeeStudent.getIntro() != null) {
            sql.append("`intro` = ?, ");
            parameters.add(yeeStudent.getIntro());
        }

        // 删除最后多余的逗号和空格
        if (!parameters.isEmpty()) {
            sql.delete(sql.length() - 2, sql.length());
        } else {
            return Result.error("没有可更新的字段");
        }
        // 添加WHERE条件，并追加id参数
        sql.append(" WHERE id = ?");
        parameters.add(yeeStudent.getId());

        // 获取连接、绑定参数并执行更新（使用try-with-resources避免资源泄漏）
        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement st = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                st.setObject(i + 1, parameters.get(i));
            }
            int rowsUpdated = st.executeUpdate();
            if (rowsUpdated > 0) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败：未找到匹配的记录");
            }
        }
    }

    @Override
    public YeeStudent getInfo(String authorization) throws Exception {
        if (authorization == null) {
            return null;
        }

        // 1. 解析 JWT 获取身份信息
        Claims claims = JwtTokenUtil.parseToken(authorization);
        if (claims == null) {
            return null;
        }

        String subject = claims.getSubject();
        if (subject == null || subject.trim().isEmpty()) {
            return null;
        }

        YeeStudent tempInfo;
        try {
            tempInfo = JSON.parseObject(subject, YeeStudent.class);
            if (tempInfo == null || tempInfo.getId() <= 0) {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // 2. 验证学校是否允许访问
        SlSchool slSchool = slSchoolMapper.selectById(tempInfo.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return null;
        }

        YeeStudent yeeStudent = null;

        // 3. ✅ 从从库查询 yee_student 完整信息
        String studentSql = "SELECT * FROM yee_student WHERE id = ?";
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(studentSql)) {

            ps.setLong(1, tempInfo.getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    yeeStudent = new YeeStudent();
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
                    yeeStudent.setPoint(rs.getLong("point"));
                    yeeStudent.setArea(rs.getString("area"));
                    yeeStudent.setProvince(rs.getLong("province"));
                    yeeStudent.setCity(rs.getLong("city"));
                    yeeStudent.setRegion(rs.getLong("region"));
                    yeeStudent.setAddress(rs.getString("address"));
                    yeeStudent.setAddTime(rs.getTimestamp("addTime"));
                    yeeStudent.setSchoolId(rs.getLong("schoolId"));
                    yeeStudent.setTipPass(rs.getLong("tipPass"));
                    yeeStudent.setSignature(rs.getString("signature"));
                    yeeStudent.setStudyDuration(rs.getLong("studyDuration"));
                    yeeStudent.setDiscJoin(rs.getLong("discJoin"));
                    yeeStudent.setDiscReply(rs.getLong("discReply"));
                    yeeStudent.setCompleteCourse(rs.getLong("completeCourse"));
                    yeeStudent.setStudyCourse(rs.getLong("studyCourse"));
                    yeeStudent.setCircleCount(rs.getLong("circleCount"));
                    yeeStudent.setErrorCount(rs.getLong("errorCount"));
                    yeeStudent.setErrorTime(rs.getLong("errorTime"));
                    yeeStudent.setPassport(rs.getString("passport"));
                    yeeStudent.setAddDate(rs.getDate("addDate"));

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // 如果没查到学生
        if (yeeStudent == null) {
            return null;
        }

        // 4. 查询班级名称
        String classSql = "SELECT name FROM yee_classes WHERE id = ?";
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(classSql)) {
            ps.setLong(1, yeeStudent.getClassId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    yeeStudent.setClassName(rs.getString("name"));
                } else {
                    yeeStudent.setClassName("未知班级");
                }
            }
        } catch (Exception e) {
            yeeStudent.setClassName("未知班级");
        }

        // 5. 查询学院名称
        String collegeSql = "SELECT name FROM yee_college WHERE id = ?";
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(collegeSql)) {
            ps.setLong(1, yeeStudent.getCollegeId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    yeeStudent.setCollegeName(rs.getString("name"));
                } else {
                    yeeStudent.setCollegeName("未知学院");
                }
            }
        } catch (Exception e) {
            yeeStudent.setCollegeName("未知学院");
        }

        return yeeStudent;
    }

    @Override
    public Result updatePhone(String mobile, String Authorization) throws Exception {

        //根据令牌获取数据库的用户信息
        YeeStudent currentYee = getInfo(Authorization);

        SlSchool slSchool = slSchoolMapper.selectById((int) currentYee.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        String sql = "UPDATE yee_student SET mobile = ? WHERE id = ?";
        // 执行SQL更新语句
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 设置参数
            ps.setString(1, mobile);
            ps.setLong(2, currentYee.getId());

            // 执行更新
            int rowsUpdated = ps.executeUpdate();

            // 检查更新结果
            if (rowsUpdated == 0) {
                return Result.error("更新失败：未找到匹配的学生记录或学生不属于该学校");
            } else {
                return Result.success("手机号修改成功");
            }
        } catch (Exception e) {
            return Result.error("用户不存在");
        }
    }


    @Override
    public Result updateEmail(String email, String Authorization) throws Exception {
        //根据令牌获取数据库的用户信息
        YeeStudent currentYee = getInfo(Authorization);
        SlSchool slSchool = slSchoolMapper.selectById((int) currentYee.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        String sql = "UPDATE yee_student SET email = ? WHERE id = ?";
        // 执行SQL更新语句
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // 设置参数
            ps.setString(1, email);
            ps.setLong(2, currentYee.getId());

            // 执行更新
            int rowsUpdated = ps.executeUpdate();

            // 检查更新结果
            if (rowsUpdated == 0) {
                return Result.error("更新失败：未找到匹配的学生记录或学生不属于该学校");
            } else {
                return Result.success("邮箱修改成功");
            }
        } catch (Exception e) {
            return Result.error("用户不存在");
        }
    }


    @Override
    public Result infoUpdatePassword(String oldPassword, String newPassword, String Authorization) throws Exception {
        // 参数校验
        if (StringUtils.isEmpty(oldPassword)) {
            return Result.error("旧密码不能为空");
        }
        if (StringUtils.isEmpty(newPassword)) {
            return Result.error("新密码不能为空");
        }

        // 根据令牌获取数据库的用户信息
        YeeStudent currentYee = getInfo(Authorization);
        if (currentYee == null) {
            return Result.error("用户未登录或信息无效");
        }
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        SlSchool slSchool = slSchoolMapper.selectById((int) currentYee.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            connection.setAutoCommit(false);

            String getPasswordSql = "SELECT password FROM yee_student WHERE id = ?";
            try (PreparedStatement getPasswordSt = connection.prepareStatement(getPasswordSql)) {
                getPasswordSt.setLong(1, currentYee.getId());
                try (ResultSet rs = getPasswordSt.executeQuery()) {
                    if (!rs.next()) {
                        return Result.error("用户不存在");
                    }
                    String currentPassword = rs.getString("password");
                    if (currentPassword == null) {
                        return Result.error("用户密码数据异常");
                    }

                    // 判断旧密码是否正确
                    if (bCryptPasswordEncoder.matches(oldPassword, currentPassword)) {
                        // 更新密码
                        String updatePasswordSql = "UPDATE yee_student SET password = ? WHERE id = ?";
                        try (PreparedStatement updatePasswordSt = connection.prepareStatement(updatePasswordSql)) {
                            String encodedNewPassword = bCryptPasswordEncoder.encode(newPassword);
                            updatePasswordSt.setString(1, encodedNewPassword);
                            updatePasswordSt.setLong(2, currentYee.getId());

                            int rowsUpdated = updatePasswordSt.executeUpdate();
                            if (rowsUpdated > 0) {
                                connection.commit();
                                return Result.success("密码修改成功");
                            } else {
                                connection.rollback();
                                return Result.error("密码更新失败");
                            }
                        }
                    } else {
                        return Result.error("旧密码错误");
                    }
                }
            } catch (Exception e) {
                connection.rollback();
                return Result.error("密码修改失败");
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return Result.error("密码修改失败");
        }
    }

    @Override
    public Result forgetPassword(ResetPasswordDTO dto, int schoolId) {
        //根据学号+学校ID 查出数据库中对应学生的邮箱
        Optional<String> storedIdCardOpt = databaseUtil.query(schoolId)
                .sql("SELECT email FROM yee_student")
                .eq("schoolId", schoolId)
                .eq("number", dto.getStuNumber())
                .scalar(rs -> rs.getString("email"));

        //验证数据库邮箱与前端传入邮箱是否相等
        boolean emailMatch = storedIdCardOpt
                .filter(StringUtils::isNotBlank) // 先过滤：确保长度足够
                .map(dbEmail -> dbEmail.equals(dto.getEmail().trim())) // 比对邮箱是否相等
                .orElse(false); // 如果查不到 → 返回 false

        //对应学生邮箱为空时
        String dbEmail = storedIdCardOpt.get();
        if (StringUtils.isEmpty(dbEmail)) {
            return Result.error("该学生未绑定邮箱信息，无法重置密码");
        }

        // 邮箱与后端不匹配
        if (!emailMatch) {
            return Result.error("邮箱验证失败");
        }

        String stuName = databaseUtil.query(schoolId)
                .sql("SELECT name FROM yee_student")
                .eq("schoolId", schoolId)
                .eq("number", dto.getStuNumber())
                .scalar(rs -> rs.getString("name"))
                .orElse("同学");

        //生成6位随机数字密码
        String randomPwd = RandomPwdUtil.generateRandomCode(10);
        String encodePwd = encodePassword(randomPwd);

        //更新密码(通过邮箱)
        int rows = databaseUtil.update(Math.toIntExact(schoolId))
                .table("yee_student")
                .set("password",encodePwd)
                .eq("number",dto.getStuNumber())
                .eq("schoolId",schoolId)
                .update();

        if (rows == 0) {
            throw new BusinessException("密码更新失败，请稍后重试");
        }

        //发送邮件,将临时6位密码发送到学生邮箱
        String studentEmail = storedIdCardOpt.get();
        String emailContent = String.format("%s同学,您的新密码为: %s,请登录后及时修改重置密码",stuName,randomPwd);
        //调用邮件发送工具
        mailService.sendSimpleMail(studentEmail, emailContent,emailContent);

        return Result.success("密码重置成功,新密码已发送到预留邮箱,注意查收");


    }
}
