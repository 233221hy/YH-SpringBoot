package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.entity.mhsch.YeeQuestion;
import cn.xfywz.guozespring.service.teacher.QuestionExportService;
import cn.xfywz.guozespring.service.teacher.YeeQuestionService;
import cn.xfywz.guozespring.service.teacher.impl.QuestionExportServiceImpl;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Author: ChengLin
 * 试题题库 yee_question
 */
@RestController
@RequestMapping("/school")
public class YeeQuestionController {

    @Autowired
    private YeeQuestionService yeeQuestionService;
    
    @Autowired
    private QuestionExportService questionExportService;
    @GetMapping("/yee_question_list")
    public Result selectAll(@RequestParam int schoolId,
                            @RequestParam(required = false) Integer pageSize,
                            @RequestParam(required = false) Integer pageNum,
                            @RequestParam(required = false) String topic,
                            @RequestParam(required = false) Integer createId,
                            @RequestParam(required = false) String creatorName,
                            @RequestParam(required = false) Integer type,
                            @RequestParam(required = false) Integer level,
                            @RequestParam(required = false) Integer cateBid,
                            @RequestParam(required = false) Integer cateMid,
                            @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeeQuestionService.selectAll(schoolId, pageSize, pageNum, topic, createId, creatorName, type, level, cateBid, cateMid);
        }else return Result.error("非法访问");
    }
    @PostMapping("/yee_question_add")
    public Result add(@RequestBody YeeQuestion yeeQuestion , @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, (int)yeeQuestion.getSchoolId())){
            return yeeQuestionService.add(yeeQuestion);
        }else return Result.error("非法访问");
    }
    @PostMapping("/yee_question_update")
    public Result update(@RequestBody YeeQuestion yeeQuestion , @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, (int)yeeQuestion.getSchoolId())){
            return yeeQuestionService.update(yeeQuestion);
        }else return Result.error("非法访问");
    }
    @PostMapping("/yee_question_delete")
    public Result delete(int schoolId, int id , @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeeQuestionService.delete(schoolId, id);
        }else return Result.error("非法访问");
    }
    
    /**
     * 批量删除试题
     */
    @PostMapping("/yee_question_batch_delete")
    public Result batchDelete(@RequestParam int schoolId,
                              @RequestParam List<Integer> ids,
                              @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeeQuestionService.batchDelete(schoolId, ids);
        }else return Result.error("非法访问");
    }

    @GetMapping("/yee_question_getById")
    public Result getById(int schoolId, int id , @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)){
            return yeeQuestionService.getById(schoolId, id);
        }else return Result.error("非法访问");
    }

    /**
     * 导出试题数据为Excel
     */
    @GetMapping("/yee_question_export")
    public void exportQuestions(HttpServletResponse response,
                               @RequestParam Integer schoolId,
                               @RequestParam(required = false) String topic,
                               @RequestParam(required = false) Integer createId,
                               @RequestParam(required = false) Integer type,
                               @RequestParam(required = false) Integer level,
                               @RequestParam(required = false) Integer cateBid,
                               @RequestParam(required = false) Integer cateMid,
                               @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            questionExportService.exportQuestions(response, schoolId, topic, createId, type, level, cateBid, cateMid);
        } else {
            response.setStatus(403);
            response.getWriter().write("非法访问");
        }
    }

    /**
     * 从Excel导入试题数据
     */
    @PostMapping("/yee_question_import")
    public Result importQuestions(@RequestParam Integer schoolId,
                                 @RequestParam Integer createId,
                                 @RequestParam("file") MultipartFile file,
                                 @RequestParam Integer cateBid,
                                 @RequestParam Integer cateMid,
                                 @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeQuestionService.importQuestions(schoolId, createId, file, cateBid, cateMid);
        } else {
            return Result.error("非法访问");
        }
    }

}
