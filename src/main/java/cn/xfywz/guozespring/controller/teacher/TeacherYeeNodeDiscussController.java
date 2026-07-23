package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeNodeDiscuss;
import cn.xfywz.guozespring.service.teacher.YeeNodeDiscussService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class TeacherYeeNodeDiscussController {

    @Autowired
    private YeeNodeDiscussService yeeNodeDiscussService;


    // 添加章节讨论评论（教师端）
    @PostMapping("/yee_node_discuss_add")
    public Result add(@RequestBody YeeNodeDiscuss yeeNodeDiscuss ) throws Exception{
        return yeeNodeDiscussService.add(yeeNodeDiscuss);
    }

    // 更新章节讨论评论内容（教师端）
    @PostMapping("/yee_node_discuss_update")
    public Result update(@RequestParam int id,
                         @RequestParam int schoolId,
                         @RequestParam String content) throws Exception {
        return yeeNodeDiscussService.update(id, schoolId, content);
    }

    // 删除章节讨论评论（教师端）
    @GetMapping("/yee_node_discuss_delete")
    public Result delete(@RequestParam int id,
                         @RequestParam int schoolId) throws Exception {
        return yeeNodeDiscussService.delete(id, schoolId);
    }

    // 节点讨论点赞/取消点赞（教师端）
    @PostMapping("/yee_node_reply_like")
    public Result discussReplyLike(@RequestParam int id,
                                   @RequestParam int schoolId,
                                   @RequestParam int userId) throws Exception {
        return yeeNodeDiscussService.yeeNodeReplyLike(id, schoolId, userId);
    }

    // 节点讨论列表（教师端）
    @GetMapping("/node_discuss_list")
    public Result list(@RequestParam int nodeId,
                       @RequestParam int schoolId,
                       @RequestParam int userId,
                       @RequestParam int pageNum,
                       @RequestParam int pageSize) throws Exception {
        return yeeNodeDiscussService.discussList(pageNum, pageSize, schoolId, userId, nodeId);
    }

}
