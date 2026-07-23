package cn.xfywz.guozespring.service.admin;

import cn.xfywz.guozespring.entity.dto.SlOpenCourseQueryDTO;
import cn.xfywz.guozespring.entity.mhmain.SlOpenCourse;
import cn.xfywz.guozespring.entity.vo.TplCourseLike;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

@Service
public interface SlOpenCourseService {
    SlOpenCourse selectNameById(int id);
    Result selectById(int id);
    Result add(SlOpenCourse slOpenCourse);
    Result del(int  id);
    Result update(SlOpenCourse slOpenCourse);
    Result selectLike(TplCourseLike courseLike,int PageSize, int PageNum);
    Result publish(int  id);
    Result openCourseList(SlOpenCourseQueryDTO queryDTO, int schoolId);
    Result openCourseTemplateImport(long tplId, SlOpenCourse input);

}
