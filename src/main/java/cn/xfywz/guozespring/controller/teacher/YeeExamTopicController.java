package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeExamTopic;
import cn.xfywz.guozespring.service.teacher.YeeExamTopicService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: ChengLin
 * 考试题目 yee_exam_topic
 */
@RestController
@RequestMapping("/school")
public class YeeExamTopicController {

    @Autowired
    private YeeExamTopicService yeeExamTopicService;

    @GetMapping("/yee_exam_topic_list")
    public Result selectAll(@RequestParam int schoolId,
                            @RequestParam Integer pageSize,
                            @RequestParam Integer pageNum,
                            @RequestParam Integer examId,
                            @RequestParam(required = false) String topic,
                            @RequestParam(required = false) Integer type,
                            @RequestParam(required = false) Integer level,
                            @RequestParam(required = false) Integer cateBid,
                            @RequestParam(required = false) Integer cateMid,
                            @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamTopicService.selectAll(schoolId, pageSize, pageNum, examId, topic, type, level, cateBid, cateMid);
        } else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_exam_topic_add")
    public Result add(@RequestBody YeeExamTopic yeeExamTopic, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) yeeExamTopic.getSchoolId())) {
            return yeeExamTopicService.add(yeeExamTopic);
        } else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_exam_topic_update")
    public Result update(@RequestBody YeeExamTopic yeeExamTopic, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) yeeExamTopic.getSchoolId())) {
            return yeeExamTopicService.update(yeeExamTopic);
        } else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_exam_topic_delete")
    public Result delete(int schoolId, int id, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamTopicService.delete(schoolId, id);
        } else {
            return Result.error("非法访问");
        }
    }

    @GetMapping("/yee_exam_topic_getById")
    public Result getById(int schoolId, int id, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamTopicService.getById(schoolId, id);
        } else {
            return Result.error("非法访问");
        }
    }

    @GetMapping("/yee_exam_topic_sortByNumber")
    public Result sortByNumber(@RequestParam int schoolId,
                               @RequestParam int id1,
                               @RequestParam int id2,
                               @RequestParam int number1,
                               @RequestParam int number2,
                               @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamTopicService.sortByNumber(schoolId, id1, id2, number1, number2);
        } else {
            return Result.error("非法访问");
        }
    }
}
