package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeDiscussScore;
import cn.xfywz.guozespring.service.teacher.YeeDiscussScoreService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeDiscussScoreController {
    @Autowired
    private YeeDiscussScoreService yeeDiscussScoreService;
    @GetMapping("/yee_discuss_score_list")
    public Result list(@RequestParam Integer schoolId,
                       @RequestParam Integer discussId,
                       @RequestParam Integer pageSize,
                       @RequestParam Integer pageNum) throws Exception {
        return yeeDiscussScoreService.list(schoolId,discussId,pageSize,pageNum);
    }
    @PostMapping("/yee_discuss_score_update")
    public Result update(YeeDiscussScore yeeDiscussScore) throws Exception {
        return yeeDiscussScoreService.update(yeeDiscussScore);
    }


    @GetMapping("/list_student_discuss_score")
    public Result listStudentDiscussScore(
            @RequestParam Integer schoolId,
            @RequestParam Integer courseId,
            @RequestParam Integer discussId,
            @RequestParam(required = false) String studentKeyword,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer totalPostsMin,
            @RequestParam(required = false) Integer postCountMin,
            @RequestParam(required = false) Integer replyCountMin,
            @RequestParam(required = false) Integer likeCountMin,
            @RequestParam(required = false) Integer scoredStatus,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) throws Exception {
        return yeeDiscussScoreService.listStudentDiscussScore(schoolId, courseId,discussId, studentKeyword,
                classId, totalPostsMin, postCountMin, replyCountMin, likeCountMin,
                scoredStatus, pageNum, pageSize
        );
    }
}
