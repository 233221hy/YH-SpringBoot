package cn.xfywz.guozespring.service.teacher.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.xfywz.guozespring.constant.DataAuth;
import cn.xfywz.guozespring.entity.dto.CourseStudentEnrollmentExportDto;
import cn.xfywz.guozespring.entity.mhmain.*;
import cn.xfywz.guozespring.entity.mhsch.*;
import cn.xfywz.guozespring.entity.vo.*;
import cn.xfywz.guozespring.entity.dto.YeeCourseQueryParam;
import cn.xfywz.guozespring.mapper.*;
import cn.xfywz.guozespring.service.teacher.YeeCourseService;
import cn.xfywz.guozespring.util.JwtTokenUtil;
import cn.xfywz.guozespring.util.PageResult;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSONArray;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import cn.xfywz.guozespring.excel.ResponseExportUtil;
import org.springframework.util.StringUtils;


/**
 * 课程管理
 * 课程基础操作：
 * selectAll：分页查询所有课程
 * add：添加新课程
 * update：更新课程信息
 * deleteById：删除指定课程
 * like：按条件模糊查询课程
 * 课程模板导入：
 * courseTemplateImport：从模板库导入课程到当前学校
 * 包含完整的事务处理，同时导入课程、章节、节点和文件
 * 数据导出：
 * exportCourseData：将课程数据导出为Excel文件
 * 条件查询：
 * selectAllWithConditions：支持多种条件的分页查询
 */

@Service
public class YeeCourseServiceImpl implements YeeCourseService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private SlTplChapterMapper slTplChapterMapper;
    @Autowired
    private SlTplNodeMapper slTplNode;
    @Autowired
    private SlTplNodeFilesMapper slTplNodeFilesMapper;
    @Autowired
    private DatabaseUtil databaseUtil;

    private List<YeeCourseExportVo> rsToYeeCourseExportVo(ResultSet rs, SlSchool slSchool) throws SQLException {
        List<YeeCourseExportVo> exportData = new ArrayList<>();
        String schoolName = slSchool.getName();
        while (rs.next()) {
            YeeCourseExportVo exportVo = new YeeCourseExportVo();
            exportVo.setId(rs.getLong("id"));
            exportVo.setName(rs.getString("name"));
            exportVo.setCode(rs.getString("code"));
            exportVo.setMode(rs.getLong("mode") == 2 ? "选修课" : "必修课");
            exportVo.setTplId(rs.getLong("tplId") != 0 ? "引用" : "自建");
            exportVo.setStartDate(rs.getObject("startDate", LocalDateTime.class));
            exportVo.setEndDate(rs.getObject("endDate", LocalDateTime.class));
            exportVo.setCredit(rs.getDouble("credit"));
            exportVo.setAllow(rs.getLong("allow") == 0 ? "禁用" : "正常");
            exportVo.setStuCount(rs.getLong("stuCount"));
            exportVo.setSchoolId(schoolName);
            exportVo.setAddDate(rs.getObject("addDate", LocalDateTime.class));
            exportVo.setCreateName(rs.getString("createName"));
            exportVo.setCollegeName(rs.getString("collegeName"));
            exportData.add(exportVo);
        }
        return exportData;
    }

    private List<YeeCourseListVo> rsToYeeCourseListVo(ResultSet rs) throws SQLException {
        List<YeeCourseListVo> yeeCourseList = new ArrayList<>();
        while (rs.next()) {
            YeeCourseListVo courseVo = new YeeCourseListVo();
            courseVo.setId(rs.getLong("id"));
            courseVo.setName(rs.getString("name"));
            courseVo.setCode(rs.getString("code"));
            courseVo.setMode(rs.getLong("mode") == 2 ? "选修课" : "必修课");
            courseVo.setTplId(rs.getLong("tplId") != 0 ? "引用" : "自建");
            courseVo.setStartDate(rs.getDate("startDate"));
            courseVo.setEndDate(rs.getDate("endDate"));
            courseVo.setCredit(rs.getDouble("credit"));
            courseVo.setAllow(rs.getLong("allow") == 0 ? "禁用" : "正常");
            courseVo.setStuCount(rs.getLong("stuCount"));
            courseVo.setSchoolId(String.valueOf(rs.getLong("schoolId")));
            courseVo.setAddDate(rs.getDate("addDate"));
            courseVo.setCreateName(rs.getString("createName"));
            courseVo.setCollegeName(rs.getString("collegeName"));
            //是否为实践课
            courseVo.setIsPractice(((rs.getInt("isPractice") == 1 ? "是" : "否")));
            yeeCourseList.add(courseVo);
        }
        return yeeCourseList;
    }

    @Override
    public Result selectAll(int schoolId, int pageSize, int pageNum, String authorization) throws Exception {
        // === 1. 从 JWT 解析用户上下文（替代 StpUtil，避免 Sa-Token 会话失效导致 NotLoginException） ===
        Claims claims = JwtTokenUtil.parseToken(authorization);
        String subject = claims.get("sub").toString();
        LoginUser loginUser = JSON.parseObject(subject, LoginUser.class);

        YeeManage manage = loginUser.getYeeManage();
        if (manage == null) {
            return Result.error("仅支持 YeeManage 用户访问课程列表");
        }

        DataAuth dataAuth = DataAuth.fromValue(loginUser.getDataAuth());
        if (dataAuth == null) {
            dataAuth = DataAuth.OWN;
        }

        // === 3. 构建数据查询 SQL ===
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT yc.*, ym.name AS createName " +
                        "FROM yee_course yc " +
                        "LEFT JOIN yee_manage ym ON yc.createId = ym.id AND yc.schoolId = ym.schoolId " +
                        "WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        // === 4. 根据 dataAuth 动态拼接条件 ===
        switch (dataAuth) {
            case ALL:
                sqlBuilder.append(" AND yc.schoolId = ?");
                params.add((long) schoolId);
                break;

            case COLLEGE:
                long mainCollegeId = manage.getCollegeId();
                long lecturerId = manage.getId();

                sqlBuilder.append(" AND (");

                sqlBuilder.append("yc.collegeId = ?");
                params.add(mainCollegeId);

                String collegesJson = manage.getColleges();
                List<Long> cids = new ArrayList<>();
                if (collegesJson != null && !collegesJson.trim().isEmpty()) {
                    try {
                        JSONArray arr = JSONArray.parseArray(collegesJson);
                        for (Object o : arr) {
                            if (o instanceof Number) {
                                cids.add(((Number) o).longValue());
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (!cids.isEmpty()) {
                    sqlBuilder.append(" OR yc.collegeId IN (")
                            .append(String.join(",", Collections.nCopies(cids.size(), "?")))
                            .append(")");
                    for (Long cid : cids) {
                        params.add(cid);
                    }
                }

                sqlBuilder.append(" OR JSON_CONTAINS(yc.lecturers, CAST(")
                        .append(lecturerId)
                        .append(" AS JSON))");
                sqlBuilder.append(" OR yc.createId = ?");
                params.add(lecturerId);

                sqlBuilder.append(" OR EXISTS (")
                        .append("SELECT 1 FROM yee_course_class ycc ")
                        .append("WHERE ycc.courseId = yc.id ")
                        .append("AND ycc.teacherId = ? ")
                        .append("AND ycc.schoolId = ? ")
                        .append("AND ycc.allow = 1)");
                params.add(lecturerId);
                params.add((long) schoolId);

                sqlBuilder.append(")");
                sqlBuilder.append(" AND yc.schoolId = ?");
                params.add((long) schoolId);
                break;

            case OWN:
                long lecturerIdOwn = manage.getId();
                sqlBuilder.append(" AND (")
                        .append("JSON_CONTAINS(yc.lecturers, CAST(")
                        .append(lecturerIdOwn)
                        .append(" AS JSON))")
                        .append(" OR yc.createId = ?")
                        .append(" OR EXISTS (")
                        .append("SELECT 1 FROM yee_course_class ycc ")
                        .append("WHERE ycc.courseId = yc.id ")
                        .append("AND ycc.teacherId = ? ")
                        .append("AND ycc.schoolId = ? ")
                        .append("AND ycc.allow = 1)")
                        .append(")")
                        .append(" AND yc.schoolId = ?");

                params.add(lecturerIdOwn);
                params.add(lecturerIdOwn);
                params.add((long) schoolId);
                params.add((long) schoolId);
                break;
        }

        // === 5. 使用 QueryBuilder 分页查询（封装连接管理 + count 子查询） ===
        PageResult<CourseTreeVo> pageResult = databaseUtil.query(schoolId)
                .sql(sqlBuilder.toString())
                .params(params.toArray())
                .orderBy("yc.addTime DESC")
                .page(this::mapCourseTreeVoRow, pageNum, pageSize);

        return Result.success(pageResult.getRows(), pageResult.getTotal());
    }

    @Override
    public Result add(YeeCourse yeeCourse) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById((int) yeeCourse.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

        StringBuilder columns = new StringBuilder("INSERT INTO yee_course (");
        StringBuilder values = new StringBuilder("VALUES (");
        ArrayList<Object> parameters = new ArrayList<>();

        // 必填字段：schoolId
        columns.append("`schoolId`, ");
        values.append("?, ");
        parameters.add(yeeCourse.getSchoolId());

        // 动态添加可选字段
        if (yeeCourse.getName() != null && !yeeCourse.getName().trim().isEmpty()) {
            columns.append("`name`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getName());
        }

        if (yeeCourse.getMode() >= 0) {
            columns.append("`mode`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getMode());
        }

        if (yeeCourse.getCollegeId() > 0) {
            columns.append("`collegeId`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCollegeId());
        }

        if (yeeCourse.getCategoryId() != null && !yeeCourse.getCategoryId().trim().isEmpty()) {
            columns.append("`categoryId`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCategoryId());
        }

        if (yeeCourse.getLecturers() != null && !yeeCourse.getLecturers().trim().isEmpty()) {
            columns.append("`lecturers`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getLecturers());
        }

        if (yeeCourse.getStartDate() != null) {
            columns.append("`startDate`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getStartDate());
        }

        if (yeeCourse.getEndDate() != null) {
            columns.append("`endDate`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getEndDate());
        }

        if (yeeCourse.getCover() != null && !yeeCourse.getCover().trim().isEmpty()) {
            columns.append("`cover`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCover());
        }

        if (yeeCourse.getContent() != null && !yeeCourse.getContent().trim().isEmpty()) {
            columns.append("`content`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getContent());
        }

        if (yeeCourse.getCredit() > 0) {
            columns.append("`credit`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCredit());
        }

        if (yeeCourse.getAllow() >= 0) {
            columns.append("`allow`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getAllow());
        }

        if (yeeCourse.getIntro() != null && !yeeCourse.getIntro().trim().isEmpty()) {
            columns.append("`intro`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getIntro());
        }

        if (yeeCourse.getTeacherIntro() != null && !yeeCourse.getTeacherIntro().trim().isEmpty()) {
            columns.append("`teacherIntro`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getTeacherIntro());
        }

        if (yeeCourse.getCode() != null && !yeeCourse.getCode().trim().isEmpty()) {
            columns.append("`code`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCode());
        }

        if (yeeCourse.getStuCount() >= 0) {
            columns.append("`stuCount`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getStuCount());
        }

        if (yeeCourse.getProclamation() != null && !yeeCourse.getProclamation().trim().isEmpty()) {
            columns.append("`proclamation`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getProclamation());
        }

        if (yeeCourse.getClusterId() > 0) {
            columns.append("`clusterId`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getClusterId());
        }

        if (yeeCourse.getPeriodName() != null && !yeeCourse.getPeriodName().trim().isEmpty()) {
            columns.append("`periodName`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getPeriodName());
        }

        if (yeeCourse.getCreateId() > 0) {
            columns.append("`createId`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCreateId());
        }

        if (yeeCourse.getCateBid() > 0) {
            columns.append("`cateBid`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCateBid());
        }

        if (yeeCourse.getCateMid() > 0) {
            columns.append("`cateMid`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCateMid());
        }

        if (yeeCourse.getSignStartTime() != null) {
            columns.append("`signStartTime`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getSignStartTime());
        }

        if (yeeCourse.getSignEndTime() != null) {
            columns.append("`signEndTime`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getSignEndTime());
        }

        if (yeeCourse.getSignScope() >= 0) {
            columns.append("`signScope`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getSignScope());
        }

        if (yeeCourse.getSignClass() != null && !yeeCourse.getSignClass().trim().isEmpty()) {
            columns.append("`signClass`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getSignClass());
        }

        if (yeeCourse.getLecturerName() != null && !yeeCourse.getLecturerName().trim().isEmpty()) {
            columns.append("`lecturerName`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getLecturerName());
        }

        if (yeeCourse.getOffline() >= 0) {
            columns.append("`offline`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getOffline());
        }

        if (yeeCourse.getMission() >= 0) {
            columns.append("`mission`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getMission());
        }

        if (yeeCourse.getSignLimit() >= 0) {
            columns.append("`signLimit`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getSignLimit());
        }

        if (yeeCourse.getLineLock() >= 0) {
            columns.append("`lineLock`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getLineLock());
        }

        if (yeeCourse.getTplId() > 0) {
            columns.append("`tplId`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getTplId());
        }

        if (yeeCourse.getIsPractice() >= 0) {
            columns.append("`isPractice`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getIsPractice());
        }

        // 自动设置添加时间
        columns.append("`addTime`, ");
        values.append("?, ");
        parameters.add(yeeCourse.getAddTime() != null ? yeeCourse.getAddTime() : new Timestamp(System.currentTimeMillis()));

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
            } else if (param instanceof Long) {
                st.setLong(i + 1, (Long) param);
            } else if (param instanceof Integer) {
                st.setInt(i + 1, (Integer) param);
            } else if (param instanceof Double) {
                st.setDouble(i + 1, (Double) param);
            } else if (param instanceof Date) {
                st.setDate(i + 1, (Date) param);
            } else if (param instanceof Timestamp) {
                st.setTimestamp(i + 1, (Timestamp) param);
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
    }

    @Override
    public Result like(LikeYeeCourse likeYeeCourse, Integer pageSize, Integer pageNum, String authorization) throws Exception {
        // 1. 分页参数默认值
        if (pageSize == null || pageSize <= 0) pageSize = 10;
        if (pageNum == null || pageNum <= 0) pageNum = 1;

        // 2. 基础参数校验
        if (likeYeeCourse == null || likeYeeCourse.getSchoolId() == null) {
            return Result.error("参数错误：学校ID不能为空");
        }
        int schoolId = likeYeeCourse.getSchoolId();

        // ========== 改用 JWT 解析替代 Sa-Token 会话 ==========
        LoginUser loginUser;
        YeeManage manage;
        DataAuth dataAuth;
        try {
            Claims claims = JwtTokenUtil.parseToken(authorization);
            String subject = claims.get("sub").toString();
            loginUser = JSON.parseObject(subject, LoginUser.class);
            manage = loginUser.getYeeManage();
            if (manage == null) {
                return Result.error("仅支持 YeeManage 用户访问课程列表");
            }
            // 从 JWT 中提取 dataAuth（登录时已写入 LoginUser）
            dataAuth = DataAuth.fromValue(loginUser.getDataAuth());
            if (dataAuth == null) {
                dataAuth = DataAuth.OWN;
            }
        } catch (Exception e) {
            // 解析失败（token 过期、格式错误等）返回 401
            return Result.error(String.valueOf(401), "登录状态已失效，请重新登录");
        }

        long currentUserId = manage.getId();
        long mainCollegeId = manage.getCollegeId();
        String collegesJson = manage.getColleges();

        // 5. 校验学校状态（不变）
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 6. 构建查询SQL（关联createName）
        StringBuilder sqlBuilder = new StringBuilder(
                "SELECT yc.*, ym.name AS createName " +
                        "FROM yee_course yc " +
                        "LEFT JOIN yee_manage ym ON yc.createId = ym.id AND yc.schoolId = ym.schoolId " +
                        "WHERE 1=1"
        );
        StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) FROM yee_course WHERE 1=1");
        List<Object> params = new ArrayList<>();

        // 7. 数据权限条件拼接（不变）
        long lecturerId = manage.getId();
        switch (dataAuth) {
            case ALL:
                sqlBuilder.append(" AND yc.schoolId = ?");
                countSqlBuilder.append(" AND schoolId = ?");
                params.add((long) schoolId);
                break;

            case COLLEGE:
                sqlBuilder.append(" AND (");
                countSqlBuilder.append(" AND (");

                // 主学院
                sqlBuilder.append("yc.collegeId = ?");
                countSqlBuilder.append("collegeId = ?");
                params.add(mainCollegeId);

                // 兼职学院
                List<Long> cids = new ArrayList<>();
                if (collegesJson != null && !collegesJson.trim().isEmpty()) {
                    try {
                        JSONArray arr = JSONArray.parseArray(collegesJson);
                        for (Object o : arr) {
                            if (o instanceof Number) {
                                cids.add(((Number) o).longValue());
                            }
                        }
                    } catch (Exception ignored) {}
                }
                if (!cids.isEmpty()) {
                    sqlBuilder.append(" OR yc.collegeId IN (")
                            .append(String.join(",", Collections.nCopies(cids.size(), "?")))
                            .append(")");
                    countSqlBuilder.append(" OR collegeId IN (")
                            .append(String.join(",", Collections.nCopies(cids.size(), "?")))
                            .append(")");
                    for (Long cid : cids) {
                        params.add(cid);
                    }
                }

                sqlBuilder.append(" OR JSON_CONTAINS(yc.lecturers, CAST(")
                        .append(lecturerId).append(" AS JSON))");
                countSqlBuilder.append(" OR JSON_CONTAINS(lecturers, CAST(")
                        .append(lecturerId).append(" AS JSON))");

                sqlBuilder.append(" OR yc.createId = ?");
                countSqlBuilder.append(" OR createId = ?");
                params.add(lecturerId);

                sqlBuilder.append(" OR EXISTS (")
                        .append("SELECT 1 FROM yee_course_class ycc ")
                        .append("WHERE ycc.courseId = yc.id ")
                        .append("AND ycc.teacherId = ? ")
                        .append("AND ycc.schoolId = ? ")
                        .append("AND ycc.allow = 1)");
                countSqlBuilder.append(" OR EXISTS (")
                        .append("SELECT 1 FROM yee_course_class ycc ")
                        .append("WHERE ycc.courseId = yee_course.id ")
                        .append("AND ycc.teacherId = ? ")
                        .append("AND ycc.schoolId = ? ")
                        .append("AND ycc.allow = 1)");
                params.add(lecturerId);
                params.add((long) schoolId);

                sqlBuilder.append(")");
                countSqlBuilder.append(")");
                sqlBuilder.append(" AND yc.schoolId = ?");
                countSqlBuilder.append(" AND schoolId = ?");
                params.add((long) schoolId);
                break;

            case OWN:
                sqlBuilder.append(" AND (")
                        .append("JSON_CONTAINS(yc.lecturers, CAST(")
                        .append(lecturerId).append(" AS JSON))")
                        .append(" OR yc.createId = ?")
                        .append(" OR EXISTS (")
                        .append("SELECT 1 FROM yee_course_class ycc ")
                        .append("WHERE ycc.courseId = yc.id ")
                        .append("AND ycc.teacherId = ? ")
                        .append("AND ycc.schoolId = ? ")
                        .append("AND ycc.allow = 1)")
                        .append(")")
                        .append(" AND yc.schoolId = ?");
                countSqlBuilder.append(" AND (")
                        .append("JSON_CONTAINS(lecturers, CAST(")
                        .append(lecturerId).append(" AS JSON))")
                        .append(" OR createId = ?")
                        .append(" OR EXISTS (")
                        .append("SELECT 1 FROM yee_course_class ycc ")
                        .append("WHERE ycc.courseId = yee_course.id ")
                        .append("AND ycc.teacherId = ? ")
                        .append("AND ycc.schoolId = ? ")
                        .append("AND ycc.allow = 1)")
                        .append(")")
                        .append(" AND schoolId = ?");
                params.add(lecturerId);
                params.add(lecturerId);
                params.add((long) schoolId);
                params.add((long) schoolId);
                break;
        }

        // 8. 原有业务查询条件（不变）
        if (likeYeeCourse.getName() != null && !likeYeeCourse.getName().trim().isEmpty()) {
            sqlBuilder.append(" AND yc.name LIKE ?");
            countSqlBuilder.append(" AND name LIKE ?");
            params.add("%" + likeYeeCourse.getName().trim() + "%");
        }
        if (likeYeeCourse.getMod() != null) {
            sqlBuilder.append(" AND yc.mode = ?");
            countSqlBuilder.append(" AND mode = ?");
            params.add(likeYeeCourse.getMod());
        }
        if (likeYeeCourse.getCateBid() != null && likeYeeCourse.getCateBid() > 0) {
            sqlBuilder.append(" AND yc.cateBid = ?");
            countSqlBuilder.append(" AND cateBid = ?");
            params.add(likeYeeCourse.getCateBid());
        }
        if (likeYeeCourse.getCateMid() != null && likeYeeCourse.getCateMid() > 0) {
            sqlBuilder.append(" AND yc.cateMid = ?");
            countSqlBuilder.append(" AND cateMid = ?");
            params.add(likeYeeCourse.getCateMid());
        }
        if (likeYeeCourse.getCode() != null && !likeYeeCourse.getCode().trim().isEmpty()) {
            sqlBuilder.append(" AND yc.code LIKE ?");
            countSqlBuilder.append(" AND code LIKE ?");
            params.add("%" + likeYeeCourse.getCode().trim() + "%");
        }
        if (likeYeeCourse.getStartDate() != null) {
            sqlBuilder.append(" AND yc.startDate >= ?");
            countSqlBuilder.append(" AND startDate >= ?");
            params.add(likeYeeCourse.getStartDate());
        }
        if (likeYeeCourse.getEndDate() != null) {
            sqlBuilder.append(" AND yc.endDate <= ?");
            countSqlBuilder.append(" AND endDate <= ?");
            params.add(likeYeeCourse.getEndDate().plusDays(1));
        }

        // ========== 创建人姓名查询（已改用 JWT 中的 dataAuth） ==========
        String createName = likeYeeCourse.getCreateName();
        if (createName != null && !createName.trim().isEmpty()) {
            Connection userConn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            List<Integer> createIdList = new ArrayList<>();
            try {
                StringBuilder userSqlBuilder = new StringBuilder("SELECT id FROM yee_manage WHERE schoolId = ?");
                List<Object> userParams = new ArrayList<>();
                userParams.add(schoolId);

                switch (dataAuth) {
                    case ALL:
                        userSqlBuilder.append(" AND name LIKE ?");
                        userParams.add("%" + createName.trim() + "%");
                        break;
                    case COLLEGE:
                        userSqlBuilder.append(" AND name LIKE ? AND (collegeId = ?");
                        userParams.add("%" + createName.trim() + "%");
                        userParams.add(mainCollegeId);
                        if (collegesJson != null && !collegesJson.trim().isEmpty()) {
                            try {
                                JSONArray arr = JSONArray.parseArray(collegesJson);
                                List<Long> partTimeColleges = new ArrayList<>();
                                for (Object o : arr) {
                                    if (o instanceof Number) {
                                        partTimeColleges.add(((Number) o).longValue());
                                    }
                                }
                                if (!partTimeColleges.isEmpty()) {
                                    userSqlBuilder.append(" OR collegeId IN (")
                                            .append(String.join(",", Collections.nCopies(partTimeColleges.size(), "?")))
                                            .append(")");
                                    for (Long cid : partTimeColleges) {
                                        userParams.add(cid);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        userSqlBuilder.append(")");
                        break;
                    case OWN:
                        userSqlBuilder.append(" AND id = ? AND name LIKE ?");
                        userParams.add(currentUserId);
                        userParams.add("%" + createName.trim() + "%");
                        break;
                }

                PreparedStatement userSt = userConn.prepareStatement(userSqlBuilder.toString());
                for (int i = 0; i < userParams.size(); i++) {
                    userSt.setObject(i + 1, userParams.get(i));
                }
                ResultSet userRs = userSt.executeQuery();
                while (userRs.next()) {
                    createIdList.add(userRs.getInt("id"));
                }
            } finally {
                if (userConn != null) try { userConn.close(); } catch (SQLException ignored) {}
            }

            if (!createIdList.isEmpty()) {
                sqlBuilder.append(" AND yc.createId IN (")
                        .append(String.join(",", Collections.nCopies(createIdList.size(), "?")))
                        .append(")");
                countSqlBuilder.append(" AND createId IN (")
                        .append(String.join(",", Collections.nCopies(createIdList.size(), "?")))
                        .append(")");
                for (Integer cid : createIdList) {
                    params.add(cid);
                }
            } else {
                return Result.success(new ArrayList<CourseTreeVo>(), 0L);
            }
        }

        // 9. 敏感字段条件（仅 ALL 权限可见）
        if (dataAuth == DataAuth.ALL) {
            if (likeYeeCourse.getCreateId() != null && likeYeeCourse.getCreateId() > 0) {
                sqlBuilder.append(" AND yc.createId = ?");
                countSqlBuilder.append(" AND createId = ?");
                params.add(likeYeeCourse.getCreateId());
            }
            if (likeYeeCourse.getCollegeId() != null && likeYeeCourse.getCollegeId() > 0) {
                sqlBuilder.append(" AND yc.collegeId = ?");
                countSqlBuilder.append(" AND collegeId = ?");
                params.add(likeYeeCourse.getCollegeId());
            }
        }

        // 10. 分页处理
        sqlBuilder.append(" ORDER BY yc.addTime DESC LIMIT ? OFFSET ?");
        int offset = (pageNum - 1) * pageSize;
        params.add(pageSize);
        params.add(offset);

        // 11. 执行查询（此处使用原生的 Connection 方式，与原有逻辑一致，若后续想封装可参考 selectAll 的 QueryBuilder）
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        try {
            PreparedStatement countSt = connection.prepareStatement(countSqlBuilder.toString());
            for (int i = 0; i < params.size() - 2; i++) {
                countSt.setObject(i + 1, params.get(i));
            }
            ResultSet countRs = countSt.executeQuery();
            int totalCount = countRs.next() ? countRs.getInt(1) : 0;

            PreparedStatement st = connection.prepareStatement(sqlBuilder.toString());
            for (int i = 0; i < params.size(); i++) {
                st.setObject(i + 1, params.get(i));
            }
            ResultSet rs = st.executeQuery();
            List<CourseTreeVo> courseList = rsToCourseTreeVo(rs);

            return Result.success(courseList, (long) totalCount);
        } finally {
            if (connection != null) {
                try { connection.close(); } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public Result update(YeeCourse yeeCourse) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById((int) yeeCourse.getSchoolId());
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

        StringBuilder sql = new StringBuilder("UPDATE yee_course SET ");
        ArrayList<Object> parameters = new ArrayList<>();

        // 动态添加更新字段
        if (yeeCourse.getName() != null && !yeeCourse.getName().trim().isEmpty()) {
            sql.append("`name` = ?, ");
            parameters.add(yeeCourse.getName());
        }

        if (yeeCourse.getMode() >= 0) {
            sql.append("`mode` = ?, ");
            parameters.add(yeeCourse.getMode());
        }

        if (yeeCourse.getCollegeId() > 0) {
            sql.append("`collegeId` = ?, ");
            parameters.add(yeeCourse.getCollegeId());
        }

        if (yeeCourse.getCategoryId() != null && !yeeCourse.getCategoryId().trim().isEmpty()) {
            sql.append("`categoryId` = ?, ");
            parameters.add(yeeCourse.getCategoryId());
        }

        if (yeeCourse.getLecturers() != null && !yeeCourse.getLecturers().trim().isEmpty()) {
            sql.append("`lecturers` = ?, ");
            parameters.add(yeeCourse.getLecturers());
        }

        if (yeeCourse.getStartDate() != null) {
            sql.append("`startDate` = ?, ");
            parameters.add(yeeCourse.getStartDate());
        }

        if (yeeCourse.getEndDate() != null) {
            sql.append("`endDate` = ?, ");
            parameters.add(yeeCourse.getEndDate());
        }

        if (yeeCourse.getCover() != null && !yeeCourse.getCover().trim().isEmpty()) {
            sql.append("`cover` = ?, ");
            parameters.add(yeeCourse.getCover());
        }

        if (yeeCourse.getContent() != null && !yeeCourse.getContent().trim().isEmpty()) {
            sql.append("`content` = ?, ");
            parameters.add(yeeCourse.getContent());
        }

        if (yeeCourse.getCredit() > 0) {
            sql.append("`credit` = ?, ");
            parameters.add(yeeCourse.getCredit());
        }

        if (yeeCourse.getAllow() >= 0) {
            sql.append("`allow` = ?, ");
            parameters.add(yeeCourse.getAllow());
        }

        if (yeeCourse.getIntro() != null && !yeeCourse.getIntro().trim().isEmpty()) {
            sql.append("`intro` = ?, ");
            parameters.add(yeeCourse.getIntro());
        }

        if (yeeCourse.getTeacherIntro() != null && !yeeCourse.getTeacherIntro().trim().isEmpty()) {
            sql.append("`teacherIntro` = ?, ");
            parameters.add(yeeCourse.getTeacherIntro());
        }

        if (yeeCourse.getCode() != null && !yeeCourse.getCode().trim().isEmpty()) {
            sql.append("`code` = ?, ");
            parameters.add(yeeCourse.getCode());
        }

        if (yeeCourse.getStuCount() >= 0) {
            sql.append("`stuCount` = ?, ");
            parameters.add(yeeCourse.getStuCount());
        }

        if (yeeCourse.getProclamation() != null && !yeeCourse.getProclamation().trim().isEmpty()) {
            sql.append("`proclamation` = ?, ");
            parameters.add(yeeCourse.getProclamation());
        }

        if (yeeCourse.getClusterId() > 0) {
            sql.append("`clusterId` = ?, ");
            parameters.add(yeeCourse.getClusterId());
        }

        if (yeeCourse.getPeriodName() != null && !yeeCourse.getPeriodName().trim().isEmpty()) {
            sql.append("`periodName` = ?, ");
            parameters.add(yeeCourse.getPeriodName());
        }

        if (yeeCourse.getCreateId() > 0) {
            sql.append("`createId` = ?, ");
            parameters.add(yeeCourse.getCreateId());
        }

        if (yeeCourse.getCateBid() > 0) {
            sql.append("`cateBid` = ?, ");
            parameters.add(yeeCourse.getCateBid());
        }

        if (yeeCourse.getCateMid() > 0) {
            sql.append("`cateMid` = ?, ");
            parameters.add(yeeCourse.getCateMid());
        }

        if (yeeCourse.getSignStartTime() != null) {
            sql.append("`signStartTime` = ?, ");
            parameters.add(yeeCourse.getSignStartTime());
        }

        if (yeeCourse.getSignEndTime() != null) {
            sql.append("`signEndTime` = ?, ");
            parameters.add(yeeCourse.getSignEndTime());
        }

        if (yeeCourse.getSignScope() >= 0) {
            sql.append("`signScope` = ?, ");
            parameters.add(yeeCourse.getSignScope());
        }

        if (yeeCourse.getSignClass() != null && !yeeCourse.getSignClass().trim().isEmpty()) {
            sql.append("`signClass` = ?, ");
            parameters.add(yeeCourse.getSignClass());
        }

        if (yeeCourse.getLecturerName() != null && !yeeCourse.getLecturerName().trim().isEmpty()) {
            sql.append("`lecturerName` = ?, ");
            parameters.add(yeeCourse.getLecturerName());
        }

        if (yeeCourse.getOffline() >= 0) {
            sql.append("`offline` = ?, ");
            parameters.add(yeeCourse.getOffline());
        }

        if (yeeCourse.getMission() >= 0) {
            sql.append("`mission` = ?, ");
            parameters.add(yeeCourse.getMission());
        }

        if (yeeCourse.getSignLimit() >= 0) {
            sql.append("`signLimit` = ?, ");
            parameters.add(yeeCourse.getSignLimit());
        }

        if (yeeCourse.getLineLock() >= 0) {
            sql.append("`lineLock` = ?, ");
            parameters.add(yeeCourse.getLineLock());
        }

        if (yeeCourse.getTplId() > 0) {
            sql.append("`tplId` = ?, ");
            parameters.add(yeeCourse.getTplId());
        }

        if (yeeCourse.getIsPractice() >= 0) {
            sql.append("`isPractice` = ?, ");
            parameters.add(yeeCourse.getIsPractice());
        }

        if (yeeCourse.getAddTime() != null) {
            sql.append("`addTime` = ?, ");
            parameters.add(yeeCourse.getAddTime());
        }

        // 检查是否有可更新的字段
        if (parameters.isEmpty()) {
            connection.close();
            return Result.error("没有可更新的字段");
        }

        // 删除最后的逗号和空格
        sql.delete(sql.length() - 2, sql.length());

        // 添加WHERE条件
        sql.append(" WHERE id = ?");
        parameters.add(yeeCourse.getId());

        PreparedStatement st = connection.prepareStatement(sql.toString());
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            if (param instanceof String) {
                st.setString(i + 1, (String) param);
            } else if (param instanceof Long) {
                st.setLong(i + 1, (Long) param);
            } else if (param instanceof Integer) {
                st.setInt(i + 1, (Integer) param);
            } else if (param instanceof Double) {
                st.setDouble(i + 1, (Double) param);
            } else if (param instanceof Date) {
                st.setDate(i + 1, (Date) param);
            } else if (param instanceof Timestamp) {
                st.setTimestamp(i + 1, (Timestamp) param);
            }
        }

        int rowsUpdated = st.executeUpdate();
        st.close();
        connection.close();

        if (rowsUpdated > 0) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败：未找到匹配的记录");
        }
    }

    @Override
    public Result deleteById(int schoolId, int id) throws Exception {
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        String sql = "DELETE FROM yee_course WHERE id = ?";
        PreparedStatement st = connection.prepareStatement(sql);
        st.setInt(1, id);

        int rowsDeleted = st.executeUpdate();
        st.close();
        connection.close();

        if (rowsDeleted > 0) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败：未找到匹配的记录");
        }
    }

    @Override
    public Result selectAllWithConditions(YeeCourseQueryParam param) throws Exception {
        // 验证学校
        Integer schoolId = param.getSchoolId();
        if (schoolId == null) {
            return Result.error("学校ID不能为空");
        }

        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        try {
            // 构建查询SQL
            StringBuilder sqlBuilder = new StringBuilder("SELECT yc.id, yc.isPractice, yc.name, yc.code, yc.mode, yc.tplId, yc.startDate, yc.endDate, yc.credit, yc.allow, yc.stuCount, yc.createId, yc.schoolId, yc.collegeId, yc.addDate, ym.name AS createName, yco.name AS collegeName FROM yee_course yc LEFT JOIN yee_manage ym ON yc.createId = ym.id LEFT JOIN yee_college yco ON yc.collegeId = yco.id WHERE yc.schoolId = ?");
            StringBuilder countSqlBuilder = new StringBuilder("SELECT COUNT(*) FROM yee_course yc LEFT JOIN yee_manage ym ON yc.createId = ym.id LEFT JOIN yee_college yco ON yc.collegeId = yco.id WHERE yc.schoolId = ?");

            // 参数列表
            List<Object> parameters = new ArrayList<>();
            parameters.add(schoolId);

            // 动态添加查询条件
            if (param.getName() != null && !param.getName().trim().isEmpty()) {
                sqlBuilder.append(" AND yc.name LIKE ?");
                countSqlBuilder.append(" AND yc.name LIKE ?");
                parameters.add("%" + param.getName().trim() + "%");
            }

            if (param.getId() != null && param.getId() > 0) {
                sqlBuilder.append(" AND yc.id = ?");
                countSqlBuilder.append(" AND yc.id = ?");
                parameters.add(param.getId());
            }

            if (param.getMode() != null) {
                sqlBuilder.append(" AND yc.mode = ?");
                countSqlBuilder.append(" AND yc.mode = ?");
                parameters.add(param.getMode());
            }

            if (param.getCollegeId() != null && param.getCollegeId() > 0) {
                sqlBuilder.append(" AND yc.collegeId = ?");
                countSqlBuilder.append(" AND yc.collegeId = ?");
                parameters.add(param.getCollegeId());
            }

            if (param.getCode() != null && !param.getCode().trim().isEmpty()) {
                sqlBuilder.append(" AND yc.code LIKE ?");
                countSqlBuilder.append(" AND yc.code LIKE ?");
                parameters.add("%" + param.getCode().trim() + "%");
            }

            if (param.getCreateId() != null && param.getCreateId() > 0) {
                sqlBuilder.append(" AND yc.createId = ?");
                countSqlBuilder.append(" AND yc.createId = ?");
                parameters.add(param.getCreateId());
            }

            if (param.getCateBid() != null && param.getCateBid() > 0) {
                sqlBuilder.append(" AND yc.cateBid = ?");
                countSqlBuilder.append(" AND yc.cateBid = ?");
                parameters.add(param.getCateBid());
            }

            if (param.getCateMid() != null && param.getCateMid() > 0) {
                sqlBuilder.append(" AND yc.cateMid = ?");
                countSqlBuilder.append(" AND yc.cateMid = ?");
                parameters.add(param.getCateMid());
            }

            if (param.getStartDate() != null) {
                sqlBuilder.append(" AND yc.startDate >= ?");
                countSqlBuilder.append(" AND yc.startDate >= ?");
                parameters.add(param.getStartDate());
            }

            if (param.getEndDate() != null) {
                sqlBuilder.append(" AND yc.endDate <= ?");
                countSqlBuilder.append(" AND yc.endDate <= ?");
                parameters.add(param.getEndDate());
            }

            if (param.getSelfBuilt() != null) {
                if (param.getSelfBuilt()) {
                    // 自建课程 (tplId = 0)
                    sqlBuilder.append(" AND yc.tplId = 0");
                    countSqlBuilder.append(" AND yc.tplId = 0");
                } else {
                    // 非自建课程 (tplId > 0)
                    sqlBuilder.append(" AND yc.tplId > 0");
                    countSqlBuilder.append(" AND yc.tplId > 0");
                }
            }

            if (param.getAllow() != null) {
                sqlBuilder.append(" AND yc.allow = ?");
                countSqlBuilder.append(" AND yc.allow = ?");
                parameters.add(param.getAllow());
            }

            // 添加排序
            sqlBuilder.append(" ORDER BY yc.addDate DESC");

            // 分页参数
            int pageSize = param.getPageSize() != null && param.getPageSize() > 0 ? param.getPageSize() : 10;
            int pageNum = param.getPageNum() != null && param.getPageNum() >= 1 ? param.getPageNum() : 1;
            int offset = (pageNum - 1) * pageSize;

            // 添加分页
            sqlBuilder.append(" LIMIT ? OFFSET ?");
            parameters.add(pageSize);
            parameters.add(offset);

            // 执行总数查询
            PreparedStatement countSt = connection.prepareStatement(countSqlBuilder.toString());
            for (int i = 0; i < parameters.size() - 2; i++) { // 总数查询不需要分页参数
                countSt.setObject(i + 1, parameters.get(i));
            }
            ResultSet countRs = countSt.executeQuery();
            long total = 0;
            if (countRs.next()) {
                total = countRs.getLong(1);
            }
            countRs.close();
            countSt.close();

            // 执行列表查询
            PreparedStatement st = connection.prepareStatement(sqlBuilder.toString());
            for (int i = 0; i < parameters.size(); i++) {
                st.setObject(i + 1, parameters.get(i));
            }
            ResultSet rs = st.executeQuery();
            List<YeeCourseListVo> yeeCourses = rsToYeeCourseListVo(rs);

            rs.close();
            st.close();

            return Result.success(yeeCourses, total);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询失败：" + e.getMessage());
        } finally {
            // 确保连接关闭
            if (connection != null && !connection.isClosed()) {
                try {
                    connection.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public Result selectCourseContent(int schoolId, int id, Integer classId) throws Exception {
        // === 1. 校验学校状态（可选，根据你系统要求）===
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未启用");
        }

        // === 2. 获取该学校的从库连接 ===
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        try {
            Map<String, List<Map<String, Object>>> result = new HashMap<>();

            // --- 视频节点：yee_node ---
            String videoSql = "SELECT id, name FROM yee_node WHERE courseId = ? AND tabVideo = 1";
            List<Map<String, Object>> videos = executeQuery(connection, videoSql, id);
            result.put("videos", videos);

            // --- 作业：yee_work ---
            StringBuilder workSql = new StringBuilder(
                    "SELECT id, title AS name FROM yee_work WHERE courseId = ? AND allow = 1"
            );
            List<Object> workParams = new ArrayList<>();
            workParams.add(id);

            if (classId != null) {
                workSql.append(" AND (")
                        .append("classList IS NULL ")
                        .append("OR classList = '' ")
                        .append("OR JSON_LENGTH(classList) = 0 ")
                        .append("OR JSON_CONTAINS(classList, CAST(? AS JSON))")
                        .append(")");
                workParams.add(classId);
            }
            // 如果 classId == null，则不加任何班级条件（即全部返回）

            List<Map<String, Object>> works = executeQuery(connection, workSql.toString(), workParams.toArray());
            result.put("works", works);

            // --- 考试：yee_exam ---
            StringBuilder examSql = new StringBuilder(
                    "SELECT id, title AS name FROM yee_exam WHERE courseId = ? AND allow = 1"
            );
            List<Object> examParams = new ArrayList<>();
            examParams.add(id);

            if (classId != null) {
                examSql.append(" AND (")
                        .append("classList IS NULL ")
                        .append("OR classList = '' ")
                        .append("OR JSON_LENGTH(classList) = 0 ")
                        .append("OR JSON_CONTAINS(classList, CAST(? AS JSON))")
                        .append(")");
                examParams.add(classId);
            }

            List<Map<String, Object>> exams = executeQuery(connection, examSql.toString(), examParams.toArray());
            result.put("exams", exams);

            // --- 讨论：yee_discuss（无需班级过滤）---
            String discussSql = "SELECT id, title AS name FROM yee_discuss WHERE courseId = ? AND isDelete = 0";
            List<Map<String, Object>> discussions = executeQuery(connection, discussSql, id);
            result.put("discussions", discussions);

            return Result.success(result);

        } finally {
            if (connection != null) {
                try { connection.close(); } catch (SQLException ignored) {}
            }
        }
    }

    private List<Map<String, Object>> executeQuery(Connection conn, String sql, Object... params) throws SQLException {
        PreparedStatement ps = conn.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        ResultSet rs = ps.executeQuery();
        List<Map<String, Object>> list = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int colCount = metaData.getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= colCount; i++) {
                String colName = metaData.getColumnLabel(i);
                row.put(colName, rs.getObject(i));
            }
            list.add(row);
        }
        rs.close();
        ps.close();
        return list;
    }

    /**
     * 从 mhmain（管理员库）读取课程模板，写入当前租户库（如 mhsch_1）
     */
    @Override
    public Result courseTemplateImport(YeeCourse yeeCourse) {
        long schoolId = yeeCourse.getSchoolId();
        long templateId = yeeCourse.getId();

        //验证模板课程存在
        SlTplCourse slTplCourse = courseMapper.selectByIdSlTplCourse((int) templateId);
        if(slTplCourse == null){
            return Result.error("课程模板信息不存在");
        }
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        Connection conn = null;
        try {
            conn = SlaveMysqlConnectionUtil.getConnection(slSchool);
            conn.setAutoCommit(false); // 👈 开启事务

            // 1. 读取模板课程
            SlTplCourse template = courseMapper.selectByIdSlTplCourse(Math.toIntExact(templateId));
            if (template == null) {
                return Result.error("课程模板不存在");
            }

            // 2. 插入课程
            Long newCourseId = insertYeeCourseForImport(yeeCourse, conn); // 需新增此方法（类似章节插入）

            // 3. 导入章
            Map<Long, Long> chapterMap = importChapters(Math.toIntExact(templateId), newCourseId, schoolId, conn);

            // 4. 导入节
            Map<Long, Long> nodeMap = importNodes(newCourseId, chapterMap, schoolId, conn);

            // 5. 导入文件
            Set<Long> oldNodeIds = nodeMap.keySet();
            importNodeFiles(oldNodeIds, nodeMap, newCourseId, schoolId, conn);

            // 6. 提交事务
            conn.commit();
            return Result.success("课程模板导入成功");

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback(); // 回滚
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return Result.error("课程导入失败: " + e.getMessage());
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // 恢复默认
                    conn.close(); // 或由连接池管理（根据你的工具类决定是否 close）
                } catch (SQLException ignored) {}
            }
        }
    }

    private Long insertYeeCourseForImport(YeeCourse yeeCourse, Connection conn) throws SQLException {
        StringBuilder columns = new StringBuilder("INSERT INTO yee_course (");
        StringBuilder values = new StringBuilder("VALUES (");
        ArrayList<Object> parameters = new ArrayList<>();

        // 必填字段：schoolId
        columns.append("`schoolId`, ");
        values.append("?, ");
        parameters.add(yeeCourse.getSchoolId());

        // 动态添加可选字段
        if (yeeCourse.getName() != null && !yeeCourse.getName().trim().isEmpty()) {
            columns.append("`name`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getName());
        }

        if (yeeCourse.getMode() >= 0) {
            columns.append("`mode`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getMode());
        }

        if (yeeCourse.getCollegeId() > 0) {
            columns.append("`collegeId`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCollegeId());
        }

        if (yeeCourse.getCategoryId() != null && !yeeCourse.getCategoryId().trim().isEmpty()) {
            columns.append("`categoryId`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCategoryId());
        }

        if (yeeCourse.getLecturers() != null && !yeeCourse.getLecturers().trim().isEmpty()) {
            columns.append("`lecturers`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getLecturers());
        }

        if (yeeCourse.getStartDate() != null) {
            columns.append("`startDate`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getStartDate());
        }

        if (yeeCourse.getEndDate() != null) {
            columns.append("`endDate`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getEndDate());
        }

        if (yeeCourse.getCover() != null && !yeeCourse.getCover().trim().isEmpty()) {
            columns.append("`cover`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCover());
        }

        if (yeeCourse.getContent() != null && !yeeCourse.getContent().trim().isEmpty()) {
            columns.append("`content`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getContent());
        }

        if (yeeCourse.getCredit() > 0) {
            columns.append("`credit`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCredit());
        }

        if (yeeCourse.getAllow() >= 0) {
            columns.append("`allow`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getAllow());
        }

        if (yeeCourse.getIntro() != null && !yeeCourse.getIntro().trim().isEmpty()) {
            columns.append("`intro`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getIntro());
        }

        if (yeeCourse.getTeacherIntro() != null && !yeeCourse.getTeacherIntro().trim().isEmpty()) {
            columns.append("`teacherIntro`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getTeacherIntro());
        }

        if (yeeCourse.getCode() != null && !yeeCourse.getCode().trim().isEmpty()) {
            columns.append("`code`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCode());
        }

        if (yeeCourse.getStuCount() >= 0) {
            columns.append("`stuCount`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getStuCount());
        }

        if (yeeCourse.getProclamation() != null && !yeeCourse.getProclamation().trim().isEmpty()) {
            columns.append("`proclamation`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getProclamation());
        }

        if (yeeCourse.getClusterId() > 0) {
            columns.append("`clusterId`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getClusterId());
        }

        if (yeeCourse.getPeriodName() != null && !yeeCourse.getPeriodName().trim().isEmpty()) {
            columns.append("`periodName`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getPeriodName());
        }

        if (yeeCourse.getCreateId() > 0) {
            columns.append("`createId`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCreateId());
        }

        if (yeeCourse.getCateBid() > 0) {
            columns.append("`cateBid`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCateBid());
        }

        if (yeeCourse.getCateMid() > 0) {
            columns.append("`cateMid`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getCateMid());
        }

        if (yeeCourse.getSignStartTime() != null) {
            columns.append("`signStartTime`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getSignStartTime());
        }

        if (yeeCourse.getSignEndTime() != null) {
            columns.append("`signEndTime`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getSignEndTime());
        }

        if (yeeCourse.getSignScope() >= 0) {
            columns.append("`signScope`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getSignScope());
        }

        if (yeeCourse.getSignClass() != null && !yeeCourse.getSignClass().trim().isEmpty()) {
            columns.append("`signClass`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getSignClass());
        }

        if (yeeCourse.getLecturerName() != null && !yeeCourse.getLecturerName().trim().isEmpty()) {
            columns.append("`lecturerName`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getLecturerName());
        }

        if (yeeCourse.getOffline() >= 0) {
            columns.append("`offline`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getOffline());
        }

        if (yeeCourse.getMission() >= 0) {
            columns.append("`mission`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getMission());
        }

        if (yeeCourse.getSignLimit() >= 0) {
            columns.append("`signLimit`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getSignLimit());
        }

        if (yeeCourse.getLineLock() >= 0) {
            columns.append("`lineLock`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getLineLock());
        }

        if (yeeCourse.getTplId() > 0) {
            columns.append("`tplId`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getTplId());
        }

        if (yeeCourse.getIsPractice() >= 0) {
            columns.append("`isPractice`, ");
            values.append("?, ");
            parameters.add(yeeCourse.getIsPractice());
        }

        // 自动设置添加时间
        columns.append("`addTime`, ");
        values.append("?, ");
        parameters.add(yeeCourse.getAddTime() != null ? yeeCourse.getAddTime() : new Timestamp(System.currentTimeMillis()));

        // 删除最后的逗号和空格
        columns.delete(columns.length() - 2, columns.length());
        values.delete(values.length() - 2, values.length());

        // 构建完整SQL
        columns.append(") ");
        values.append(")");
        String sql = columns.toString() + values.toString();
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        bindParameters(ps, parameters);

        int rows = ps.executeUpdate();
        if (rows == 0) {
            ps.close();
            throw new SQLException("插入课程失败");
        }

        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                long id = rs.getLong(1);
                ps.close();
                return id;
            } else {
                ps.close();
                throw new SQLException("未获取到课程ID");
            }
        }
    }

    /**
     * 导入章
     */
    private Map<Long, Long> importChapters(int templateId, Long newCourseId, long schoolId,Connection connection) throws Exception {
        List<SlTplChapter> tplChapters = slTplChapterMapper.selectByCourseId(templateId);
        Map<Long, Long> chapterMap = new HashMap<>();

        for (SlTplChapter chapter : tplChapters) {
            YeeChapter yeeChapter = new YeeChapter();
            yeeChapter.setName(chapter.getName());
            yeeChapter.setCourseId(newCourseId.intValue());
            yeeChapter.setSort(chapter.getSort());
            yeeChapter.setSchoolId(schoolId);

            Long newChapterId = insertYeeChapterForImport(yeeChapter, connection);

            chapterMap.put(chapter.getId(), newChapterId);
        }
        return chapterMap;
    }

    /**
     * 导入节
     */
    private Map<Long, Long> importNodes( Long newCourseId, Map<Long, Long> chapterMap, long schoolId,Connection connection) throws Exception {

        // ✅ 先获取所有旧章节ID
        Set<Long> oldChapterIds = chapterMap.keySet();

        // ✅ 关键修复：如果旧章节ID集合为空，直接返回空映射
        if (oldChapterIds == null || oldChapterIds.isEmpty()) {
            return new HashMap<>();
        }
        // ✅ 按 chapterId 集合查询节点
        List<SlTplNode> tplNodes = slTplNode.selectByChapterIds(oldChapterIds);

        Map<Long, Long> nodeMap = new HashMap<>();

        for (SlTplNode node : tplNodes) {
            YeeNode yeeNode = new YeeNode();
            yeeNode.setName(node.getName());
            yeeNode.setCourseId(newCourseId.intValue());
            yeeNode.setVideoFile(node.getVideoFile());
            yeeNode.setVideoDuration(node.getVideoDuration());
            yeeNode.setLocalFile(node.getLocalFile());
            yeeNode.setVotingPath(node.getVotingPath());
            yeeNode.setType(node.getType());
            yeeNode.setTabVideo(node.getTabVideo());
            yeeNode.setTabFile(node.getTabFile());
            yeeNode.setTabVote(node.getTabVote());
            yeeNode.setTabWork(node.getTabWork());
            yeeNode.setTabExam(node.getTabExam());
            yeeNode.setSort(node.getSort());
            yeeNode.setVideoMode(node.getVideoMode());
            yeeNode.setSchoolId(schoolId);
            yeeNode.setLock(0);
            yeeNode.setUnlockTime(0);

            // ⚠️ 关键：替换 chapterId
            Long oldChapterId = node.getChapterId();
            if (oldChapterId != null && chapterMap.containsKey(oldChapterId)) {
                yeeNode.setChapterId(chapterMap.get(oldChapterId).intValue());
            }
            if (oldChapterId != null && chapterMap.containsKey(oldChapterId)) {
                yeeNode.setChapterId(chapterMap.get(oldChapterId).intValue());
            }

            Long newNodeId = insertYeeNodeForImport(yeeNode, connection);
            nodeMap.put(node.getId(), newNodeId);
        }
        return nodeMap;
    }

    /**
     * 导入节点文件
     */
    private void importNodeFiles(Set<Long> oldNodeIds, Map<Long, Long> nodeMap, Long newCourseId, long schoolId, Connection connection) throws Exception {
        if (oldNodeIds == null || oldNodeIds.isEmpty()) {
            return;
        }

        List<SlTplNodeFiles> tplFiles = slTplNodeFilesMapper.selectByNodeIds(oldNodeIds);
        if (tplFiles == null || tplFiles.isEmpty()) {
            return;
        }

        int insertedCount = 0;
        for (SlTplNodeFiles file : tplFiles) {
            // 检查节点映射
            Long newNodeId = nodeMap.get(file.getNodeId());
            if (newNodeId == null) {
                continue;
            }

            YeeNodeFiles yeeFile = new YeeNodeFiles();
            yeeFile.setNodeId(newNodeId.intValue());
            yeeFile.setCourseId(newCourseId != null ? newCourseId.intValue() : 0);
            yeeFile.setSchoolId(schoolId);

            // 注意：name 和 fileName 是否应相同？根据业务确认
            yeeFile.setName(file.getFileName());
            yeeFile.setFileName(file.getFileName());
            yeeFile.setUploadPath(file.getUploadPath());
            yeeFile.setTimeView(file.getTimeView()); // 允许 -1 或 null，由 insert 方法处理
            yeeFile.setCreateUserId(file.getCreateUserId());
            yeeFile.setAddTime(file.getAddTime());

            try {
                Long fileId = insertYeeNodeFilesForImport(yeeFile, connection);
                if (fileId != null && fileId > 0) {
                    insertedCount++;
                }
            } catch (Exception e) {
                // 可选择继续或抛出异常
            }
        }

    }

    public Long insertYeeChapterForImport(YeeChapter chapter, Connection conn) throws SQLException {
        StringBuilder columns = new StringBuilder("INSERT INTO yee_chapter (");
        StringBuilder values = new StringBuilder("VALUES (");
        List<Object> parameters = new ArrayList<>();

        // 必填字段
        columns.append("`schoolId`, `courseId`");
        values.append("?, ?");
        parameters.add(chapter.getSchoolId());
        parameters.add(chapter.getCourseId());

        // 可选字段
        if (chapter.getName() != null && !chapter.getName().trim().isEmpty()) {
            columns.append(", `name`");
            values.append(", ?");
            parameters.add(chapter.getName());
        }
        if (chapter.getSort() >= 0) {
            columns.append(", `sort`");
            values.append(", ?");
            parameters.add(chapter.getSort());
        }

        String sql = columns + ") " + values + ")";

        // 👇 关键：要求返回生成的主键
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            if (param instanceof String) {
                ps.setString(i + 1, (String) param);
            } else if (param instanceof Long) {
                ps.setLong(i + 1, (Long) param);
            } else if (param instanceof Integer) {
                ps.setInt(i + 1, (Integer) param);
            }
        }

        int rows = ps.executeUpdate();
        if (rows == 0) {
            ps.close();
            throw new SQLException("插入章节失败");
        }

        // 获取自增主键
        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                long id = rs.getLong(1);
                ps.close();
                return id;
            } else {
                ps.close();
                throw new SQLException("未获取到章节ID");
            }
        }
    }

    public Long insertYeeNodeForImport(YeeNode node, Connection conn) throws SQLException {
        StringBuilder columns = new StringBuilder("INSERT INTO yee_node (");
        StringBuilder values = new StringBuilder("VALUES (");
        List<Object> parameters = new ArrayList<>();

        // 必填字段
        columns.append("`schoolId`, `courseId`");
        values.append("?, ?");
        parameters.add(node.getSchoolId());
        parameters.add(node.getCourseId());

        // 动态字段
        if (node.getName() != null && !node.getName().trim().isEmpty()) {
            columns.append(", `name`");
            values.append(", ?");
            parameters.add(node.getName());
        }
        if (node.getType() != null && !node.getType().trim().isEmpty()) {
            columns.append(", `type`");
            values.append(", ?");
            parameters.add(node.getType());
        }
        if (node.getChapterId() > 0) {
            columns.append(", `chapterId`");
            values.append(", ?");
            parameters.add(node.getChapterId());
        }
        if (node.getVideoFile() != null && !node.getVideoFile().trim().isEmpty()) {
            columns.append(", `videoFile`");
            values.append(", ?");
            parameters.add(node.getVideoFile());
        }
        if (node.getVideoDuration() >= 0) {
            columns.append(", `videoDuration`");
            values.append(", ?");
            parameters.add(node.getVideoDuration());
        }
        if (node.getVotingPath() != null && !node.getVotingPath().trim().isEmpty()) {
            columns.append(", `votingPath`");
            values.append(", ?");
            parameters.add(node.getVotingPath());
        }
        if (node.getTabVideo() >= 0) {
            columns.append(", `tabVideo`");
            values.append(", ?");
            parameters.add(node.getTabVideo());
        }
        if (node.getTabFile() >= 0) {
            columns.append(", `tabFile`");
            values.append(", ?");
            parameters.add(node.getTabFile());
        }
        if (node.getTabVote() >= 0) {
            columns.append(", `tabVote`");
            values.append(", ?");
            parameters.add(node.getTabVote());
        }
        if (node.getTabWork() >= 0) {
            columns.append(", `tabWork`");
            values.append(", ?");
            parameters.add(node.getTabWork());
        }
        if (node.getTabExam() >= 0) {
            columns.append(", `tabExam`");
            values.append(", ?");
            parameters.add(node.getTabExam());
        }
        if (node.getSort() >= 0) {
            columns.append(", `sort`");
            values.append(", ?");
            parameters.add(node.getSort());
        }
        if (node.getVideoMode() >= 0) {
            columns.append(", `videoMode`");
            values.append(", ?");
            parameters.add(node.getVideoMode());
        }
        if (node.getLocalFile() != null && !node.getLocalFile().trim().isEmpty()) {
            columns.append(", `localFile`");
            values.append(", ?");
            parameters.add(node.getLocalFile());
        }
        // ✅ 修复点：给 lock 加反引号 `lock`
        if (node.getLock() >= 0) {
            columns.append(", `lock`");
            values.append(", ?");
            parameters.add(node.getLock());
        }
        if (node.getUnlockTime() >= 0) {
            columns.append(", `unlockTime`");
            values.append(", ?");
            parameters.add(node.getUnlockTime());
        }

        String sql = columns + ") " + values + ")";
        PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

        bindParameters(ps, parameters);

        int rows = ps.executeUpdate();
        if (rows == 0) {
            ps.close();
            throw new SQLException("插入节点失败");
        }

        try (ResultSet rs = ps.getGeneratedKeys()) {
            if (rs.next()) {
                long id = rs.getLong(1);
                ps.close();
                return id;
            } else {
                ps.close();
                throw new SQLException("未获取到节点ID");
            }
        }
    }

    public Long insertYeeNodeFilesForImport(YeeNodeFiles nodeFiles, Connection conn) throws SQLException {
        StringBuilder columns = new StringBuilder("INSERT INTO yee_node_files (");
        StringBuilder values = new StringBuilder("VALUES (");
        List<Object> parameters = new ArrayList<>();

        // 必填字段（假设 nodeId 和 schoolId 总是存在）
        columns.append("`schoolId`, `nodeId`");
        values.append("?, ?");
        parameters.add(nodeFiles.getSchoolId());
        parameters.add(nodeFiles.getNodeId());

        // 使用 Objects.nonNull 判断更安全（避免 trim() 报 NPE）
        if (nodeFiles.getCourseId() > 0) {
            columns.append(", `courseId`");
            values.append(", ?");
            parameters.add(nodeFiles.getCourseId());
        }
        if (nodeFiles.getName() != null && !nodeFiles.getName().isEmpty()) {
            columns.append(", `name`");
            values.append(", ?");
            parameters.add(nodeFiles.getName());
        }
        if (nodeFiles.getUploadPath() != null && !nodeFiles.getUploadPath().isEmpty()) {
            columns.append(", `uploadPath`");
            values.append(", ?");
            parameters.add(nodeFiles.getUploadPath());
        }
        // timeView 允许 0 或正数，也允许 null（若 DB 允许）
        if (nodeFiles.getTimeView() > 0) {
            columns.append(", `timeView`");
            values.append(", ?");
            parameters.add(nodeFiles.getTimeView());
        }
        if (nodeFiles.getCreateUserId() > 0) {
            columns.append(", `createUserId`");
            values.append(", ?");
            parameters.add(nodeFiles.getCreateUserId());
        }
        if (nodeFiles.getFileName() != null && !nodeFiles.getFileName().isEmpty()) {
            columns.append(", `fileName`");
            values.append(", ?");
            parameters.add(nodeFiles.getFileName());
        }

        // addTime 处理
        Timestamp addTime = nodeFiles.getAddTime();
        if (addTime == null) {
            addTime = new Timestamp(System.currentTimeMillis());
        }
        columns.append(", `addTime`");
        values.append(", ?");
        parameters.add(addTime);

        String sql = columns + ") " + values + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParameters(ps, parameters);
            int rows = ps.executeUpdate();
            if (rows <= 0) {
                throw new SQLException("插入节点文件失败，影响行数为0");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                } else {
                    throw new SQLException("未能获取自增主键");
                }
            }
        }
    }

    private void bindParameters(PreparedStatement ps, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object param = parameters.get(i);
            if (param == null) {
                ps.setObject(i + 1, null);
            } else if (param instanceof String) {
                ps.setString(i + 1, (String) param);
            } else if (param instanceof Long) {
                ps.setLong(i + 1, (Long) param);
            } else if (param instanceof Integer) {
                ps.setInt(i + 1, (Integer) param);
            } else if (param instanceof Double) {
                ps.setDouble(i + 1, (Double) param);
            } else if (param instanceof Float) {
                ps.setFloat(i + 1, (Float) param);
            } else if (param instanceof Boolean) {
                ps.setBoolean(i + 1, (Boolean) param);
            } else if (param instanceof java.sql.Date) {
                ps.setDate(i + 1, (java.sql.Date) param);
            } else if (param instanceof java.sql.Timestamp) {
                ps.setTimestamp(i + 1, (Timestamp) param);
            } else if (param instanceof java.math.BigDecimal) {
                ps.setBigDecimal(i + 1, (BigDecimal) param);
            } else {
                // 如果还有其他未覆盖的类型，可继续扩展；否则保留异常便于排查
                throw new IllegalArgumentException("Unsupported parameter type: " + param.getClass());
            }
        }
    }

    @Override
    public void exportCourseData(YeeCourseQueryParam param, HttpServletResponse response) throws Exception {
        // 验证学校
        Integer schoolId = param.getSchoolId();
        if (schoolId == null) {
            throw new Exception("学校ID不能为空");
        }

        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            throw new Exception("学校不存在或未审核");
        }

        // 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            throw new Exception("无法获取数据库连接");
        }

        try {
            // 构建查询SQL（导出不需要分页）
            StringBuilder sqlBuilder = new StringBuilder("SELECT yc.id, yc.name, yc.code, yc.mode, yc.tplId, yc.startDate, yc.endDate, yc.credit, yc.allow, yc.stuCount, yc.createId, yc.schoolId, yc.collegeId, yc.addDate, ym.name AS createName, yco.name AS collegeName FROM yee_course yc LEFT JOIN yee_manage ym ON yc.createId = ym.id LEFT JOIN yee_college yco ON yc.collegeId = yco.id WHERE yc.schoolId = ?");

            // 参数列表
            List<Object> parameters = new ArrayList<>();
            parameters.add(schoolId);

            // 动态添加查询条件
            if (param.getName() != null && !param.getName().trim().isEmpty()) {
                sqlBuilder.append(" AND yc.name LIKE ?");
                parameters.add("%" + param.getName().trim() + "%");
            }

            if (param.getId() != null && param.getId() > 0) {
                sqlBuilder.append(" AND yc.id = ?");
                parameters.add(param.getId());
            }

            if (param.getMode() != null) {
                sqlBuilder.append(" AND yc.mode = ?");
                parameters.add(param.getMode());
            }

            if (param.getCollegeId() != null && param.getCollegeId() > 0) {
                sqlBuilder.append(" AND yc.collegeId = ?");
                parameters.add(param.getCollegeId());
            }

            if (param.getCode() != null && !param.getCode().trim().isEmpty()) {
                sqlBuilder.append(" AND yc.code LIKE ?");
                parameters.add("%" + param.getCode().trim() + "%");
            }

            if (param.getCreateId() != null && param.getCreateId() > 0) {
                sqlBuilder.append(" AND yc.createId = ?");
                parameters.add(param.getCreateId());
            }

            if (param.getCateBid() != null && param.getCateBid() > 0) {
                sqlBuilder.append(" AND yc.cateBid = ?");
                parameters.add(param.getCateBid());
            }

            if (param.getCateMid() != null && param.getCateMid() > 0) {
                sqlBuilder.append(" AND yc.cateMid = ?");
                parameters.add(param.getCateMid());
            }

            if (param.getStartDate() != null) {
                sqlBuilder.append(" AND yc.startDate >= ?");
                parameters.add(param.getStartDate());
            }

            if (param.getEndDate() != null) {
                sqlBuilder.append(" AND yc.endDate <= ?");
                parameters.add(param.getEndDate());
            }

            if (param.getSelfBuilt() != null) {
                if (param.getSelfBuilt()) {
                    // 自建课程 (tplId = 0)
                    sqlBuilder.append(" AND yc.tplId = 0");
                } else {
                    // 非自建课程 (tplId > 0)
                    sqlBuilder.append(" AND yc.tplId > 0");
                }
            }

            if (param.getAllow() != null) {
                sqlBuilder.append(" AND yc.allow = ?");
                parameters.add(param.getAllow());
            }

            // 添加排序
            sqlBuilder.append(" ORDER BY yc.addDate DESC");

            // 执行查询
            PreparedStatement st = connection.prepareStatement(sqlBuilder.toString());
            for (int i = 0; i < parameters.size(); i++) {
                st.setObject(i + 1, parameters.get(i));
            }
            ResultSet rs = st.executeQuery();
            List<YeeCourseExportVo> exportData = rsToYeeCourseExportVo(rs, slSchool);

            rs.close();
            st.close();

            // 设置响应头
            LocalDateTime now = LocalDateTime.now();
            String fileName = "试题题目导出_" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            fileName = URLEncoder.encode(fileName, "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName);

            // 使用ByteArrayOutputStream作为中间缓冲区
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            EasyExcel.write(byteArrayOutputStream, YeeCourseExportVo.class)
                    .sheet("课程数据")
                    .doWrite(exportData);

            // 将数据写入响应流
            byteArrayOutputStream.writeTo(response.getOutputStream());
            response.getOutputStream().flush();

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("导出失败：" + e.getMessage());
        } finally {
            // 确保连接关闭
            if (connection != null && !connection.isClosed()) {
                try {
                    connection.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public void exportCourseStudentEnrollmentData(YeeCourseQueryParam param, HttpServletResponse response) throws Exception {
        Connection connection = null;
        LocalDate startDate = param.getStartDate().toLocalDate();
        LocalDate endDate = param.getEndDate().toLocalDate();
        try {
            // 获取学校信息
            SlSchool slSchool = slSchoolMapper.selectById(param.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                throw new Exception("学校不存在或未审核");
            }

            // 获取数据库连接
            connection = SlaveMysqlConnectionUtil.getConnection(slSchool);

            // 构建基础查询SQL
            String baseSql = """
                SELECT 
                    c.name AS courseName, 
                    c.mode AS courseMode, 
                    c.code AS courseCode, 
                    c.lecturerName AS lecturerName, 
                    c.startDate AS startDate, 
                    c.endDate AS endDate, 
                    cs.addTime AS enrollmentTime, 
                    s.number AS studentId, 
                    s.name AS studentName, 
                    s.gender AS gender, 
                    s.idCard AS idCard, 
                    col.name AS collegeName, 
                    cl.name AS className, 
                    s.entryYear AS grade 
                FROM yee_course c 
                JOIN yee_course_student cs ON c.id = cs.courseId 
                JOIN yee_student s ON cs.studentId = s.id 
                LEFT JOIN yee_college col ON s.collegeId = col.id 
                LEFT JOIN yee_classes cl ON s.classId = cl.id 
                WHERE 1=1 
                """;

            StringBuilder sql = new StringBuilder(baseSql);
            List<Object> params = new ArrayList<>();

            // 添加日期范围筛选条件
            if (startDate != null) {
                sql.append("AND c.startDate >= ? ");
                params.add(startDate);
            }

            if (endDate != null) {
                sql.append("AND c.endDate <= ? ");
                params.add(endDate);
            }

            // 添加学校过滤条件
            sql.append("AND c.schoolId = ? ");
            params.add(param.getSchoolId());

            // 准备SQL语句
            PreparedStatement st = connection.prepareStatement(sql.toString());
            for (int i = 0; i < params.size(); i++) {
                st.setObject(i + 1, params.get(i));
            }

            // 设置查询超时和获取结果集
            st.setQueryTimeout(300); // 5分钟超时
            ResultSet rs = st.executeQuery();

            // 设置响应头
            String fileName = "学生选课记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            // 使用统一的Excel响应头设置工具
            ResponseExportUtil.setExcelRespProp(response, fileName);

            // 为大数据量创建Excel写入器
            ExcelWriter excelWriter = EasyExcel.write(response.getOutputStream(), CourseStudentEnrollmentExportDto.class)
                .autoCloseStream(false)
                .build();

            WriteSheet writeSheet = EasyExcel.writerSheet("课程学生选课数据").build();

            // 分批处理数据，避免内存溢出
            List<CourseStudentEnrollmentExportDto> batch = new ArrayList<>();
            final int batchSize = 1000; // 每批处理1000条记录
            int count = 0;

            try {
                while (rs.next()) {
                    CourseStudentEnrollmentExportDto dto = new CourseStudentEnrollmentExportDto();

                    dto.setCourseName(rs.getString("courseName"));
                    dto.setCourseMode(rs.getInt("courseMode") == 2 ? "选修课" : "必修课");
                    dto.setCourseCode(rs.getString("courseCode"));
                    dto.setLecturerName(rs.getString("lecturerName"));
                    dto.setStartDate(rs.getObject("startDate", LocalDateTime.class));
                    dto.setEndDate(rs.getObject("endDate", LocalDateTime.class));
                    dto.setEnrollmentTime(rs.getObject("enrollmentTime", LocalDateTime.class));
                    dto.setStudentId(rs.getString("studentId"));
                    dto.setStudentName(rs.getString("studentName"));
//                    dto.setGender(rs.getString("gender"));
//                    dto.setIdCard(rs.getString("idCard"));
                    dto.setCollegeName(rs.getString("collegeName"));
                    dto.setClassName(rs.getString("className"));
                    dto.setGrade(rs.getObject("grade") != null ? rs.getInt("grade") : null);

                    batch.add(dto);
                    count++;

                    // 当达到批次大小时，写入数据
                    if (batch.size() >= batchSize) {
                        excelWriter.write(batch, writeSheet);
                        batch.clear(); // 清空批次，释放内存
                        System.gc(); // 建议进行垃圾回收
                    }
                }

                // 写入最后一批数据（如果有的话）
                if (!batch.isEmpty()) {
                    excelWriter.write(batch, writeSheet);
                    batch.clear();
                }

            } finally {
                // 确保Excel写入器被正确关闭
                if (excelWriter != null) {
                    excelWriter.finish();
                }
            }

            rs.close();
            st.close();

        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("导出失败: " + e.getMessage());
        } finally {
            // 确保连接被正确关闭
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     *
     * @param courseId 课程ID
     * @param conn 课程所在学校的数据库连接
     * @return 学校ID（不存在返回-1）
     * @throws SQLException 仅抛出SQL异常，与上层方法兼容
     */
    private int getSchoolIdByCourseId(Long courseId, Connection conn) throws SQLException {
        String sql = "SELECT schoolId FROM yee_course WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("schoolId");
                }
            }
        }
        return -1;
    }

    /**
     * 课程复制（最终版）
     * 核心逻辑：复制「课程+章节+节点+节点文件+作业+考试」，无班级相关操作
     * @param yeeCourse 前端传入的课程对象（含sourceCourseId/targetSchoolId/name）
     * @return 复制结果
     */
    @Override
    public Result copyCourse(YeeCourse yeeCourse) {
        // 1. 解析核心参数
        Long sourceCourseId = yeeCourse.getId();     // 源课程ID（仅用于复制章节/节点/文件结构）
        Long targetSchoolId = yeeCourse.getSchoolId();// 目标学校ID（前端传入）
        String newCourseName = yeeCourse.getName();  // 新课程名称（前端传入）

        // 2. 基础参数校验
        if (sourceCourseId == null || sourceCourseId <= 0) {
            return Result.error("参数错误：源课程ID不能为空且必须大于0");
        }
        if (targetSchoolId == null || targetSchoolId <= 0) {
            return Result.error("参数错误：目标学校ID不能为空且必须大于0");
        }
        if (StringUtils.isEmpty(newCourseName)) {
            return Result.error("参数错误：新课程名称不能为空"); // 新增：前端必须传名称，不再默认加副本
        }

        // 新增：校验前端传入的开课/结束时间（作业/考试依赖）
        if (yeeCourse.getStartDate() == null) {
            return Result.error("参数错误：新课程开课时间不能为空");
        }
        if (yeeCourse.getEndDate() == null) {
            return Result.error("参数错误：新课程结束时间不能为空");
        }

        Connection conn = null;
        Connection sourceConn = null;
        Connection tempConn = null;
        try {
            // 3. 校验目标学校有效性
            SlSchool targetSchool = slSchoolMapper.selectById(targetSchoolId.intValue());
            if (targetSchool == null || targetSchool.getAllow() == 0) {
                return Result.error("目标学校不存在或未审核");
            }

            // 4. 查询源课程所属学校ID（仅用于获取源课程结构）
            tempConn = SlaveMysqlConnectionUtil.getConnection(targetSchool);
            Integer sourceSchoolId = getSchoolIdByCourseId(sourceCourseId, tempConn);
            if (sourceSchoolId == null || sourceSchoolId == -1) {
                return Result.error("源课程不存在");
            }

            // 5. 获取源课程所在学校的数据库连接（仅用于复制结构）
            SlSchool sourceSchool = slSchoolMapper.selectById(sourceSchoolId);
            if (sourceSchool == null) {
                return Result.error("源课程所属学校不存在");
            }
            sourceConn = SlaveMysqlConnectionUtil.getConnection(sourceSchool);

            // 6. 校验源课程是否存在（仅校验，不再复用其属性）
            YeeCourse sourceCourse = getCourseById(sourceConn, sourceCourseId);
            if (sourceCourse == null) {
                return Result.error("源课程信息不存在，无法复制结构");
            }

            // 核心修改：从前端传入的yeeCourse获取开课/结束时间（作业/考试用）
            Long courseStartTime = yeeCourse.getStartDate().getTime() / 1000; // 转秒级时间戳
            Long courseEndTime = yeeCourse.getEndDate().getTime() / 1000;

            // 7. 构建目标课程信息（核心修改：完全使用前端传入的yeeCourse属性）
            YeeCourse targetCourse = new YeeCourse();
            // ✅ 关键：复制前端传入的所有属性（除了id/addTime/stuCount）
            BeanUtils.copyProperties(yeeCourse, targetCourse, "id", "addTime", "stuCount");
            // 强制覆盖：确保学校ID是前端指定的目标学校
            targetCourse.setSchoolId(targetSchoolId);
            // 强制重置：学生数、创建时间、创建人（固定逻辑）
            targetCourse.setStuCount(0L);
            targetCourse.setAddTime(new Timestamp(System.currentTimeMillis()));
            // 设置创建人（当前登录教师）
            LoginUser loginUser = (LoginUser) StpUtil.getSession().get(StpUtil.getLoginId().toString());
            if (loginUser != null && loginUser.getYeeManage() != null) {
                targetCourse.setCreateId(loginUser.getYeeManage().getId());
            }

            // 8. 开启目标学校事务，执行复制流程
            conn = SlaveMysqlConnectionUtil.getConnection(targetSchool);
            conn.setAutoCommit(false);

            // 8.1 复制课程主信息（完全用前端传入的属性）
            Long newCourseId = insertYeeCourseForImport(targetCourse, conn);
            if (newCourseId == null || newCourseId <= 0) {
                conn.rollback();
                return Result.error("复制课程主信息失败");
            }

            // 8.2 复制课程章节（仅复制结构，归属新课程）
            Map<Long, Long> chapterMap = copyCourseChapters(sourceConn, sourceCourseId, sourceSchoolId,
                    newCourseId, targetSchoolId, conn);

            // 8.3 复制章节节点（仅复制结构，归属新课程）
            Map<Long, Long> nodeMap = copyCourseNodes(sourceConn, sourceCourseId, chapterMap, newCourseId, targetSchoolId, conn);

            // 8.4 复制节点文件（仅复制文件关联，归属新课程）
            copyCourseNodeFiles(sourceConn, sourceCourseId, nodeMap.keySet(), nodeMap, newCourseId, targetSchoolId, conn);

            // 8.5 复制作业 + 获取作业ID映射
            Map<Long, Long> workIdMap = copyCourseWorks(sourceConn, sourceCourseId, nodeMap, newCourseId, targetSchoolId, courseStartTime, courseEndTime, conn);

            // 8.6 复制考试 + 获取考试ID映射
            Map<Long, Long> examIdMap = copyCourseExams(sourceConn, sourceCourseId, nodeMap, newCourseId, targetSchoolId, courseStartTime, courseEndTime, conn);

            // 8.7 复制作业题目
            copyWorkTopics(sourceConn, workIdMap, targetSchoolId.intValue(), conn);

            // 8.8 复制考试题目
            copyExamTopics(sourceConn, examIdMap, targetSchoolId.intValue(), conn);
            // 9. 提交事务
            conn.commit();

            // 返回结果
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("newCourseId", newCourseId);
            resultData.put("newCourseName", targetCourse.getName());
            return Result.success("课程复制成功！");

        } catch (Exception e) {
            // 异常回滚
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
            e.printStackTrace();
            return Result.error("课程复制失败：" + e.getMessage());
        } finally {
            // 关闭所有数据库连接
            if (tempConn != null) {
                try { tempConn.close(); } catch (SQLException ignored) {}
            }
            if (sourceConn != null) {
                try { sourceConn.close(); } catch (SQLException ignored) {}
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * 复制课程作业
     */
    /**
     * 复制课程作业，并返回【旧作业ID → 新作业ID】映射
     */
    private Map<Long, Long> copyCourseWorks(Connection sourceConn, Long sourceCourseId, Map<Long, Long> nodeMap,
                                            Long newCourseId, Long targetSchoolId, Long courseStartTime,
                                            Long courseEndTime, Connection targetConn) throws SQLException {
        Map<Long, Long> workIdMap = new HashMap<>(); // 旧作业ID → 新作业ID

        // 查询源课程所有作业
        String workSql = "SELECT id, userId, title, topicNumber, score, type, remarks, addTime, sequence, " +
                "nodeId, courseId, startTime, endTime, paperId, createUserId, isPrivate, classList, " +
                "teacherType, allow, frequency, scoringRules, hasCollect, `lock`, schoolId, parsing " +
                "FROM yee_work WHERE courseId = ? AND schoolId = ?";

        try (PreparedStatement ps = sourceConn.prepareStatement(workSql)) {
            ps.setLong(1, sourceCourseId);
            int sourceSchoolId = getSchoolIdByCourseId(sourceCourseId, sourceConn);
            ps.setInt(2, sourceSchoolId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long sourceWorkId = rs.getLong("id"); // 旧作业ID
                    Long sourceNodeId = rs.getLong("nodeId");
                    Long newNodeId = nodeMap.get(sourceNodeId);
                    if (newNodeId == null) {
                        continue;
                    }

                    // 插入新作业SQL
                    String insertSql = "INSERT INTO yee_work (userId, title, topicNumber, score, type, remarks, " +
                            "addTime, sequence, nodeId, courseId, startTime, endTime, paperId, createUserId, " +
                            "isPrivate, classList, teacherType, allow, frequency, scoringRules, hasCollect, " +
                            "`lock`, schoolId, parsing) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement insertPs = targetConn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        // 1. 基础字段赋值
                        insertPs.setInt(1, rs.getInt("userId"));
                        insertPs.setString(2, rs.getString("title"));
                        insertPs.setInt(3, rs.getInt("topicNumber"));
                        insertPs.setInt(4, rs.getInt("score"));
                        insertPs.setInt(5, rs.getInt("type"));
                        insertPs.setString(6, rs.getString("remarks"));
                        insertPs.setTimestamp(7, new Timestamp(System.currentTimeMillis()));
                        insertPs.setInt(8, rs.getInt("sequence"));
                        insertPs.setLong(9, newNodeId);
                        insertPs.setLong(10, newCourseId);
                        insertPs.setLong(11, courseStartTime);
                        insertPs.setLong(12, courseEndTime);
                        insertPs.setInt(13, rs.getInt("paperId"));
                        insertPs.setInt(14, rs.getInt("createUserId"));
                        insertPs.setInt(15, rs.getInt("isPrivate"));
                        insertPs.setString(16, "[]");
                        insertPs.setInt(17, rs.getInt("teacherType"));
                        insertPs.setInt(18, rs.getInt("allow"));
                        insertPs.setInt(19, rs.getInt("frequency"));
                        insertPs.setInt(20, rs.getInt("scoringRules"));
                        insertPs.setInt(21, rs.getInt("hasCollect"));
                        insertPs.setInt(22, rs.getInt("lock"));
                        insertPs.setLong(23, targetSchoolId);
                        insertPs.setInt(24, rs.getInt("parsing"));

                        insertPs.executeUpdate();

                        // 获取新作业ID并保存映射
                        try (ResultSet keysRs = insertPs.getGeneratedKeys()) {
                            if (keysRs.next()) {
                                Long newWorkId = keysRs.getLong(1);
                                workIdMap.put(sourceWorkId, newWorkId);
                            }
                        }
                    }
                }
            }
        }
        return workIdMap; // 返回映射
    }

    /**
     * 复制课程考试
     */
    /**
     * 复制课程考试，并返回【旧考试ID → 新考试ID】映射
     */
    private Map<Long, Long> copyCourseExams(Connection sourceConn, Long sourceCourseId, Map<Long, Long> nodeMap,
                                            Long newCourseId, Long targetSchoolId, Long courseStartTime,
                                            Long courseEndTime, Connection targetConn) throws SQLException {
        Map<Long, Long> examIdMap = new HashMap<>(); // 旧考试ID → 新考试ID

        // 查询源课程所有考试
        String examSql = "SELECT id, userId, title, topicNumber, score, addTime, nodeId, courseId, limitedTime, " +
                "sequence, remarks, paperId, startTime, endTime, createUserId, classList, isPrivate, " +
                "teacherType, allow, frequency, hasCollect, schoolId, parsing, random, randData, randNumber " +
                "FROM yee_exam WHERE courseId = ? AND schoolId = ?";

        try (PreparedStatement ps = sourceConn.prepareStatement(examSql)) {
            ps.setLong(1, sourceCourseId);
            int sourceSchoolId = getSchoolIdByCourseId(sourceCourseId, sourceConn);
            ps.setInt(2, sourceSchoolId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long sourceExamId = rs.getLong("id"); // 旧考试ID
                    Long sourceNodeId = rs.getLong("nodeId");
                    Long newNodeId = nodeMap.get(sourceNodeId);
                    if (newNodeId == null) {
                        continue;
                    }

                    // 插入新考试SQL
                    String insertSql = "INSERT INTO yee_exam (userId, title, topicNumber, score, addTime, nodeId, " +
                            "courseId, limitedTime, sequence, remarks, paperId, startTime, endTime, createUserId, " +
                            "classList, isPrivate, teacherType, allow, frequency, hasCollect, schoolId, parsing, " +
                            "random, randData, randNumber) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                    try (PreparedStatement insertPs = targetConn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        // 1. 基础字段赋值
                        insertPs.setInt(1, rs.getInt("userId"));
                        insertPs.setString(2, rs.getString("title"));
                        insertPs.setInt(3, rs.getInt("topicNumber"));
                        insertPs.setInt(4, rs.getInt("score"));
                        insertPs.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                        insertPs.setLong(6, newNodeId);
                        insertPs.setLong(7, newCourseId);
                        insertPs.setInt(8, rs.getInt("limitedTime"));
                        insertPs.setInt(9, rs.getInt("sequence"));
                        insertPs.setString(10, rs.getString("remarks"));
                        insertPs.setInt(11, rs.getInt("paperId"));
                        insertPs.setLong(12, courseStartTime);
                        insertPs.setLong(13, courseEndTime);
                        insertPs.setInt(14, rs.getInt("createUserId"));
                        insertPs.setString(15, "[]");
                        insertPs.setInt(16, rs.getInt("isPrivate"));
                        insertPs.setInt(17, rs.getInt("teacherType"));
                        insertPs.setInt(18, rs.getInt("allow"));
                        insertPs.setInt(19, rs.getInt("frequency"));
                        insertPs.setInt(20, rs.getInt("hasCollect"));
                        insertPs.setLong(21, targetSchoolId);
                        insertPs.setInt(22, rs.getInt("parsing"));
                        insertPs.setInt(23, rs.getInt("random"));
                        insertPs.setString(24, rs.getString("randData"));
                        insertPs.setInt(25, rs.getInt("randNumber"));

                        insertPs.executeUpdate();

                        // 获取新考试ID并保存映射
                        try (ResultSet keysRs = insertPs.getGeneratedKeys()) {
                            if (keysRs.next()) {
                                Long newExamId = keysRs.getLong(1);
                                examIdMap.put(sourceExamId, newExamId);
                            }
                        }
                    }
                }
            }
        }
        return examIdMap;
    }

    /**
     * 复制作业题目（根据作业ID映射）
     */
    private void copyWorkTopics(Connection sourceConn, Map<Long, Long> workIdMap,
                                int targetSchoolId, Connection targetConn) throws SQLException {
        if (workIdMap.isEmpty()) return;

        String sql = "SELECT id, topic, type, level, score, missScore, option1, option2, option3, analysis, " +
                "pid, workId, title, oid, number, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid " +
                "FROM yee_work_topic WHERE workId = ? ";

        for (Map.Entry<Long, Long> entry : workIdMap.entrySet()) {
            Long sourceWorkId = entry.getKey();
            Long newWorkId = entry.getValue();

            try (PreparedStatement ps = sourceConn.prepareStatement(sql)) {
                ps.setLong(1, sourceWorkId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String insertSql = "INSERT INTO yee_work_topic " +
                                "(topic, type, level, score, missScore, option1, option2, option3, analysis, " +
                                "pid, workId, title, oid, number, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

                        try (PreparedStatement insertPs = targetConn.prepareStatement(insertSql)) {
                            insertPs.setString(1, rs.getString("topic"));
                            insertPs.setInt(2, rs.getInt("type"));
                            insertPs.setInt(3, rs.getInt("level"));
                            insertPs.setInt(4, rs.getInt("score"));
                            insertPs.setString(5, rs.getString("missScore"));
                            insertPs.setString(6, rs.getString("option1"));
                            insertPs.setString(7, rs.getString("option2"));
                            insertPs.setString(8, rs.getString("option3"));
                            insertPs.setString(9, rs.getString("analysis"));
                            insertPs.setInt(10, rs.getInt("pid"));
                            insertPs.setLong(11, newWorkId); // 新作业ID
                            insertPs.setString(12, rs.getString("title"));
                            insertPs.setInt(13, rs.getInt("oid"));
                            insertPs.setInt(14, rs.getInt("number"));
                            insertPs.setString(15, rs.getString("upload"));
                            insertPs.setString(16, rs.getString("option"));
                            insertPs.setInt(17, rs.getInt("scoreMode"));
                            insertPs.setInt(18, targetSchoolId); // 目标学校
                            insertPs.setString(19, rs.getString("categoryId"));
                            insertPs.setInt(20, rs.getInt("cateBid"));
                            insertPs.setInt(21, rs.getInt("cateMid"));

                            insertPs.executeUpdate();
                        }
                    }
                }
            }
        }
    }

    /**
     * 复制考试题目（根据考试ID映射）
     */
    private void copyExamTopics(Connection sourceConn, Map<Long, Long> examIdMap,
                                int targetSchoolId, Connection targetConn) throws SQLException {
        if (examIdMap.isEmpty()) return;

        String sql = "SELECT id, topic, type, level, score, missScore, option1, option2, option3, analysis, " +
                "pid, examId, title, oid, number, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid " +
                "FROM yee_exam_topic WHERE examId = ? ";

        for (Map.Entry<Long, Long> entry : examIdMap.entrySet()) {
            Long sourceExamId = entry.getKey();
            Long newExamId = entry.getValue();

            try (PreparedStatement ps = sourceConn.prepareStatement(sql)) {
                ps.setLong(1, sourceExamId);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String insertSql = "INSERT INTO yee_exam_topic " +
                                "(topic, type, level, score, missScore, option1, option2, option3, analysis, " +
                                "pid, examId, title, oid, number, upload, `option`, scoreMode, schoolId, categoryId, cateBid, cateMid) " +
                                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

                        try (PreparedStatement insertPs = targetConn.prepareStatement(insertSql)) {
                            insertPs.setString(1, rs.getString("topic"));
                            insertPs.setInt(2, rs.getInt("type"));
                            insertPs.setInt(3, rs.getInt("level"));
                            insertPs.setInt(4, rs.getInt("score"));
                            insertPs.setString(5, rs.getString("missScore"));
                            insertPs.setString(6, rs.getString("option1"));
                            insertPs.setString(7, rs.getString("option2"));
                            insertPs.setString(8, rs.getString("option3"));
                            insertPs.setString(9, rs.getString("analysis"));
                            insertPs.setInt(10, rs.getInt("pid"));
                            insertPs.setLong(11, newExamId); // 新考试ID
                            insertPs.setString(12, rs.getString("title"));
                            insertPs.setInt(13, rs.getInt("oid"));
                            insertPs.setInt(14, rs.getInt("number"));
                            insertPs.setString(15, rs.getString("upload"));
                            insertPs.setString(16, rs.getString("option"));
                            insertPs.setInt(17, rs.getInt("scoreMode"));
                            insertPs.setInt(18, targetSchoolId); // 目标学校
                            insertPs.setString(19, rs.getString("categoryId"));
                            insertPs.setInt(20, rs.getInt("cateBid"));
                            insertPs.setInt(21, rs.getInt("cateMid"));

                            insertPs.executeUpdate();
                        }
                    }
                }
            }
        }
    }
    /**
     * 根据ID查询课程完整信息
     */
    private YeeCourse getCourseById(Connection conn, Long courseId) throws SQLException {
        String sql = "SELECT * FROM yee_course WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, courseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    YeeCourse course = new YeeCourse();
                    course.setId(rs.getLong("id"));
                    course.setName(rs.getString("name"));
                    course.setMode(rs.getLong("mode"));
                    course.setCollegeId(rs.getLong("collegeId"));
                    course.setCategoryId(rs.getString("categoryId"));
                    course.setLecturers(rs.getString("lecturers"));
                    course.setStartDate(rs.getDate("startDate"));
                    course.setEndDate(rs.getDate("endDate"));
                    course.setCover(rs.getString("cover"));
                    course.setContent(rs.getString("content"));
                    course.setCredit(rs.getDouble("credit"));
                    course.setAllow(rs.getLong("allow"));
                    course.setIntro(rs.getString("intro"));
                    course.setTeacherIntro(rs.getString("teacherIntro"));
                    course.setCode(rs.getString("code"));
                    course.setStuCount(rs.getLong("stuCount"));
                    course.setProclamation(rs.getString("proclamation"));
                    course.setClusterId(rs.getLong("clusterId"));
                    course.setPeriodName(rs.getString("periodName"));
                    course.setAddTime(rs.getTimestamp("addTime"));
                    course.setCreateId(rs.getLong("createId"));
                    course.setSchoolId(rs.getLong("schoolId"));
                    course.setCateBid(rs.getLong("cateBid"));
                    course.setCateMid(rs.getLong("cateMid"));
                    course.setSignStartTime(rs.getTimestamp("signStartTime"));
                    course.setSignEndTime(rs.getTimestamp("signEndTime"));
                    course.setSignScope(rs.getLong("signScope"));
                    course.setSignClass(rs.getString("signClass"));
                    course.setLecturerName(rs.getString("lecturerName"));
                    course.setOffline(rs.getLong("offline"));
                    course.setMission(rs.getLong("mission"));
                    course.setSignLimit(rs.getLong("signLimit"));
                    course.setLineLock(rs.getLong("lineLock"));
                    course.setAddDate(rs.getDate("addDate"));
                    course.setTplId(rs.getLong("tplId"));
                    return course;
                }
            }
        }
        return null;
    }

    /**
     * 复制课程章节
     */
    private Map<Long, Long> copyCourseChapters(Connection sourceConn, Long sourceCourseId, int sourceSchoolId,
                                               Long newCourseId, Long targetSchoolId, Connection targetConn) throws SQLException {
        Map<Long, Long> chapterMap = new HashMap<>();
        String chapterSql = "SELECT id, name, sort FROM yee_chapter WHERE courseId = ? AND schoolId = ?";

        try (PreparedStatement ps = sourceConn.prepareStatement(chapterSql)) {
            ps.setLong(1, sourceCourseId);
            ps.setInt(2, sourceSchoolId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long sourceChapterId = rs.getLong("id");
                    YeeChapter newChapter = new YeeChapter();
                    newChapter.setName(rs.getString("name"));
                    newChapter.setCourseId(newCourseId.intValue());
                    newChapter.setSort(rs.getInt("sort"));
                    newChapter.setSchoolId(targetSchoolId);

                    Long newChapterId = insertYeeChapterForImport(newChapter, targetConn);
                    chapterMap.put(sourceChapterId, newChapterId);
                }
            }
        }
        return chapterMap;
    }

    /**
     * 复制章节节点
     */
    private Map<Long, Long> copyCourseNodes(Connection sourceConn, Long sourceCourseId, Map<Long, Long> chapterMap,
                                            Long newCourseId, Long targetSchoolId, Connection targetConn) throws SQLException {
        Map<Long, Long> nodeMap = new HashMap<>();
        if (chapterMap.isEmpty()) {
            return nodeMap;
        }

        // lock是MySQL关键字，需加反引号转义
        String nodeSql = "SELECT id, name, chapterId, videoFile, videoDuration, localFile, votingPath, type, " +
                "tabVideo, tabFile, tabVote, tabWork, tabExam, sort, videoMode, `lock`, unlockTime " +
                "FROM yee_node WHERE courseId = ? AND schoolId = ?";

        try (PreparedStatement ps = sourceConn.prepareStatement(nodeSql)) {
            ps.setLong(1, sourceCourseId);
            int sourceSchoolId = getSchoolIdByCourseId(sourceCourseId, sourceConn);
            ps.setInt(2, sourceSchoolId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long sourceNodeId = rs.getLong("id");
                    YeeNode newNode = new YeeNode();
                    newNode.setName(rs.getString("name"));
                    newNode.setCourseId(newCourseId.intValue());
                    newNode.setChapterId(chapterMap.get(rs.getLong("chapterId")).intValue());
                    newNode.setVideoFile(rs.getString("videoFile"));
                    newNode.setVideoDuration(rs.getLong("videoDuration"));
                    newNode.setLocalFile(rs.getString("localFile"));
                    newNode.setVotingPath(rs.getString("votingPath"));
                    newNode.setType(rs.getString("type"));
                    newNode.setTabVideo(rs.getLong("tabVideo"));
                    newNode.setTabFile(rs.getLong("tabFile"));
                    newNode.setTabVote(rs.getLong("tabVote"));
                    newNode.setTabWork(rs.getLong("tabWork"));
                    newNode.setTabExam(rs.getLong("tabExam"));
                    newNode.setSort(rs.getInt("sort"));
                    newNode.setVideoMode(rs.getLong("videoMode"));
                    newNode.setLock(rs.getLong("lock"));
                    newNode.setUnlockTime(rs.getLong("unlockTime"));
                    newNode.setSchoolId(targetSchoolId);

                    Long newNodeId = insertYeeNodeForImport(newNode, targetConn);
                    nodeMap.put(sourceNodeId, newNodeId);
                }
            }
        }
        return nodeMap;
    }

    /**
     * 复制课程节点文件
     */
    private void copyCourseNodeFiles(Connection sourceConn, Long sourceCourseId, Set<Long> sourceNodeIds, Map<Long, Long> nodeMap,
                                     Long newCourseId, Long targetSchoolId, Connection targetConn) throws SQLException {
        if (sourceNodeIds.isEmpty() || nodeMap.isEmpty()) {
            return;
        }

        // 构建IN条件，批量查询节点文件
        String inSql = String.join(",", Collections.nCopies(sourceNodeIds.size(), "?"));
        String fileSql = "SELECT id, nodeId, name, fileName, uploadPath, timeView, createUserId, addTime " +
                "FROM yee_node_files WHERE nodeId IN (" + inSql + ") AND schoolId = ?";

        try (PreparedStatement ps = sourceConn.prepareStatement(fileSql)) {
            // 设置节点ID参数
            int paramIndex = 1;
            for (Long nodeId : sourceNodeIds) {
                ps.setLong(paramIndex++, nodeId);
            }
            // 设置源学校ID
            int sourceSchoolId = getSchoolIdByCourseId(sourceCourseId, sourceConn);
            ps.setInt(paramIndex++, sourceSchoolId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long sourceNodeId = rs.getLong("nodeId");
                    Long newNodeId = nodeMap.get(sourceNodeId);
                    if (newNodeId == null) {
                        continue;
                    }

                    // 构建新节点文件（重置创建时间）
                    YeeNodeFiles newFile = new YeeNodeFiles();
                    newFile.setNodeId(newNodeId.intValue());
                    newFile.setCourseId(newCourseId.intValue());
                    newFile.setSchoolId(targetSchoolId);
                    newFile.setName(rs.getString("name"));
                    newFile.setFileName(rs.getString("fileName"));
                    newFile.setUploadPath(rs.getString("uploadPath"));
                    newFile.setTimeView(rs.getLong("timeView"));
                    newFile.setCreateUserId(rs.getLong("createUserId"));
                    newFile.setAddTime(new Timestamp(System.currentTimeMillis()));

                    // 插入新文件
                    insertYeeNodeFilesForImport(newFile, targetConn);
                }
            }
        }
    }

    /**
     * 将 ResultSet 当前行映射为 CourseTreeVo（供 QueryBuilder.page 单行映射使用）
     */
    private CourseTreeVo mapCourseTreeVoRow(ResultSet rs) {
        try {
            CourseTreeVo vo = new CourseTreeVo();
            vo.setCourseId(rs.getLong("id"));
            vo.setCourseName(rs.getString("name"));
            vo.setMode(rs.getInt("mode"));
            vo.setCollegeId(rs.getInt("collegeId"));
            vo.setCategoryId(rs.getString("categoryId"));
            vo.setLecturers(rs.getString("lecturers"));
            vo.setStartDate(rs.getDate("startDate"));
            vo.setEndDate(rs.getDate("endDate"));
            vo.setCover(rs.getString("cover"));
            vo.setContent(rs.getString("content"));
            BigDecimal credit = rs.getBigDecimal("credit");
            if (credit != null) {
                try {
                    vo.setCredit(credit.setScale(2, RoundingMode.UNNECESSARY));
                } catch (ArithmeticException e) {
                    vo.setCredit(credit.setScale(2, RoundingMode.HALF_UP));
                }
            }
            vo.setAllow(rs.getInt("allow"));
            vo.setIntro(rs.getString("intro"));
            vo.setTeacherIntro(rs.getString("teacherIntro"));
            vo.setCode(rs.getString("code"));
            vo.setStuCount(rs.getInt("stuCount"));
            vo.setProclamation(rs.getString("proclamation"));
            vo.setClusterId(rs.getInt("clusterId"));
            vo.setPeriodName(rs.getString("periodName"));
            vo.setAddTime(rs.getDate("addTime"));
            vo.setCreateId(rs.getInt("createId"));
            vo.setSchoolId(rs.getInt("schoolId"));
            vo.setCateBid(rs.getInt("cateBid"));
            vo.setCateMid(rs.getInt("cateMid"));
            vo.setSignStartTime(rs.getTimestamp("signStartTime"));
            vo.setSignEndTime(rs.getTimestamp("signEndTime"));
            vo.setSignScope(rs.getInt("signScope"));
            vo.setSignClass(rs.getString("signClass"));
            vo.setLecturerName(rs.getString("lecturerName"));
            vo.setOffline(rs.getInt("offline"));
            vo.setMission(rs.getInt("mission"));
            vo.setSignLimit(rs.getInt("signLimit"));
            vo.setLineLock(rs.getInt("lineLock"));
            vo.setTplId(rs.getInt("tplId"));
            vo.setCreateName(rs.getString("createName"));
            vo.setChapterList(null);
            return vo;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 ResultSet 映射为 CourseTreeVo 列表（chapterList 固定为空）
     */
    private List<CourseTreeVo> rsToCourseTreeVo(ResultSet rs) throws SQLException {
        List<CourseTreeVo> list = new ArrayList<>();
        while (rs.next()) {
            CourseTreeVo vo = new CourseTreeVo();
            // 严格映射 CourseTreeVo 所有字段
            vo.setCourseId(rs.getLong("id"));          // 课程ID（对应原表id）
            vo.setCourseName(rs.getString("name"));    // 课程名称（对应原表name）
            vo.setMode(rs.getInt("mode"));             // 课程模式
            vo.setCollegeId(rs.getInt("collegeId"));   // 学院ID
            vo.setCategoryId(rs.getString("categoryId")); // 分类ID
            vo.setLecturers(rs.getString("lecturers"));   // 讲师列表（JSON字符串）
            vo.setStartDate(rs.getDate("startDate"));     // 开始日期
            vo.setEndDate(rs.getDate("endDate"));         // 结束日期
            vo.setCover(rs.getString("cover"));           // 封面
            vo.setContent(rs.getString("content"));       // 内容
            BigDecimal credit = rs.getBigDecimal("credit");
            if (credit != null) {
                try {
                    vo.setCredit(credit.setScale(2, RoundingMode.UNNECESSARY));
                } catch (ArithmeticException e) {
                    vo.setCredit(credit.setScale(2, RoundingMode.HALF_UP));
                }
            } else {
                vo.setCredit(null);
            }
            vo.setAllow(rs.getInt("allow"));              // 状态（是否启用）
            vo.setIntro(rs.getString("intro"));           // 课程介绍
            vo.setTeacherIntro(rs.getString("teacherIntro")); // 教师介绍
            vo.setCode(rs.getString("code"));             // 课程编码
            vo.setStuCount(rs.getInt("stuCount"));        // 学生数量
            vo.setProclamation(rs.getString("proclamation")); // 公告
            vo.setClusterId(rs.getInt("clusterId"));      // 集群ID
            vo.setPeriodName(rs.getString("periodName")); // 学期名称
            vo.setAddTime(rs.getDate("addTime"));         // 添加时间
            vo.setCreateId(rs.getInt("createId"));        // 创建人ID
            vo.setSchoolId(rs.getInt("schoolId"));        // 学校ID
            vo.setCateBid(rs.getInt("cateBid"));          // 大分类ID
            vo.setCateMid(rs.getInt("cateMid"));          // 中分类ID
            vo.setSignStartTime(rs.getDate("signStartTime")); // 报名开始时间
            vo.setSignEndTime(rs.getDate("signEndTime"));     // 报名结束时间
            vo.setSignScope(rs.getInt("signScope"));          // 报名范围
            vo.setSignClass(rs.getString("signClass"));       // 报名班级
            vo.setLecturerName(rs.getString("lecturerName")); // 讲师姓名
            vo.setOffline(rs.getInt("offline"));              // 是否线下
            vo.setMission(rs.getInt("mission"));              // 任务数
            vo.setSignLimit(rs.getInt("signLimit"));          // 报名限制
            vo.setLineLock(rs.getInt("lineLock"));            // 线上锁定
            vo.setTplId(rs.getInt("tplId"));                  // 模板ID
            vo.setCreateName(rs.getString("createName"));     // 创建人名称（关联查询）

            // 关键：chapterList 设为空
            vo.setChapterList(null);

            list.add(vo);
        }
        return list;
    }

}
