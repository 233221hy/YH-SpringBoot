package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.service.teacher.YeeCoursePointService;
import cn.xfywz.guozespring.service.teacher.YeeExamService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @Author: ChengLin
 * yee_course_point
 */
@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeCoursePointController {

    @Autowired
    private YeeCoursePointService yeeCoursePointService;

    @GetMapping("/yee_course_point_allList")
    public Result selectAll(@RequestParam int schoolId,
                            @RequestParam Integer courseId,
                            @RequestParam(required = false) String title,
                            @RequestParam(required = false) Integer classId) throws Exception {
        return yeeCoursePointService.selectAll(schoolId, courseId, title, classId);
    }

    @GetMapping("/yee_course_point_todayList")
    public Result selectToday(@RequestParam int schoolId,
                            @RequestParam Integer courseId,
                            @RequestParam(required = false) String title,
                            @RequestParam(required = false) Integer classId,
                            @RequestParam String date) throws Exception {
        return yeeCoursePointService.selectTodayAll(schoolId, courseId, title, classId, date);
    }

    @GetMapping("/yee_course_point_monthList")
    public Result selectMonth(@RequestParam int schoolId,
                                 @RequestParam Integer courseId,
                                 @RequestParam(required = false) String title,
                                 @RequestParam(required = false) Integer classId,
                                 @RequestParam String date) throws Exception {
        return yeeCoursePointService.selectMonthAll(schoolId, courseId, title, classId, date);
    }

    // ======== 导出接口：全部/今日/本月 ========
    @PostMapping("/yee_course_point_allExport")
    public void exportAll(@RequestParam int schoolId,
                          @RequestParam Integer courseId,
                          @RequestParam(required = false) String title,
                          @RequestParam(required = false) Integer classId,
                          HttpServletResponse response) throws Exception {
        yeeCoursePointService.exportAll(schoolId, courseId, title, classId, response);
    }

    @PostMapping("/yee_course_point_todayExport")
    public void exportToday(@RequestParam int schoolId,
                            @RequestParam Integer courseId,
                            @RequestParam(required = false) String title,
                            @RequestParam(required = false) Integer classId,
                            @RequestParam String date,
                            HttpServletResponse response) throws Exception {
        yeeCoursePointService.exportToday(schoolId, courseId, title, classId, date, response);
    }

    @PostMapping("/yee_course_point_monthExport")
    public void exportMonth(@RequestParam int schoolId,
                            @RequestParam Integer courseId,
                            @RequestParam(required = false) String title,
                            @RequestParam(required = false) Integer classId,
                            @RequestParam String date,
                            HttpServletResponse response) throws Exception {
        yeeCoursePointService.exportMonth(schoolId, courseId, title, classId, date, response);
    }
}
