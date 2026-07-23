package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeNode;
import cn.xfywz.guozespring.util.Result;

public interface YeeNodeService {
    Result selectByCourseId(Integer schoolId, long chapterId);
    Result add(YeeNode node);
    Result update(YeeNode node);
    Result delete(Integer schoolId,long id);
    Result getCourseChapterTreeForStudent(int schoolId, long courseId, int studentId);
}
