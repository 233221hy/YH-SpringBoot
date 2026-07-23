package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.service.student.YeePracticeReportService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


@RestController("stuYeePracticeReportController")
@RequestMapping("/user")
public class YeePracticeReportController {

    @Resource
    private YeePracticeReportService yeePracticeReportService;

    @GetMapping("/practice_report_my")
    public Result myReport(@RequestParam int schoolId,
                           @RequestParam int courseId,
                           @RequestParam int studentId,
                           @RequestHeader String Authorization) throws Exception {
        if (!AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return Result.error("非法访问");
        }
        return yeePracticeReportService.myReport(schoolId, courseId, studentId);
    }

    @PostMapping("/practice_report_submit")
    public Result submit(@RequestParam int schoolId,
                         @RequestParam int courseId,
                         @RequestParam int studentId,
                         @RequestParam String title,
                         @RequestParam String content,
                         @RequestParam(required = false) String files,
                         @RequestHeader String Authorization) throws Exception {
        if (!AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return Result.error("非法访问");
        }
        return yeePracticeReportService.submit(schoolId, courseId, studentId,
                title, content, files);
    }
}