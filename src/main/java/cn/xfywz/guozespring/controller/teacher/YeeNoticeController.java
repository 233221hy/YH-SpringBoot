package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeNotice;
import cn.xfywz.guozespring.service.teacher.YeeNoticeService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeNoticeController {
    @Autowired
    private YeeNoticeService yeeNoticeService;

    @GetMapping("/yee_notice_select")
    public Result select(@RequestParam Integer schoolId,
                         @RequestParam(required = false) String title,
                         @RequestParam(required = false) Integer type,
                         @RequestParam(required = false) Long courseId,
                         @RequestParam(defaultValue = "1") int pageNum,
                         @RequestParam(defaultValue = "10") int pageSize) throws Exception {
        return yeeNoticeService.teacherSelect(schoolId, title, type, courseId, pageNum, pageSize);
    }

    @PostMapping("/yee_notice_add")
    public Result add(@RequestBody YeeNotice yeeNotice) throws Exception {
        return yeeNoticeService.add(yeeNotice);
    }

    @PostMapping("/yee_notice_update")
    public Result update(@RequestBody YeeNotice yeeNotice) throws Exception {
        return yeeNoticeService.update(yeeNotice);
    }

    @GetMapping("/yee_notice_delete")
    public Result delete(@RequestParam Integer schoolId,
                         @RequestParam long id) throws Exception {
        return yeeNoticeService.delete(schoolId, id);
    }
}