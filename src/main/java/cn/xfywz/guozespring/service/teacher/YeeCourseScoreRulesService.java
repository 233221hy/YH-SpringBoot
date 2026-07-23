package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeCourseScoreRules;
import cn.xfywz.guozespring.util.Result;

public interface YeeCourseScoreRulesService {
    Result info(int schoolId, long courseId,long classId);
    Result add(YeeCourseScoreRules yeeCourseScoreRules);
    Result update(YeeCourseScoreRules yeeCourseScoreRules);
    Result delete(int schoolId, long courseId, long classId, int id);

    Result publish(int schoolId, Integer id, Integer announce);
}
