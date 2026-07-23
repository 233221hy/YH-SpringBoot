package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenCourseCluster;
import cn.xfywz.guozespring.entity.vo.TplCourseLike;
import cn.xfywz.guozespring.service.admin.SlOpenCourseClusterService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlOpenCourseClusterController {
    @Autowired
    private SlOpenCourseClusterService slOpenCourseClusterService;
    @PostMapping("/open_course_cluster_list")
    public Result selectAll(TplCourseLike courseLike, @RequestParam int PageSize, @RequestParam int PageNum) {
        return slOpenCourseClusterService.selectAll(PageSize,PageNum,courseLike);
    }
    @PostMapping("/open_course_cluster_add")
    public Result add(SlOpenCourseCluster slOpenCourseCluster) {
        return slOpenCourseClusterService.add(slOpenCourseCluster);
    }
    @GetMapping("/del_open_course_cluster")
    public Result del(@RequestParam int id) {
        return slOpenCourseClusterService.del(id);
    }

    @PostMapping("/open_course_cluster_update")
    public Result update(SlOpenCourseCluster slOpenCourseCluster) {
        return slOpenCourseClusterService.update(slOpenCourseCluster);
    }
}
