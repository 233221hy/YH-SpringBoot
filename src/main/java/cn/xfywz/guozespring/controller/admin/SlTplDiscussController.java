package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplDiscuss;
import cn.xfywz.guozespring.service.admin.SlTplDiscussService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlTplDiscussController {
    @Autowired
    private SlTplDiscussService slTplDiscussService;
    @GetMapping("/tpl_discuss_list")
    public Result selectAll(@RequestParam int PageSize, @RequestParam int PageNum, @RequestParam int courseId){
        return slTplDiscussService.showAll(PageSize,PageNum,courseId);
    }

    @PostMapping("/tpl_discuss_add")
    public Result add(SlTplDiscuss slTplDiscuss){
        return slTplDiscussService.add(slTplDiscuss);
    }

    @GetMapping("/del_tpl_discuss")
    public Result del(@RequestParam int id){
        return slTplDiscussService.del(id);
    }

    @PostMapping("/tpl_discuss_update")
    public Result update(SlTplDiscuss slTplDiscuss){
        return slTplDiscussService.update(slTplDiscuss);
    }

}
