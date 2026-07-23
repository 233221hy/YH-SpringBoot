package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.BatchDiscussScoreReq;
import cn.xfywz.guozespring.entity.mhsch.YeeDiscuss;
import cn.xfywz.guozespring.service.teacher.YeeDiscussService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeDiscussController {
    @Autowired
    private YeeDiscussService yeeDiscussService;

    @GetMapping("/yee_discuss_list")
    public Result list(@RequestParam(defaultValue = "1") int pageNum,
                      @RequestParam(defaultValue = "10") int pageSize,
                      @RequestParam int schoolId,
                      @RequestParam long courseId) throws Exception {
        return yeeDiscussService.list(pageNum, pageSize, schoolId, courseId);
    }

    @PostMapping("/yee_discuss_add")
    public Result add(@RequestBody YeeDiscuss yeeDiscuss) throws Exception {
        return yeeDiscussService.add(yeeDiscuss);
    }

    @PostMapping("/yee_discuss_update")
    public Result update(@RequestBody YeeDiscuss yeeDiscuss) throws Exception {
        return yeeDiscussService.update(yeeDiscuss);
    }


    @GetMapping("/yee_discuss_delete")
    public Result delete(@RequestParam long id,
                        @RequestParam int schoolId) throws Exception {
        return yeeDiscussService.delete(id, schoolId);
    }

    @GetMapping("/yee_discuss_like")
    public Result like(@RequestParam int schoolId,
                      @RequestParam long courseId,
                      @RequestParam String title) throws Exception {
        return yeeDiscussService.like(schoolId, courseId, title);
    }

    @PostMapping("/discuss_assign_score")
    public Result score(@RequestBody BatchDiscussScoreReq req) throws Exception {
        return yeeDiscussService.batchUpdateScore(
                req.getSchoolId(),
                req.getDiscussId(),
                req.getScores());
    }
}