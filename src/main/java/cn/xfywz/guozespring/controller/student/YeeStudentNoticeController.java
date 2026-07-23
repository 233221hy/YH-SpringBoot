package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.service.teacher.YeeNoticeService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user")
public class YeeStudentNoticeController {

    @Autowired
    private YeeNoticeService yeeNoticeService;

    @GetMapping("/yee_notice_list")
    public Result selectList(@RequestParam Integer schoolId, 
                             @RequestParam long studentId, 
                             @RequestParam(defaultValue = "1") int pageNum,
                             @RequestParam(defaultValue = "10") int pageSize,
                             @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeNoticeService.studentSelect(schoolId,studentId,pageSize,pageNum);
        } else {
            return Result.error("非法访问");
        }
    }

    //消息详细信息
    @GetMapping("/yee_notice_select")
    public Result select(@RequestParam Integer schoolId,
                         @RequestParam long noticeId,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeNoticeService.selectById(schoolId, noticeId);
        } else {
            return Result.error("非法访问");
        }
    }


}
