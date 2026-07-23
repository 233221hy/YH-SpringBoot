package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplCourse;
import cn.xfywz.guozespring.entity.vo.TplCourseLike;
import cn.xfywz.guozespring.service.admin.TplCourseService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class TplCourseController {
    @Autowired
    private TplCourseService tplCourseService;
    @GetMapping("/tpl_course_list")
    public Result selectAll(@RequestParam int PageSize, @RequestParam int PageNum){
        return tplCourseService.selectAll(PageSize,PageNum);
    }
    @PostMapping("/tpl_course_add")
    public Result add(SlTplCourse slTplCourse){
        return tplCourseService.add(slTplCourse);
    }
    @GetMapping("/del_tpl_course")
    public Result del(@RequestParam int id){
        return tplCourseService.del(id);
    }
    @PostMapping("/tpl_course_update")
    public Result update(SlTplCourse slTplCourse){
        return tplCourseService.update(slTplCourse);
    }
    @PostMapping("/tpl_course_search")
    public Result search(TplCourseLike tplCourseLike, Integer pageNum, Integer pageSize){
        return tplCourseService.search(tplCourseLike,pageNum,pageSize);
    }
}
