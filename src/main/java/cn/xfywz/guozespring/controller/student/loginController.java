package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.service.student.LoginService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class loginController {
    @Autowired
    private LoginService loginService;
    @GetMapping("/login")
    public Result login(@RequestParam String number, @RequestParam String password, @RequestParam Integer schoolId) throws Exception {
        if (schoolId == null) {
            return Result.error("schoolId 不能为空");
        }
        return loginService.login(number,password,schoolId);
    }

    @GetMapping("/logout")
    public Result logout(@RequestParam String number, @RequestHeader String Authorization) throws Exception {
        return loginService.logout(number);
    }

}
