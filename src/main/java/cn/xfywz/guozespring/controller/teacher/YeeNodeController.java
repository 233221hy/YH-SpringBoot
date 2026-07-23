package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeNode;
import cn.xfywz.guozespring.service.teacher.YeeChapterService;
import cn.xfywz.guozespring.service.teacher.YeeNodeService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeNodeController {
    @Autowired
    private YeeNodeService yeeNodeService;
    @Autowired
    private YeeChapterService yeeChapterService;
    @GetMapping("/yee_node_select")
    public Result select(@RequestParam Integer schoolId,
                         @RequestParam long chapterId) throws Exception {
        return yeeNodeService.selectByCourseId(schoolId, chapterId);
    }

    @PostMapping("/yee_node_add")
    public Result add(YeeNode yeeNode) throws Exception {
        return yeeNodeService.add(yeeNode);
    }
    @PostMapping("/yee_node_update")
    public Result update(YeeNode yeeNode) throws Exception {
        return yeeNodeService.update(yeeNode);
    }
    @GetMapping("/yee_node_delete")
    public Result delete(@RequestParam Integer schoolId,
                         @RequestParam long id) throws Exception {
        return yeeNodeService.delete(schoolId, id);
    }

    // 获取课程所有章节信息
    @GetMapping("/yee_all_node_select")
    public Result selectAllNodesByCourseId(@RequestParam Integer schoolId,
                                           @RequestParam long courseId) throws Exception {
        return yeeChapterService.getCourseChapterTreeWithExams(schoolId, courseId);
    }

}
