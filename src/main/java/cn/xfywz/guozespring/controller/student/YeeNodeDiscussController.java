package cn.xfywz.guozespring.controller.student;


import cn.xfywz.guozespring.entity.mhsch.YeeNodeDiscuss;
import cn.xfywz.guozespring.service.teacher.NodeDiscussStatsService;
import cn.xfywz.guozespring.service.teacher.YeeNodeDiscussService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 节点讨论
 * @TableName yee_node_discuss
 */
@RestController
@RequestMapping("/user")
public class YeeNodeDiscussController {

    @Autowired
    private YeeNodeDiscussService yeeNodeDiscussService;

    // 添加章节讨论评论
    @PostMapping("/yee_node_discuss_add")
    public Result add(@RequestHeader String Authorization, @RequestBody YeeNodeDiscuss yeeNodeDiscuss ) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, yeeNodeDiscuss.getSchoolId())) {
            return yeeNodeDiscussService.add(yeeNodeDiscuss);
        } else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_node_discuss_update")
    public Result update(@RequestHeader String Authorization,int id,  int schoolId, String content) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeNodeDiscussService.update(id,schoolId,content);
        } else {
            return Result.error("非法访问");
        }
    }

    //删除课程章节讨论评论
    @GetMapping("/yee_node_discuss_delete")
    public Result delete(@RequestParam int id,
                         @RequestParam int schoolId,
                         @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeNodeDiscussService.delete(id, schoolId);
        } else {
            return Result.error("非法访问");
        }
    }

    //节点讨论点赞
    @PostMapping("/yee_node_reply_like")
    public Result discussReplyLike(@RequestParam int id,
                                   @RequestParam int schoolId,
                                   @RequestParam int userId,
                                   @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeNodeDiscussService.yeeNodeReplyLike(id, schoolId, userId);
        } else {
            return Result.error("非法访问");
        }
    }


    @GetMapping("/node_discuss_list")
    public Result list(@RequestParam int nodeId,
                       @RequestParam int schoolId,
                       @RequestParam int userId,
                       @RequestParam int pageNum,
                       @RequestParam int pageSize,
                       @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization,schoolId)) {
            return yeeNodeDiscussService.discussList( pageNum,  pageSize,  schoolId,  userId, nodeId);
        } else return Result.error("非法访问");
    }


}
