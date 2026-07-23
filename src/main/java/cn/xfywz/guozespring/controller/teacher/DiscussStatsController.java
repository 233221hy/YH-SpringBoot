package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.DiscussStatsDTO;
import cn.xfywz.guozespring.service.teacher.DiscussStatsService;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class DiscussStatsController {

    @Autowired
    private DiscussStatsService discussStatsService;

    @PostMapping("/discuss_stats_list")
    public Result list(@RequestBody DiscussStatsDTO param) throws Exception {
        return Result.success(discussStatsService.list(param));
    }

    // 讨论详情列表：按用户维度展示该学生/老师的发表或回复
    @PostMapping("/discuss_stats_detail_list")
    public Result detailList(@RequestBody DiscussStatsDTO param) throws Exception {
        return discussStatsService.detailList(param);
    }

    @PostMapping("/discuss_stats_export")
    public void exportData(@RequestBody DiscussStatsDTO param, HttpServletResponse response
                       ) throws Exception {
        discussStatsService.exportData(param, response);
    }

    @GetMapping("/yee_discuss_detail")
    public Result discussDetail(@RequestParam int schoolId,
                             @RequestParam long discussId,
                             @RequestParam int pageNum,
                             @RequestParam int pageSize) {
        return discussStatsService.discussDetail(schoolId, discussId, pageNum, pageSize);
    }

}
