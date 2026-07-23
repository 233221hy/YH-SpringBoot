package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.service.teacher.YeeCategoryService;
import cn.xfywz.guozespring.service.teacher.YeeCourseClassService;
import cn.xfywz.guozespring.service.teacher.YeeCourseFilesService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/user")
public class StuYeeOpenController {
    @Autowired
    private YeeCategoryService yeeCategoryService;
    @Autowired
    private YeeCourseClassService yeeCourseClassService;
    @Autowired
    private YeeCourseFilesService yeeCourseFilesService;

    @GetMapping("/course_category_list")
    public Result selectAll(int schoolId, @RequestParam(required = false) Integer allow) throws Exception {
        return yeeCategoryService.selectAll(schoolId, allow);
    }

    @GetMapping("/course_class_list")
    public Result selectAll(@RequestParam Integer schoolId,
                            @RequestParam long courseId,
                            @RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "100") int pageSize) throws Exception {
        return yeeCourseClassService.selectAll(schoolId, courseId, pageNum, pageSize);
    }

    // 课程资料列表
    @GetMapping("/yee_course_files_list")
    public Result list(@RequestParam int pageNum,
                       @RequestParam int pageSize,
                       @RequestParam int schoolId,
                       @RequestParam long courseId) throws Exception {
        return yeeCourseFilesService.list(pageSize, pageNum, schoolId, courseId);
    }

    // 课程资料按名称查询
    @PostMapping("/yee_course_files_like")
    public Result like(@RequestParam int schoolId,
                       @RequestParam long courseId,
                       @RequestParam String name) throws Exception {
        return yeeCourseFilesService.like(schoolId, courseId, name);
    }

}
