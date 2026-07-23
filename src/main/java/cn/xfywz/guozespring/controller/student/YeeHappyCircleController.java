package cn.xfywz.guozespring.controller.student;

import cn.xfywz.guozespring.entity.mhsch.YeeHappyCircle;
import cn.xfywz.guozespring.service.teacher.YeeHappyCService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class YeeHappyCircleController {


    @Autowired
    private YeeHappyCService happyService;

    // 主评论列表（带回复数据，一次性获取）
    @GetMapping("/happy_list")
    public Result list(@RequestParam int pageNum,
                       @RequestParam int pageSize,
                       @RequestParam int schoolId,
                       @RequestParam(required = false, defaultValue = "0") long userId,
                       @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return happyService.list(pageNum, pageSize, schoolId, userId);
        }
        return Result.error("非法访问");
    }

    // 新增主评论
    @PostMapping("/happy_add")
    public Result add(@RequestBody YeeHappyCircle circle,
                      @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) circle.getSchoolId())) {
            return happyService.add(circle);
        }
        return Result.error("非法访问");
    }


    // 点赞（toggle，存在则取消，不存在则点赞，返回详细状态）
    @PostMapping("/happy_like_toggle")
    public Result likeToggle(@RequestParam int schoolId,
                             @RequestParam long replyId,
                             @RequestParam long userId,
                             @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return happyService.likeToggle(schoolId, replyId, userId);
        }
        return Result.error("非法访问");
    }

    // 新增回复（replyId 为主贴ID，或某条回复ID）
    @PostMapping("/happy_reply_add")
    public Result addReply(@RequestBody YeeHappyCircle reply,
                           @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) reply.getSchoolId())) {
            return happyService.addReply(reply);
        }
        return Result.error("非法访问");
    }

    // 删除（逻辑删除）
    @GetMapping("/happy_delete")
    public Result delete(@RequestParam long id,
                         @RequestParam int schoolId,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return happyService.delete(id, schoolId);
        }
        return Result.error("非法访问");
    }

}
