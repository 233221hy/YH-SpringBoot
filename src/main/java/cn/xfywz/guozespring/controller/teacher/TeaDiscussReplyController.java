package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeDiscussReply;
import cn.xfywz.guozespring.service.student.YeeDiscussReplyService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.JwtTokenUtil;
import cn.xfywz.guozespring.util.Result;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/school")
public class TeaDiscussReplyController {

    @Autowired
    private YeeDiscussReplyService yeeNodeDiscussService;

    @PostMapping("/yee_discuss_reply_add")
    public Result add(@RequestHeader String Authorization, @RequestBody YeeDiscussReply yeeDiscussReply ) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, yeeDiscussReply.getSchoolId())) {
            Claims claims = JwtTokenUtil.parseToken(Authorization);
            JSONObject sub = JSON.parseObject(claims.getSubject());
            Integer userType; // 1=学生, 2=老师
            // 判断身份
            if (sub.containsKey("role")) {
                userType = 2;    // 老师
            } else {
                userType = 1;    // 学生
            }
            return yeeNodeDiscussService.add(yeeDiscussReply, userType);
        } else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_discuss_reply_update")
    public Result update(@RequestHeader String Authorization,int id,  int schoolId, String content) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeNodeDiscussService.update(id,schoolId,content);
        } else {
            return Result.error("非法访问");
        }
    }

    //删除课程主题讨论评论
    @GetMapping("/yee_discuss_reply_delete")
    public Result delete(@RequestParam int id,
                         @RequestParam int schoolId,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            Claims claims = JwtTokenUtil.parseToken(Authorization);
            JSONObject sub = JSON.parseObject(claims.getSubject());

            Integer userId = sub.getInteger("id");
            if (userId == null || userId <= 0) {
                return Result.error("用户信息无效");
            }

            Integer userType;
            if (sub.containsKey("role")) {
                userType = 2; // 老师
            } else {
                userType = 1; // 学生
            }

            return yeeNodeDiscussService.delete(id, schoolId, userId, userType);
        } else {
            return Result.error("非法访问");
        }
    }

    //课程主题讨论评论点赞
    @PostMapping("/yee_discuss_reply_like")
    public Result discussReplyLike(@RequestParam int replyId,
                                   @RequestParam int schoolId,
                                   @RequestParam int userId,
                                   @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeNodeDiscussService.discussReplyLike(replyId, schoolId, userId);
        } else {
            return Result.error("非法访问");
        }
    }
}
