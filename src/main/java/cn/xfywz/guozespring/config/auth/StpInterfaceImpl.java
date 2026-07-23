package cn.xfywz.guozespring.config.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.xfywz.guozespring.constant.DataAuth;
import cn.xfywz.guozespring.entity.mhmain.SlManageNode;
import cn.xfywz.guozespring.entity.mhmain.SlRole;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import cn.xfywz.guozespring.mapper.SlManageNodeMapper;
import cn.xfywz.guozespring.mapper.SlRoleMapper;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static cn.xfywz.guozespring.constant.CacheConstant.CACHE_KEY_SEPARATOR;
import static cn.xfywz.guozespring.constant.CacheConstant.USER_PERMISSION_LIST_CACHE_KEY;

/**
 * 自定义权限验证接口
 *
 * @author chenglin
 */
@Component
@Slf4j
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SlRoleMapper slRoleMapper;

    @Autowired
    private SlManageNodeMapper slManageNodeMapper;

    @Autowired
    private RedisTemplate redisTemplate;


    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {

        List<String> userPermissionList = (List<String>) redisTemplate.opsForValue().get(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + loginId);

        if (userPermissionList != null) {
            return userPermissionList;
        }

        // 一般走不到这里, 登录成功后, 缓存用户权限列表, 这里防止redis失效后访问接口时,注解会走到下面代码 (redis缓存失效)
        // 拿到当前登录用户信息
        LoginUser userInfo = (LoginUser) StpUtil.getSession().get((String) loginId);

        if (userInfo.getSlManage() != null) {
            long role = userInfo.getSlManage().getRole();
            if (role == 1) {
                SlRole slRole = slRoleMapper.selectByIdAndSchoolId(role, 0);
                return getList(loginId, slRole);
            } else {
                SlRole slRole = slRoleMapper.selectByIdAndSchoolId(role, userInfo.getSlManage().getSchoolId());
                return getList(loginId, slRole);
            }
        } else {
            long role = userInfo.getYeeManage().getRole();
            if (role == 1) {
                SlRole slRole = slRoleMapper.selectByIdAndSchoolId(role, 0);
                return getList(loginId, slRole);
            } else {
                SlRole slRole = slRoleMapper.selectByIdAndSchoolId(role, userInfo.getYeeManage().getSchoolId());
                return getList(loginId, slRole);
            }
        }



    }

    // todo 获取角色权限 可能用处不大
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        LoginUser userInfo = (LoginUser) StpUtil.getSessionByLoginId(loginId).get((String) loginId);

        // 本 list 仅做模拟，实际项目中要根据具体业务逻辑来查询角色
        List<String> list = new ArrayList<String>();
//        list.add("admin");
//        list.add("super-admin");
        return list;
    }

    private List<String> getList(Object loginId, SlRole slRole) {
        if (slRole == null){
            return null;
        }
        String nodes = slRole.getNodes();
        List list = JSON.parseObject(nodes, List.class);
        if (list.isEmpty()){
            return new ArrayList<>();
        }
        List<SlManageNode> slManages = slManageNodeMapper.selectByIds(list);
        List<String> controllerActionList = new ArrayList<String>();
        String controllerAction = null;
        for (SlManageNode slManageNode : slManages){
            // action: add,import  有两个操作标识
            if (slManageNode.getAction().contains(",")){
                String[] actions = slManageNode.getAction().split(",");
                for (String action : actions){
                    controllerAction = slManageNode.getController() + "." + action;
                    controllerActionList.add(controllerAction);
                }
            }else {
                controllerAction = slManageNode.getController() + "." + slManageNode.getAction();
                controllerActionList.add(controllerAction);
            }
        }
        redisTemplate.opsForValue().set(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + loginId, controllerActionList);
        return controllerActionList;
    }
}
