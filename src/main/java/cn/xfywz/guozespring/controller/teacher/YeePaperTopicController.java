package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeePaperTopic;
import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import cn.xfywz.guozespring.service.teacher.YeePaperTopicService;
import cn.xfywz.guozespring.service.teacher.YeeQuestionService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.http.parser.Authorization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author: ChengLin
 * 试卷题目 yee_paper_topic
 */
@RestController
@RequestMapping("/school")
public class YeePaperTopicController {

    @Autowired
    private YeePaperTopicService yeePaperTopicService;
    @GetMapping("/yee_paper_topic_list")
    public Result selectAll(@RequestParam int schoolId,
                            @RequestParam Integer pageSize,
                            @RequestParam Integer pageNum,
                            @RequestParam Integer paperId,
                            @RequestParam(required = false) String topic,
                            @RequestParam(required = false) Integer type,
                            @RequestParam(required = false) Integer level,
                            @RequestParam(required = false) Integer cateBid,
                            @RequestParam(required = false) Integer cateMid,
                            @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeePaperTopicService.selectAll(schoolId, pageSize, pageNum, paperId, topic, type, level, cateBid, cateMid);
        }else return Result.error("非法访问");
    }
    @PostMapping("/yee_paper_topic_add")
    public Result add(@RequestBody YeePaperTopic yeePaperTopic , @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, (int)yeePaperTopic.getSchoolId())){
            return yeePaperTopicService.add(yeePaperTopic);
        }else return Result.error("非法访问");
    }
    @PostMapping("/yee_paper_topic_update")
    public Result update(@RequestBody YeePaperTopic yeePaperTopic , @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, (int)yeePaperTopic.getSchoolId())){
            return yeePaperTopicService.update(yeePaperTopic);
        }else return Result.error("非法访问");
    }
    @PostMapping("/yee_paper_topic_delete")
    public Result delete(int schoolId, int id , @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeePaperTopicService.delete(schoolId, id);
        }else return Result.error("非法访问");
    }

    @GetMapping("/yee_paper_topic_getById")
    public Result getById(int schoolId, int id , @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeePaperTopicService.getById(schoolId, id);
        }else return Result.error("非法访问");
    }

    @GetMapping("/yee_paper_topic_sortByNumber")
    public Result sortByNumber(@RequestParam int schoolId,
                               @RequestParam int id1,
                               @RequestParam int id2,
                               @RequestParam int number1,
                               @RequestParam int number2,
                               @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeePaperTopicService.sortByNumber(schoolId, id1, id2, number1, number2);
        }else return Result.error("非法访问");
    }

    /**
     * 导出试题数据为Excel
     */
    @GetMapping("/yee_paper_topic_export")
    public void exportQuestions(HttpServletResponse response,
                                @RequestParam Integer schoolId,
                                @RequestParam(required = false) String topic,
                                @RequestParam(required = false) Integer createId,
                                @RequestParam(required = false) Integer type,
                                @RequestParam(required = false) Integer level,
                                @RequestParam(required = false) Integer cateBid,
                                @RequestParam(required = false) Integer cateMid,
                                @RequestParam Integer paperId,
                                @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            yeePaperTopicService.exportQuestions(response, schoolId, topic, createId, type, level, cateBid, cateMid, paperId);
        } else {
            response.setStatus(403);
            response.getWriter().write("非法访问");
        }
    }

    /**
     * 从Excel导入试题数据
     */
    @PostMapping("/yee_paper_topic_import")
    public Result importQuestions(@RequestParam Integer schoolId,
                                  @RequestParam Integer createId,
                                  @RequestParam("file") MultipartFile file,
                                  @RequestParam Integer cateBid,
                                  @RequestParam Integer cateMid,
                                  @RequestParam Integer paperId,
                                  @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeePaperTopicService.importQuestions(schoolId, createId, file, cateBid, cateMid, paperId);
        } else {
            return Result.error("非法访问");
        }
    }


}
