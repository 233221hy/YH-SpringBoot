package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.mhmain.SlRole;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.mhsch.YeeManage;
import cn.xfywz.guozespring.entity.vo.LoginUser;
import cn.xfywz.guozespring.entity.dto.YeeManageQueryParam;
import cn.xfywz.guozespring.entity.vo.YeeManageListVO;
import cn.xfywz.guozespring.entity.vo.YeeManageVO;
import cn.xfywz.guozespring.exception.BusinessException;
import cn.xfywz.guozespring.exception.DatabaseException;
import cn.xfywz.guozespring.mapper.SlRoleMapper;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.teacher.TeacherYeeManageService;
import cn.xfywz.guozespring.util.*;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.QueryBuilder;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import cn.xfywz.guozespring.util.db.BuiltSql;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import cn.xfywz.guozespring.excel.ExcelImportBuilder;
import cn.xfywz.guozespring.excel.ImportResult;
import cn.xfywz.guozespring.excel.validation.TeacherImportValidationFactory;
import cn.xfywz.guozespring.entity.dto.TeacherExcelData;
import java.util.stream.Collectors;

import static cn.xfywz.guozespring.constant.CacheConstant.CACHE_KEY_SEPARATOR;
import static cn.xfywz.guozespring.constant.CacheConstant.USER_PERMISSION_LIST_CACHE_KEY;
import static cn.xfywz.guozespring.util.EncodePasswordUtil.encodePassword;


@Slf4j
@Service("teacherYeeManageService")
@RequiredArgsConstructor
public class TeacherYeeManageServiceImpl implements TeacherYeeManageService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private SlRoleMapper slRoleMapper;
    @Autowired
    private RedisTemplate redisTemplate;

    private final DatabaseUtil databaseUtil;

    private final BusinessValidator businessValidator;

    //====================== 构建SQL ======================

    /**
     * 构建基础查询SQL
     * @return SQL语句
     */
    private String buildQuerySql() {
        return """
        SELECT
        ym.id, ym.account, ym.name, ym.lastIp, ym.lastTime, ym.isLock, ym.email,
        ym.avatar, ym.role, ym.mobile, ym.collegeId, ym.active, yc.name AS collegeName
        FROM yee_manage ym
        LEFT JOIN yee_college yc ON ym.collegeId = yc.id AND yc.schoolId = ym.schoolId
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


    //=============== 执行方法 ======================

    /**
     * 根据JWT授权令牌获取用户管理信息
     */
    @Override
    public YeeManage getInfo(String Authorization) throws Exception {
        //解析授权令牌
        Claims claims = JwtTokenUtil.parseToken(Authorization);
        //解析成功则返回用户管理信息对象
        if (claims != null) {
            LoginUser loginTeacher = JSON.parseObject(claims.getSubject(), LoginUser.class);
            return loginTeacher.getYeeManage();
        }
        return null;
    }

    /**
     * 更新个人信息
     */
    @Override
    public Result infoUpdate(String Authorization,YeeManage yeeManage) {
        try {
            // 根据令牌获取数据库的用户信息
            YeeManage currentYee = getInfo(Authorization);
            if (currentYee == null) {
                return Result.error("用户信息获取失败");
            }
            // 更新操作
            int rows = databaseUtil.update(currentYee.getSchoolId())
                    .table("yee_manage")
                    .setIfNotEmpty("name", yeeManage.getName())
                    .setIfNotEmpty("email", yeeManage.getEmail())
                    .setIfNotEmpty("avatar", yeeManage.getAvatar())
                    .setIfNotEmpty("mobile", yeeManage.getMobile())
                    .setIfNotNull("gender", yeeManage.getGender())
                    .setIfNotEmpty("wechat", yeeManage.getWeChat())
                    .setIfNotEmpty("intro", yeeManage.getIntro())
                    .eq("id", yeeManage.getId())
                    .eq("schoolId", yeeManage.getSchoolId())
                    .update();

            if (rows > 0) {
                return Result.success("更新成功");
            } else {
                return Result.error("更新失败：未找到匹配的记录");
            }
        } catch (Exception e) {
            log.error("更新个人信息失败: id={}, schoolId={}",
                    yeeManage.getId(), yeeManage.getSchoolId(), e);
            throw new RuntimeException("更新个人信息失败: " + e.getMessage(), e);
        }
    }


    /**
     * 修改用户密码
     */
    @Override
    public Result infoUpdatePassword(String oldPassword, String newPassword, String Authorization) {
        try {
            // 根据令牌获取数据库的用户信息
            YeeManage currentYee = getInfo(Authorization);
            if (currentYee == null) {
                return Result.error("用户信息获取失败");
            }


            // 使用统一的数据库工具进行操作
            // 1. 查询当前用户的密码
            Optional<String> currentPasswordOpt = databaseUtil.query((int) currentYee.getSchoolId())
                    .sql("SELECT password FROM yee_manage WHERE id = ?")
                    .param(currentYee.getId())
                    .single(rs -> {
                        try {
                            return rs.getString("password");
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                    });

            // 2. 验证旧密码
            if (!currentPasswordOpt.map(encodedPass -> EncodePasswordUtil.matches(oldPassword, encodedPass))
                .orElse(false)) {
                return Result.error("旧密码错误");
            }

            // 3. 验证新密码强度
            String passwordStrengthError = EncodePasswordUtil.validatePasswordStrength(newPassword);
            if (passwordStrengthError != null) {
                return Result.error("新密码不符合要求: " + passwordStrengthError);
            }

            // 4. 新旧密码不能相同
            if (EncodePasswordUtil.matches(newPassword, oldPassword)) {
                return Result.error("新密码不能与旧密码相同");
            }

            // 5. 加密新密码并更新
            String encodedNewPassword = encodePassword(newPassword);

            int rowsUpdated = databaseUtil.update((int) currentYee.getSchoolId())
                    .table("yee_manage")
                    .set("password", encodedNewPassword)
                    .eq("id", currentYee.getId())
                    .update();

            if (rowsUpdated > 0) {
                return Result.success("密码修改成功");
            } else {
                return Result.error("密码更新失败");
            }
        } catch (Exception e) {
            log.error("修改密码失败: schoolId={}",
                    Thread.currentThread().getStackTrace()[1].getMethodName(), e);
            throw new RuntimeException("修改密码失败: " + e.getMessage(), e);
        }
    }


    /**
     * 查询所有YeeManage记录（含角色名称、学院名称）
     */
    @Override
    public Result selectAll(YeeManageQueryParam param) {
        try {
            // 分页查询教师列表
            PageResult<YeeManageListVO> pageResult = databaseUtil.query(param.getSchoolId())
                    .sql(buildQuerySql()) // 构建基础SQL
//                    .param(param.getSchoolId()) // 设置学校ID参数
//                    .param(param.getSchoolId()) // 设置WHERE条件中的学校ID
                    .eq("ym.schoolId", param.getSchoolId())
                    .apply(qb -> applyQueryConditions(qb, param)) // 应用查询条件
                    .orderBy("ym.id DESC") // 按ID降序排列
                    .page(YeeManageListVO::fromResultSet, param.getPageNum(), param.getPageSize());

            // 获取所有角色ID
            Set<Long> roleIds = pageResult.getRows().stream()
                    .map(YeeManageListVO::getRole)
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

    /**
     * 添加YeeManage记录
     */
    @Override
    public Result add(YeeManage yeeManage) {

        //  验证账号唯一性
        businessValidator.validateTeacherAccountUnique(databaseUtil, yeeManage.getSchoolId(),
                yeeManage.getAccount(), null);
        //  插入数据
        Long generatedId = databaseUtil.update(yeeManage.getSchoolId())
                .table("yee_manage")
                .set("schoolId", yeeManage.getSchoolId())
                .set("addTime", yeeManage.getAddTime() != null ?
                        yeeManage.getAddTime() : new Timestamp(System.currentTimeMillis()))
                .set("account", yeeManage.getAccount())
                .set("password", encodePassword(yeeManage.getPassword() != null ? yeeManage.getPassword() : "a123456"))
                .set("collegeId", yeeManage.getCollegeId())
                .setIfNotEmpty("name", yeeManage.getName())
                .setIfNotEmpty("email", yeeManage.getEmail())
                .setIfNotEmpty("mobile", yeeManage.getMobile())
                .setIfNotEmpty("gender", yeeManage.getGender())
                .setIfNotEmpty("wechat", yeeManage.getWeChat())
                .setIfNotEmpty("intro", yeeManage.getIntro())
                .setIfNotEmpty("avatar", yeeManage.getAvatar())
                .setIfPositive("role", yeeManage.getRole())
                .setIfNotNegative("recommend", yeeManage.getRecommend())
                .setIfNotNegative("force", yeeManage.getForce())
                .setIfNotEmpty("colleges", yeeManage.getColleges())
                .set("general", 1)
                .set("active", yeeManage.getActive() != 0 ? 1 : 0)
                .insert();
        if (generatedId != null && generatedId > 0) {
            yeeManage.setId(generatedId);
            return Result.success("添加用户成功");
        } else {
            return Result.error("添加用户失败");
        }

    }


    /**
     * 修改教师账号信息
     */
    @Override
    public void update(YeeManage yeeManage) {
        try {
            // 检验账号唯一性
            businessValidator.validateTeacherAccountUnique(databaseUtil, yeeManage.getSchoolId(),yeeManage.getAccount(),yeeManage.getId());
            // 构建更新操作
            int rows = databaseUtil.update(yeeManage.getSchoolId())
                    .table("yee_manage")
                    .setIfNotEmpty("account", yeeManage.getAccount())
                    .setIfNotEmpty("name", yeeManage.getName())
                    .setIfNotEmpty("email", yeeManage.getEmail())
                    .setIfNotEmpty("mobile", yeeManage.getMobile())
                    .setIfNotEmpty("wechat", yeeManage.getWeChat())
                    .setIfNotEmpty("intro", yeeManage.getIntro())
                    .setIfNotEmpty("avatar", yeeManage.getAvatar())
                    .setIfNotEmpty("colleges", yeeManage.getColleges())
                    .setIfNotNegative("collegeId", yeeManage.getCollegeId()) // >=0
                    .setIfPositive("role", yeeManage.getRole())             // >0
                    .setIfNotNegative("isLock", yeeManage.getIsLock())      // >=0
                    .setIfNotNegative("recommend", yeeManage.getRecommend())// >=0
                    .setIfNotEmpty("gender", yeeManage.getGender())
                    //激活状态
                    .setIfNotNegative("active", yeeManage.getActive())
                    .apply(builder -> {
                        // 密码特殊处理：非空则加密
                        if (StringUtils.hasText(yeeManage.getPassword())) {
                            builder.set("password", EncodePasswordUtil.encodePassword(yeeManage.getPassword()));
                        }
                    })
                    .eq("id", yeeManage.getId())
                    .eq("schoolId", yeeManage.getSchoolId())
                    .update();

            // 如果更新了 role 字段，需清除该用户的权限缓存
            if (yeeManage.getRole() >= 0) {
                if (rows > 0 && StringUtils.hasText(yeeManage.getAccount())) {
                    redisTemplate.delete(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + yeeManage.getAccount());
                }
            }

            if (rows > 0) {
                Result.success("更新成功");
            } else {
                Result.error("更新失败：教师不存在或数据未变化");
            }

        } catch (Exception e) {
            log.error("更新教师未知异常: id={}", yeeManage.getId(), e);
            Result.error("更新失败");
        }
    }


    /**
     * 删除教师账号
     */
    @Override
    public void delete(Long id, int schoolId, String account) {
        try {
            //  执行删除
            int rows = databaseUtil.update(schoolId)
                    .table("yee_manage")
                    .eq("id", id)
                    .eq("schoolId", schoolId)
                    .delete();

            //  判断是否删除成功
            if (rows == 0) {
                throw new BusinessException("删除失败：教师不存在或已被删除");
            }

            //  清除该用户的权限缓存（如果有 account）
            if (StringUtils.hasText(account)) {
                redisTemplate.delete(USER_PERMISSION_LIST_CACHE_KEY + CACHE_KEY_SEPARATOR + account);
            }

            // 记录日志
        } catch (Exception e) {
            // 其他异常包装为业务异常
            log.error("删除教师失败: id={}, schoolId={}, account={}", id, schoolId, account, e);
            throw new BusinessException("删除教师失败");
        }
    }


    /**
     * 锁定或解锁用户（0 ↔ 1 切换）
     */
    @Override
    public void lock(Long id, int schoolId){
//        int newLock = databaseUtil.query(schoolId)
//                .sql("SELECT isLock FROM yee_manage WHERE id = ?")
//                .param(id)
//                .scalar(rs -> rs.getInt("isLock"))
//                .map(lock -> 1 - lock)                     // 切换状态：0→1, 1→0
//                .orElseThrow(() -> BusinessException.schoolNotApproved(schoolId));
//
//        //  执行更新
//        int rows = databaseUtil.update(schoolId)
//                .table("yee_manage")
//                .set("isLock", newLock)
//                .eq("id", id)
//                .eq("schoolId", schoolId)
//                .update();
        int rows = databaseUtil.update(schoolId)
                .table("yee_manage")
                .setRaw("isLock = 1 - COALESCE(isLock, 0)")
                .eq("id", id)
                .eq("schoolId", schoolId)
                .update();

        if (rows == 0) {
            throw new BusinessException("操作失败：用户记录未更新");
        }

//        //  记录日志
//        String action = (newLock == 1) ? "锁定" : "解锁";
    }

    @Override
    public Result searchByCondition(YeeManageQueryParam param) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        class SqlBuilder {
            // 构建列表查询 SQL
            BuiltSql buildListSql(YeeManageQueryParam p) {
                StringBuilder sql = new StringBuilder(
                        "SELECT ym.*, yc.name AS collegeName " +
                                "FROM yee_manage ym " +
                                "LEFT JOIN yee_college yc ON ym.collegeId = yc.id AND yc.schoolId = ?"
                );

                List<Object> params = new ArrayList<>();
                params.add(p.getSchoolId());

                List<String> conditions = buildConditions(p, params);

                if (!conditions.isEmpty()) {
                    sql.append(" WHERE ").append(String.join(" AND ", conditions));
                }

                if (p.getPageNum() > 0 && p.getPageSize() > 0) {
                    int offset = (p.getPageNum() - 1) * p.getPageSize();
                    sql.append(" LIMIT ? OFFSET ?");
                    params.add(p.getPageSize());
                    params.add(offset);
                }

                return BuiltSql.of(sql.toString(), params);
            }

            // 构建 COUNT 查询 SQL
            BuiltSql buildCountSql(YeeManageQueryParam p) {
                StringBuilder sql = new StringBuilder(
                        "SELECT COUNT(*) FROM yee_manage ym " +
                                "LEFT JOIN yee_college yc ON ym.collegeId = yc.id AND yc.schoolId = ?"
                );

                List<Object> params = new ArrayList<>();
                params.add(p.getSchoolId());

                List<String> conditions = buildConditions(p, params);

                if (!conditions.isEmpty()) {
                    sql.append(" WHERE ").append(String.join(" AND ", conditions));
                }

                return BuiltSql.of(sql.toString(), params);
            }

            // 提取公共条件构建逻辑
            private List<String> buildConditions(YeeManageQueryParam p, List<Object> params) {
                List<String> conditions = new ArrayList<>();

                if (p.getAccount() != null && !p.getAccount().isEmpty()) {
                    conditions.add("ym.account LIKE ?");
                    params.add("%" + p.getAccount() + "%");
                }
                if (p.getName() != null && !p.getName().isEmpty()) {
                    conditions.add("ym.name LIKE ?");
                    params.add("%" + p.getName() + "%");
                }
                if (p.getCollegeId() != null && p.getCollegeId() > 0) {
                    conditions.add("ym.collegeId = ?");
                    params.add(p.getCollegeId());
                }
                if (p.getRole() != null) {
                    conditions.add("ym.role = ?");
                    params.add(p.getRole());
                }
                if (p.getRecommend() != null) {
                    conditions.add("ym.recommend = ?");
                    params.add(p.getRecommend());
                }
                if (p.getIsLock() != null) {
                    conditions.add("ym.isLock = ?");
                    params.add(p.getIsLock());
                }
                if (p.getActive() != null) {
                    conditions.add("ym.active = ?");
                    params.add(p.getActive());
                }

                return conditions;
            }
        }

        class DbExecutor {
            List<YeeManageVO> queryList(SlSchool school, BuiltSql built) throws Exception {
                try (Connection connection = SlaveMysqlConnectionUtil.getConnection(school);
                     PreparedStatement st = connection.prepareStatement(built.sql())) {
                    for (int i = 0; i < built.params().size(); i++) {
                        st.setObject(i + 1, built.params().get(i));
                    }
                    try (ResultSet rs = st.executeQuery()) {
                        List<YeeManageVO> list = new ArrayList<>();
                        while (rs.next()) {
                            YeeManageVO vo = new YeeManageVO();
                            vo.setId(rs.getLong("id"));
                            vo.setAccount(rs.getString("account"));
                            vo.setName(rs.getString("name"));
                            vo.setLastIp(rs.getString("lastIp"));
                            vo.setLastTime(rs.getTimestamp("lastTime"));
                            vo.setIsLock(rs.getInt("isLock"));
                            vo.setEmail(rs.getString("email"));
                            vo.setAvatar(rs.getString("avatar"));
                            vo.setRole(rs.getLong("role")); // Long 类型
                            vo.setMobile(rs.getString("mobile"));
                            vo.setCollegeId(rs.getLong("collegeId"));
                            vo.setActive(rs.getInt("active"));
                            vo.setCollegeName(rs.getString("collegeName"));
                            list.add(vo);
                        }
                        return list;
                    }
                }
            }

            long queryCount(SlSchool school, BuiltSql built) throws Exception {
                try (Connection connection = SlaveMysqlConnectionUtil.getConnection(school);
                     PreparedStatement st = connection.prepareStatement(built.sql())) {
                    for (int i = 0; i < built.params().size(); i++) {
                        st.setObject(i + 1, built.params().get(i));
                    }
                    try (ResultSet rs = st.executeQuery()) {
                        if (rs.next()) {
                            return rs.getLong(1);
                        }
                        return 0L;
                    }
                }
            }
        }

        try {
            SqlBuilder builder = new SqlBuilder();
            DbExecutor executor = new DbExecutor();

            // 查询总数
            BuiltSql countSql = builder.buildCountSql(param);
            long total = executor.queryCount(slSchool, countSql);

            // 查询列表
            BuiltSql listSql = builder.buildListSql(param);
            List<YeeManageVO> list = executor.queryList(slSchool, listSql);

            // 批量获取 roleName（主库）
            Set<Long> roleIds = list.stream()
                    .map(YeeManageVO::getRole)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Map<Long, String> roleMap = new HashMap<>();
            for (Long roleId : roleIds) {
                SlRole slRole = slRoleMapper.selectByIdAndSchoolId(roleId, param.getSchoolId());
                if (slRole != null) {
                    roleMap.put(roleId, slRole.getName());
                }
            }

            // 回填 roleName
            for (YeeManageVO vo : list) {
                vo.setRoleName(roleMap.get(vo.getRole()));
            }

            return Result.success(list, total); // ✅ 带 total 的分页结果
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Override
    public Result searchById(Long id, int schoolId) {
        String sql = """
                SELECT
                       ym.id,
                       ym.account,
                       ym.name,
                       ym.email,
                       ym.avatar,
                       ym.mobile,
                       ym.gender,
                       ym.wechat,
                       ym.intro,
                       ym.isLock,
                       ym.collegeId,
                       ym.role,
                       ym.lastTime,
                       ym.lastIp,
                       ym.recommend,
                       ym.active,
                       yc.name as collegeName,
                       ym.colleges,
                       ym.passport
                   FROM yee_manage ym
                   LEFT JOIN yee_college yc ON ym.collegeId = yc.id
                """;
        return databaseUtil.query(schoolId)
                .sql(sql)
                .eq("ym.id", id)
                .single(YeeManageVO::fromResultSet)
                .map(teacher -> {
                    // 1. 角色名称（主库，可用 Mapper）
                    if (teacher.getRole() != null && teacher.getRole() > 0) {
                        SlRole slRole = slRoleMapper.selectByIdAndSchoolId(teacher.getRole(), schoolId);
                        teacher.setRoleName(slRole != null ? slRole.getName() : "未知角色");
                    }

                    // 2. 兼职学院名称
                    List<Long> collegeIds = YeeManageVO.parseCollegeIds(teacher.getColleges());
                    if (!collegeIds.isEmpty()) {
                        // 使用 DatabaseUtil 从当前学校的从库查询学院名
                        List<String> collegeNames = databaseUtil.query(schoolId)
                                .sql("SELECT name FROM yee_college")
                                .in("id", collegeIds)
                                .list(rs -> {
                                    try {
                                        return rs.getString("name");
                                    } catch (SQLException e) {
                                        throw new DatabaseException("读取学院名称失败", e);
                                    }
                                });
                        teacher.setCollegeNames(collegeNames != null ? collegeNames : Collections.emptyList());
                    } else {
                        teacher.setCollegeNames(Collections.emptyList());
                    }

                    return Result.success(teacher);
                })
                .orElse(Result.error("未找到对应的教师信息"));


    }

    @Override
    public Result importData(int schoolId, Long collegeId, MultipartFile file) {
        // 基础校验
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }
        if (collegeId == null || collegeId <= 0) {
            return Result.error("请选择所属学院");
        }
        if (file == null || file.isEmpty()) {
            return Result.error("文件不能为空");
        }
    
        // 查询教师角色ID（只需一次）
        List<SlRole> roleList = slRoleMapper.selectList(
                new QueryWrapper<SlRole>().eq("name", "教师")
                                          .eq("schoolId", schoolId)
                                          .last("LIMIT 1")
        );
        if (roleList.isEmpty()) {
            return Result.error("未找到教师角色");
        }
        Long teacherRoleId = roleList.get(0).getId();
    
//        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        // 预先计算一次默认密码哈希，避免循环内重复BCrypt加密
        String defaultHashedPassword = EncodePasswordUtil.encodePassword("a123456");
        long startTime = System.currentTimeMillis();
    
        try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            if (connection == null) {
                return Result.error("无法获取数据库连接");
            }
            // 1) 检查学院是否存在（复用同一连接）
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM yee_college WHERE id = ? AND schoolId = ?")) {
                ps.setLong(1, collegeId);
                ps.setInt(2, schoolId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next() || rs.getInt(1) == 0) {
                        return Result.error("学院不存在或不属于该学校");
                    }
                }
            }

            Set<String> existingAccounts = new HashSet<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT account FROM yee_manage WHERE schoolId = ?")) {
                ps.setInt(1, schoolId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String account = rs.getString("account");
                        if (account != null && !account.isBlank()) {
                            existingAccounts.add(account);
                        }
                    }
                }
            }

            TeacherImportValidationFactory.TeacherImportValidationContext ctx =
                    TeacherImportValidationFactory.createContext(existingAccounts);

            // 收集校验通过的行（先收集，后统一插入，保持"全量校验通过才入库"语义）
            List<TeacherExcelData> validRows = new ArrayList<>();

            ImportResult result = ExcelImportBuilder
                    .of(TeacherExcelData.class)
                    .from(file.getInputStream())
                    .preprocess(TeacherExcelData::cleanData)
                    .businessValidator(TeacherImportValidationFactory.createBusinessValidator(ctx))
                    .batchPersist(batch -> {
                        validRows.addAll(batch);
                        return batch.size();
                    })
                    .execute();

            if (!result.isSuccess()) {
                return Result.error(result.getFailMessage("部分数据校验失败，已全部回滚"), result.toMap());
            }

            // 全量校验通过后统一入库
            List<YeeManage> toInsert = new ArrayList<>();
            Timestamp now = new Timestamp(System.currentTimeMillis());
            for (TeacherExcelData row : validRows) {
                YeeManage m = new YeeManage();
                m.setSchoolId(schoolId);
                m.setAddTime(now);
                m.setAccount(row.getLoginAccount());
                m.setPassword(defaultHashedPassword);
                m.setName(row.getName());
                m.setEmail(row.getEmail());
                m.setMobile(row.getPhone());
                m.setGender(row.getGender());
                m.setCollegeId(collegeId);
                m.setRecommend(0L);
                m.setForce(0L);
                m.setRole(teacherRoleId);
                m.setGeneral(1);
                m.setActive(1);
                toInsert.add(m);
            }

            int inserted = batchInsert(connection, toInsert);
            result.setDbInserted(inserted);
            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);

            return Result.success("导入成功", (Object) result.toMap());
        } catch (Exception e) {
            log.error("导入失败：" + e.getMessage(), e);
            return Result.error("导入失败：" + e.getMessage(),
                    ImportResult.systemError(e.getMessage(), System.currentTimeMillis() - startTime).toMap());
        }
    }


    // 批量插入教师数据
    private int batchInsert(Connection connection, List<YeeManage> manageList) throws Exception {
        if (manageList == null || manageList.isEmpty()) {
            return 0;
        }
    
        String sql = "INSERT INTO yee_manage (schoolId, addTime, account, password, collegeId, name, email, mobile, gender, role, recommend, `force`, `general`, active) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    
        boolean prevAutoCommit = connection.getAutoCommit();
        int inserted = 0;
        try (PreparedStatement pst = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false);
            int counter = 0;
            for (YeeManage m : manageList) {
                pst.setInt(1, (int) m.getSchoolId());
                pst.setTimestamp(2, m.getAddTime());
                pst.setString(3, m.getAccount());
                pst.setString(4, m.getPassword());
                pst.setLong(5, m.getCollegeId());
                pst.setString(6, m.getName());
                pst.setString(7, m.getEmail());
                pst.setString(8, m.getMobile());
                pst.setString(9, m.getGender());
                pst.setLong(10, m.getRole());
                pst.setLong(11, m.getRecommend());
                pst.setLong(12, m.getForce());
                pst.setInt(13, (int) m.getGeneral());
                pst.setInt(14, (int) m.getActive());
    
                pst.addBatch();
                counter++;
                if (counter % 1000 == 0) {
                    int[] results = pst.executeBatch();
                    for (int r : results) {
                        if (r == java.sql.Statement.SUCCESS_NO_INFO || r > 0) {
                            inserted++;
                        }
                    }
                    pst.clearBatch();
                }
            }
            int[] results = pst.executeBatch();
            for (int r : results) {
                if (r == java.sql.Statement.SUCCESS_NO_INFO || r > 0) {
                    inserted++;
                }
            }
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(prevAutoCommit);
        }
    
        return inserted;
    }




    // 批量重置教师密码
    @Override
    public Result passwordReset(int schoolId, List<Long> ids) {
        // 1. 基本校验
        if (ids == null || ids.isEmpty()) {
            return Result.error("缺少用户ID列表");
        }
        if (ids.size() > 1000) {
            return Result.error("单次重置用户数量不能超过1000");
        }

        try {
            // 2. 使用工具类加密默认密码
            String encodedPassword = EncodePasswordUtil.encodePassword("a123456");

            // 3. 使用 DmlBuilder 执行批量更新
            int rowsUpdated = databaseUtil.update(schoolId)
                    .table("yee_manage")
                    .set("password", encodedPassword)
                    .in("id", ids)
                    .eq("schoolId", schoolId)
                    .update();

            // 4. 返回结果
            if (rowsUpdated > 0) {
                return Result.success("密码重置成功", rowsUpdated);
            } else {
                return Result.error("密码重置失败：未匹配到任何记录，请检查用户ID是否正确");
            }

        } catch (IllegalArgumentException e) {
            // 密码加密参数异常
            log.error("密码加密参数错误", e);
            return Result.error("密码加密失败：" + e.getMessage());
        } catch (Exception e) {
            // 其他异常（包括数据库操作异常）
            log.error("密码重置失败，schoolId: {}, ids: {}", schoolId, ids, e);
            return Result.error("密码重置失败：" + e.getMessage());
        }
    }

}




