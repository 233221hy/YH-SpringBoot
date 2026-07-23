package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlOpenAnnouncement;
import cn.xfywz.guozespring.service.admin.SlOpenAnnouncementService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlOpenAnnouncementController {
    @Autowired
    private SlOpenAnnouncementService slOpenAnnouncementService;
    @GetMapping("/open_announcement_select")
    public Result select(@RequestParam Integer id)
    {
        return slOpenAnnouncementService.select(id);
    }
    @PostMapping("/open_announcement_add")
    public Result add(SlOpenAnnouncement slOpenAnnouncement){
        return slOpenAnnouncementService.add(slOpenAnnouncement);
    }
    @PostMapping("/open_announcement_update")
    public Result update(SlOpenAnnouncement slOpenAnnouncement){
        return slOpenAnnouncementService.update(slOpenAnnouncement);
    }
    @GetMapping("/open_announcement_del")
    public Result delete(@RequestParam Integer id){
        return slOpenAnnouncementService.delete(id);
    }
}
