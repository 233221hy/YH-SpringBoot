package cn.xfywz.guozespring.controller.student;


import cn.xfywz.guozespring.entity.mhsch.FileInfo;
import cn.xfywz.guozespring.service.student.YeeStudentCourseService;
import cn.xfywz.guozespring.service.student.YeeStudentCourseWorkService;
import cn.xfywz.guozespring.service.teacher.YeeWorkService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class YeeStudentCourseWorkController {

    @Autowired
    private YeeStudentCourseWorkService yeeStudentCourseWorkService;

    @Autowired
    private YeeWorkService yeeWorkService;

    /**
     * 作业列表
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param nodeId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_course_student_work_list")
    public Result selectRecordAll(@RequestParam int schoolId,
                                  @RequestParam Integer courseId,
                                  @RequestParam Integer studentId,
                                  @RequestParam(required = false) Integer nodeId,
                                  @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseWorkService.selectStudentWorkList(schoolId, courseId, studentId, nodeId);
        } else return Result.error("非法访问");
    }

    /**
     * 作业详情
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param workId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_course_student_work_detail")
    public Result selectRecordDetail(@RequestParam int schoolId,
                                     @RequestParam Integer courseId,
                                     @RequestParam Integer studentId,
                                     @RequestParam Integer workId,
                                     @RequestParam String title,
                                     @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseWorkService.selectStudentWorkDetail(schoolId, courseId, studentId, workId, title);
        } else return Result.error("非法访问");
    }


    /**
     * 作业开始答题
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param workId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_course_student_work_start")
    public Result startWork(@RequestParam int schoolId,
                            @RequestParam Integer courseId,
                            @RequestParam Integer studentId,
                            @RequestParam Integer workId,
                            @RequestParam Integer createUserId,
                            @RequestParam String platform,
                            @RequestParam Integer classId,
                            @RequestParam Integer paperId,
                            @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseWorkService.startWork(schoolId, courseId, studentId, workId, createUserId, platform, classId, paperId);
        } else return Result.error("非法访问");
    }

    /**
     * 提交 单选,多选,判断 题目答案
     * @return
     * @throws Exception
     */
    @PostMapping("/yee_work_answer_add")
    public Result addWorkAnswer(@RequestBody Map params, @RequestHeader String Authorization) throws Exception {
        int schoolId = (int) params.get("schoolId");
        Integer courseId = (Integer) params.get("courseId");
        Integer userId = (Integer) params.get("userId");
        List<String> answer = (List<String>) params.get("answer");
        Integer topicId = (Integer) params.get("topicId");
        Integer workId = (Integer) params.get("workId");
        Integer recordId = (Integer) params.get("recordId");
        Integer type = (Integer) params.get("type");
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseWorkService.addWorkAnswer(schoolId, courseId, userId, answer, topicId, workId, recordId, type);
        }else return Result.error("非法访问");
    }

    /**
     * 提交 简答题 题目答案
     * @return
     * @throws Exception
     */
    @PostMapping("/yee_work_answer_addText")
    public Result addWorkAnswerText(@RequestBody Map params, @RequestHeader String Authorization) throws Exception {
        int schoolId = (int) params.get("schoolId");
        Integer courseId = (Integer) params.get("courseId");
        Integer userId = (Integer) params.get("userId");
        String answer = (String) params.get("answer");
        Integer topicId = (Integer) params.get("topicId");
        Integer workId = (Integer) params.get("workId");
        Integer recordId = (Integer) params.get("recordId");
        Integer type = (Integer) params.get("type");
        List<FileInfo> images = (List<FileInfo>) params.get("images");
        List<FileInfo> files = (List<FileInfo>) params.get("files");
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseWorkService.addWorkAnswerText(schoolId, courseId, userId, answer, topicId, workId, recordId, type, images, files);
        }else return Result.error("非法访问");
    }

    /**
     * 提交 填空题 题目答案
     * @return
     * @throws Exception
     */
    @PostMapping("/yee_work_answer_addBlank")
    public Result addWorkAnswerBlank(@RequestBody Map params, @RequestHeader String Authorization) throws Exception {
        int schoolId = (int) params.get("schoolId");
        Integer courseId = (Integer) params.get("courseId");
        Integer userId = (Integer) params.get("userId");
        Map<String, String> answer = (Map<String, String>) params.get("answer");
        Integer topicId = (Integer) params.get("topicId");
        Integer workId = (Integer) params.get("workId");
        Integer recordId = (Integer) params.get("recordId");
        Integer type = (Integer) params.get("type");
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseWorkService.addWorkAnswerBlank(schoolId, courseId, userId, answer, topicId, workId, recordId, type);
        }else return Result.error("非法访问");
    }

    /**
     * 完成答题
     * @param schoolId
     * @param courseId
     * @param workId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_work_answer_finish")
    public Result finishWorkAnswer(@RequestParam int schoolId,
                                @RequestParam Integer courseId,
                                @RequestParam Integer userId,
                                @RequestParam Integer workId,
                                @RequestParam Integer recordId,
                                @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseWorkService.finishWorkAnswer(schoolId, courseId, userId, workId, recordId);
        }else return Result.error("非法访问");
    }


    /**
     * 再来一遍答题
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param workId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_course_student_work_restart")
    public Result reStartWork(@RequestParam int schoolId,
                            @RequestParam Integer courseId,
                            @RequestParam Integer studentId,
                            @RequestParam Integer workId,
                            @RequestParam Integer createUserId,
                            @RequestParam String platform,
                            @RequestParam Integer classId,
                            @RequestParam Integer paperId,
                            @RequestParam Integer recordId,
                            @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseWorkService.reStartWork(schoolId, courseId, studentId, workId, createUserId, platform, classId, paperId, recordId);
        } else return Result.error("非法访问");
    }

    @GetMapping("/yee_work_record_consult_list")
    public Result selectWorkRecordConsult(@RequestParam int schoolId, @RequestParam Integer userId, @RequestParam Integer workId, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeWorkService.selectWorkRecordConsult(schoolId, userId, workId);
        } else return Result.error("非法访问");
    }

    /**
     * 收藏题目
     * @param schoolId
     * @param topicId
     * @param workId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_collection_topic_add")
    public Result addCollectionTopic(@RequestParam int schoolId,
                                     @RequestParam Integer userId,
                                     @RequestParam Integer workId,
                                     @RequestParam Integer topicId,
                                     @RequestParam Integer courseId,
                                     @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseWorkService.addCollectionTopic(schoolId, userId, workId, topicId, courseId);
        } else return Result.error("非法访问");
    }

    /**
     * 取消收藏题目
     * @param schoolId
     * @param userId
     * @param workId
     * @param topicId
     * @param courseId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_collection_topic_delete")
    public Result deleteCollectionTopic(@RequestParam int schoolId,
                                        @RequestParam Integer userId,
                                        @RequestParam Integer workId,
                                        @RequestParam Integer topicId,
                                        @RequestParam Integer courseId,
                                        @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseWorkService.deleteCollectionTopic(schoolId, userId, workId, topicId, courseId);
        } else return Result.error("非法访问");
    }


}
