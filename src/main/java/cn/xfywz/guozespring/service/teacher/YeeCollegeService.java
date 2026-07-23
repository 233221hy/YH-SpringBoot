package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.dto.YeeCollegeQueryDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeCollege;
import cn.xfywz.guozespring.util.Result;

public interface YeeCollegeService {
    Result selectAll(YeeCollegeQueryDTO queryDTO) throws Exception;
    void add(YeeCollege yeeCollege) throws Exception;
    Result update(YeeCollege yeeCollege) throws Exception;
    Result delete(int schoolId, int id) throws Exception;
    Result selectById(int schoolId, long id) throws Exception;
}
