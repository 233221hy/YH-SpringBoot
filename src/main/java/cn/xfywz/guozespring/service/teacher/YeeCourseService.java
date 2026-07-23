package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeCourse;
import cn.xfywz.guozespring.entity.vo.LikeYeeCourse;
import cn.xfywz.guozespring.entity.dto.YeeCourseQueryParam;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

public interface YeeCourseService {
    Result selectAll(int schoolId, int pageSize, int pageNum, String authorization) throws Exception;
    Result deleteById(int schoolId, int id) throws Exception;
    Result update(YeeCourse yeeCourse) throws Exception;
    Result add(YeeCourse yeeCourse) throws Exception;
    Result like(LikeYeeCourse likeYeeCourse,Integer pageSize,Integer pageNum, String authorization) throws Exception;
    
    // 新增分页查询列表接口，支持动态条件查询
    Result selectAllWithConditions(YeeCourseQueryParam param) throws Exception;

    // 流式导出课程-学生选课数据（支持大数据量）
    void exportCourseStudentEnrollmentData(YeeCourseQueryParam param, HttpServletResponse response) throws Exception;

    Result courseTemplateImport(YeeCourse yeeCourse);
    //查询课程下的所有视频、考试、作业、讨论
    Result selectCourseContent(int schoolId, int id, Integer classId) throws Exception;

    void exportCourseData(YeeCourseQueryParam param, HttpServletResponse response) throws Exception;

    Result copyCourse(YeeCourse yeeCourse);
}
