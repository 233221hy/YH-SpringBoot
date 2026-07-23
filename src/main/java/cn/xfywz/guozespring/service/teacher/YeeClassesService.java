package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeClasses;
import cn.xfywz.guozespring.entity.dto.YeeClassesQueryParam;
import cn.xfywz.guozespring.util.Result;

public interface YeeClassesService {
    Result selectAll(int schoolId, int pageNum, int pageSize) throws Exception;

    Result selectById(int schoolId, long id) throws Exception;

    void add(YeeClasses classes) throws Exception;

    void update(YeeClasses classes) throws Exception;

    void delete(Long id, int schoolId) throws Exception;

    Result searchByCondition(YeeClassesQueryParam param) throws Exception;

    void lock(Long id, int schoolId) throws Exception;

    boolean hasClassesByCollegeId(int schoolId, int collegeId);

}
