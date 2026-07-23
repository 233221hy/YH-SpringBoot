package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlTplChapter;
import cn.xfywz.guozespring.service.admin.SlTplChapterService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlTplChapterController {
    @Autowired
    private SlTplChapterService slTplChapterService;
    @PostMapping("/tpl_chapter_add")
    public Result add(SlTplChapter slTplChapter) {
        return slTplChapterService.add(slTplChapter);
    }
    @PostMapping("/tpl_chapter_update")
    public Result update(SlTplChapter slTplChapter) {
        return slTplChapterService.update(slTplChapter);
    }
    @GetMapping("/del_tpl_chapter")
    public Result del(@RequestParam int id) {
        return slTplChapterService.del(id);
    }
}
