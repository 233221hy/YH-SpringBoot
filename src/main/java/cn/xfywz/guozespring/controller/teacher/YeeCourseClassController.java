package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeCourseClass;
import cn.xfywz.guozespring.service.teacher.YeeCourseClassService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeCourseClassController {
    @Autowired
    private YeeCourseClassService yeeCourseClassService;

    @GetMapping("/yee_course_class_selectAll")
    public Result selectAll(@RequestParam Integer schoolId,
                           @RequestParam long courseId,
                           @RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "100") int pageSize) throws Exception {
        return yeeCourseClassService.selectAll(schoolId,courseId,pageNum, pageSize);
    }

    @PostMapping("/yee_course_class_add")
    public Result add(YeeCourseClass yeeCourseClass) throws Exception {
        return yeeCourseClassService.add(yeeCourseClass);
    }

    @PostMapping("/yee_course_class_update")
    public Result update(YeeCourseClass yeeCourseClass) throws Exception {
        return yeeCourseClassService.update(yeeCourseClass);
    }

    @GetMapping("/yee_course_class_delete")
    public Result delete(@RequestParam Integer schoolId,
                        @RequestParam int id) throws Exception {
        return yeeCourseClassService.delete(schoolId, id);
    }

    @GetMapping("/yee_course_class_like")
    public Result like(@RequestParam Integer schoolId,
                      @RequestParam long courseId,
                      @RequestParam String name) throws Exception {
        return yeeCourseClassService.like(schoolId, courseId, name);
    }


}