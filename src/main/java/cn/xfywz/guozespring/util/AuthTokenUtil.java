package cn.xfywz.guozespring.util;

import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthTokenUtil {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenUtil.class);

    public static boolean verifyToken(String token, int schoolId) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            Claims claims = JwtTokenUtil.parseToken(token);
            if (claims == null) {
                return false;
            }

            String subject = claims.get("sub").toString();

            LoginUser loginUser = JSON.parseObject(subject, LoginUser.class);
            if (loginUser == null) {
                return verifyStudentToken(subject, schoolId);
            }

            if (loginUser.getSlManage() != null) {
                return loginUser.getSlManage().getSchoolId() == schoolId;
            } else if (loginUser.getYeeManage() != null) {
                return loginUser.getYeeManage().getSchoolId() == schoolId;
            } else {
                return verifyStudentToken(subject, schoolId);
            }
        } catch (Exception e) {
            log.error("JWT令牌校验失败: schoolId={}", schoolId, e);
            return false;
        }
    }

    private static boolean verifyStudentToken(String subject, int schoolId) {
        try {
            YeeStudent yeeStudent = JSON.parseObject(subject, YeeStudent.class);
            return yeeStudent != null && yeeStudent.getSchoolId() == schoolId;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从 JWT token 中提取当前用户 ID，不校验 schoolId。
     * 用于鉴权通过后获取用户身份（供业务层使用）。
     *
     * @return 用户 ID，解析失败返回 null
     */
    public static Long extractUserId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = JwtTokenUtil.parseToken(token);
            if (claims == null) {
                return null;
            }
            String subject = claims.get("sub").toString();
            // 先尝试解析为 LoginUser（教师/管理员）
            LoginUser loginUser = JSON.parseObject(subject, LoginUser.class);
            if (loginUser != null) {
                if (loginUser.getSlManage() != null) {
                    return loginUser.getSlManage().getId();
                }
                if (loginUser.getYeeManage() != null) {
                    return loginUser.getYeeManage().getId();
                }
            }
            // 再尝试解析为学生
            YeeStudent yeeStudent = JSON.parseObject(subject, YeeStudent.class);
            if (yeeStudent != null) {
                return yeeStudent.getId();
            }
            return null;
        } catch (Exception e) {
            log.error("从JWT提取userId失败", e);
            return null;
        }
    }

    /**
     * 从 JWT token 中提取 dataAuth（数据权限级别）。
     * 用于替代 StpUtil.getSession().get("dataAuth")，避免依赖 Sa-Token 会话。
     *
     * @return dataAuth 值，解析失败返回 null
     */
    public static Integer extractDataAuth(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Claims claims = JwtTokenUtil.parseToken(token);
            if (claims == null) {
                return null;
            }
            String subject = claims.get("sub").toString();
            LoginUser loginUser = JSON.parseObject(subject, LoginUser.class);
            if (loginUser != null) {
                return loginUser.getDataAuth();
            }
            return null;
        } catch (Exception e) {
            log.error("从JWT提取dataAuth失败", e);
            return null;
        }
    }
}
