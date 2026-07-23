package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.dto.SlOpenCourseQueryDTO;
import cn.xfywz.guozespring.entity.mhmain.SlOpenCourse;
import cn.xfywz.guozespring.entity.vo.TplCourseLike;
import cn.xfywz.guozespring.service.admin.SlOpenCourseService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlOpenCourseController {
    @Autowired
    private SlOpenCourseService slOpenCourseService;

    @GetMapping("/open_course_id")
    public Result selectById(@RequestParam int id){
        return slOpenCourseService.selectById(id);
    }

    @PostMapping("/open_course_add")
    public Result add(@RequestBody SlOpenCourse slOpenCourse){
        return slOpenCourseService.add(slOpenCourse);
    }

    @GetMapping("/del_open_course")
    public Result del(@RequestParam int id){
        return slOpenCourseService.del(id);
    }

    @PostMapping("/open_course_update")
    public Result update(@RequestBody SlOpenCourse slOpenCourse){
        return slOpenCourseService.update(slOpenCourse);
    }

    @PostMapping("/open_course_like")
    public Result  selectList(TplCourseLike courseLike,@RequestParam int PageSize,@RequestParam int PageNum){
        return slOpenCourseService.selectLike(courseLike,PageSize,PageNum);
    }
    // 审核发布/上架发布
    @GetMapping("/publish_open_course")
    public Result publish(@RequestParam int id){
        return slOpenCourseService.publish(id);
    }

    // 公开课模板导入
    @PostMapping("/openCourse_template_import")
    public Result openCourseTemplateImport(@RequestParam long tplId,@RequestBody SlOpenCourse input) {
        return slOpenCourseService.openCourseTemplateImport(tplId, input);
    }
}
