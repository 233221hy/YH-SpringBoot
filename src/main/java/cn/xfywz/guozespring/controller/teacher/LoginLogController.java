package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.vo.LoginLog;
import cn.xfywz.guozespring.service.teacher.LoginLogService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class LoginLogController {

    @Autowired
    private LoginLogService loginLogService;

    // 学生登录日志
    @PostMapping("/login_log_student_list")
    public Result studentList(@RequestBody LoginLog param) throws Exception {
        return loginLogService.studentList(param);
    }

    // 老师登录日志
    @PostMapping("/login_log_teacher_list")
    public Result teacherList(@RequestBody LoginLog param) throws Exception {
        return loginLogService.teacherList(param);
    }
}
