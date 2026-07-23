package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.dto.CourseSignUpDTO;
import cn.xfywz.guozespring.entity.dto.YeeCourseSignStudentDTO;
import cn.xfywz.guozespring.util.Result;

import java.util.List;

public interface YeeCourseSignStudentService {

    Result list(YeeCourseSignStudentDTO dto);
    Result add(Integer schoolId, Integer courseId, Integer studentId) throws Exception;
    Result delete(Integer schoolId, Integer id);
    Result join(Integer schoolId, long courseId, long classId, List<Long> studentIds);
    Result exit(Integer schoolId, long courseId, long classId, List<Long> studentIds);
    Result stuList(CourseSignUpDTO dto);
}
