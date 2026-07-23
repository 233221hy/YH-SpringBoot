  package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.xfywz.guozespring.constant.DataAuth;
import cn.xfywz.guozespring.entity.mhmain.SlManage;
import cn.xfywz.guozespring.entity.mhmain.SlManageNode;
import cn.xfywz.guozespring.entity.mhmain.SlRole;
import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import cn.xfywz.guozespring.mapper.SlManageMapper;
import cn.xfywz.guozespring.mapper.SlManageNodeMapper;
import cn.xfywz.guozespring.mapper.SlRoleMapper;
import cn.xfywz.guozespring.service.admin.UserLoginService;
import cn.xfywz.guozespring.service.admin.YeeManageService;
import cn.xfywz.guozespring.util.JwtTokenUtil;
import cn.xfywz.guozespring.util.RedisUtils;
import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import cn.xfywz.guozespring.util.IpUtils;


import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.xfywz.guozespring.constant.CacheConstant.CACHE_KEY_SEPARATOR;
import static cn.xfywz.guozespring.constant.CacheConstant.USER_PERMISSION_LIST_CACHE_KEY;

@Slf4j
@Service
public class UserLoginServiceImpl implements UserLoginService {
    @Autowired
    private AuthenticationManager authenticationManager;
    @Autowired
    private RedisUtils redisUtils;
    @Autowired
    private SlManageMapper slManageMapper;

    @Autowired
    private SlRoleMapper slRoleMapper;

    @Autowired
    private SlManageNodeMapper slManageNodeMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private YeeManageService yeeManageService;


    /**
     * 默认登录超时时间：7天
     */
    @Value("${sa-token.timeout}")
    private Integer DEFAULT_LOGIN_SESSION_TIMEOUT;

//    @Override
//    public Map login(SlManage slManage) throws Exception {
//        Map result = new HashMap<>();
//        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(slManage.getAccount(), slManage.getPassword());
//        try {
//            Authentication authenticate = authenticationManager.authenticate(auth);
//            if (authenticate == null){
//                throw new RuntimeException("登录失败");
//            }
//            LoginUser loginUser = (LoginUser) authenticate.getPrincipal();
//            String token=JwtTokenUtil.getToken(loginUser);
//            if (loginUser.getSlManage()!= null){
//                redisUtils.set(loginUser.getSlManage().getAccount(),token);
//                SlManage slManage1=slManageMapper.selectById(loginUser.getSlManage().getId());
//                slManage1.setLastTime(slManage1.getThisTime());
//                slManage1.setThisTime(new java.sql.Timestamp(System.currentTimeMillis()));
//                slManage1.setLastIp(slManage1.getThisIp());
//                slManage1.setThisIp(GetOutIpUtil.getOutIp());
//                slManageMapper.updateById(slManage1);
//
//                // Sa-Token 内部记录
//                StpUtil.login(loginUser.getSlManage().getAccount(), new SaLoginModel().setIsLastingCookie(false).setTimeout(DEFAULT_LOGIN_SESSION_TIMEOUT));
//                StpUtil.getSession().set(String.valueOf(loginUser.getSlManage().getAccount()), loginUser);
//
//                List<String> userPermissionList = (List<String>) redisTemplate.opsForValue().get(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + loginUser.getSlManage().getAccount());
//                if (userPermissionList == null || userPermissionList.isEmpty()) {
//                    // 权限列表
//                    long role = loginUser.getSlManage().getRole();
//                    SlRole slRole = slRoleMapper.selectByIdAndSchoolId(role, loginUser.getSlManage().getSchoolId());
//                    userPermissionList = getList(StpUtil.getLoginId(), slRole);
//                    result.put("userPermissionList",userPermissionList);
//                } else {
//                    result.put("userPermissionList",userPermissionList);
//                }
//
//            }else {
//                redisUtils.set(loginUser.getYeeManage().getAccount(),token);
//
//                // Sa-Token 内部记录
//                StpUtil.login(loginUser.getYeeManage().getAccount(), new SaLoginModel().setIsLastingCookie(false).setTimeout(DEFAULT_LOGIN_SESSION_TIMEOUT));
//                StpUtil.getSession().set(String.valueOf(loginUser.getYeeManage().getAccount()), loginUser);
//
//                List<String> userPermissionList = (List<String>) redisTemplate.opsForValue().get(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + loginUser.getYeeManage().getAccount());
//                if (userPermissionList == null || userPermissionList.isEmpty()) {
//                    // 权限列表
//                    long role = loginUser.getYeeManage().getRole();
//                    SlRole slRole = slRoleMapper.selectByIdAndSchoolId(role, loginUser.getYeeManage().getSchoolId());
//                    userPermissionList = getList(StpUtil.getLoginId(), slRole);
//                    result.put("userPermissionList",userPermissionList);
//                } else {
//                    result.put("userPermissionList",userPermissionList);
//                }
//            }
//            result.put("token",token);
//            return result;
//        }catch (Exception e){
//            e.printStackTrace();
//            result.put("msg", "账号或密码错误");
//            return result;
//        }
//    }

    @Override
    public Map<String, Object> login(SlManage slManage, HttpServletRequest request) throws Exception {
        Map<String, Object> result = new HashMap<>();

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(slManage.getAccount(), slManage.getPassword());

        try {
            Authentication authenticate = authenticationManager.authenticate(auth);
            if (authenticate == null) {
                throw new RuntimeException("登录失败");
            }

            LoginUser loginUser = (LoginUser) authenticate.getPrincipal();

            // ====== 在 JWT 生成前提取 dataAuth 并写入 LoginUser，确保 JWT 中包含 dataAuth ======
            int dataAuthValue;
            if (loginUser.getSlManage() != null) {
                SlManage manage = loginUser.getSlManage();
                SlRole slRole = slRoleMapper.selectByIdAndSchoolId(manage.getRole(), manage.getSchoolId());
                dataAuthValue = (slRole != null && slRole.getDataAuth() != null)
                        ? slRole.getDataAuth()
                        : DataAuth.OWN.getValue();
            } else if (loginUser.getYeeManage() != null) {
                YeeManage manage = loginUser.getYeeManage();
                SlRole slRole = slRoleMapper.selectByIdAndSchoolId(manage.getRole(), manage.getSchoolId());
                dataAuthValue = (slRole != null && slRole.getDataAuth() != null)
                        ? slRole.getDataAuth()
                        : DataAuth.OWN.getValue();
            } else {
                dataAuthValue = DataAuth.OWN.getValue();
            }
            loginUser.setDataAuth(dataAuthValue);

            String token = JwtTokenUtil.getToken(loginUser);
            String jti = JwtTokenUtil.extractJti(token);

            // ========== 处理 SlManage 用户 ==========
            if (loginUser.getSlManage() != null) {
                SlManage manage = loginUser.getSlManage();
                // 不再全量清理旧 token，避免多端登录互踢
                // 旧 token 通过 Redis TTL 自然过期（24h），登出时仍会清理
                redisUtils.set(JwtTokenUtil.buildTokenRedisKey(manage.getAccount(), jti), token, JwtTokenUtil.TOKEN_EXPIRE_SECONDS);
                String clientIp = IpUtils.getClientIp(request);
                // 更新登录时间和IP
                SlManage updateManage = slManageMapper.selectById(manage.getId());
                if (updateManage != null) {
                    updateManage.setLastTime(updateManage.getThisTime());
                    updateManage.setThisTime(new java.sql.Timestamp(System.currentTimeMillis()));
                    updateManage.setLastIp(updateManage.getThisIp());
                    updateManage.setThisIp(clientIp);
                    slManageMapper.updateById(updateManage);
                }

                // Sa-Token 登录
                StpUtil.login(manage.getAccount(),
                        new SaLoginModel().setIsLastingCookie(false).setTimeout(DEFAULT_LOGIN_SESSION_TIMEOUT));
                StpUtil.getSession().set(String.valueOf(manage.getAccount()), loginUser);

                StpUtil.getSession().set("dataAuth", dataAuthValue);

                // ====== 权限列表缓存逻辑 ======
                long roleId = manage.getRole();
                long schoolId = manage.getSchoolId();
                SlRole slRole = slRoleMapper.selectByIdAndSchoolId(roleId, schoolId);

                @SuppressWarnings("unchecked")
                List<String> userPermissionList = (List<String>) redisTemplate.opsForValue()
                        .get(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + manage.getAccount());

                if (userPermissionList == null || userPermissionList.isEmpty()) {
                    userPermissionList = getList(StpUtil.getLoginId(), slRole);
                    // 可选：缓存权限列表（你已有逻辑，此处省略写入 Redis）
                }
                result.put("userPermissionList", userPermissionList);

            }
            // ========== 处理 YeeManage 用户 ==========
            else if (loginUser.getYeeManage() != null) {
                YeeManage manage = loginUser.getYeeManage();
                // 不再全量清理旧 token，避免多端登录互踢
                // 旧 token 通过 Redis TTL 自然过期（24h），登出时仍会清理
                redisUtils.set(JwtTokenUtil.buildTokenRedisKey(manage.getAccount(), jti), token, JwtTokenUtil.TOKEN_EXPIRE_SECONDS);

                // ====== 新增：更新登录时间/IP ======
                String clientIp = IpUtils.getClientIp(request);
                YeeManage updateDto = new YeeManage();
                updateDto.setId(manage.getId());
                updateDto.setSchoolId(manage.getSchoolId());
                updateDto.setThisIp(clientIp);
                updateDto.setThisTime(new Timestamp(System.currentTimeMillis()));

                yeeManageService.updateLoginInfo(updateDto);

                // Sa-Token 登录
                StpUtil.login(manage.getAccount(),
                        new SaLoginModel().setIsLastingCookie(false).setTimeout(DEFAULT_LOGIN_SESSION_TIMEOUT));
                StpUtil.getSession().set(String.valueOf(manage.getAccount()), loginUser);

                // 获取角色信息（权限列表需要 slRole）
                long roleId = manage.getRole();
                long schoolId = manage.getSchoolId();
                SlRole slRole = slRoleMapper.selectByIdAndSchoolId(roleId, schoolId);

                // dataAuth 已在 JWT 生成前写入 LoginUser，此处仅保持 Sa-Token 会话兼容
                StpUtil.getSession().set("dataAuth", dataAuthValue);

                // ====== 权限列表缓存逻辑 ======
                List<String> userPermissionList = (List<String>) redisTemplate.opsForValue()
                        .get(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + manage.getAccount());

                if (userPermissionList == null || userPermissionList.isEmpty()) {
                    userPermissionList = getList(StpUtil.getLoginId(), slRole);
                }
                result.put("userPermissionList", userPermissionList);

            } else {
                throw new RuntimeException("用户类型异常，无法识别管理账号类型");
            }

            result.put("token", token);
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            result.put("msg", "账号或密码错误");
            return result;
        }
    }

//    @Override
//    public void loginOut(String auth) throws Exception {
//        Claims claims= JwtTokenUtil.parseToken(auth);
//        Object sub= null;
//        if (claims != null) {
//            sub = claims.getSubject();
//            SlManage loginUser= JSON.parseObject(sub.toString(), SlManage.class);
//            redisUtils.delete(loginUser.getAccount());
//            redisTemplate.delete(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + StpUtil.getLoginId());
//            StpUtil.logout();
//        }
//    }
@Override
public void loginOut(String auth) throws Exception {
    Claims claims = JwtTokenUtil.parseToken(auth);
    if (claims != null) {
        Object sub = claims.getSubject();
        LoginUser loginUser = JSON.parseObject(sub.toString(), LoginUser.class);
        String jti = (String) claims.get("jti");

        String account = null;
        if (loginUser.getSlManage() != null) {
            account = loginUser.getSlManage().getAccount();
        } else if (loginUser.getYeeManage() != null) {
            account = loginUser.getYeeManage().getAccount();
        }

        if (account != null && !account.trim().isEmpty() && jti != null) {
            redisUtils.delete(JwtTokenUtil.buildTokenRedisKey(account, jti));
        }

        try {
            Object loginId = StpUtil.getLoginId();
            if (loginId != null) {
                redisTemplate.delete(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + loginId);
                StpUtil.logout();
            }
        } catch (Exception e) {
            log.warn("退出登录时清理权限缓存失败", e);
        }
    }
}

/** 清理该账号的所有旧 token，用于新登录时使旧会话失效 */
private void cleanupOldTokens(String account) {
    try {
        String pattern = String.format(JwtTokenUtil.TOKEN_REDIS_KEY_PATTERN, account);
        for (String key : redisUtils.keys(pattern)) {
            redisUtils.delete(key);
        }
    } catch (Exception e) {
        log.warn("清理旧token失败: account={}", account, e);
    }
}

    private List<String> getList(Object loginId, SlRole slRole) {
        // 如果角色为空，返回空列表（不能返回 null！）
        if (slRole == null || slRole.getNodes() == null || slRole.getNodes().trim().isEmpty()) {
            return new ArrayList<>(); // 返回空 ArrayList，不是 null
        }

        try {
            // 解析 nodes 为 List（假设它是 JSON 数组字符串）
            List<?> list = JSON.parseArray(slRole.getNodes(), Object.class);
            if (list == null || list.isEmpty()) {
                return new ArrayList<>();
            }

            // 转为 Long ID 列表（兼容数字或字符串形式的 ID）
            List<Long> ids = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Number) {
                    ids.add(((Number) item).longValue());
                } else if (item instanceof String && ((String) item).matches("\\d+")) {
                    ids.add(Long.parseLong((String) item));
                }
            }

            if (ids.isEmpty()) {
                return new ArrayList<>();
            }

            // 查询节点
            List<SlManageNode> slManages = slManageNodeMapper.selectByIds(ids);
            List<String> controllerActionList = new ArrayList<>();

            if (slManages != null) {
                for (SlManageNode node : slManages) {
                    if (node == null || node.getController() == null || node.getAction() == null) continue;

                    String controller = node.getController().trim();
                    String action = node.getAction().trim();

                    if (controller.isEmpty() || action.isEmpty()) continue;

                    if (action.contains(",")) {
                        for (String act : action.split(",")) {
                            act = act.trim();
                            if (!act.isEmpty()) {
                                controllerActionList.add(controller + "." + act);
                            }
                        }
                    } else {
                        controllerActionList.add(controller + "." + action);
                    }
                }
            }

            // 缓存（可选，但建议保留）
            redisTemplate.opsForValue().set(
                    USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + loginId,
                    controllerActionList
            );

            return controllerActionList;

        } catch (Exception e) {
            // 任何解析或查询错误，都降级为返回空权限，但不中断登录
            return new ArrayList<>();
        }
    }
}
