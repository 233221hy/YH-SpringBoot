package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.entity.mhsch.YeeSignInRecord;
import cn.xfywz.guozespring.service.student.YeeSignInRecordService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 学生端签到记录控制类
 */
@RestController
@RequestMapping("/user")
public class YeeSignInRecordController {

    @Autowired
    private YeeSignInRecordService yeeSignInRecordService;

    @PostMapping("/sign_in_record_add")
    public Result add(@RequestBody YeeSignInRecord param, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) param.getSchoolId())){
            yeeSignInRecordService.add(param);
            return Result.success();
        }else return Result.error("非法请求");
    }


}
