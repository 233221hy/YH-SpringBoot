package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenChapter;
import cn.xfywz.guozespring.service.admin.SlOpenChapterService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlOpenChapterController {
    @Autowired
    private SlOpenChapterService slOpenChapterService;
    @GetMapping("/open_chapter_id")
    public Result selectById(@RequestParam int id){
        return slOpenChapterService.selectId(id);
    }
    @PostMapping("/open_chapter_add")
    public Result add(SlOpenChapter slOpenChapter){
        return slOpenChapterService.add(slOpenChapter);
    }
    @GetMapping("/del_open_chapter")
    public Result del(@RequestParam int id){
        return slOpenChapterService.del(id);
    }
    @PostMapping("/open_chapter_update")
    public Result update(SlOpenChapter slOpenChapter){
        return slOpenChapterService.update(slOpenChapter);
    }
}
