package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.dto.YeeManageQueryParam;
import cn.xfywz.guozespring.entity.mhmain.SlRole;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import cn.xfywz.guozespring.entity.vo.ManageListVO;
import cn.xfywz.guozespring.entity.vo.YeeManageLike;
import cn.xfywz.guozespring.entity.vo.YeeManageListVO;
import cn.xfywz.guozespring.exception.DatabaseException;
import cn.xfywz.guozespring.mapper.SlRoleMapper;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.admin.YeeManageService;
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
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YeeManageServiceImpl implements YeeManageService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private SlRoleMapper slRoleMapper;
    @Autowired
    private DatabaseUtil databaseUtil;

    private List<YeeManage> rsToYeeManage(ResultSet rs) throws Exception{
        List<YeeManage> yeeManages = new ArrayList<>();
        while (rs.next()){
            YeeManage yeeManage = new YeeManage();
            yeeManage.setId(rs.getInt("id"));
            yeeManage.setAccount(rs.getString("account"));
//        yeeManage.setPassword(rs.getString("password"));
            yeeManage.setName(rs.getString("name"));
            yeeManage.setErrorCount(rs.getInt("errorCount"));
            yeeManage.setErrorTime(rs.getInt("errorTime"));
            yeeManage.setThisTime(rs.getTimestamp("thisTime"));
            yeeManage.setLastTime(rs.getTimestamp("lastTime"));
            yeeManage.setThisIp(rs.getString("thisIp"));
            yeeManage.setLastIp(rs.getString("lastIp"));
            yeeManage.setIsLock(rs.getInt("isLock"));
            yeeManage.setEmail(rs.getString("email"));
            yeeManage.setRole(rs.getInt("role"));
            yeeManage.setAvatar(rs.getString("avatar"));
            yeeManage.setMobile(rs.getString("mobile"));
            yeeManage.setGender(rs.getString("gender"));
            yeeManage.setWeChat(rs.getString("wechat"));
            yeeManage.setIntro(rs.getString("intro"));
            yeeManage.setSchoolId(rs.getInt("schoolId"));
            yeeManage.setSl_super(rs.getInt("super"));
            yeeManage.setCollegeId(rs.getInt("collegeId"));
            yeeManage.setGeneral(rs.getInt("general"));
            yeeManage.setLoginCode(rs.getString("loginCode"));
            yeeManage.setRecommend(rs.getInt("recommend"));
            yeeManage.setActive(rs.getInt("active"));
            yeeManage.setColleges(rs.getString("colleges"));
            yeeManage.setAddTime(rs.getTimestamp("addTime"));
            yeeManage.setForce(rs.getInt("force"));
            yeeManage.setPassport(rs.getString("passport"));
            yeeManage.setBindId(rs.getInt("bindId"));
            yeeManage.setAddDate(rs.getDate("addDate"));
            yeeManages.add(yeeManage);
        }
        return yeeManages;
    }

    /**
     * 构建基础查询SQL
     * @return SQL语句
     */
    private String buildQuerySql() {
        return """
        SELECT
        ym.id, ym.account, ym.name, ym.lastIp, ym.lastTime, ym.isLock, ym.email,
        ym.avatar, ym.role, ym.mobile, ym.active, ym.schoolId
        FROM yee_manage ym
        WHERE 1=1
        """;

    }

    /**
     * 构建查询条件
     */
    private void applyQueryConditions(QueryBuilder queryBuilder, YeeManageQueryParam param) {
        log.debug("开始应用教师查询条件: {}", param);

        // 教师姓名模糊查询
        if (StringUtils.hasText(param.getName())) {
            queryBuilder.like("ym.name", param.getName());
        }

        // 账户名模糊查询
        if (StringUtils.hasText(param.getAccount())) {
            queryBuilder.like("ym.account", param.getAccount());
        }

        log.debug("教师查询条件应用完成");

    }

    /**
     * 查询所有YeeManage记录（含角色名称、学院名称）
     */
    @Override
    public Result selectAll(YeeManageQueryParam param) {
        try {
            // 分页查询教师列表
            PageResult<ManageListVO> pageResult = databaseUtil.query(param.getSchoolId())
                    .sql(buildQuerySql()) // 构建基础SQL
//                    .param(param.getSchoolId()) // 设置学校ID参数
//                    .param(param.getSchoolId()) // 设置WHERE条件中的学校ID
                    .eq("ym.schoolId", param.getSchoolId())
                    .apply(qb -> applyQueryConditions(qb, param)) // 应用查询条件
                    .orderBy("ym.id DESC") // 按ID降序排列
                    .page(ManageListVO::fromResultSet, param.getPageNum(), param.getPageSize());

            // 获取所有角色ID
            Set<Long> roleIds = pageResult.getRows().stream()
                    .map(ManageListVO::getRole)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            // 批量查询角色名称
            Map<Long, String> roleMap = new HashMap<>();
            for (Long roleId : roleIds) {
                SlRole slRole = slRoleMapper.selectByIdAndSchoolId(roleId, param.getSchoolId());
                if (slRole != null) {
                    roleMap.put(roleId, slRole.getName());
                }
            }

            return Result.success(pageResult.getRows(), pageResult.getTotal());

        } catch (Exception e) {
            log.error("查询教师列表失败: ", e);
            throw new DatabaseException("查询教师列表失败: " + e.getMessage(), e);
        }
    }
//    @Override
//    public Result selectAll(int schoolId, int pageSize, int pageNum) throws Exception {
//        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("id",schoolId);
//        queryWrapper.eq("allow",1);
//        List<SlSchool> slSchools =slSchoolMapper.selectList(queryWrapper);
//        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));
//        String sql = "select * from yee_manage LIMIT ? OFFSET ?";
//        String countSql = "SELECT count(*) FROM yee_student";
//        PreparedStatement countSt = connection.prepareStatement(countSql);
//        ResultSet countRs = countSt.executeQuery();
//        int totalCount = 0;
//        if (countRs.next()) { // 将指针移动到第一行（结果集一定有数据）
//            totalCount = countRs.getInt(1); // 获取第一列的整数值（即count(*)结果）
//        }
//        PreparedStatement st = connection.prepareStatement(sql);
//        int offset = (pageNum - 1) * pageSize;
//        st.setInt(1, pageSize);
//        st.setInt(2, offset);
//        ResultSet rs = st.executeQuery();
//        List<YeeManage> yeeManages = new ArrayList<>(rsToYeeManage(rs));
//        connection.close();
//        st.close();
//        rs.close();
//        return Result.success((Object) yeeManages, (long) totalCount);
//    }

    @Override
    public Result selectById(int schoolId, int id) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",schoolId);
        queryWrapper.eq("allow",1);
        List<SlSchool> slSchools =slSchoolMapper.selectList(queryWrapper);
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));
        String sql = "select * from yee_manage where id = ?";
        PreparedStatement st = connection.prepareStatement(sql);
        st.setInt(1, id);
        ResultSet rs = st.executeQuery();
        YeeManage yeeManage = rsToYeeManage(rs).get(0);
        connection.close();
        st.close();
        rs.close();
        return Result.success(yeeManage);
    }

    @Override
    public Result deleteById(int schoolId, int id) {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",schoolId);
        queryWrapper.eq("allow",1);
        List<SlSchool> slSchools =slSchoolMapper.selectList(queryWrapper);
        for (SlSchool slSchool : slSchools){
            try {
                Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
                String sql = "delete from yee_manage where id = ?";
                PreparedStatement st = connection.prepareStatement(sql);
                st.setInt(1, id);
                int update = st.executeUpdate();
                if (update == 0){
                    return Result.error("删除失败");
                }
            } catch (Exception e) {
                return Result.error("删除失败");
            }
        }
        return Result.success("删除成功");
    }

    @Override
    public Result update(YeeManage yeeManage) {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", yeeManage.getSchoolId());
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        for (SlSchool slSchool : slSchools) {
            try {
                Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
                StringBuilder sql = new StringBuilder("UPDATE yee_manage SET ");
                List<Object> parameters = new ArrayList<>();

                if (yeeManage.getAccount() != null) {
                    sql.append("`account` = ?, ");
                    parameters.add(yeeManage.getAccount());
                }
                if (yeeManage.getName() != null) {
                    sql.append("`name` = ?, ");
                    parameters.add(yeeManage.getName());
                }
                if (yeeManage.getErrorCount() > 0) {
                    sql.append("`errorCount` = ?, ");
                    parameters.add(yeeManage.getErrorCount());
                }
                if (yeeManage.getErrorTime() > 0) {
                    sql.append("`errorTime` = ?, ");
                    parameters.add(yeeManage.getErrorTime());
                }
                if (yeeManage.getThisTime() != null) {
                    sql.append("`thisTime` = ?, ");
                    parameters.add(yeeManage.getThisTime());
                }
                if (yeeManage.getLastTime() != null) {
                    sql.append("`lastTime` = ?, ");
                    parameters.add(yeeManage.getLastTime());
                }
                if (yeeManage.getThisIp() != null) {
                    sql.append("`thisIp` = ?, ");
                    parameters.add(yeeManage.getThisIp());
                }
                if (yeeManage.getLastIp() != null) {
                    sql.append("`lastIp` = ?, ");
                    parameters.add(yeeManage.getLastIp());
                }
                if (yeeManage.getIsLock() >= 0) {
                    sql.append("`isLock` = ?, ");
                    parameters.add(yeeManage.getIsLock());
                }
                if (yeeManage.getEmail() != null) {
                    sql.append("`email` = ?, ");
                    parameters.add(yeeManage.getEmail());
                }
                if (yeeManage.getRole() >= 0) {
                    sql.append("`role` = ?, ");
                    parameters.add(yeeManage.getRole());
                }
                if (yeeManage.getAvatar() != null) {
                    sql.append("`avatar` = ?, ");
                    parameters.add(yeeManage.getAvatar());
                }
                if (yeeManage.getPassword() != null) {
                    sql.append("`password` = ?, ");
                    parameters.add(bCryptPasswordEncoder.encode(yeeManage.getPassword()));
                }
                if (yeeManage.getMobile() != null) {
                    sql.append("`mobile` = ?, ");
                    parameters.add(yeeManage.getMobile());
                }
                if (yeeManage.getGender() != null) {
                    sql.append("`gender` = ?, ");
                    parameters.add(yeeManage.getGender());
                }
                if (yeeManage.getWeChat() != null) {
                    sql.append("`wechat` = ?, ");
                    parameters.add(yeeManage.getWeChat());
                }
                if (yeeManage.getIntro() != null) {
                    sql.append("`intro` = ?, ");
                    parameters.add(yeeManage.getIntro());
                }
                if (yeeManage.getSl_super() >= 0) {
                    sql.append("`super` = ?, ");
                    parameters.add(yeeManage.getSl_super());
                }
                if (yeeManage.getCollegeId() > 0) {
                    sql.append("`collegeId` = ?, ");
                    parameters.add(yeeManage.getCollegeId());
                }
                if (yeeManage.getGeneral() >= 0) {
                    sql.append("`general` = ?, ");
                    parameters.add(yeeManage.getGeneral());
                }
                if (yeeManage.getLoginCode() != null) {
                    sql.append("`loginCode` = ?, ");
                    parameters.add(yeeManage.getLoginCode());
                }
                if (yeeManage.getRecommend() >= 0) {
                    sql.append("`recommend` = ?, ");
                    parameters.add(yeeManage.getRecommend());
                }
                if (yeeManage.getActive() >= 0) {
                    sql.append("`active` = ?, ");
                    parameters.add(yeeManage.getActive());
                }
                if (yeeManage.getColleges() != null) {
                    sql.append("`colleges` = ?, ");
                    parameters.add(yeeManage.getColleges());
                }
                if (yeeManage.getAddTime() != null) {
                    sql.append("`addTime` = ?, ");
                    parameters.add(yeeManage.getAddTime());
                }
                if (yeeManage.getForce() >= 0) {
                    sql.append("`force` = ?, ");  // 修改这里：将 force 改为 `force`
                    parameters.add(yeeManage.getForce());
                }
                if (yeeManage.getPassport() != null) {
                    sql.append("`passport` = ?, ");
                    parameters.add(yeeManage.getPassport());
                }
                if (yeeManage.getBindId() > 0) {
                    sql.append("`bindId` = ?, ");
                    parameters.add(yeeManage.getBindId());
                }
                if (yeeManage.getAddDate() != null) {
                    sql.append("`addDate` = ?, ");
                    parameters.add(yeeManage.getAddDate());
                }

                // 删除最后的逗号和空格
                if (!parameters.isEmpty()) {
                    sql.delete(sql.length() - 2, sql.length());
                } else {
                    return Result.error("没有可更新的字段");
                }

                sql.append(" WHERE id = ?");
                parameters.add(yeeManage.getId());

                PreparedStatement st = connection.prepareStatement(sql.toString());
                for (int i = 0; i < parameters.size(); i++) {
                    Object param = parameters.get(i);
                    if (param instanceof String) {
                        st.setString(i + 1, (String) param);
                    } else if (param instanceof Integer) {
                        st.setInt(i + 1, (Integer) param);
                    } else if (param instanceof Long) {
                        st.setLong(i + 1, (Long) param);
                    } else if (param instanceof Double) {
                        st.setDouble(i + 1, (Double) param);
                    } else if (param instanceof Boolean) {
                        st.setBoolean(i + 1, (Boolean) param);
                    } else if (param instanceof java.util.Date) {
                        if (param instanceof java.sql.Timestamp) {
                            st.setTimestamp(i + 1, (java.sql.Timestamp) param);
                        } else if (param instanceof java.sql.Date) {
                            st.setDate(i + 1, (java.sql.Date) param);
                        } else {
                            // 普通 java.util.Date 转换为 sql.Date
                            st.setDate(i + 1, new java.sql.Date(((java.util.Date) param).getTime()));
                        }
                    }
                }

                int rowsUpdated = st.executeUpdate();
                if (rowsUpdated == 0) {
                    return Result.error("更新失败：未找到匹配的记录");
                }

                connection.close();
                st.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
//                return Result.error("更新失败：" + e.getMessage());
            }
        }
        return Result.success("更新成功");
    }

    @Override
    public Result lock(int schoolId, int id) {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId);
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);
        try {
            SlSchool slSchool = slSchools.get(0);
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "select * from yee_manage where id = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, id);
            ResultSet rs = st.executeQuery();
            YeeManage yeeManage = rsToYeeManage(rs).get(0);
            String sql1 = "UPDATE yee_manage SET isLock = ? WHERE id = ?";
            PreparedStatement st1 = connection.prepareStatement(sql1);
            st1.setInt(2, id);
            if (yeeManage.getIsLock() == 1){
                st1.setInt(1, 0);
                int rowsUpdated = st1.executeUpdate();
                if (rowsUpdated == 0) {
                    return Result.error("更新失败：未找到匹配的记录");
                }else return Result.success("解锁成功");
            }else {
                st1.setInt(1, 1);
                int rowsUpdated = st1.executeUpdate();
                if (rowsUpdated == 0) {
                    return Result.error("更新失败：未找到匹配的记录");
                }else return Result.success("锁定成功");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result active(int schoolId, int id) {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId);
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);
        try {
            SlSchool slSchool = slSchools.get(0);
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
            String sql = "UPDATE yee_manage SET active = ? WHERE id = ?";
            PreparedStatement st = connection.prepareStatement(sql);
            st.setInt(1, 1);
            st.setInt(2, id);
            int rowsUpdated = st.executeUpdate();
            if (rowsUpdated == 0) {
                return Result.error("更新失败：未找到匹配的记录");
            }else return Result.success("激活成功");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Result searchByCondition(YeeManageLike yeeManageLike) {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("allow", 1);
        queryWrapper.eq("id", yeeManageLike.getSchoolId());
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);
        List<YeeManage> yeeManages = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM yee_manage");

        // 动态拼接查询条件
        List<Object> params = new ArrayList<>();

        boolean hasWhereClause = false; // 标记是否已经添加了 WHERE 子句
        if (yeeManageLike.getLike() != null && !yeeManageLike.getLike().isEmpty()) {
            sqlBuilder.append(" WHERE (account LIKE ? OR name LIKE ? OR email LIKE ? OR mobile LIKE ?)");
            String likeKeyword = "%" + yeeManageLike.getLike() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
            hasWhereClause = true;
        }

        if (yeeManageLike.getRole() != null) {
            if (!hasWhereClause) {
                sqlBuilder.append(" WHERE role = ?");
                hasWhereClause = true;
            } else {
                sqlBuilder.append(" AND role = ?");
            }
            params.add(yeeManageLike.getRole());
        }

        if (yeeManageLike.getIsLock() != null) {
            if (!hasWhereClause) {
                sqlBuilder.append(" WHERE isLock = ?");
                hasWhereClause = true;
            } else {
                sqlBuilder.append(" AND isLock = ?");
            }
            params.add(yeeManageLike.getIsLock());
        }

        if (yeeManageLike.getIsActive() != null) {
            if (!hasWhereClause) {
                sqlBuilder.append(" WHERE active = ?");
                hasWhereClause = true;
            } else {
                sqlBuilder.append(" AND active = ?");
            }
            params.add(yeeManageLike.getIsActive());
        }

        for (SlSchool slSchool : slSchools) {
            try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
                // 打印最终 SQL 和参数用于调试
                PreparedStatement st = connection.prepareStatement(sqlBuilder.toString());
                for (int i = 0; i < params.size(); i++) {
                    Object param = params.get(i);
                    if (param instanceof String) {
                        st.setString(i + 1, (String) param);
                    } else if (param instanceof Integer) {
                        st.setInt(i + 1, (Integer) param);
                    }
                }
                ResultSet rs = st.executeQuery();
                yeeManages.addAll(rsToYeeManage(rs));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return Result.success(yeeManages);
    }

    @Override
    public Result actives(int schoolId, ArrayList<Integer> id) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId);
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));
        String sql = "UPDATE yee_manage SET active = 1 WHERE id = ?";
        PreparedStatement st = connection.prepareStatement(sql);
        for (Integer integer : id) {
            st.setInt(1, integer);
            if (st.executeUpdate() > 0)
                st.clearParameters();
            else return Result.error("更新失败：未找到匹配的记录");
        }
        st.close();
        connection.close();
        return Result.success("更新成功");
    }

    @Override
    public Result password(int schoolId, ArrayList<Integer> id) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", schoolId);
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchools = slSchoolMapper.selectList(queryWrapper);
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchools.get(0));
        String sql = "UPDATE yee_manage SET password = ? WHERE id = ?";
        PreparedStatement st = connection.prepareStatement(sql);
        for (Integer integer : id) {
            st.setString(1, bCryptPasswordEncoder.encode("b123456"));
            st.setInt(2, integer);
            if (st.executeUpdate() > 0)
                st.clearParameters();
            else return Result.error("更新失败：未找到匹配的记录");
        }
        st.close();
        connection.close();
        return Result.success("更新成功", "b123456");
    }

    @Override
    public Result add(YeeManage yeeManage) throws Exception {
        QueryWrapper<SlSchool> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id",yeeManage.getSchoolId());
        queryWrapper.eq("allow", 1);
        List<SlSchool> slSchool = slSchoolMapper.selectList(queryWrapper);
        if (!slSchool.isEmpty()) {
            Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool.get(0));
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            
            StringBuilder columns = new StringBuilder("INSERT INTO yee_manage (");
            StringBuilder values = new StringBuilder("VALUES (");
            List<Object> parameters = new ArrayList<>();
            
            // 必填字段：schoolId
            columns.append("`schoolId`, ");
            values.append("?, ");
            parameters.add(yeeManage.getSchoolId());
            
            // 动态添加可选字段
            if (yeeManage.getAccount() != null && !yeeManage.getAccount().trim().isEmpty()) {
                columns.append("`account`, ");
                values.append("?, ");
                parameters.add(yeeManage.getAccount());
            }
            
            // 密码字段：如果没有提供则使用默认密码
            columns.append("`password`, ");
            values.append("?, ");
            if (yeeManage.getPassword() != null && !yeeManage.getPassword().trim().isEmpty()) {
                parameters.add(bCryptPasswordEncoder.encode(yeeManage.getPassword()));
            } else {
                parameters.add(bCryptPasswordEncoder.encode("b123456"));
            }
            
            if (yeeManage.getName() != null && !yeeManage.getName().trim().isEmpty()) {
                columns.append("`name`, ");
                values.append("?, ");
                parameters.add(yeeManage.getName());
            }
            
            if (yeeManage.getEmail() != null && !yeeManage.getEmail().trim().isEmpty()) {
                columns.append("`email`, ");
                values.append("?, ");
                parameters.add(yeeManage.getEmail());
            }
            
            if (yeeManage.getMobile() != null && !yeeManage.getMobile().trim().isEmpty()) {
                columns.append("`mobile`, ");
                values.append("?, ");
                parameters.add(yeeManage.getMobile());
            }
            
            if (yeeManage.getGender() != null && !yeeManage.getGender().trim().isEmpty()) {
                columns.append("`gender`, ");
                values.append("?, ");
                parameters.add(yeeManage.getGender());
            }
            
            if (yeeManage.getWeChat() != null && !yeeManage.getWeChat().trim().isEmpty()) {
                columns.append("`wechat`, ");
                values.append("?, ");
                parameters.add(yeeManage.getWeChat());
            }
            
            if (yeeManage.getIntro() != null && !yeeManage.getIntro().trim().isEmpty()) {
                columns.append("`intro`, ");
                values.append("?, ");
                parameters.add(yeeManage.getIntro());
            }
            
            if (yeeManage.getSl_super() >= 0) {
                columns.append("`super`, ");
                values.append("?, ");
                parameters.add(yeeManage.getSl_super());
            }
            
            if (yeeManage.getCollegeId() > 0) {
                columns.append("`collegeId`, ");
                values.append("?, ");
                parameters.add(yeeManage.getCollegeId());
            }
            
            if (yeeManage.getGeneral() >= 0) {
                columns.append("`general`, ");
                values.append("?, ");
                parameters.add(yeeManage.getGeneral());
            }
            
            if (yeeManage.getLoginCode() != null && !yeeManage.getLoginCode().trim().isEmpty()) {
                columns.append("`loginCode`, ");
                values.append("?, ");
                parameters.add(yeeManage.getLoginCode());
            }
            
            if (yeeManage.getRecommend() >= 0) {
                columns.append("`recommend`, ");
                values.append("?, ");
                parameters.add(yeeManage.getRecommend());
            }
            
            if (yeeManage.getActive() >= 0) {
                columns.append("`active`, ");
                values.append("?, ");
                parameters.add(yeeManage.getActive());
            }
            
            if (yeeManage.getColleges() != null && !yeeManage.getColleges().trim().isEmpty()) {
                columns.append("`colleges`, ");
                values.append("?, ");
                parameters.add(yeeManage.getColleges());
            }
            
            if (yeeManage.getRole() >= 0) {
                columns.append("`role`, ");
                values.append("?, ");
                parameters.add(yeeManage.getRole());
            }
            
            if (yeeManage.getAvatar() != null && !yeeManage.getAvatar().trim().isEmpty()) {
                columns.append("`avatar`, ");
                values.append("?, ");
                parameters.add(yeeManage.getAvatar());
            }
            
            if (yeeManage.getForce() >= 0) {
                columns.append("`force`, ");
                values.append("?, ");
                parameters.add(yeeManage.getForce());
            }
            
            if (yeeManage.getPassport() != null && !yeeManage.getPassport().trim().isEmpty()) {
                columns.append("`passport`, ");
                values.append("?, ");
                parameters.add(yeeManage.getPassport());
            }
            
            if (yeeManage.getBindId() > 0) {
                columns.append("`bindId`, ");
                values.append("?, ");
                parameters.add(yeeManage.getBindId());
            }
            
            // 自动设置添加时间
            columns.append("`addTime`, ");
            values.append("?, ");
            parameters.add(yeeManage.getAddTime() != null ? yeeManage.getAddTime() : new java.sql.Timestamp(System.currentTimeMillis()));
            
            // addDate是生成列，不需要手动插入
            
            // 删除最后的逗号和空格
            columns.delete(columns.length() - 2, columns.length());
            values.delete(values.length() - 2, values.length());
            
            // 构建完整SQL
            columns.append(") ");
            values.append(")");
            String sql = columns.toString() + values.toString();
            
            PreparedStatement st = connection.prepareStatement(sql);
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof String) {
                    st.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    st.setInt(i + 1, (Integer) param);
                } else if (param instanceof Long) {
                    st.setLong(i + 1, (Long) param);
                } else if (param instanceof Double) {
                    st.setDouble(i + 1, (Double) param);
                } else if (param instanceof Boolean) {
                    st.setBoolean(i + 1, (Boolean) param);
                } else if (param instanceof java.util.Date) {
                    if (param instanceof java.sql.Timestamp) {
                        st.setTimestamp(i + 1, (java.sql.Timestamp) param);
                    } else if (param instanceof java.sql.Date) {
                        st.setDate(i + 1, (java.sql.Date) param);
                    } else {
                        st.setDate(i + 1, new java.sql.Date(((java.util.Date) param).getTime()));
                    }
                }
            }
            
            int rowsInserted = st.executeUpdate();
            st.close();
            connection.close();
            
            if (rowsInserted > 0) {
                return Result.success("添加成功");
            } else {
                return Result.error("添加失败");
            }
        } else {
            return Result.error("学校不存在或未审核");
        }
    }
    // 在 YeeManageServiceImpl 中新增
    public void updateLoginInfo(YeeManage updateDto) {
        QueryWrapper<SlSchool> qw = new QueryWrapper<>();
        qw.eq("id", updateDto.getSchoolId()).eq("allow", 1);
        SlSchool school = slSchoolMapper.selectOne(qw);
        if (school == null) {
            throw new RuntimeException("学校不存在或未启用");
        }

        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(school)) {
            // 先查出当前记录的 thisTime 和 thisIp
            String selectSql = "SELECT thisTime, thisIp FROM yee_manage WHERE id = ?";
            PreparedStatement selSt = conn.prepareStatement(selectSql);
            selSt.setLong(1, updateDto.getId());
            ResultSet rs = selSt.executeQuery();

            Timestamp oldThisTime = null;
            String oldThisIp = null;
            if (rs.next()) {
                oldThisTime = rs.getTimestamp("thisTime");
                oldThisIp = rs.getString("thisIp");
            }
            rs.close();
            selSt.close();

            // 再更新
            String updateSql = "UPDATE yee_manage SET " +
                    "lastTime = ?, lastIp = ?, " +
                    "thisTime = ?, thisIp = ? " +
                    "WHERE id = ?";
            PreparedStatement updSt = conn.prepareStatement(updateSql);
            updSt.setTimestamp(1, oldThisTime);
            updSt.setString(2, oldThisIp);
            updSt.setTimestamp(3, updateDto.getThisTime());
            updSt.setString(4, updateDto.getThisIp());
            updSt.setLong(5, updateDto.getId());

            int rows = updSt.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("YeeManage 用户不存在，ID: " + updateDto.getId());
            }
            updSt.close();
        } catch (Exception e) {
            throw new RuntimeException("更新 YeeManage 登录信息失败", e);
        }
    }
}
