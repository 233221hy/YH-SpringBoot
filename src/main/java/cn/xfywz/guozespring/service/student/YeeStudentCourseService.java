package cn.xfywz.guozespring.service.student;


import cn.xfywz.guozespring.util.Result;

public interface YeeStudentCourseService {

    Result selectList(int schoolId, int studentId,int type, int pageSize, int pageNum) throws Exception;

    Result selectById(int schoolId, int courseId, int studentId) throws Exception;

}
