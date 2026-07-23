package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.DiscussStatsDTO;
import cn.xfywz.guozespring.service.teacher.NodeDiscussStatsService;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class NodeDiscussStatsController {

    @Autowired
    private NodeDiscussStatsService nodeDiscussStatsService;

    @PostMapping("/node_discuss_stats_list")
    public Result list(@RequestBody DiscussStatsDTO param,
                       @RequestHeader(required = false, name = "schoolId") Long schoolId) {
        if (schoolId != null) param.setSchoolId(schoolId);
        return nodeDiscussStatsService.list(param);
    }

    @PostMapping("/node_discuss_stats_detail_list")
    public Result detailList(@RequestBody DiscussStatsDTO param,
                             @RequestHeader(required = false, name = "schoolId") Long schoolId) {
        if (schoolId != null) param.setSchoolId(schoolId);
        return nodeDiscussStatsService.detailList(param);
    }

    @PostMapping("/node_discuss_stats_export")
    public void exportData(@RequestBody DiscussStatsDTO param,
                           @RequestHeader(required = false, name = "schoolId") Long schoolId,
                           HttpServletResponse response) {
        if (schoolId != null) param.setSchoolId(schoolId);
        nodeDiscussStatsService.exportData(param, response);
    }
}
