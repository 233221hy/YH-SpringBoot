package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlSchoolThematic;
import cn.xfywz.guozespring.util.Result;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SlSchoolThematicService extends IService<SlSchoolThematic> {
    Result selectById(SlSchoolThematic thematic);
    Result getThematicsWithCoursesByDomain(String domain);
}
