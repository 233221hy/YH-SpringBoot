package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeCourseClass;
import cn.xfywz.guozespring.util.Result;

public interface YeeCourseClassService {
    Result selectAll(int schoolId,long courseId ,int pageNum, int pageSize) throws Exception;
    Result add(YeeCourseClass yeeCourseClass);
    Result update(YeeCourseClass yeeCourseClass);
    Result delete(int schoolId, int id);
    Result like(int schoolId, long courseId ,String name);

}
