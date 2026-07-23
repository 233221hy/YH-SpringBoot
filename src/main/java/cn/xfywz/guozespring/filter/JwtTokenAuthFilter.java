package cn.xfywz.guozespring.filter;

import cn.xfywz.guozespring.constant.RedisKeyConstants;
import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import cn.xfywz.guozespring.util.JwtTokenUtil;
import cn.xfywz.guozespring.util.RedisUtils;
import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

@Slf4j
@Component
public class JwtTokenAuthFilter extends OncePerRequestFilter {

    @Autowired
    private RedisUtils redisUtils;
    // Token有效期（秒），建议和你的JWT过期时间一致
    private static final long TOKEN_EXPIRE_SECONDS = 3600 * 24;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (uri.contains("/async_export_result")
                || uri.contains("/async_export_download")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        if (uri.equals("/user/study_session_heartbeat") || uri.equals("/user/study_session_end")) {
            filterChain.doFilter(request, response);
            return;
        }


        // 增加入口调试日志，便于回溯请求
        if (Objects.equals(uri, "/manage/login") || uri.startsWith("/user/") || uri.startsWith("/course/") || uri.startsWith("/test/") || uri.startsWith("/captcha") || uri.startsWith("/verify") || uri.startsWith("/manage/school_list_noLogin")) {
            if (Objects.equals(uri, "/user/login")) {
                filterChain.doFilter(request, response);
            } else if (uri.startsWith("/user/")) {
                String token = request.getHeader("Authorization");
                if (token == null) {
                    unauthorized(response, "缺少Authorization");
                } else {
                    try {
                        Claims claims = JwtTokenUtil.parseToken(token);
                        if (claims != null) {
                            Object sub = claims.getSubject();
                            YeeStudent yeeStudent = JSON.parseObject((String) sub, YeeStudent.class);
                            String jti = (String) claims.get("jti");
                            if (validateRedisToken(yeeStudent.getNumber(), jti, token)) {
                                request.setAttribute("currentStudent", yeeStudent);
                                extendTokenTtl(yeeStudent.getNumber(), jti);
                                updateUserLastActiveTime(RedisKeyConstants.USER_TYPE_STUDENT,
                                        yeeStudent.getSchoolId(),
                                        yeeStudent.getNumber());
                                // JWT 临期自动续签：剩余不足 1 小时，颁发新 token
                                YeeStudent studentForRefresh = yeeStudent;
                                refreshTokenIfNeeded(claims, yeeStudent.getNumber(), jti,
                                        () -> {
                                            try { return JwtTokenUtil.getToken(studentForRefresh); }
                                            catch (Exception e) { throw new RuntimeException(e); }
                                        }, response);
                                filterChain.doFilter(request, response);
                            } else {
                                unauthorized(response, "令牌已失效或不匹配");
                            }
                        } else {
                            unauthorized(response, "JWT claims为空");
                        }
                    } catch (SignatureException e) {
                        unauthorized(response, "JWT签名无效");
                        log.error("Invalid JWT signature: uri={}, msg={}", uri, e.getMessage());
                    } catch (ExpiredJwtException e) {
                        unauthorized(response, "JWT已过期");
                        log.warn("Expired JWT token: uri={}, msg={}", uri, e.getMessage());
                    } catch (MalformedJwtException e) {
                        unauthorized(response, "JWT格式错误");
                        log.warn("Malformed JWT token: uri={}, msg={}", uri, e.getMessage());
                    } catch (UnsupportedJwtException e) {
                        unauthorized(response, "不支持的JWT");
                        log.warn("Unsupported JWT token: uri={}, msg={}", uri, e.getMessage());
                    } catch (IllegalArgumentException e) {
                        unauthorized(response, "JWT claims为空字符串");
                        log.warn("JWT claims string is empty: uri={}, msg={}", uri, e.getMessage());
                    } catch (Exception e) {
                        unauthorized(response, "认证失败");
                        log.error("auth error: uri={}, error=", uri, e);
                    }
                }
            } else {
                filterChain.doFilter(request, response);
            }
        } else {
            // 管理员/教师端：jti 维度 Redis key + Redis 不可用时降级为 JWT 签名验证
            String token = request.getHeader("Authorization");
            if (token == null) {
                unauthorized(response, "缺少Authorization");
            } else {
                try {
                    Claims claims = JwtTokenUtil.parseToken(token);
                    if (claims != null) {
                        Object sub = claims.getSubject();
                        LoginUser loginUser = JSON.parseObject((String) sub, LoginUser.class);
                        String jti = (String) claims.get("jti");

                        if (loginUser.getSlManage() != null) {
                            String account = loginUser.getSlManage().getAccount();
                            Long schoolId = loginUser.getSlManage().getSchoolId();
                            if (validateRedisToken(account, jti, token)) {
                                extendTokenTtl(account, jti);
                                LoginUser loginUserForRefresh = loginUser;
                                refreshTokenIfNeeded(claims, account, jti,
                                        () -> {
                                            try { return JwtTokenUtil.getToken(loginUserForRefresh); }
                                            catch (Exception e) { throw new RuntimeException(e); }
                                        }, response);
                                setAuthenticationAndProceed(request, response, filterChain, loginUser,
                                        RedisKeyConstants.USER_TYPE_MANAGE, schoolId, account);
                            } else {
                                unauthorized(response, "令牌已失效或不匹配");
                            }
                        } else if (loginUser.getYeeManage() != null) {
                            String account = loginUser.getYeeManage().getAccount();
                            Long schoolId = null;
                            Integer schoolIdInteger = loginUser.getYeeManage().getSchoolId();
                            if (schoolIdInteger != null) {
                                schoolId = schoolIdInteger.longValue();
                            }
                            if (validateRedisToken(account, jti, token)) {
                                extendTokenTtl(account, jti);
                                refreshTokenIfNeeded(claims, account, jti,
                                        () -> {
                                            try { return JwtTokenUtil.getToken(loginUser); }
                                            catch (Exception e) { throw new RuntimeException(e); }
                                        }, response);
                                setAuthenticationAndProceed(request, response, filterChain, loginUser,
                                        RedisKeyConstants.USER_TYPE_MANAGE, schoolId, account);
                            } else {
                                unauthorized(response, "令牌已失效或不匹配");
                            }
                        } else {
                            unauthorized(response, "不支持的用户类型");
                        }
                    } else {
                        unauthorized(response, "JWT claims为空");
                    }
                } catch (SignatureException e) {
                    unauthorized(response, "JWT签名无效");
                    log.error("Invalid JWT signature: uri={}, msg={}", uri, e.getMessage());
                } catch (ExpiredJwtException e) {
                    unauthorized(response, "JWT已过期");
                    log.warn("Expired JWT token: uri={}, msg={}", uri, e.getMessage());
                } catch (MalformedJwtException e) {
                    unauthorized(response, "JWT格式错误");
                    log.warn("Malformed JWT token: uri={}, msg={}", uri, e.getMessage());
                } catch (UnsupportedJwtException e) {
                    unauthorized(response, "不支持的JWT");
                    log.warn("Unsupported JWT token: uri={}, msg={}", uri, e.getMessage());
                } catch (IllegalArgumentException e) {
                    unauthorized(response, "JWT claims为空字符串");
                    log.warn("JWT claims string is empty: uri={}, msg={}", uri, e.getMessage());
                } catch (Exception e) {
                    unauthorized(response, "认证错误");
                    log.error("auth error: uri={}, error=", uri, e);
                }
            }
        }
    }

    /**
     * 验证 Redis 中的 token，Redis 不可用时降级为仅 JWT 签名验证（放行）。
     */
    private boolean validateRedisToken(String account, String jti, String token) {
        String redisKey = JwtTokenUtil.buildTokenRedisKey(account, jti);
        try {
            Object redisToken = redisUtils.get(redisKey);
            return redisToken != null && token.equals(redisToken);
        } catch (Exception e) {
            log.warn("Redis不可用，降级为JWT签名验证放行: account={}", account, e);
            return true;
        }
    }

    /**
     * 延长 Redis 中 token 的 TTL，防止活跃用户因 token 到期被踢出。
     * 每次认证成功的请求都会续期，确保正在考试的学生不会因 token 过期而闪退。
     */
    private void extendTokenTtl(String account, String jti) {
        try {
            String redisKey = JwtTokenUtil.buildTokenRedisKey(account, jti);
            redisUtils.expire(redisKey, TOKEN_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.debug("延长token TTL失败: account={}, jti={}", account, jti, e);
        }
    }

    /**
     * JWT 临期自动续签：如果 JWT 剩余有效时间不足 1 小时，颁发新 token。
     * 新 token 通过 X-New-Token 响应头返回给客户端，客户端应替换本地存储的旧 token。
     *
     * @param claims     当前请求的 JWT claims
     * @param account    用户账号（学号或登录账号）
     * @param oldJti     当前 token 的 jti
     * @param tokenGenerator 新 token 生成函数
     * @param response   用于设置响应头
     */
    private void refreshTokenIfNeeded(Claims claims, String account, String oldJti,
                                       Supplier<String> tokenGenerator,
                                       HttpServletResponse response) {
        try {
            long remaining = JwtTokenUtil.getRemainingSeconds(claims);
            if (remaining > 0 && remaining < JwtTokenUtil.REFRESH_THRESHOLD_SECONDS) {
                String newToken = tokenGenerator.get();
                String newJti = JwtTokenUtil.extractJti(newToken);
                String newRedisKey = JwtTokenUtil.buildTokenRedisKey(account, newJti);
                redisUtils.set(newRedisKey, newToken, JwtTokenUtil.TOKEN_EXPIRE_SECONDS);
                // 删除旧 token（可选：保留旧 token 一段时间作为过渡期）
                String oldRedisKey = JwtTokenUtil.buildTokenRedisKey(account, oldJti);
                redisUtils.expire(oldRedisKey, 300); // 旧 token 保留 5 分钟过渡期
                response.setHeader("X-New-Token", newToken);
                log.info("JWT临期自动续签: account={}, remaining={}s", account, remaining);
            }
        } catch (Exception e) {
            log.warn("JWT自动续签失败: account={}", account, e);
        }
    }

    private void setAuthenticationAndProceed(HttpServletRequest request, HttpServletResponse response,
                                              FilterChain filterChain, LoginUser loginUser,
                                              String userType, Long schoolId, String account)
            throws ServletException, IOException {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        updateUserLastActiveTime(userType, schoolId, account);
        filterChain.doFilter(request, response);
    }

    // ========== 更新用户最后活跃时间（带学校维度，不影响原有逻辑） ==========
    /**
     * 更新用户最后活跃时间（带学校ID）
     * @param userType 用户类型（student/manage）
     * @param schoolId 学校ID
     * @param userId 用户标识（学号/账号）
     */
    private void updateUserLastActiveTime(String userType, Long schoolId, String userId) {
        // 空值校验：避免学校ID为空导致统计异常
        if (schoolId == null || schoolId <= 0) {
            return;
        }
        try {
            String redisKey = RedisKeyConstants.buildOnlineUserKey(userType, schoolId, userId);
            // 存储当前时间戳（毫秒），设置过期时间和token一致
            redisUtils.set(redisKey, String.valueOf(System.currentTimeMillis()), TOKEN_EXPIRE_SECONDS);
        } catch (Exception e) {
            log.error("更新用户最后活跃时间失败: userType={}, schoolId={}, userId={}", userType, schoolId, userId, e);
        }
    }

    // 安全打印token前缀，避免日志泄露完整token
    private String mask(String token) {
        if (token == null) return "null";
        int n = Math.min(token.length(), 12);
        return token.substring(0, n) + "...(" + token.length() + ")";
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) return;
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":401,\"msg\":\"" + message + "\"}";
//        response.getWriter().write(Arrays.toString(body.getBytes(StandardCharsets.UTF_8)));
        response.getWriter().write(body);
        response.getWriter().flush();
    }
}