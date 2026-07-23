package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeCategory;
import cn.xfywz.guozespring.util.Result;

public interface YeeCategoryService {
    Result selectAll(int schoolId, Integer allow) throws Exception;
    Result add(YeeCategory yeeCategory) throws Exception;
    Result update(YeeCategory yeeCategory) throws Exception;
    Result delete(int schoolId, int id) throws Exception;
}
