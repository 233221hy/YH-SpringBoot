package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeBasket;
import cn.xfywz.guozespring.entity.mhsch.YeePaper;
import cn.xfywz.guozespring.util.Result;
import org.springframework.web.bind.annotation.RequestParam;

public interface YeePaperService {
    Result selectAll(int schoolId, Integer userId, String title, Integer type, Integer allow, Integer cateBid, Integer cateMid, Integer pageNo, Integer pageSize) throws Exception;

    Result add(YeePaper yeePaper) throws Exception;

    Result addBlank(YeePaper yeePaper) throws Exception;

    Result update(YeePaper yeePaper) throws Exception;

    Result delete(int schoolId, int id, Integer userId) throws Exception;

    Result allow(int schoolId, int id, Integer userId, Byte allow) throws Exception;

    Result getById(int schoolId, int id, Integer userId) throws Exception;

    Result changeTeacher(int schoolId, int id, Integer userId, Integer teacherId) throws  Exception;
}
