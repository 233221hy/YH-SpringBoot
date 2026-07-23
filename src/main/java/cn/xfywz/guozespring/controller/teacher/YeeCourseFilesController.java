package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeCourseFiles;
import cn.xfywz.guozespring.service.teacher.YeeCourseFilesService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeCourseFilesController {
    @Autowired
    private YeeCourseFilesService yeeCourseFilesService;
    @GetMapping("/yee_course_files_list")
    public Result list(@RequestParam int pageNum,
                       @RequestParam int pageSize,
                       @RequestParam int schoolId,
                       @RequestParam long courseId) throws Exception {
        return yeeCourseFilesService.list(pageSize,pageNum, schoolId, courseId);
    }
    @PostMapping("/yee_course_files_add")
    public Result add(YeeCourseFiles yeeCourseFiles) throws Exception {
        return yeeCourseFilesService.add(yeeCourseFiles);
    }

   @GetMapping("/yee_course_files_delete")
    public Result delete(@RequestParam int schoolId,
                         @RequestParam long id,
                         @RequestParam long courseId) throws Exception {
        return yeeCourseFilesService.delete(id, schoolId, courseId);
    }
    @PostMapping("/yee_course_files_like")
    public Result like(@RequestParam int schoolId,
                       @RequestParam long courseId,
                       @RequestParam String name) throws Exception {
        return yeeCourseFilesService.like(schoolId, courseId, name);
    }
}
