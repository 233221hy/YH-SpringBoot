package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.mhmain.SlSchoolBanner;
import cn.xfywz.guozespring.entity.dto.SlSchBannerQueryParam;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface SlSchBannerService {


    Result list(int pageSize, int pageNum, Integer schoolId);

    Result add(SlSchoolBanner slSchBanner);

    Result info(long id, int schoolId);

    Result update(SlSchoolBanner slSchBanner);

    Result delete(Integer id);

    Result searchByCondition(SlSchBannerQueryParam param);

//    Result selectAll(int pageSize, int pageNum);
}
