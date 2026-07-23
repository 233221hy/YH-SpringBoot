package cn.xfywz.guozespring.service.admin.serviceImpl;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.entity.vo.StatisticsExportVo;
import cn.xfywz.guozespring.entity.dto.StatisticsQueryParam;
import cn.xfywz.guozespring.entity.vo.StatisticsResultMoreVo;
import cn.xfywz.guozespring.entity.vo.StatisticsResultVo;
import cn.xfywz.guozespring.excel.ResponseExportUtil;
import cn.xfywz.guozespring.mapper.SlSchoolMapper;
import cn.xfywz.guozespring.service.admin.StatisticsService;
import cn.xfywz.guozespring.util.Result;
import cn.xfywz.guozespring.util.db.SlaveMysqlConnectionUtil;
import com.alibaba.excel.EasyExcel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    @Autowired
    private SlSchoolMapper slSchoolMapper;

    @Override
    public Result getStatistics(StatisticsQueryParam param) throws Exception {
        // 获取统计信息
        StatisticsResultVo result = new StatisticsResultVo();

        int schoolId = param.getSchoolId();
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);

        // 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        try {
            // 构建日期条件
            String dateCondition = "";
            if (!StringUtils.isEmpty(param.getStartDate()) && !StringUtils.isEmpty(param.getEndDate())) {
                dateCondition = " AND addTime BETWEEN ? AND ? ";
            } else if (!StringUtils.isEmpty(param.getStartDate())) {
                dateCondition = " AND addTime >= ? ";
            } else if (!StringUtils.isEmpty(param.getEndDate())) {
                dateCondition = " AND addTime <= ? ";
            }

            result.setSchoolName(slSchool.getName());

            // 1. 学生人数
            result.setStudentCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_student WHERE 1=1 " + dateCondition, param));

            // 2. 老师人数
            result.setTeacherCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_manage WHERE 1=1 " + dateCondition, param));

            // 3. 行政班级数
            result.setClassCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_classes WHERE 1=1 " + dateCondition, param));

            // 4. 建课数
            result.setCourseCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_course WHERE 1=1 " + dateCondition, param));

            // 5. 选课人次
            result.setCourseSelectionCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_course_student WHERE 1=1 " + dateCondition, param));

            // 6. 开课数(状态正常)
            result.setActiveCourseCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_course WHERE allow = 1 " + dateCondition, param));

            // 7. 选课人数 (去重)
            String courseStudentQuery = "SELECT COUNT(DISTINCT studentId) FROM yee_course_student WHERE 1=1 " + dateCondition;
            result.setCourseStudentCount(executeCountQuery(connection, courseStudentQuery, param));

            // 8. 必修课数
            result.setRequiredCourseCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_course WHERE mode = 3 " + dateCondition, param));

            // 9. 选修课数
            result.setElectiveCourseCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_course WHERE mode = 2 " + dateCondition, param));

            // 10. 教学班级数
            result.setTeachingClassCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_course_class WHERE 1=1 " + dateCondition, param));

            // 11. 主题讨论数
            result.setTopicDiscussionCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_discuss WHERE 1=1 " + dateCondition, param));

            // 12. 评论回复数
            result.setCommentReplyCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_discuss_reply WHERE 1=1 " + dateCondition, param));

            // 13. 视频讨论数
            result.setVideoDiscussionCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_node_discuss WHERE 1=1 " + dateCondition, param));

            // 14. 试卷数
            result.setPaperCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_paper WHERE 1=1 " + dateCondition, param));

            // 15. 试题数
            result.setQuestionCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_question WHERE 1=1 " + dateCondition, param));

            // 16. 作业数
            result.setWorkCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_work WHERE 1=1 " + dateCondition, param));

            // 17. 考试数
            result.setExamCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_exam WHERE 1=1 " + dateCondition, param));

            return Result.success(result);
        } finally {
            if (connection != null && !connection.isClosed()) {
                try {
                    connection.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public void exportStatistics(StatisticsQueryParam param, HttpServletResponse response) throws Exception {
        // 先获取统计数据
        Result result = getStatistics(param);
        if (!(result.getCode() == 200)) {
            throw new Exception("获取统计数据失败: " + result.getMsg());
        }
        
        StatisticsResultVo statisticsResult = (StatisticsResultVo) result.getData();
        
        // 创建导出数据对象
        List<StatisticsExportVo> exportData = new ArrayList<>();
        exportData.add(convertToExportVo(statisticsResult));
        
        // 使用带时间戳的文件名格式
        LocalDateTime now = LocalDateTime.now();
        String fileName = "统计数据导出_" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        ResponseExportUtil.setExcelRespProp(response, fileName);
        
        try {
            EasyExcel.write(response.getOutputStream(), StatisticsExportVo.class)
                    .autoCloseStream(false)
                    .sheet("统计数据")
                    .doWrite(exportData);
            response.flushBuffer();
        } catch (Exception e) {
            throw new Exception("导出Excel失败: " + e.getMessage());
        }
    }

    @Override
    public Result getStatisticsMore(StatisticsQueryParam param) throws Exception {
        // 获取统计信息
        StatisticsResultMoreVo result = new StatisticsResultMoreVo();

        int schoolId = param.getSchoolId();
        SlSchool slSchool = slSchoolMapper.selectById(schoolId);

        // 获取数据库连接
        Connection connection = SlaveMysqlConnectionUtil.getConnection(slSchool);
        if (connection == null) {
            return Result.error("无法获取数据库连接");
        }

        try {
            // 构建日期条件
            String dateCondition = "";
            if (param.getStartDate() != null && param.getEndDate() != null) {
                dateCondition = " AND addDate BETWEEN ? AND ? ";
            } else if (param.getStartDate() != null) {
                dateCondition = " AND addDate >= ? ";
            } else if (param.getEndDate() != null) {
                dateCondition = " AND addDate <= ? ";
            }

            result.setSchoolName(slSchool.getName());

            // ==========  橘色部分 ==========
            // 1. 学生人数
            result.setStudentCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_student WHERE 1=1 " + dateCondition, param));

            // 2. 教师人数
            result.setTeacherCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_manage WHERE 1=1 " + dateCondition, param));

            // 3. 行政班级数
            result.setClassCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_classes WHERE 1=1 " + dateCondition, param));

            // 4. 建课数
            result.setCourseCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_course WHERE 1=1 " + dateCondition, param));

            // 5. 选课人次
            result.setCourseSelectionCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_course_student WHERE 1=1 " + dateCondition, param));

            // 6. 试卷数
            result.setPaperCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_paper WHERE 1=1 " + dateCondition, param));

            // 7. 试题数
            result.setQuestionCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_question WHERE 1=1 " + dateCondition, param));

            // 8. 作业数
            result.setWorkCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_work WHERE 1=1 " + dateCondition, param));

            // 9. 作业答题数
            result.setWorkCountRecord(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_work_record WHERE 1=1 " + dateCondition, param));

            // 10. 考试数
            result.setExamCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_exam WHERE 1=1 " + dateCondition, param));

            // 11. 考试答题数
            result.setExamCountRecord(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_exam_record WHERE 1=1 " + dateCondition, param));

            // 12. 主题讨论数
            result.setTopicDiscussionCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_discuss WHERE 1=1 " + dateCondition, param));

            // 13. 视频回帖数
            result.setVideoDiscussionCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_node_discuss WHERE 1=1 " + dateCondition, param));

            // 14. 乐学圈
            result.setHappyCircleCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_happy_circle WHERE 1=1 " + dateCondition, param));


            // ==========  灰色部分 ==========
            // 1. 建课数 已审核
            result.setActiveCourseCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_course WHERE 1=1 " + dateCondition, param));

            // 2. 建课数 未审核
            result.setNotActiveCourseCount(result.getCourseCount() - result.getActiveCourseCount());

            // 3. 必修课数
            result.setRequiredCourseCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_course WHERE mode = 3 " + dateCondition, param));

            // 4. 选修课数
            result.setElectiveCourseCount(result.getCourseSelectionCount() - result.getRequiredCourseCount());

            // 5. 试卷份数
            result.setAllowPaperCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_paper WHERE allow = 1 " + dateCondition, param));

            // 6. 试卷份数 不允许
            result.setNotAllowPaperCount(result.getPaperCount() - result.getAllowPaperCount());

            // 7. 作业份数
            result.setAllowWorkCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_work WHERE allow = 1 " + dateCondition, param));

            // 8. 作业份数 不允许
            result.setNotAllowWorkCount(result.getWorkCount() - result.getAllowWorkCount());

            // 9. 考试份数
            result.setAllowExamCount(executeCountQuery(connection, "SELECT COUNT(*) FROM yee_exam WHERE allow = 1 " + dateCondition, param));

            // 10. 考试份数 不允许
            result.setNotAllowExamCount(result.getExamCount() - result.getAllowExamCount());


            return Result.success(result);
        } finally {
            if (connection != null && !connection.isClosed()) {
                try {
                    connection.close();
                } catch (SQLException ignored) {}
            }
        }
    }


    private StatisticsExportVo convertToExportVo(StatisticsResultVo result) {
        StatisticsExportVo exportVo = new StatisticsExportVo();
        exportVo.setSchoolName(result.getSchoolName());
        exportVo.setStudentCount(result.getStudentCount());
        exportVo.setTeacherCount(result.getTeacherCount());
        exportVo.setClassCount(result.getClassCount());
        exportVo.setCourseCount(result.getCourseCount());
        exportVo.setCourseSelectionCount(result.getCourseSelectionCount());
        exportVo.setActiveCourseCount(result.getActiveCourseCount());
        exportVo.setCourseStudentCount(result.getCourseStudentCount());
        exportVo.setRequiredCourseCount(result.getRequiredCourseCount());
        exportVo.setElectiveCourseCount(result.getElectiveCourseCount());
        exportVo.setTeachingClassCount(result.getTeachingClassCount());
        exportVo.setTopicDiscussionCount(result.getTopicDiscussionCount());
        exportVo.setCommentReplyCount(result.getCommentReplyCount());
        exportVo.setVideoDiscussionCount(result.getVideoDiscussionCount());
        exportVo.setPaperCount(result.getPaperCount());
        exportVo.setQuestionCount(result.getQuestionCount());
        exportVo.setWorkCount(result.getWorkCount());
        exportVo.setExamCount(result.getExamCount());
        return exportVo;
    }

    private Long executeCountQuery(Connection connection, String sql, StatisticsQueryParam param) throws Exception {
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            st = connection.prepareStatement(sql);
            int paramIndex = 1;

            // 设置日期参数
            if (!StringUtils.isEmpty(param.getStartDate()) && !StringUtils.isEmpty(param.getEndDate())) {
                st.setObject(paramIndex++, param.getStartDate());
                st.setObject(paramIndex++, param.getEndDate());
            } else if (!StringUtils.isEmpty(param.getStartDate())) {
                st.setObject(paramIndex++, param.getStartDate());
            } else if (!StringUtils.isEmpty(param.getEndDate())) {
                st.setObject(paramIndex++, param.getEndDate());
            }

            rs = st.executeQuery();
            Long count = 0L;
            if (rs.next()) {
                count = rs.getLong(1);
            }
            return count;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException ignored) {}
            }
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ignored) {}
            }
        }
    }
}