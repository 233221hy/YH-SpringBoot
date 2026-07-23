package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeCourseScoreRules;
import cn.xfywz.guozespring.service.teacher.YeeCourseScoreRulesService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeCourseScoreRulesController {
    
    @Autowired
    private YeeCourseScoreRulesService yeeCourseScoreRulesService;
    
    @GetMapping("/yee_course_score_rules_info")
    public Result info(@RequestParam int schoolId,
                       @RequestParam long courseId,
                       @RequestParam long classId) throws Exception {
        return yeeCourseScoreRulesService.info(schoolId, courseId, classId);
    }
    
    @PostMapping("/yee_course_score_rules_add")
    public Result add(YeeCourseScoreRules yeeCourseScoreRules) throws Exception {
        return yeeCourseScoreRulesService.add(yeeCourseScoreRules);
    }
    
    @PostMapping("/yee_course_score_rules_update")
    public Result update(YeeCourseScoreRules yeeCourseScoreRules) throws Exception {
        return yeeCourseScoreRulesService.update(yeeCourseScoreRules);
    }
    
    @GetMapping("/yee_course_score_rules_delete")
    public Result delete(@RequestParam int schoolId,
                         @RequestParam long courseId,
                         @RequestParam long classId,
                         @RequestParam int id) throws Exception {
        return yeeCourseScoreRulesService.delete(schoolId, courseId, classId, id);
    }

    //公布成绩
    @PostMapping("/yee_course_score_rules_publish")
    public Result publish(@RequestParam int schoolId,
                          @RequestParam Integer id,
                          @RequestParam Integer announce) throws Exception {
        return yeeCourseScoreRulesService.publish(schoolId,id,announce);
    }
}