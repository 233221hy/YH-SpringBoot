package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpinion;
import cn.xfywz.guozespring.entity.vo.SlOpinionLike;
import cn.xfywz.guozespring.service.admin.SlOpinionService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlOpinionController {
    @Autowired
    SlOpinionService slOpinionService;
    @GetMapping("/opinion_list")
    public Result selectAll(@RequestParam int PageSize, @RequestParam int PageNum) {
        return slOpinionService.list(PageNum, PageSize);
    }
    @PostMapping("/opinion_add")
    public Result add(SlOpinion slOpinion) {
        return slOpinionService.add(slOpinion);
    }
    @GetMapping("/opinion_del")
    public Result delete(@RequestParam Integer id) {
        return slOpinionService.delete(id);
    }
    @PostMapping("/opinion_like")
    public Result selectLike(SlOpinionLike slOpinionLike) {
        return slOpinionService.like(slOpinionLike);
    }

}
