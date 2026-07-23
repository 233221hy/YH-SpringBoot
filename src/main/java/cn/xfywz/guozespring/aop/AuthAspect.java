package cn.xfywz.guozespring.aop;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.CurrentUserUtil;
import cn.xfywz.guozespring.util.JwtTokenUtil;
import cn.xfywz.guozespring.util.Result;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

/**
 * 权限鉴权 AOP 切面。
 * 拦截所有标记了 {@link RequireAuth} 的方法，自动完成 JWT token + schoolId 校验，
 * 替代每个 Controller 方法中手动编写的 {@code AuthTokenUtil.verifyToken()} 重复代码。
 */
@Slf4j
@Aspect
@Component
public class AuthAspect {


    /** 匹配带 @RequireAuth 的方法，或类上有 @RequireAuth 的方法 */
    @Around("@annotation(cn.xfywz.guozespring.annotation.RequireAuth) || @within(cn.xfywz.guozespring.annotation.RequireAuth)")
    public Object checkAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取 HTTP 请求
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Result.error("无法获取请求上下文");
        }
        HttpServletRequest request = attributes.getRequest();

        // 2. 提取 Authorization token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            return Result.error("非法访问");
        }

        // 3. 提取 schoolId
        int schoolId = extractSchoolId(joinPoint);
        if (schoolId <= 0) {
            log.warn("无法从方法参数中提取 schoolId: {}.{}",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName());
            return Result.error("非法访问：无法获取学校ID");
        }

        // 4. 鉴权校验
        if (AuthTokenUtil.verifyToken(token, schoolId)) {
            // 鉴权通过后提取 subject JSON 和 userId 存入 request，供业务层通过 CurrentUserUtil 读取
            try {
                Claims claims = JwtTokenUtil.parseToken(token);
                String subject = claims.get("sub").toString();
                request.setAttribute(CurrentUserUtil.CURRENT_USER_SUBJECT_ATTR, subject);
            } catch (Exception ignored) {
                // subject 解析失败不影响主流程
            }
            Long userId = AuthTokenUtil.extractUserId(token);
            if (userId != null) {
                request.setAttribute(CurrentUserUtil.CURRENT_USER_ID_ATTR, userId);
            }
            return joinPoint.proceed();
        } else {
            return Result.error("非法访问");
        }
    }

    /**
     * 从方法参数中提取 schoolId，按优先级尝试多种策略。
     */
    private int extractSchoolId(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Parameter[] parameters = method.getParameters();
        Object[] args = joinPoint.getArgs();

        for (int i = 0; i < parameters.length; i++) {
            // 策略1：参数名直接叫 "schoolId"（@RequestParam 场景）
            if ("schoolId".equals(parameters[i].getName()) && args[i] instanceof Number) {
                return ((Number) args[i]).intValue();
            }

            // 策略2：跳过 Spring 框架类（HttpServletRequest、HttpServletResponse 等）
            if (args[i] == null || isSpringType(args[i].getClass())) {
                continue;
            }

            // 策略3：参数有 getSchoolId() 方法（DTO 场景）
            Integer id = callGetSchoolId(args[i]);
            if (id != null) {
                return id;
            }

            // 策略4：参数是 Map 且包含 "schoolId" key
            if (args[i] instanceof Map) {
                Object sid = ((Map<?, ?>) args[i]).get("schoolId");
                if (sid instanceof Number) {
                    return ((Number) sid).intValue();
                }
            }
        }
        return 0;
    }

    /** 通过反射调用对象的 getSchoolId() 方法 */
    private Integer callGetSchoolId(Object obj) {
        try {
            Method m = obj.getClass().getMethod("getSchoolId");
            Object result = m.invoke(obj);
            if (result instanceof Number) {
                return ((Number) result).intValue();
            }
        } catch (Exception e) {
            // 没有该方法或调用失败，忽略
        }
        return null;
    }

    /** 判断是否为 Spring/Java 基础类型（不需要在这些参数中找 schoolId） */
    private boolean isSpringType(Class<?> clazz) {
        String name = clazz.getName();
        return name.startsWith("jakarta.servlet") || name.startsWith("org.springframework");
    }
}