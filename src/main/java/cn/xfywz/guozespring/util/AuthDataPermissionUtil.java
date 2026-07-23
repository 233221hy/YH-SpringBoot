package cn.xfywz.guozespring.util;

import cn.dev33.satoken.stp.StpUtil;
import cn.xfywz.guozespring.constant.DataAuth;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class AuthDataPermissionUtil {

    private static final Logger log = LoggerFactory.getLogger(AuthDataPermissionUtil.class);

    // ===================== 从 Session 拿当前用户 ID =====================
    public static Long getCurrentUserId() {
        // 优先从 Sa-Token Session 读取
        try {
            Object loginId = StpUtil.getLoginId();
            LoginUser loginUser = (LoginUser) StpUtil.getSession().get(loginId.toString());
            if (loginUser != null && loginUser.getYeeManage() != null) {
                return loginUser.getYeeManage().getId();
            }
        } catch (Exception e) {
            log.debug("从 Sa-Token Session 获取用户ID失败，尝试降级到 SecurityContext");
        }
        // 降级：从 SecurityContext 读取（JWT 过滤器已设置）
        LoginUser loginUser = getLoginUserFromSecurityContext();
        if (loginUser != null && loginUser.getYeeManage() != null) {
            return loginUser.getYeeManage().getId();
        }
        throw new RuntimeException("用户未登录：Sa-Token Session 和 SecurityContext 中均无用户信息");
    }

    // ===================== 核心：从 Session 拿 dataAuth =====================
    public static DataAuth getCurrentDataAuth() {
        // 优先从 Sa-Token Session 读取
        try {
            Integer dataAuthValue = (Integer) StpUtil.getSession().get("dataAuth");
            if (dataAuthValue != null) {
                return DataAuth.fromValue(dataAuthValue);
            }
        } catch (Exception e) {
            log.debug("从 Sa-Token Session 获取 dataAuth 失败，尝试降级到 JWT");
        }
        // 降级：从 SecurityContext/JWT 中的 LoginUser.dataAuth 读取
        LoginUser loginUser = getLoginUserFromSecurityContext();
        if (loginUser != null && loginUser.getDataAuth() != null) {
            return DataAuth.fromValue(loginUser.getDataAuth());
        }
        // 最终降级：OWN（仅自己创建的课程/负责的班级）
        log.warn("无法获取 dataAuth，降级为 OWN");
        return DataAuth.OWN;
    }

    /** 从 Spring SecurityContext 获取 JWT 中解析出的 LoginUser */
    private static LoginUser getLoginUserFromSecurityContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return (LoginUser) auth.getPrincipal();
        }
        return null;
    }

    // ===================== 原有权限方法 100% 保留不动 =====================
    public static void buildDataPermission(
            StringBuilder sql,
            List<Object> params,
            String courseColumn,
            String classColumn
    ) {
        Long userId = getCurrentUserId();
        DataAuth dataAuth = getCurrentDataAuth();

        // 管理员：直接放行，不加任何条件
        if (dataAuth == DataAuth.ALL) {
            return;
        }

        sql.append(" AND ( ");
        // 课程创建者
        sql.append(" EXISTS (SELECT 1 FROM yee_course yc WHERE yc.id = ").append(courseColumn).append(" AND yc.createId = ?) ");
        params.add(userId);

        // 班级责任教师
        if (classColumn != null && !classColumn.isBlank()) {
            sql.append(" OR EXISTS (SELECT 1 FROM yee_course_class ycc WHERE ycc.id = ").append(classColumn).append(" AND ycc.teacherId = ?) ");
            params.add(userId);
        }
        sql.append(" ) ");
    }

    // ===================== 作业权限方法 不动 =====================
    public static void buildWorkClassPermission(StringBuilder sql, Long teacherId) {
        sql.append(" AND ( ");
        sql.append(" JSON_LENGTH(w.classList) = 0 ");
        sql.append(" OR ");
        sql.append(" EXISTS ( ");
        sql.append("   SELECT 1 FROM yee_course_class ycc ");
        sql.append("   WHERE ycc.teacherId = ").append(teacherId);
        sql.append("   AND JSON_CONTAINS(w.classList, CAST(ycc.id AS JSON)) ");
        sql.append(" ) ");
        sql.append(" ) ");
    }
}