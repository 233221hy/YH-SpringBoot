package cn.xfywz.guozespring.service.teacher.impl;

import cn.xfywz.guozespring.entity.dto.CourseSignUpDTO;
import cn.xfywz.guozespring.entity.dto.StuNameAndNum;
import cn.xfywz.guozespring.entity.dto.YeeCourseSignStudentDTO;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.vo.CourseSignUpVO;
import cn.xfywz.guozespring.entity.vo.YeeCourseSignStudentListVO;
import cn.xfywz.guozespring.exception.DatabaseException;
import cn.xfywz.guozespring.service.teacher.YeeCourseSignStudentService;
import cn.xfywz.guozespring.util.PageResult;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.util.db.*;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class YeeCourseSignStudentServiceImpl implements YeeCourseSignStudentService {
    @Autowired
    private SlSchoolMapper slSchoolMapper;
    @Autowired
    private DatabaseUtil databaseUtil;
    @Autowired
    private RedisTemplate redisTemplate;

    // 本地限流：单机每秒最多 2000 个报名请求（防雪崩）
    @SuppressWarnings("UnstableApiUsage")
    private final RateLimiter rateLimiter = RateLimiter.create(2000);


    @Override
    public Result list(YeeCourseSignStudentDTO dto) {
    // 1. 基础 SQL（不含 WHERE 和 ORDER BY）
    String baseSql = """
        SELECT DISTINCT
            scs.id,
            s.id AS studentId,
            s.number AS studentNumber,
            s.name AS studentName,
            s.idCard AS idCard,
            s.gender AS gender,
            co.name AS collegeName,
            cl.name AS className,
            cs.classId AS courseClassId,
            CASE WHEN cs.studentId IS NULL THEN 0 ELSE 1 END AS states,
            scs.signTime
        FROM yee_course_sign_student scs
        LEFT JOIN yee_student s ON s.id = scs.studentId AND s.schoolId = scs.schoolId
        LEFT JOIN yee_college co ON co.id = s.collegeId
        LEFT JOIN yee_classes cl ON cl.id = s.classId
        LEFT JOIN yee_course_student cs ON cs.studentId = s.id AND cs.courseId = scs.courseId
        """;
//    String baseSql = """
//    SELECT
//        scs.id,
//        s.id AS studentId,
//        s.number AS studentNumber,
//        s.name AS studentName,
//        s.idCard AS idCard,
//        s.gender AS gender,
//        co.name AS collegeName,
//        cl.name AS className,
//        cs.classId AS courseClassId,
//        CASE WHEN cs.studentId IS NULL THEN 0 ELSE 1 END AS states,
//        scs.signTime
//    FROM yee_course_sign_student scs
//    LEFT JOIN yee_student s ON s.id = scs.studentId AND s.schoolId = scs.schoolId
//    LEFT JOIN yee_college co ON co.id = s.collegeId
//    LEFT JOIN yee_classes cl ON cl.id = s.classId
//    LEFT JOIN (
//        SELECT DISTINCT studentId, courseId, classId
//        FROM yee_course_student
//    ) cs ON cs.studentId = s.id AND cs.courseId = scs.courseId
//    """;

    // 2. 使用 QueryBuilder 构建动态查询
    PageResult<YeeCourseSignStudentListVO> pageResult = databaseUtil.query(dto.getSchoolId())
            .sql(baseSql)
            .apply(qb -> {
                // 固定条件
                qb.eq("scs.courseId", dto.getCourseId());

                // 关键词搜索（学号或姓名）
                if (StringUtils.hasText(dto.getKeyword())) {
                    String like = "%" + dto.getKeyword().trim() + "%";
                    qb.where("(s.number LIKE ? OR s.name LIKE ?)", like, like);
                }

                // 身份证号
                if (StringUtils.hasText(dto.getIdCard())) {
                    qb.eq("s.idCard", dto.getIdCard().trim());
                }

                // 学院
                if (dto.getCollegeId() != null) {
                    qb.eq("s.collegeId", dto.getCollegeId());
                }

                // 班级
                if (dto.getClassId() != null) {
                    qb.eq("s.classId", dto.getClassId());
                }

                // 性别
                if (StringUtils.hasText(dto.getGender())) {
                    qb.eq("s.gender", dto.getGender().trim());
                }

                // 状态筛选（是否已加入课程）
                if (StringUtils.hasText(dto.getStates())) {
                    if ("1".equals(dto.getStates())) {
                        qb.where("cs.studentId IS NOT NULL");
                    } else if ("0".equals(dto.getStates())) {
                        qb.where("cs.studentId IS NULL");
                    }
                }
            })
            .orderBy("scs.signTime DESC")
            .page(YeeCourseSignStudentListVO::fromResultSet,
                    dto.getPageNum(), dto.getPageSize());

    return Result.success(pageResult.getRows(), pageResult.getTotal());
}


    @Override
    public Result stuList(CourseSignUpDTO dto) {

        if (dto == null || dto.getStudentId() == null || dto.getSchoolId() == null) {
            return Result.error("缺少必要的查询参数（学生ID或学校ID）");
        }
        Integer studentId = dto.getStudentId();
        Integer schoolId = dto.getSchoolId();
        Integer collegeId = dto.getCollegeId();
        String classIdStr = null;
        if (dto.getClassId() != null) {
            classIdStr = String.valueOf(dto.getClassId());
        }
        Integer pageNum = dto.getPageNum();
        Integer pageSize = dto.getPageSize();
        int offset = (pageNum - 1) * pageSize;

        // 分支1: signScope = 0 (全校课) → 3个?
        String globalScopeSql = """
    SELECT
        c.id, c.name AS courseName, c.cover, c.signStartTime, c.signEndTime, c.mode,
        'available' AS signStatus,
        c.startDate, c.endDate, c.credit, c.lecturerName, c.addTime,
        0 AS signId
    FROM yee_course c
    LEFT JOIN yee_course_sign_student scs ON scs.courseId = c.id AND scs.studentId = ?
    LEFT JOIN yee_course_student cs ON cs.courseId = c.id AND cs.studentId = ?
    LEFT JOIN (
        SELECT courseId, COUNT(*) AS signCount
        FROM yee_course_sign_student
        GROUP BY courseId
    ) signStats ON signStats.courseId = c.id
    WHERE c.schoolId = ? AND c.allow = 1 AND c.mode = 2 AND c.signScope = 0
      AND c.signStartTime <= NOW() AND c.signEndTime >= NOW()
      AND (c.signLimit <= 0 OR signStats.signCount IS NULL OR signStats.signCount < c.signLimit)
      AND scs.studentId IS NULL AND cs.studentId IS NULL
    """;

        // 分支2: signScope = 1 (学院课) → 4个?
        String collegeScopeSql = """
    SELECT
        c.id, c.name AS courseName, c.cover, c.signStartTime, c.signEndTime, c.mode,
        'available' AS signStatus,
        c.startDate, c.endDate, c.credit, c.lecturerName, c.addTime,
        0 AS signId
    FROM yee_course c
    LEFT JOIN yee_course_sign_student scs ON scs.courseId = c.id AND scs.studentId = ?
    LEFT JOIN yee_course_student cs ON cs.courseId = c.id AND cs.studentId = ?
    LEFT JOIN (
        SELECT courseId, COUNT(*) AS signCount
        FROM yee_course_sign_student
        GROUP BY courseId
    ) signStats ON signStats.courseId = c.id
    WHERE c.schoolId = ? AND c.allow = 1 AND c.mode = 2 AND c.signScope = 1 AND c.collegeId = ?
      AND c.signStartTime <= NOW() AND c.signEndTime >= NOW()
      AND (c.signLimit <= 0 OR signStats.signCount IS NULL OR signStats.signCount < c.signLimit)
      AND scs.studentId IS NULL AND cs.studentId IS NULL
    """;

        // 分支3: signScope = 2 (班级课) → 4个?
        String classScopeSql = """
    SELECT
        c.id, c.name AS courseName, c.cover, c.signStartTime, c.signEndTime, c.mode,
        'available' AS signStatus,
        c.startDate, c.endDate, c.credit, c.lecturerName, c.addTime,
        0 AS signId
    FROM yee_course c
    LEFT JOIN yee_course_sign_student scs ON scs.courseId = c.id AND scs.studentId = ?
    LEFT JOIN yee_course_student cs ON cs.courseId = c.id AND cs.studentId = ?
    LEFT JOIN (
        SELECT courseId, COUNT(*) AS signCount
        FROM yee_course_sign_student
        GROUP BY courseId
    ) signStats ON signStats.courseId = c.id
    WHERE c.schoolId = ? AND c.allow = 1 AND c.mode = 2 AND c.signScope = 2
      AND JSON_CONTAINS(c.signClass, JSON_QUOTE(?))
      AND c.signStartTime <= NOW() AND c.signEndTime >= NOW()
      AND (c.signLimit <= 0 OR signStats.signCount IS NULL OR signStats.signCount < c.signLimit)
      AND scs.studentId IS NULL AND cs.studentId IS NULL
    """;

        // 4. Pending (报名审核中) → 3个?
        String pendingSql = """
    SELECT
        c.id, c.name AS courseName, c.cover, c.signStartTime, c.signEndTime, c.mode,
        'pending' AS signStatus,
        c.startDate, c.endDate, c.credit, c.lecturerName, c.addTime,
        scs.id AS signId
    FROM yee_course c
    INNER JOIN yee_course_sign_student scs ON scs.courseId = c.id AND scs.studentId = ?
    LEFT JOIN yee_course_student cs ON cs.courseId = c.id AND cs.studentId = ?
    WHERE c.schoolId = ? AND c.allow = 1 AND c.mode = 2
      AND cs.studentId IS NULL
    """;

        // 5. Joined (已加入) → 2个?
        String joinedSql = """
    SELECT
        c.id, c.name AS courseName, c.cover, c.signStartTime, c.signEndTime, c.mode,
        'joined' AS signStatus,
        c.startDate, c.endDate, c.credit, c.lecturerName, c.addTime,
        0 AS signId
    FROM yee_course c
    INNER JOIN yee_course_student cs ON cs.courseId = c.id AND cs.studentId = ?
    WHERE c.schoolId = ? AND c.allow = 1 AND c.mode = 2
    """;

        // 拼接 UNION ALL
        String unionSql = String.format("(%s) UNION ALL (%s) UNION ALL (%s) UNION ALL (%s) UNION ALL (%s)",
                globalScopeSql, collegeScopeSql, classScopeSql, pendingSql, joinedSql);
        String dataSql = "SELECT * FROM (" + unionSql + ") t ORDER BY t.addTime DESC LIMIT ? OFFSET ?";
        String countSql = "SELECT COUNT(*) FROM (" + unionSql + ") t";

        // ====================== 核心修复：参数严格匹配 16 个 ======================
        List<Object> params = new ArrayList<>();
        // 1. globalScopeSql (3个)
        params.add(studentId);
        params.add(studentId);
        params.add(schoolId);
        // 2. collegeScopeSql (4个)
        params.add(studentId);
        params.add(studentId);
        params.add(schoolId);
        params.add(collegeId);
        // 3. classScopeSql (4个)
        params.add(studentId);
        params.add(studentId);
        params.add(schoolId);
        params.add(classIdStr);
        // 4. pendingSql (3个)
        params.add(studentId);
        params.add(studentId);
        params.add(schoolId);
        // 5. joinedSql (2个)
        params.add(studentId);
        params.add(schoolId);
        // ========================================================================

        // 分页参数（额外 2 个，总 18 个）
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        // 执行查询
        try {
            PageResult<CourseSignUpVO> pageResult = databaseUtil.queryPage(
                    schoolId,
                    BuiltSql.of(dataSql, dataParams),
                    BuiltSql.of(countSql, params),
                    CourseSignUpVO::fromResultSet
            );
            return Result.success(pageResult.getRows(), pageResult.getTotal());
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }


    @Override
    public Result add(Integer schoolId, Integer courseId, Integer studentId) {
        // 1. 限流
        if (!rateLimiter.tryAcquire()) {
            return Result.error("系统繁忙，请稍后再试");
        }

        // 3. 基础校验
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);
        if (slSchool == null || slSchool.getAllow() == 0) {
            return Result.error("学校不存在或未审核");
        }

        // 4. 数据库事务操作
        try (Connection conn = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
            conn.setAutoCommit(false);

            // 5. 锁定课程并校验
            String checkCourseSql = """
            SELECT id, signLimit, allow, mode, signStartTime, signEndTime
            FROM yee_course
            WHERE id = ? AND schoolId = ?
            FOR UPDATE
        """;

            try (PreparedStatement pst = conn.prepareStatement(checkCourseSql)) {
                pst.setInt(1, courseId);
                pst.setInt(2, schoolId);

                try (ResultSet rs = pst.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return Result.error("课程不存在");
                    }

                    int allow = rs.getInt("allow");
                    int mode = rs.getInt("mode");
                    Timestamp start = rs.getTimestamp("signStartTime");
                    Timestamp end = rs.getTimestamp("signEndTime");
                    int signLimit = rs.getInt("signLimit");

                    if (allow != 1 || mode != 2) {
                        conn.rollback();
                        return Result.error("课程不可报名");
                    }

                    long now = System.currentTimeMillis();
                    if (start == null || end == null || start.getTime() > now || end.getTime() < now) {
                        conn.rollback();
                        return Result.error("不在报名时间");
                    }

                    // 5. 名额校验（原子操作，不超卖）
                    if (signLimit > 0) {
                        String countSql = """
                        SELECT COUNT(*) FROM yee_course_sign_student
                        WHERE courseId = ? AND schoolId = ?
                    """;

                        try (PreparedStatement cntStmt = conn.prepareStatement(countSql)) {
                            cntStmt.setInt(1, courseId);
                            cntStmt.setInt(2, schoolId);

                            try (ResultSet cntRs = cntStmt.executeQuery()) {
                                if (cntRs.next()) {
                                    int current = cntRs.getInt(1);
                                    if (current >= signLimit) {
                                        conn.rollback();
                                        return Result.error("课程名额已满");
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. 插入报名记录（唯一索引防重）
            String insertSql = "INSERT INTO yee_course_sign_student (studentId, courseId, schoolId, signTime) VALUES (?,?,?,?)";
            try (PreparedStatement ist = conn.prepareStatement(insertSql)) {
                ist.setInt(1, studentId);
                ist.setInt(2, courseId);
                ist.setInt(3, schoolId);
                ist.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
                ist.executeUpdate();
            } catch (SQLIntegrityConstraintViolationException e) {
                // 捕获重复报名异常
                conn.rollback();
                return Result.error("您已报名该课程");
            }

            // 7. 提交事务
            conn.commit();
            return Result.success("报名成功");

        } catch (SQLIntegrityConstraintViolationException e) {
            // 捕获外层插入的重复异常
            return Result.error("您已报名该课程");
        } catch (Exception e) {
            return Result.error("系统繁忙");
        } finally {
//            // 4. 释放锁（可选，设置较短的过期时间后也可不主动删除）
//            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public Result delete(Integer schoolId, Integer id) {
        int rows = databaseUtil.update(schoolId)
                .table("yee_course_sign_student")
                .where("id = ? AND schoolId = ?", id, schoolId)
                .delete();
        return rows > 0 ? Result.success("删除成功") : Result.error("删除失败");
    }


    /**
     * 1 校验学校是否存在且已审核；
     * 2 查询课程下的视频、作业、考试节点数量；
     * 3 筛选出未加入课程的学生ID；
     * 4 批量插入学生到课程学生表，并初始化学习记录；
     * 5 更新课程学生总数；
     * 6 插入学生的成绩初始记录；
     */
    @Override
    public Result join(Integer schoolId, long courseId, long classId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Result.error("学生列表不能为空");
        }

        Integer insertedCount = databaseUtil.executeInTransaction(schoolId, conn -> {
            // 1. 查询课程下节点数量
            NodeCounts counts = queryNodeCounts(conn, courseId);

            // 2. 查询已存在的学生ID
            Set<Long> existingIds = queryExistingStudentIds(conn, courseId, studentIds);

            // 3. 筛选出未加入的学生
            List<Long> newStudentIds = studentIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .collect(Collectors.toList());

            if (newStudentIds.isEmpty()) {
                return 0;
            }

            // 4. 批量插入 yee_course_student
            int inserted = batchInsertCourseStudent(conn, schoolId, courseId, classId, newStudentIds, counts);

            // 5. 更新课程人数
            updateCourseStuCount(conn, courseId, inserted);

            // 6. 查询新增学生的姓名学号
            Map<Long, StuNameAndNum> studentInfoMap = queryStudentInfoMap(conn, newStudentIds);

            // 7. 批量插入 yee_course_results
            batchInsertCourseResults(conn, schoolId, courseId, classId, newStudentIds, studentInfoMap);

            return inserted;
        });

        return Result.success("本次加入人数 " + insertedCount);
    }

    // 辅助内部类，用于封装节点数量
    private record NodeCounts(int videoCount, int workCount, int examCount) {}

    // 查询节点数量
    private NodeCounts queryNodeCounts(Connection conn, long courseId) {
        String sql = """
        SELECT
            COALESCE(SUM(tabVideo), 0) AS videoCount,
            COALESCE(SUM(tabWork), 0) AS workCount,
            COALESCE(SUM(tabExam), 0) AS examCount
        FROM yee_node
        WHERE courseId = ?
        """;
        return databaseUtil.executeQuery(conn, BuiltSql.of(sql, courseId), rs -> {
            if (rs.next()) {
                return new NodeCounts(
                        rs.getInt("videoCount"),
                        rs.getInt("workCount"),
                        rs.getInt("examCount")
                );
            }
            return new NodeCounts(0, 0, 0);
        });
    }

    // 查询已存在于该课程（任意班级）的学生ID
    private Set<Long> queryExistingStudentIds(Connection conn, long courseId, List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Collections.emptySet();
        }
        String placeholders = studentIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT studentId FROM yee_course_student WHERE courseId = ? AND studentId IN (" + placeholders + ")";

        List<Object> params = new ArrayList<>();
        params.add(courseId);
        params.addAll(studentIds);

        return databaseUtil.executeQuery(conn, BuiltSql.of(sql, params), rs -> {
            Set<Long> set = new HashSet<>();
            while (rs.next()) {
                set.add(rs.getLong("studentId"));
            }
            return set;
        });
    }

    // 批量插入 yee_course_student
    private int batchInsertCourseStudent(Connection conn, int schoolId, long courseId, long classId,
                                         List<Long> studentIds, NodeCounts counts) {
        String sql = """
        INSERT INTO yee_course_student (
            schoolId, classId, courseId, studentId,
            videoLearned, videoCount, lastNodeId,
            workLearned, workCount,
            examLearned, examCount,
            discussJoin, discussCount,
            studyTime, `change`, calculate, addTime
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        Timestamp now = new Timestamp(System.currentTimeMillis());
        BatchBuilder batchBuilder = new BatchBuilder(sql);

        for (Long studentId : studentIds) {
            batchBuilder.addParams(
                    schoolId, classId, courseId, studentId,
                    0, counts.videoCount(), 0,
                    0, counts.workCount(),
                    0, counts.examCount(),
                    0, 0,
                    0, 0, 0, now
            );
        }

        return databaseUtil.executeBatch(conn, batchBuilder);
    }

    // 更新课程人数
    private void updateCourseStuCount(Connection conn, long courseId, int increment) {
        String sql = "UPDATE yee_course SET stuCount = stuCount + ? WHERE id = ?";
        int updated = databaseUtil.executeUpdate(conn, BuiltSql.of(sql, increment, courseId));
        if (updated <= 0) {
            throw new DatabaseException("更新课程人数失败，courseId=" + courseId);
        }
    }

    // 查询学生姓名学号
    private Map<Long, StuNameAndNum> queryStudentInfoMap(Connection conn, List<Long> studentIds) {
        if (studentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = studentIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String sql = "SELECT id, name, number FROM yee_student WHERE id IN (" + placeholders + ")";

        return databaseUtil.executeQuery(conn, BuiltSql.of(sql, studentIds.toArray()), rs -> {
            Map<Long, StuNameAndNum> map = new HashMap<>();
            while (rs.next()) {
                long id = rs.getLong("id");
                String name = rs.getString("name");
                String number = rs.getString("number");
                map.put(id, new StuNameAndNum(name, number));
            }
            return map;
        });
    }

    // 批量插入 yee_course_results
    private void batchInsertCourseResults(Connection conn, int schoolId, long courseId, long classId,
                                          List<Long> studentIds, Map<Long, StuNameAndNum> infoMap) {
        String sql = """
        INSERT INTO yee_course_results (
            schoolId, courseId, userId, classId,
            score, videoScore, examScore, workScore, discussScore, extraScore,
            ranking, stuName, stuNumber, videoResult, examResult, workResult, discussResult,
            calcDate
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        BigDecimal ZERO = BigDecimal.ZERO;
        BatchBuilder batchBuilder = new BatchBuilder(sql);

        for (Long studentId : studentIds) {
            StuNameAndNum info = infoMap.getOrDefault(studentId, new StuNameAndNum("", ""));
            batchBuilder.addParams(
                    schoolId, courseId, studentId, classId,
                    ZERO, ZERO, ZERO, ZERO, ZERO, ZERO,
                    1, info.getName(), info.getNumber(),
                    ZERO, ZERO, ZERO, ZERO,
                    null
            );
        }

        databaseUtil.executeBatch(conn, batchBuilder);
    }

    @Override
    public Result exit(Integer schoolId, long courseId, long classId, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return Result.error("学生列表不能为空");
        }

        // 在事务中执行删除和更新操作
        Integer deleted = databaseUtil.executeInTransaction(schoolId, conn -> {
            // 1. 使用 DmlBuilder 构建删除操作
            int deletedRows = databaseUtil.update(schoolId)
                    .table("yee_course_student")
                    .where("courseId = ? AND classId = ?", courseId, classId)  // 固定条件
                    .in("studentId", studentIds)                              // 动态 IN 条件
                    .delete(conn);                                            // 传入事务连接执行

            // 2. 如果删除了记录，更新课程人数
            if (deletedRows > 0) {
                databaseUtil.update(schoolId)
                        .table("yee_course")
                        .setRaw("stuCount = CASE WHEN stuCount >= ? THEN stuCount - ? ELSE 0 END", deletedRows, deletedRows)
                        .where("id = ?", courseId)
                        .update(conn);
            }
            return deletedRows;
        });

        return Result.success("已退出班级人数 " + deleted);
    }


}
