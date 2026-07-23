package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.service.student.StudyDurationService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学习时长
 */

@RestController
@RequestMapping("/user")
public class StudyDurationController {

    @Autowired
    private StudyDurationService studyDurationService;

    // 学习时长汇总（今日/7天/30天/总计，单位：分钟）
    @GetMapping("/study_duration_stats")
    public Result stats(@RequestParam int schoolId,
                        @RequestParam long studentId,
                        @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return studyDurationService.stats(schoolId, studentId);
        } else {
            return Result.error("非法访问");
        }
    }

    // 按课程分组统计学习时长对比（单位：分钟），可选days参数限定时间范围（包含今天），如7或30，不传为全部
    @GetMapping("/study_duration_course_compare")
    public Result courseCompare(@RequestParam int schoolId,
                                @RequestParam long studentId,
                                @RequestParam(required = false) Integer days,
                                @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return studyDurationService.courseCompare(schoolId, studentId, days);
        } else {
            return Result.error("非法访问");
        }
    }
}
