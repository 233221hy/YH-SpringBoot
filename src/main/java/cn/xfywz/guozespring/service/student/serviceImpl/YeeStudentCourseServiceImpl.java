package cn.xfywz.guozespring.service.student.serviceImpl;


import cn.xfywz.guozespring.entity.mhsch.*;
import cn.xfywz.guozespring.entity.vo.CourseRowMapper;
import cn.xfywz.guozespring.entity.vo.CourseSignUpVO;
import cn.xfywz.guozespring.entity.vo.StudentStats;
import cn.xfywz.guozespring.service.student.YeeStudentCourseService;
import cn.xfywz.guozespring.service.student.YeeStudentMangerService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.BuiltSql;
import cn.xfywz.guozespring.util.db.DatabaseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

@Service
public class YeeStudentCourseServiceImpl implements YeeStudentCourseService {

    @Autowired
    private YeeStudentMangerService yeeStudentMangerService;
    @Autowired
    private DatabaseUtil databaseUtil;

    @Override
    public Result selectList(int schoolId, int studentId, int type, int pageSize, int pageNum) throws Exception {
        StudentStats studentStats = yeeStudentMangerService.getStudentStats(schoolId, studentId);
        if (pageNum <= 0) pageNum = 1;
        int offset = (pageNum - 1) * pageSize;

        String dataSql, countSql;

        if (type == 2) {
            dataSql = """
            SELECT DISTINCT
                c.id,c.name courseName,c.cover,c.mode,c.startDate,c.endDate,
                c.signStartTime,c.signEndTime,c.credit,c.lecturerName,c.stuCount,
                MAX(css.id) signId, 'pending' signStatus
            FROM yee_course c
            INNER JOIN yee_course_sign_student css ON c.id=css.courseId AND css.studentId=?
            LEFT JOIN yee_course_student cs ON c.id=cs.courseId AND cs.studentId=?
            WHERE c.allow=1
            AND cs.id IS NULL
            GROUP BY c.id
            ORDER BY c.id DESC
            LIMIT ? OFFSET ?
        """;
            countSql = """
            SELECT COUNT(DISTINCT c.id)
            FROM yee_course c
            INNER JOIN yee_course_sign_student css ON c.id=css.courseId AND css.studentId=?
            LEFT JOIN yee_course_student cs ON c.id=cs.courseId AND cs.studentId=?
            WHERE c.allow=1
            AND cs.id IS NULL
        """;
        } else {
            String timeFilter;
            if (type == 1) {
                timeFilter = " AND c.endDate < NOW() ";
            } else {
                timeFilter = " AND c.startDate <= NOW() ";
            }

            dataSql = """
            SELECT DISTINCT
                c.id,c.name courseName,c.cover,c.mode,c.startDate,c.endDate,
                c.signStartTime,c.signEndTime,c.credit,c.lecturerName,c.stuCount,
                MAX(cs.id) joinId, MAX(css.id) signId,
                CASE WHEN MAX(cs.id)>0 THEN 'joined' WHEN MAX(css.id)>0 THEN 'pending' ELSE 'none' END signStatus
            FROM yee_course c
            LEFT JOIN yee_course_student cs ON c.id=cs.courseId AND cs.studentId=?
            LEFT JOIN yee_course_sign_student css ON c.id=css.courseId AND css.studentId=?
            LEFT JOIN yee_course_class cc ON cs.classId = cc.id
            WHERE c.allow=1
            AND (cs.id IS NOT NULL OR css.id IS NOT NULL)
            AND (cs.id IS NULL OR cc.allow=1)
        """ + timeFilter + """
            GROUP BY c.id
            ORDER BY c.id DESC
            LIMIT ? OFFSET ?
        """;
            countSql = """
            SELECT COUNT(DISTINCT c.id)
            FROM yee_course c
            LEFT JOIN yee_course_student cs ON c.id=cs.courseId AND cs.studentId=?
            LEFT JOIN yee_course_sign_student css ON c.id=css.courseId AND css.studentId=?
            LEFT JOIN yee_course_class cc ON cs.classId = cc.id
            WHERE c.allow=1
            AND (cs.id IS NOT NULL OR css.id IS NOT NULL)
            AND (cs.id IS NULL OR cc.allow=1)
        """ + timeFilter;
        }

        // 数据查询
        BuiltSql builtDataSql = BuiltSql.of(dataSql, Arrays.asList(studentId, studentId, pageSize, offset));
        List<CourseSignUpVO> finalList = databaseUtil.executeQuery(schoolId, builtDataSql, rs -> {
            try {
                return CourseSignUpVO.mapCourseSignUpVO(rs);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        // 计数查询
        BuiltSql builtCountSql = BuiltSql.of(countSql, Arrays.asList(studentId, studentId));
        long total = databaseUtil.executeQuery(schoolId, builtCountSql, rs -> {
            try {
                return rs.next() ? rs.getLong(1) : 0L;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        return Result.success(finalList, total).extra("studentStats", studentStats);
    }


    /**
     * 课程详情
     */
    @Override
    public Result selectById(int schoolId, int courseId, int studentId) throws Exception {
        // 1. 查询课程信息
        YeeCourse yeeCourse = databaseUtil.query(schoolId)
                .sql("SELECT * FROM yee_course WHERE id = ?")
                .param(courseId)
                .single(CourseRowMapper::fromRow)
                .orElse(null);
        if (yeeCourse == null) {
            return Result.error("没有此课程");
        }

        // 2. 查询班级 → 教师
        Map<String, Object> teacherMap = new HashMap<>();
        Integer classId = databaseUtil.query(schoolId)
                .sql("SELECT classId FROM yee_course_student WHERE courseId = ? AND studentId = ?")
                .params(courseId, studentId)
                .scalar(rs -> rs.getInt("classId"))
                .orElse(null);

        if (classId != null && classId > 0) {
            Integer teacherId = databaseUtil.query(schoolId)
                    .sql("SELECT teacherId FROM yee_course_class WHERE id = ?")
                    .param(classId)
                    .scalar(rs -> rs.getInt("teacherId"))
                    .orElse(null);

            if (teacherId != null && teacherId > 0) {
                teacherMap = databaseUtil.query(schoolId)
                        .sql("SELECT id, name FROM yee_manage WHERE id = ?")
                        .param(teacherId)
                        .single(rs -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("id", rs.getInt("id"));
                            map.put("name", rs.getString("name"));
                            return map;
                        })
                        .orElse(new HashMap<>());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("course", yeeCourse);
        data.put("teacher", teacherMap);
        return Result.success(data);
    }

}
