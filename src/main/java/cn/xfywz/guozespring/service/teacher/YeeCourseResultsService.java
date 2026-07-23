package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.dto.YeeCourseResultsQueryDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeCourseResults;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface YeeCourseResultsService {
    Result list(YeeCourseResultsQueryDTO queryDTO);
    void add(YeeCourseResults yeeCourseResults);
    void delete(long id,long schoolId,long courseId,long classId);
    Result update(YeeCourseResults yeeCourseResults);
    
    // 计算指定班级所有学生的成绩
    void calculateScore(int schoolId, long courseId, long classId);

    // 导出课程成绩为Excel
    void exportResults(YeeCourseResultsQueryDTO queryDTO, HttpServletResponse response) throws Exception;

    // 导出额外分数为 Excel
    void exportExtraScore(YeeCourseResultsQueryDTO queryDTO, HttpServletResponse response) throws Exception;

    // 导入额外分数
    Result importExtraScore(Integer schoolId, MultipartFile file, String courseId);
}
