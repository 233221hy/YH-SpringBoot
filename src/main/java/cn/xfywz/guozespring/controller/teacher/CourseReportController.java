package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.vo.CourseReport;
import cn.xfywz.guozespring.service.teacher.CourseReportService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class CourseReportController {

    @Autowired
    private CourseReportService courseReportService;

    // 总览卡片：视频观看次数、学习人数、学习总时长、资料情况、考试提交情况
    @PostMapping("/course_report_overview")
    public Result overview(@RequestBody CourseReport param) throws Exception {
        return courseReportService.overview(param);
    }

    // 活动统计：视频分布、实时在线
    @PostMapping("/course_report_activity")
    public Result activity(@RequestBody CourseReport param) throws Exception {
        return courseReportService.activity(param);
    }
}
