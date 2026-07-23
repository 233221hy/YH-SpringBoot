package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeChapter;
import cn.xfywz.guozespring.service.teacher.YeeChapterService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeChapterController {
    @Autowired
    private YeeChapterService yeeChapterService;
    @PostMapping("/yee_chapter_select")
    public Result selectAll(YeeChapter yeeChapter) throws Exception {
        return yeeChapterService.selectCourse(yeeChapter);
    }

    @PostMapping("/yee_chapter_add")
    public Result add(YeeChapter yeeChapter) throws Exception {
        return yeeChapterService.add(yeeChapter);
    }

    @PostMapping("/yee_chapter_update")
    public Result update(YeeChapter yeeChapter) throws Exception {
        return yeeChapterService.update(yeeChapter);
    }
    @PostMapping("/yee_chapter_delete")
    public Result delete(YeeChapter yeeChapter) throws Exception {
        return yeeChapterService.delete(yeeChapter);
    }
}