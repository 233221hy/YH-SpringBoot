package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.entity.dto.CourseResultsQueryDTO;
import cn.xfywz.guozespring.service.student.CourseResultsService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class CourseResultsController {

    @Autowired
    private CourseResultsService courseResultsService;

    //获取课程成绩
    @PostMapping("/course_results")
    public Result courseResults(@RequestBody CourseResultsQueryDTO param,
                                @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, param.getSchoolId())) {
            return courseResultsService.courseResults(param);
        } else return Result.error("非法访问");
    }
}
