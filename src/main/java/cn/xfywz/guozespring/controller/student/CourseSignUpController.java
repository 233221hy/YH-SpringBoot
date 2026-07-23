package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.entity.dto.CourseSignUpDTO;
import cn.xfywz.guozespring.service.teacher.YeeCourseSignStudentService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class CourseSignUpController {

    @Autowired
    private YeeCourseSignStudentService yeeCourseSignStudentService;

    @PostMapping("/yee_course_sign_student_add")
    public Result add(@RequestParam Integer schoolId,
                      @RequestParam Integer courseId,
                      @RequestParam Integer studentId,
                      @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeCourseSignStudentService.add(schoolId, courseId, studentId);
        } else {
            return Result.error("非法访问");
        }
    }

    @GetMapping("/yee_course_sign_student_delete")
    public Result delete(@RequestParam Integer schoolId,
                         @RequestParam Integer id,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeCourseSignStudentService.delete(schoolId, id);
        } else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_course_sign_up_list")
    public Result stuList(@RequestBody CourseSignUpDTO dto,
                       @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, dto.getSchoolId())) {
            return yeeCourseSignStudentService.stuList(dto);
        } else {
            return Result.error("非法访问");
        }
    }
}
