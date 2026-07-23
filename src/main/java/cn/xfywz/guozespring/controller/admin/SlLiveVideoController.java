package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlLiveVideo;
import cn.xfywz.guozespring.service.admin.SlLiveVideoService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/manage")
public class SlLiveVideoController {
    @Autowired
    private SlLiveVideoService slLiveVideoService;
    @GetMapping("/live_video_list")
    public Result selectAll(@RequestParam int PageSize, @RequestParam int PageNum){
        return slLiveVideoService.list(PageNum,PageSize);
    }
    @PostMapping("/live_video_add")
    public Result add(SlLiveVideo slLiveVideo){
        return slLiveVideoService.add(slLiveVideo);
    }
    @PostMapping("/live_video_update")
    public Result update(SlLiveVideo slLiveVideo){
        return slLiveVideoService.update(slLiveVideo);
    }
    @GetMapping("/live_video_del")
    public Result delete(@RequestParam Integer id){
        return slLiveVideoService.delete(id);
    }
    @GetMapping("/live_video_like")
    public Result selectLike(@RequestParam String name){
        return slLiveVideoService.selectLike(name);
    }
}
