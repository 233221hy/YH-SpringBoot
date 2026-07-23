package cn.xfywz.guozespring.controller.admin;

import cn.xfywz.guozespring.entity.mhmain.SlManage;
import cn.xfywz.guozespring.service.admin.CourseService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.GetOutIpUtil;
import cn.xfywz.guozespring.util.JwtTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    private CourseService courseService;
    @GetMapping("/success")
    public Result success() {
        return Result.success();
    }
    @GetMapping("/successMsg")
    public Result success(@RequestParam String msg) {
        return Result.success(msg);
    }
    @GetMapping("/error")
    public Result error() {
        return Result.error();
    }
    @GetMapping("/errorMsg")
    public Result error(String msg) {
        return Result.error(msg);
    }
    @GetMapping("/pagetest")
    public Result pagetest(@RequestParam int PageSize, @RequestParam int PageNum) {
        return courseService.AllList(PageSize,PageNum);
    }
    @GetMapping("/testnode")
    public Result testnode(@RequestParam int id) {
        return courseService.selectCourseNode(id);
    }


    @GetMapping("/parseToken")
    public Result parseToken(@RequestParam String token) throws Exception {
        return Result.success(JwtTokenUtil.parseToken(token));
    }
    @GetMapping("/getip")
    public Result getIp() {
        return Result.success(GetOutIpUtil.getOutIp());
    }

    @GetMapping("/authtoken")
    public Result authToken(@RequestParam String token,Integer schoolId) throws Exception {
        return Result.success(AuthTokenUtil.verifyToken(token,schoolId));
    }
}