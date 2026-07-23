package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeChapter;
import cn.xfywz.guozespring.util.Result;

public interface YeeChapterService {
    Result selectCourse(YeeChapter yeeChapter) throws Exception;
    Result add(YeeChapter yeeChapter);
    Result update(YeeChapter yeeChapter);
    Result delete(YeeChapter yeeChapter);
    Result getCourseChapterTreeWithExams(int schoolId, long courseId);
}
