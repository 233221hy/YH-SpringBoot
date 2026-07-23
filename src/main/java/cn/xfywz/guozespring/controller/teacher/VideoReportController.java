package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.vo.VideoReport;
import cn.xfywz.guozespring.service.teacher.VideoReportService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class VideoReportController {

    @Autowired
    private VideoReportService videoReportService;

    // 课程视频学习报表：按课程（可选班级）汇总学习进度、浏览与发帖概览
    @PostMapping("/video_report_overview")
    public Result overview(@RequestBody VideoReport param) throws Exception {
        return videoReportService.overview(param);
    }
}
