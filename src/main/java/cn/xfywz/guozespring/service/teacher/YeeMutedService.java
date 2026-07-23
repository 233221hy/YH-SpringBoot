package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeMuted;
import cn.xfywz.guozespring.entity.dto.YeeMutedQueryParam;
import cn.xfywz.guozespring.util.Result;


public interface YeeMutedService {

    Result list(int pageSize, int pageNum, int schoolId);

    void delete(Integer id, int schoolId);

    Result searchByCondition(YeeMutedQueryParam param);

    void add(YeeMuted muted);

}
