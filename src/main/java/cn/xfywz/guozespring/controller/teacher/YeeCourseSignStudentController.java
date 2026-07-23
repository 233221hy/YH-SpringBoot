package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.YeeCourseSignStudentDTO;
import cn.xfywz.guozespring.service.teacher.YeeCourseSignStudentService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 课程报名管理
 */

@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeCourseSignStudentController {
    @Autowired
    private YeeCourseSignStudentService yeeCourseSignStudentService;

    @PostMapping("/yee_course_sign_student_list")
    public Result list(@RequestBody YeeCourseSignStudentDTO dto) throws Exception {
        return yeeCourseSignStudentService.list(dto);
    }

    @PostMapping("/yee_course_sign_student_add")
    public Result add(@RequestParam Integer schoolId,
                      @RequestParam Integer courseId,
                      @RequestParam Integer studentId) throws Exception {
        return yeeCourseSignStudentService.add(schoolId, courseId, studentId);
    }

    @GetMapping("/yee_course_sign_student_delete")
    public Result delete(@RequestParam Integer schoolId,
                         @RequestParam Integer id) throws Exception {
        return yeeCourseSignStudentService.delete(schoolId, id);
    }

    @PostMapping("/yee_course_sign_student_join")
    public Result join(@RequestParam Integer schoolId,
                       @RequestParam long courseId,
                       @RequestParam long classId,
                       @RequestParam List<Long> studentIds) throws Exception {
        return yeeCourseSignStudentService.join(schoolId, courseId, classId, studentIds);
    }

    @PostMapping("/yee_course_sign_student_exit")
    public Result exit(@RequestParam Integer schoolId,
                       @RequestParam long courseId,
                       @RequestParam long classId,
                       @RequestParam List<Long> studentIds) throws Exception {
        return yeeCourseSignStudentService.exit(schoolId, courseId, classId, studentIds);
    }
}
