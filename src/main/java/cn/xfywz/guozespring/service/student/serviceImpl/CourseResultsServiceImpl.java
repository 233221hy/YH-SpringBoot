package cn.xfywz.guozespring.service.student.serviceImpl;

import cn.xfywz.guozespring.entity.dto.CourseResultsQueryDTO;
import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.student.CourseResultsService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseResultsServiceImpl implements CourseResultsService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result courseResults(CourseResultsQueryDTO queryDTO) {
        try {
            SlSchool slSchool = slSchoolMapper.selectById(queryDTO.getSchoolId());
            if (slSchool == null || slSchool.getAllow() == 0) {
                return Result.error("学校不存在或未审核");
            }
            try (Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool)) {
                long announce = 0;
                String announceSql = """
                SELECT announce
                FROM yee_course_score_rules
                WHERE courseId = ? AND classId = ?
                ORDER BY updateTime DESC
                LIMIT 1
                """;
                try (PreparedStatement pst = connection.prepareStatement(announceSql)) {
                    pst.setLong(1, queryDTO.getCourseId());
                    pst.setLong(2, queryDTO.getClassId());
                    try (ResultSet rs = pst.executeQuery()) {
                        if (rs.next()) announce = rs.getLong(1);
                    }
                }
                if (announce == 1) {
                    // 查询学生个人成绩
                    String dataSql = """
                    SELECT r.userId AS studentId, r.stuName AS studentName, r.stuNumber AS studentNumber,
                           r.classId, cc.name AS className, r.courseId, r.score
                    FROM yee_course_results r
                    LEFT JOIN yee_course_class cc ON cc.id = r.classId
                    WHERE r.courseId = ? AND r.classId = ? AND r.userId = ?
                    """;
                    List<Object> params = new ArrayList<>();
                    params.add(queryDTO.getCourseId());
                    params.add(queryDTO.getClassId());
                    params.add(queryDTO.getStudentId());

                    try (PreparedStatement dst = connection.prepareStatement(dataSql)) {
                        for (int i = 0; i < params.size(); i++) {
                            dst.setObject(i + 1, params.get(i));
                        }
                        try (ResultSet drs = dst.executeQuery()) {
                            if (drs.next()) {
                                Map<String, Object> row = new HashMap<>();
                                row.put("studentId", drs.getLong("studentId"));
                                row.put("studentName", drs.getString("studentName"));
                                row.put("studentNumber", drs.getString("studentNumber"));
                                row.put("classId", drs.getLong("classId"));
                                row.put("className", drs.getString("className"));
                                row.put("courseId", drs.getLong("courseId"));
                                row.put("score", drs.getDouble("score"));
                                row.put("announce", 1);
                                return Result.success(row);
                            } else {
                                return Result.error("未找到学生成绩信息");
                            }
                        }
                    }
                } else {
                    // 查询学生个人学习进度
                    String dataSql = """
                    SELECT
                        ycs.studentId,
                        ycs.classId,
                        ycs.courseId,
                        ycs.videoLearned,
                        ycs.videoCount,
                        ycs.workLearned,
                        ycs.workCount,
                        ycs.examLearned,
                        ycs.examCount,
                        ycs.discussJoin,
                        ycs.discussCount,
                        ycs.studyTime,
                        ys.name AS studentName,
                        ys.number AS studentNumber,
                        ycc.name AS className
                    FROM yee_course_student ycs
                    LEFT JOIN yee_student ys ON ys.id = ycs.studentId AND ys.schoolId = ycs.schoolId
                    LEFT JOIN yee_course_class ycc ON ycc.id = ycs.classId AND ycc.schoolId = ycs.schoolId
                    WHERE ycs.schoolId = ? AND ycs.courseId = ? AND ycs.classId = ? AND ycs.studentId = ?
                    """;
                    List<Object> params = new ArrayList<>();
                    params.add(queryDTO.getSchoolId());
                    params.add(queryDTO.getCourseId());
                    params.add(queryDTO.getClassId());
                    params.add(queryDTO.getStudentId());

                    try (PreparedStatement dst = connection.prepareStatement(dataSql)) {
                        for (int i = 0; i < params.size(); i++) {
                            dst.setObject(i + 1, params.get(i));
                        }
                        try (ResultSet drs = dst.executeQuery()) {
                            if (drs.next()) {
                                Map<String, Object> row = new HashMap<>();
                                row.put("studentId", drs.getLong("studentId"));
                                row.put("classId", drs.getLong("classId"));
                                row.put("courseId", drs.getLong("courseId"));
                                row.put("studentName", drs.getString("studentName"));
                                row.put("studentNumber", drs.getString("studentNumber"));
                                row.put("className", drs.getString("className"));
                                row.put("videoProgress", drs.getLong("videoLearned") + "/" + drs.getLong("videoCount"));
                                row.put("workProgress", drs.getLong("workLearned") + "/" + drs.getLong("workCount"));
                                row.put("examProgress", drs.getLong("examLearned") + "/" + drs.getLong("examCount"));
                                row.put("discussProgress", drs.getLong("discussJoin") + "/" + drs.getLong("discussCount"));
                                row.put("studyTime", drs.getLong("studyTime"));
                                row.put("announce", 0);
                                return Result.success(row);
                            } else {
                                return Result.error("未找到学生学习进度信息");
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }
}
