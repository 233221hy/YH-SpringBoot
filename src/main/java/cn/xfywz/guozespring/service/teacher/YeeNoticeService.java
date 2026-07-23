package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeNotice;
import cn.xfywz.guozespring.util.Result;

public interface YeeNoticeService {
    Result teacherSelect(int schoolId, String title, Integer type, Long courseId, int pageNum, int pageSize);
    Result add(YeeNotice notice);
    Result update(YeeNotice notice);
    Result delete(int schoolId, long id);
    Result selectById(int schoolId, long courseId) throws Exception;
    Result studentSelect(int schoolId, long studentId, int pageSize, int pageNum);
}
