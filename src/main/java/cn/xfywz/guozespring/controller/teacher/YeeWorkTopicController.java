package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.mhsch.YeeWorkTopic;
import cn.xfywz.guozespring.service.teacher.YeeWorkTopicService;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: ChengLin
 * 作业题目 yee_work_topic
 */
@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeWorkTopicController {

    @Autowired
    private YeeWorkTopicService yeeWorkTopicService;

    @GetMapping("/yee_work_topic_list")
    public Result selectAll(@RequestParam int schoolId,
                            @RequestParam Integer pageSize,
                            @RequestParam Integer pageNum,
                            @RequestParam Integer workId,
                            @RequestParam(required = false) String topic,
                            @RequestParam(required = false) Integer type,
                            @RequestParam(required = false) Integer level,
                            @RequestParam(required = false) Integer cateBid,
                            @RequestParam(required = false) Integer cateMid) throws Exception {
        return yeeWorkTopicService.selectAll(schoolId, pageSize, pageNum, workId, topic, type, level, cateBid, cateMid);
    }

    @PostMapping("/yee_work_topic_add")
    public Result add(@RequestBody YeeWorkTopic yeeWorkTopic) throws Exception {
        return yeeWorkTopicService.add(yeeWorkTopic);
    }

    @PostMapping("/yee_work_topic_update")
    public Result update(@RequestBody YeeWorkTopic yeeWorkTopic) throws Exception {
        return yeeWorkTopicService.update(yeeWorkTopic);
    }

    @PostMapping("/yee_work_topic_delete")
    public Result delete(@RequestParam int schoolId,
                         @RequestParam int id) throws Exception {
        return yeeWorkTopicService.delete(schoolId, id);
    }

    @GetMapping("/yee_work_topic_getById")
    public Result getById(@RequestParam int schoolId,
                          @RequestParam int id) throws Exception {
        return yeeWorkTopicService.getById(schoolId, id);
    }

    @GetMapping("/yee_work_topic_sortByNumber")
    public Result sortByNumber(@RequestParam int schoolId,
                               @RequestParam int id1,
                               @RequestParam int id2,
                               @RequestParam int number1,
                               @RequestParam int number2) throws Exception {
        return yeeWorkTopicService.sortByNumber(schoolId, id1, id2, number1, number2);
    }

    /**
     * 导出试题数据为 Excel
     */
    @GetMapping("/yee_work_topic_export_excel")
    public void exportQuestions(HttpServletResponse response,
                                @RequestParam Integer schoolId,
                                @RequestParam(required = false) String topic,
                                @RequestParam(required = false) Integer createId,
                                @RequestParam(required = false) Integer type,
                                @RequestParam(required = false) Integer level,
                                @RequestParam(required = false) Integer cateBid,
                                @RequestParam(required = false) Integer cateMid,
                                @RequestParam Integer workId) throws Exception {
        yeeWorkTopicService.exportQuestions(response, schoolId, topic, createId, type, level, cateBid, cateMid, workId);
    }
}