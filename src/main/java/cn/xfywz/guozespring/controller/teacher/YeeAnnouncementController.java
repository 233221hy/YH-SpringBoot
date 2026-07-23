package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeAnnouncement;
import cn.xfywz.guozespring.service.teacher.YeeAnnouncementService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeAnnouncementController {
    @Autowired
    private YeeAnnouncementService yeeAnnouncementService;

    @GetMapping("/yee_announcement_selectAll")
    public Result selectAll(@RequestParam Integer schoolId,
                           @RequestParam long courseId,
                           @RequestParam(defaultValue = "1") int pageNum,
                           @RequestParam(defaultValue = "10") int pageSize) throws Exception {
        return yeeAnnouncementService.selectAll(schoolId, courseId, pageNum, pageSize);
    }

    @PostMapping("/yee_announcement_add")
    public Result add(YeeAnnouncement yeeAnnouncement) throws Exception {
        return yeeAnnouncementService.add(yeeAnnouncement);
    }

    @PostMapping("/yee_announcement_update")
    public Result update(YeeAnnouncement yeeAnnouncement) throws Exception {
        return yeeAnnouncementService.update(yeeAnnouncement);
    }

    @GetMapping("/yee_announcement_delete")
    public Result delete(@RequestParam Integer schoolId,
                        @RequestParam int id) throws Exception {
        return yeeAnnouncementService.delete(schoolId, id);
    }

    @GetMapping("/yee_announcement_like")
    public Result like(@RequestParam Integer schoolId,
                      @RequestParam long courseId,
                      @RequestParam String name) throws Exception {
        return yeeAnnouncementService.like(schoolId, courseId, name);
    }
}
