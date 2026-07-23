package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.dev33.satoken.stp.StpUtil;
import cn.xfywz.guozespring.entity.mhmain.SlRole;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeRoleAuth;
import cn.xfywz.guozespring.mapper.SlRoleMapper;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.admin.YeeRoleAuthService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.xfywz.guozespring.constant.CacheConstant.CACHE_KEY_SEPARATOR;
import static cn.xfywz.guozespring.constant.CacheConstant.USER_PERMISSION_LIST_CACHE_KEY;

@Service
public class YeeRoleAuthServiceImpl implements YeeRoleAuthService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Autowired
    private SlRoleMapper slRoleMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    private Result getRoleWithAuthNodes(long schoolId) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId);
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);

        if (slSchools.isEmpty()) {
            return Result.error("学校信息不存在或未启用");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));

        try {
            // 查询该学校所有已授权的roleId（去重）
            String roleIdsSql = "SELECT DISTINCT roleId FROM yee_role_auth WHERE schoolId = ? ORDER BY roleId DESC";
            PreparedStatement roleIdsSt = connection.prepareStatement(roleIdsSql);
            roleIdsSt.setLong(1, schoolId);
            ResultSet roleIdsRs = roleIdsSt.executeQuery();

            Set<Long> roleIdsSet = new HashSet<>();
            while (roleIdsRs.next()) {
                roleIdsSet.add(roleIdsRs.getLong("roleId"));
            }
            roleIdsRs.close();
            roleIdsSt.close();

            if (roleIdsSet.isEmpty()) {
                return Result.success(new ArrayList<>());
            }

            // 查询每个角色的所有权限节点
            StringBuilder roleIdsStr = new StringBuilder();
            for (Long roleId : roleIdsSet) {
                if (roleIdsStr.length() > 0) {
                    roleIdsStr.append(",");
                }
                roleIdsStr.append(roleId);
            }

            String authNodesSql = "SELECT roleId, authId FROM yee_role_auth WHERE schoolId = ? AND roleId IN (" + roleIdsStr.toString() + ")";
            PreparedStatement authNodesSt = connection.prepareStatement(authNodesSql);
            authNodesSt.setLong(1, schoolId);
            ResultSet authNodesRs = authNodesSt.executeQuery();

            // 构建角色ID到权限节点的映射
            java.util.Map<Long, List<String>> roleAuthMap = new java.util.HashMap<>();
            while (authNodesRs.next()) {
                long roleId = authNodesRs.getLong("roleId");
                String authId = String.valueOf(authNodesRs.getLong("authId"));

                roleAuthMap.computeIfAbsent(roleId, k -> new ArrayList<>()).add(authId);
            }
            authNodesRs.close();
            authNodesSt.close();

            // 从主库查询角色详细信息
            QueryWrapper<SlRole> roleQueryWrapper = new QueryWrapper<>();
            roleQueryWrapper.in("id", roleIdsSet);
            roleQueryWrapper.orderByDesc("sort");
            List<SlRole> roles = slRoleMapper.selectList(roleQueryWrapper);

            // 为每个角色设置其对应的权限节点
            for (SlRole role : roles) {
                List<String> authIds = roleAuthMap.get(role.getId());
                if (authIds != null && !authIds.isEmpty()) {
                    String nodesJson = "[\"" + String.join("\", \"", authIds) + "\"]";
                    role.setNodes(nodesJson);
                } else {
                    role.setNodes("[]");
                }
            }

            return Result.success(roles);
        } finally {
            connection.close();
        }
    }

    @Override
    public Result roleAuth_list(long schoolId, int pageSize, int pageNum) throws Exception {
        // 校验学校
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId).eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);
        if (slSchools.isEmpty()) {
            return Result.error("学校信息不存在或未启用");
        }

        Connection connection = null;
        try {
            connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));

            // 1. 获取该学校所有已授权的 roleId
            String roleIdsSql = "SELECT DISTINCT roleId FROM yee_role_auth WHERE schoolId = ?";
            PreparedStatement roleIdsSt = connection.prepareStatement(roleIdsSql);
            roleIdsSt.setLong(1, schoolId);
            ResultSet roleIdsRs = roleIdsSt.executeQuery();

            Set<Long> roleIdsSet = new HashSet<>();
            while (roleIdsRs.next()) {
                roleIdsSet.add(roleIdsRs.getLong("roleId"));
            }
            roleIdsRs.close();
            roleIdsSt.close();

            if (roleIdsSet.isEmpty()) {
                return Result.success(new ArrayList<>(), 0L);
            }

            // 2. 查询权限映射（roleId → authId 列表）
            String placeholders = roleIdsSet.stream().map(id -> "?").collect(Collectors.joining(","));
            String authNodesSql = "SELECT roleId, authId FROM yee_role_auth WHERE schoolId = ? AND roleId IN (" + placeholders + ")";
            PreparedStatement authNodesSt = connection.prepareStatement(authNodesSql);
            authNodesSt.setLong(1, schoolId);
            int index = 2;
            for (Long roleId : roleIdsSet) {
                authNodesSt.setLong(index++, roleId);
            }
            ResultSet authNodesRs = authNodesSt.executeQuery();

            Map<Long, List<String>> roleAuthMap = new HashMap<>();
            while (authNodesRs.next()) {
                long roleId = authNodesRs.getLong("roleId");
                String authId = String.valueOf(authNodesRs.getLong("authId"));
                roleAuthMap.computeIfAbsent(roleId, k -> new ArrayList<>()).add(authId);
            }
            authNodesRs.close();
            authNodesSt.close();

            // 3. 【关键】从主库查所有角色详情（一次性查全）
            QueryWrapper<SlRole> roleQueryWrapper = new QueryWrapper<>();
            roleQueryWrapper.in("id", roleIdsSet)
                    .eq("schoolId", schoolId);
            List<SlRole> allRoles = slRoleMapper.selectList(roleQueryWrapper);

            // 4. 按 sort 字段降序排序（Java 层）
            allRoles.sort((r1, r2) -> {
                Integer s1 = Math.toIntExact(r1.getSort());
                Integer s2 = Math.toIntExact(r2.getSort());
                return s1.compareTo(s2); // DESC
            });

            // 5. 分页
            int totalCount = allRoles.size();
            int offset = (pageNum - 1) * pageSize;
            if (offset >= totalCount) {
                return Result.success(new ArrayList<>(), (long) totalCount);
            }

            int endIndex = Math.min(offset + pageSize, totalCount);
            List<SlRole> pagedRoles = allRoles.subList(offset, endIndex);

            // 6. 设置 nodes 字段
            for (SlRole role : pagedRoles) {
                List<String> authIds = roleAuthMap.get(role.getId());
                if (authIds != null && !authIds.isEmpty()) {
                    String nodesJson = "[\"" + String.join("\", \"", authIds) + "\"]";
                    role.setNodes(nodesJson);
                } else {
                    role.setNodes("[]");
                }
            }

            return Result.success(pagedRoles, (long) totalCount);

        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public Result roleAuth_add(YeeRoleAuth yeeRoleAuth) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", yeeRoleAuth.getSchoolId());
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);

        if (slSchools.isEmpty()) {
            return Result.error("学校信息不存在或未启用");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));

        try {
            String checkSql = "SELECT COUNT(*) FROM yee_role_auth WHERE roleId = ? AND authId = ? AND schoolId = ?";
            PreparedStatement checkSt = connection.prepareStatement(checkSql);
            checkSt.setLong(1, yeeRoleAuth.getRoleId());
            checkSt.setLong(2, yeeRoleAuth.getAuthId());
            checkSt.setLong(3, yeeRoleAuth.getSchoolId());
            ResultSet checkRs = checkSt.executeQuery();

            if (checkRs.next() && checkRs.getInt(1) > 0) {
                checkRs.close();
                checkSt.close();
                return Result.error("该角色权限关系已存在");
            }
            checkRs.close();
            checkSt.close();

            String sql = "INSERT INTO yee_role_auth (roleId, authId, schoolId) VALUES (?, ?, ?)";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, yeeRoleAuth.getRoleId());
            st.setLong(2, yeeRoleAuth.getAuthId());
            st.setLong(3, yeeRoleAuth.getSchoolId());
            int result = st.executeUpdate();
            st.close();

            if (result > 0) {
                // 添加成功后返回更新后的角色列表
                return getRoleWithAuthNodes(yeeRoleAuth.getSchoolId());
            } else {
                return Result.error("添加权限管理失败");
            }
        } finally {
            connection.close();
        }
    }

    @Override
    public Result roleAuth_update(YeeRoleAuth yeeRoleAuth) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", yeeRoleAuth.getSchoolId());
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);

        if (slSchools.isEmpty()) {
            return Result.error("学校信息不存在或未启用");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));

        try {
            String sql = "UPDATE yee_role_auth SET roleId = ?, authId = ? WHERE id = ? AND schoolId = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, yeeRoleAuth.getRoleId());
            st.setLong(2, yeeRoleAuth.getAuthId());
            st.setLong(3, yeeRoleAuth.getId());
            st.setLong(4, yeeRoleAuth.getSchoolId());
            int result = st.executeUpdate();
            st.close();

            if (result > 0) {
                // 更新成功后返回更新后的角色列表
                return getRoleWithAuthNodes(yeeRoleAuth.getSchoolId());
            } else {
                return Result.error("更新权限管理失败");
            }
        } finally {
            connection.close();
        }
    }

    @Override
    public Result roleAuth_delete(long id) throws Exception {
        return Result.error("删除操作需要提供schoolId参数");
    }

    public Result roleAuth_delete(long id, long schoolId) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId);
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);

        if (slSchools.isEmpty()) {
            return Result.error("学校信息不存在或未启用");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));

        try {
            String sql = "DELETE FROM yee_role_auth WHERE id = ? AND schoolId = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, id);
            st.setLong(2, schoolId);
            int result = st.executeUpdate();
            st.close();

            if (result > 0) {
                // 删除成功后返回更新后的角色列表
                return getRoleWithAuthNodes(schoolId);
            } else {
                return Result.error("删除权限管理失败");
            }
        } finally {
            connection.close();
        }
    }

    @Override
    public Result roleAuth_delete_by_role(long roleId, long schoolId) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId);
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);

        if (slSchools.isEmpty()) {
            return Result.error("学校信息不存在或未启用");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));

        try {
            String sql = "DELETE FROM yee_role_auth WHERE roleId = ? AND schoolId = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, roleId);
            st.setLong(2, schoolId);
            int result = st.executeUpdate();
            st.close();

            QueryWrapper<SlRole> roleQuery = new QueryWrapper<>();
            roleQuery.eq("id", roleId);
            roleQuery.eq("schoolId", schoolId);
            slRoleMapper.delete(roleQuery);

            return getRoleWithAuthNodes(schoolId);
        } finally {
            connection.close();
        }
    }

    private List<YeeRoleAuth> rsToYeeRoleAuth(ResultSet rs) throws Exception {
        List<YeeRoleAuth> yeeRoleAuths = new ArrayList<>();
        while (rs.next()) {
            YeeRoleAuth yeeRoleAuth = new YeeRoleAuth();
            yeeRoleAuth.setId(rs.getLong("id"));
            yeeRoleAuth.setRoleId(rs.getLong("roleId"));
            yeeRoleAuth.setAuthId(rs.getLong("authId"));
            yeeRoleAuth.setSchoolId(rs.getLong("schoolId"));
            yeeRoleAuths.add(yeeRoleAuth);
        }
        return yeeRoleAuths;
    }

    @Override
    public Result roleAuth_search_by_role(long roleId, long schoolId) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId);
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);

        if (slSchools.isEmpty()) {
            return Result.error("学校信息不存在或未启用");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));

        try {
            String sql = "SELECT * FROM yee_role_auth WHERE roleId = ? AND schoolId = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, roleId);
            st.setLong(2, schoolId);
            ResultSet rs = st.executeQuery();
            List<YeeRoleAuth> result = rsToYeeRoleAuth(rs);
            rs.close();
            st.close();

            return Result.success(result);
        } finally {
            connection.close();
        }
    }

    @Override
    public Result roleAuth_search_by_auth(long authId, long schoolId) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId);
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);

        if (slSchools.isEmpty()) {
            return Result.error("学校信息不存在或未启用");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));

        try {
            String sql = "SELECT * FROM yee_role_auth WHERE authId = ? AND schoolId = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setLong(1, authId);
            st.setLong(2, schoolId);
            ResultSet rs = st.executeQuery();
            List<YeeRoleAuth> result = rsToYeeRoleAuth(rs);
            rs.close();
            st.close();

            return Result.success(result);
        } finally {
            connection.close();
        }
    }

    @Override
    public Result roleAuth_batch_add(long roleId, long schoolId, List<Long> authIds) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId);
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);

        if (slSchools.isEmpty()) {
            return Result.error("学校信息不存在或未启用");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));

        try {
            connection.setAutoCommit(false);

            String deleteSql = "DELETE FROM yee_role_auth WHERE roleId = ? AND schoolId = ?";
            PreparedStatement deleteSt = connection.prepareStatement(deleteSql);
            deleteSt.setLong(1, roleId);
            deleteSt.setLong(2, schoolId);
            deleteSt.executeUpdate();
            deleteSt.close();

            String insertSql = "INSERT INTO yee_role_auth (roleId, authId, schoolId) VALUES (?, ?, ?)";
            PreparedStatement insertSt = connection.prepareStatement(insertSql);
            int addCount = 0;

            for (Long authId : authIds) {
                insertSt.setLong(1, roleId);
                insertSt.setLong(2, authId);
                insertSt.setLong(3, schoolId);
                int result = insertSt.executeUpdate();
                if (result > 0) {
                    addCount++;
                }
            }
            insertSt.close();

            connection.commit();

            // 批量设置成功后返回更新后的角色列表
            Result roleWithAuthNodes = getRoleWithAuthNodes(schoolId);

            // 更新角色列表 sl_role
            Object data = roleWithAuthNodes.getData();
            List<SlRole> yeeRoleAuths = (List<SlRole>) data;
            // 筛选出添加id 为 117 角色的数据
            SlRole matched = yeeRoleAuths.stream()
                    .filter(yeeRoleAuth -> yeeRoleAuth.getId() == roleId)
                    .findFirst() // 返回 Optional<YeeRoleAuth>
                    .orElse(null); // 或者抛异常、设默认值等

            int result = slRoleMapper.updateById(matched);

            redisTemplate.delete(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + StpUtil.getLoginId());

            return roleWithAuthNodes;
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }
}