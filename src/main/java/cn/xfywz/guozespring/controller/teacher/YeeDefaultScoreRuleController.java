package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeDefaultScoreRule;
import cn.xfywz.guozespring.service.teacher.YeeDefaultScoreRuleService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeDefaultScoreRuleController {
    
    @Autowired
    private YeeDefaultScoreRuleService yeeDefaultScoreRuleService;
    
    @GetMapping("/yee_default_score_rule_list")
    public Result list(@RequestParam int pageNum,
                       @RequestParam int pageSize,
                       @RequestParam int schoolId,
                       @RequestParam long courseId) throws Exception {
        return yeeDefaultScoreRuleService.list(schoolId, courseId, pageNum, pageSize);
    }
    
    @PostMapping("/yee_default_score_rule_add")
    public Result add(YeeDefaultScoreRule yeeDefaultScoreRule) throws Exception {
        return yeeDefaultScoreRuleService.add(yeeDefaultScoreRule);
    }
    
    @PostMapping("/yee_default_score_rule_update")
    public Result update(YeeDefaultScoreRule yeeDefaultScoreRule) throws Exception {
        return yeeDefaultScoreRuleService.update(yeeDefaultScoreRule);
    }
    
    @GetMapping("/yee_default_score_rule_delete")
    public Result delete(@RequestParam int schoolId,
                         @RequestParam long courseId,
                         @RequestParam int id) throws Exception {
        return yeeDefaultScoreRuleService.delete(schoolId, courseId, id);
    }
    
    @GetMapping("/yee_default_score_rule_like")
    public Result like(@RequestParam int schoolId,
                       @RequestParam long courseId,
                       @RequestParam String name) throws Exception {
        return yeeDefaultScoreRuleService.like(schoolId, courseId, name);
    }
}