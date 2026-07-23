package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.entity.mhsch.YeeStudent;
import cn.xfywz.guozespring.service.student.YeeStudentMangerService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class YeeStudentMangerController {

    @Autowired
    private YeeStudentMangerService yeeStudentMangerService;

    //获取个人信息
    @GetMapping("/yee_student_info")

    public Result getInfo(@RequestHeader String Authorization) throws Exception {
        if (Authorization != null){
            return Result.success(yeeStudentMangerService.getInfo(Authorization));
        }
        return Result.error("非法访问");
    }
    //修改个人信息
    @PostMapping("/yee_student_info_update")
    public Result StudentInfoUpdate(@RequestHeader String Authorization, @RequestBody YeeStudent yeeStudent) throws Exception {
        if (Authorization != null){
            return Result.success(yeeStudentMangerService.studentInfoUpdate(Authorization,yeeStudent));
        }
        return Result.error("非法访问");
    }

    //修改学生手机号
    @PostMapping("/yee_student_info_update_mobile")
    public Result updatePhone(String mobile,@RequestHeader String Authorization) throws Exception {
        if (Authorization != null){
            return Result.success(yeeStudentMangerService.updatePhone(mobile,Authorization));
        }
        return Result.error("非法访问");
    }

    //修改学生邮箱
    @PostMapping("/yee_student_info_update_email")
    public Result updateEmail(String email,@RequestHeader String Authorization) throws Exception {
        if (Authorization != null){
            return Result.success(yeeStudentMangerService.updateEmail(email,Authorization));
        }
        return Result.error("非法访问");
    }

    //修改学生密码
    @PostMapping("/yee_student_update_password")
    public Result InfoUpdatePassword(@RequestHeader String Authorization, String oldPassword, String newPassword) throws Exception {
        if (Authorization != null){
            return Result.success(yeeStudentMangerService.infoUpdatePassword(oldPassword,newPassword,Authorization));
        }
        return Result.error("非法访问");
    }
}
