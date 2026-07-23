package cn.xfywz.guozespring.service.student.serviceImpl;

import cn.dev33.satoken.stp.StpUtil;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.student.LoginService;
import cn.xfywz.guozespring.util.JwtTokenUtil;
import cn.xfywz.guozespring.util.RedisUtils;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Slf4j
@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Autowired
    RedisUtils redisUtils = new RedisUtils();

    @Override
    public Result login(String number, String password, int schoolId) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        YeeStudent yeeStudent = null;

        // 1. 数据库操作：查完数据立刻释放连接
        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
             PreparedStatement st = connection.prepareStatement("SELECT * FROM yee_student WHERE number = ?");
        ) {
            st.setString(1, number);
            try (ResultSet rs = st.executeQuery()) {
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
                    yeeStudent.setPassword(rs.getString("password"));
                    yeeStudent.setPoint(rs.getLong("point"));
                    yeeStudent.setArea(rs.getString("area"));
                    yeeStudent.setProvince(rs.getLong("province"));
                    yeeStudent.setCity(rs.getLong("city"));
                    yeeStudent.setRegion(rs.getLong("region"));
                    yeeStudent.setAddress(rs.getString("address"));
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
                }
            }
        }
        // 确保连接无论如何都能归还到池中

        // 2. 用户不存在的判断（此时连接已释放）
        if (yeeStudent == null) {
            return Result.error("用户不存在");
        }

        // 3. 耗时的密码验证放在连接关闭后，避免误报泄漏
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (encoder.matches(password, yeeStudent.getPassword())) {
            String token = JwtTokenUtil.getToken(yeeStudent);
            String jti = JwtTokenUtil.extractJti(token);
            // 不再全量清理旧 token，避免多端登录互踢导致考试中断
            // 旧 token 通过 Redis TTL 自然过期（24h），登出时仍会清理
            redisUtils.set(JwtTokenUtil.buildTokenRedisKey(yeeStudent.getNumber(), jti), token, JwtTokenUtil.TOKEN_EXPIRE_SECONDS);
            return Result.success("登录成功", token);
        } else {
            return Result.error("密码错误");
        }
    }

//    @Override
//    public Result login(String number, String password, int schoolId) throws Exception {
//        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
//        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
//        PreparedStatement st = null;
//        ResultSet rs = null;
//        try {
//            String sql = "SELECT * FROM yee_student WHERE number = ?";
//            st = connection.prepareStatement(sql);
//            st.setString(1, number);
//            rs = st.executeQuery();
//            YeeStudent yeeStudent = new YeeStudent();
//            if (rs.next()) {
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
//                yeeStudent.setTipPass(rs.getLong("tipPass"));
//                yeeStudent.setSignature(rs.getString("signature"));
//                yeeStudent.setStudyDuration(rs.getLong("studyDuration"));
//                yeeStudent.setDiscJoin(rs.getLong("discJoin"));
//                yeeStudent.setDiscReply(rs.getLong("discReply"));
//                yeeStudent.setCompleteCourse(rs.getLong("completeCourse"));
//                yeeStudent.setStudyCourse(rs.getLong("studyCourse"));
//                yeeStudent.setCircleCount(rs.getLong("circleCount"));
//                yeeStudent.setErrorCount(rs.getLong("errorCount"));
//                yeeStudent.setErrorTime(rs.getLong("errorTime"));
//                yeeStudent.setPassport(rs.getString("passport"));
//            }
//            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
//            if (bCryptPasswordEncoder.matches(password, yeeStudent.getPassword())) {
//                String token = JwtTokenUtil.getToken(yeeStudent);
////                redisUtils.set(yeeStudent.getNumber(), token);
//                redisUtils.set(yeeStudent.getNumber(), token, 86400);
//                return Result.success("登录成功", token);
//            } else {
//                return Result.error("密码错误");
//            }
//        } finally {
//            if (rs != null) {
//                try {
//                    rs.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            if (st != null) {
//                try {
//                    st.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            if (connection != null) {
//                try {
//                    connection.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }


    @Override
    public Result logout(String number) {
        if (number == null || number.trim().isEmpty()) {
            return Result.error("学号不能为空");
        }

        try {
            cleanupOldStudentTokens(number);
            return Result.success("退出登录成功");
        } catch (Exception e) {
            log.error("退出登录失败: number={}", number, e);
            return Result.error("退出登录失败：系统异常");
        }
    }

    /** 清理该学生的所有 token key */
    private void cleanupOldStudentTokens(String number) {
        try {
            String pattern = String.format(JwtTokenUtil.TOKEN_REDIS_KEY_PATTERN, number);
            for (String key : redisUtils.keys(pattern)) {
                redisUtils.delete(key);
            }
        } catch (Exception e) {
            log.warn("清理学生旧token失败: number={}", number, e);
        }
    }

}
