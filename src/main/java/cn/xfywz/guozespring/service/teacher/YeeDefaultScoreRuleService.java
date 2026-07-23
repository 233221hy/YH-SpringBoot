package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeDefaultScoreRule;
import cn.xfywz.guozespring.util.Result;

public interface YeeDefaultScoreRuleService {
    Result list(int schoolId, long courseId,int pageNum, int pageSize);
    Result add(YeeDefaultScoreRule yeeDefaultScoreRule);
    Result update(YeeDefaultScoreRule yeeDefaultScoreRule);
    Result delete(int schoolId, long courseId,int id);
    Result like(int schoolId, long courseId, String name);
}
