package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTeachingNews;
import cn.xfywz.guozespring.service.admin.SlTeachingNewsService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlTeachingNewsController {
    @Autowired
    private SlTeachingNewsService slTeachingNewsService;
    @GetMapping("/teaching_news_list")
    public Result selectAll(@RequestParam int PageSize, @RequestParam int PageNum){
         return Result.success(slTeachingNewsService.list(PageNum,PageSize));
    }

    @PostMapping ("/teaching_news_add")
    public Result add(SlTeachingNews slTeachingNews){
        return slTeachingNewsService.add(slTeachingNews);
    }

    @PostMapping("/teaching_news_update")
    public Result update(SlTeachingNews slTeachingNews){
        return slTeachingNewsService.update(slTeachingNews);
    }

    @GetMapping("/teaching_news_del")
    public Result delete(@RequestParam Integer id){
        return slTeachingNewsService.delete(id);
    }
    @GetMapping("/teaching_news_like")
    public Result selectLike(@RequestParam String title){
        return slTeachingNewsService.selectLike(title);
    }


}
