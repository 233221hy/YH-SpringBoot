package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.VideoRank;
import cn.xfywz.guozespring.service.teacher.VideoRankService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class VideoRankController {

    @Autowired
    private VideoRankService videoRankService;

    @PostMapping("/video_rank_list")
    public Result list(@RequestBody VideoRank param) throws  Exception{
        return Result.success(videoRankService.list(param));
    }
}
