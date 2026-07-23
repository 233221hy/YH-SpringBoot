package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplCourse;
import cn.xfywz.guozespring.entity.vo.TplCourseLike;
import cn.xfywz.guozespring.util.Result;
import org.springframework.stereotype.Service;

@Service
public interface TplCourseService {
    Result selectAll(int PageSize, int PageNum);

    Result add(SlTplCourse slTplCourse);

    Result del(int id);

    Result update(SlTplCourse slTplCourse);

    Result search(TplCourseLike tplCourseLike,Integer pageNum, Integer pageSize);
}
