package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeCourseFiles;
import cn.xfywz.guozespring.util.Result;

public interface YeeCourseFilesService {
    Result list(int pageSize, int pageNum, int schoolId, long courseId);
    Result add(YeeCourseFiles yeeCourseFiles);
    Result delete(long id, int schoolId, long courseId);
    Result like(int schoolId, long courseId, String name);
}
