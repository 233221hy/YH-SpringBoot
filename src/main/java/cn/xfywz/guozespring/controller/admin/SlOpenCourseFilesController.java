package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenCourseFiles;
import cn.xfywz.guozespring.service.admin.SlOpenCourseFilesService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlOpenCourseFilesController {
    @Autowired
    private SlOpenCourseFilesService slOpenCourseFilesService;
    @GetMapping("/open_course_files_select")
    public Result select(@RequestParam Integer id){
        return slOpenCourseFilesService.List(id);
    }
    @PostMapping("/open_course_files_add")
    public Result add(SlOpenCourseFiles slOpenCourseFiles){
        return slOpenCourseFilesService.add(slOpenCourseFiles);
    }
    @PostMapping("/open_course_files_update")
    public Result update(SlOpenCourseFiles slOpenCourseFiles){
        return slOpenCourseFilesService.update(slOpenCourseFiles);
    }
    @GetMapping("/open_course_files_del")
    public Result delete(@RequestParam Integer id){
        return slOpenCourseFilesService.del(id);
    }
}
