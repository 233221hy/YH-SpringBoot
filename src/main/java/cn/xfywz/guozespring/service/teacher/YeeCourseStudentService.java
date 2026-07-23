package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeCourseStudent;
import cn.xfywz.guozespring.util.Result;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface YeeCourseStudentService {
    Result selectAll(Integer schoolId, long courseId,long classId, int pageNum, int pageSize);
    Result add(YeeCourseStudent courseStudent);
    Result batchAdd(List<Long> studentIds, long courseId, long classId,long schoolId);
    Result update(YeeCourseStudent courseStudent);
    Result delete(Integer schoolId, long courseId, long classId, List<Long> studentIds);

    Result importCourseStudent(int schoolId,long courseId,long classId,MultipartFile file)throws Exception;
    Result courseStudentLike(int schoolId,long courseId,long classId,String name,String number,String idCard,int pageNum, int pageSize);
    Result getAllStudentsWithCourseType(int schoolId, long courseId, long classId,long teaClassId,  String name, String number, String idCard,String join, int pageNum, int pageSize);
}
