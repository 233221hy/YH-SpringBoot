package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplQuestion;
import cn.xfywz.guozespring.entity.vo.SlTplQuestionLike;
import cn.xfywz.guozespring.service.admin.SlTplQuestionService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlTplQuestionController {
    @Autowired
    private SlTplQuestionService slTplQuestionService;

    @GetMapping("/tpl_question_list")
    public Result selectAll(int PageSize, int PageNum){
        return slTplQuestionService.selectAll(PageSize, PageNum);
    }
    @PostMapping("/tpl_question_add")
    public Result add(SlTplQuestion slTplQuestion){
        return slTplQuestionService.add(slTplQuestion);
    }
    @GetMapping("/del_tpl_question")
    public Result del(@RequestParam int id){
        return slTplQuestionService.del(id);
    }
    @PostMapping("/tpl_question_update")
    public Result update(SlTplQuestion slTplQuestion){
        return slTplQuestionService.update(slTplQuestion);
    }

    @PostMapping("/tpl_question_search")
    public Result search(SlTplQuestionLike slTplQuestionLike){
        return slTplQuestionService.search(slTplQuestionLike);
    }

}
