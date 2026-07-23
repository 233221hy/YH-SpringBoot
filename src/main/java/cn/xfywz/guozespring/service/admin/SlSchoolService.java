package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlSchool;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlSchoolService {
    Result selectAll();

    Result add(SlSchool slSchool);

    Result update(SlSchool slSchool);

    Result delete(Integer id);

    Result selectDomain(String domain);

    SlSchool selectById(Integer id);
}
