package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.service.student.YeeWorkEvaluationService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class YeeWorkEvaluationController {

    @Autowired
    private YeeWorkEvaluationService yeeWorkEvaluationService;

    // 获取我的评卷列表 （进行中/已完成）
    @GetMapping("/yee_work_evaluation_list")
    public Result selectAll(@RequestParam Integer schoolId,
                            @RequestParam Integer studentId,
                            @RequestParam Integer type,
                            @RequestParam Integer pageSize,
                            @RequestParam Integer pageNum,
                            @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeeWorkEvaluationService.selectList(schoolId,studentId,type, pageSize, pageNum);
        }else return Result.error("非法访问");
    }

    //评卷详情
    @GetMapping("/yee_work_evaluation_detail")
    public Result detail(@RequestParam int schoolId,
                         @RequestParam int evaluationId,
                         @RequestHeader String Authorization) throws Exception {
        if (Authorization != null) {
            return yeeWorkEvaluationService.selectById(schoolId,evaluationId);
        } else return Result.error("非法访问");
    }
}
