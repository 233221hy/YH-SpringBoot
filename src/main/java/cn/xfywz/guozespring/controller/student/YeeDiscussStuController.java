package cn.xfywz.guozespring.controller.student;


import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.DiscussStatsDTO;
import cn.xfywz.guozespring.service.teacher.DiscussStatsService;
import cn.xfywz.guozespring.service.teacher.YeeDiscussService;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**-  新增学生端乐学圈相关接口（查询评论列表、添加主评论、附件上传、点赞/取消点赞、新增回复评论、删除评论），并测试完成
 -  获取学生个人信息接口修改
 -  主题讨论详情接口编写
 * 课程主题讨论
 * @TableName yee_discuss
 */

@RequireAuth
@RestController
@RequestMapping("/user")
public class YeeDiscussStuController {

    @Autowired
    private YeeDiscussService yeeDiscussService;
    @Autowired
    private DiscussStatsService discussStatsService;

    //课程主题讨论列表
    @GetMapping("/yee_discuss_list")
    public Result list(@RequestParam(defaultValue = "1") int pageNum,
                       @RequestParam(defaultValue = "10") int pageSize,
                       @RequestParam int schoolId,
                       @RequestParam long courseId) throws Exception {
        return yeeDiscussService.list(pageNum, pageSize, schoolId, courseId);
    }

    // 模糊查询课程主题讨论信息
    @GetMapping("/yee_discuss_like")
    public Result like(@RequestParam int schoolId,
                       @RequestParam long courseId,
                       @RequestParam String title) throws Exception {
        return yeeDiscussService.like(schoolId, courseId, title);
    }

    // 讨论详情列表：按用户维度展示该学生/老师的发表或回复
    @PostMapping("/discuss_stats_detail_list")
    public Result detailList(@RequestBody DiscussStatsDTO param) throws Exception {
        return discussStatsService.detailList(param);
    }


    @GetMapping("/yee_discuss_detail")
    public Result discussDetail(@RequestParam int schoolId,
                             @RequestParam long discussId,
                             @RequestParam int pageNum,
                             @RequestParam int pageSize) {
        return discussStatsService.discussDetail(schoolId, discussId, pageNum, pageSize);
    }
}
