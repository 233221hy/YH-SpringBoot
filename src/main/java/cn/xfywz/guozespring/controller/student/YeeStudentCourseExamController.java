package cn.xfywz.guozespring.controller.student;


import cn.xfywz.guozespring.entity.mhsch.FileInfo;
import cn.xfywz.guozespring.service.student.YeeStudentCourseExamService;
import cn.xfywz.guozespring.service.student.YeeStudentCourseService;
import cn.xfywz.guozespring.service.teacher.YeeExamService;
import cn.xfywz.guozespring.service.teacher.YeeWorkService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class YeeStudentCourseExamController {

    @Autowired
    private YeeStudentCourseExamService yeeStudentCourseExamService;

    @Autowired
    private YeeExamService yeeExamService;


    /**
     * 考试列表
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param nodeId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_course_student_exam_list")
    public Result selectExamRecordAll(@RequestParam int schoolId,
                                  @RequestParam Integer courseId,
                                  @RequestParam Integer studentId,
                                  @RequestParam(required = false) Integer nodeId,
                                  @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseExamService.selectStudentExamList(schoolId, courseId, studentId, nodeId);
        } else return Result.error("非法访问");
    }



    /**
     * 考试详情
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param examId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_course_student_exam_detail")
    public Result selectExamRecordDetail(@RequestParam int schoolId,
                                     @RequestParam Integer courseId,
                                     @RequestParam Integer studentId,
                                     @RequestParam Integer examId,
                                     @RequestParam String title,
                                     @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseExamService.selectStudentExamDetail(schoolId, courseId, studentId, examId, title);
        } else return Result.error("非法访问");
    }


    /**
     * 考试开始答题
     * @param schoolId
     * @param courseId
     * @param studentId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_course_student_exam_start")
    public Result startExam(@RequestParam int schoolId,
                            @RequestParam Integer courseId,
                            @RequestParam Integer studentId,
                            @RequestParam Integer examId,
                            @RequestParam Integer createUserId,
                            @RequestParam String platform,
                            @RequestParam Integer classId,
                            @RequestParam Integer paperId,
                            @RequestParam(required = false) Integer random,
                            @RequestParam(required = false) String randData,
                            @RequestParam(required = false) Integer randNumber,
                            @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeStudentCourseExamService.startExam(schoolId, courseId, studentId, examId, createUserId, platform, classId, paperId, random, randData, randNumber);
        } else return Result.error("非法访问");
    }


    /**
     * 提交 单选,多选,判断 题目答案
     * @return
     * @throws Exception
     */
    @PostMapping("/yee_exam_answer_add")
    public Result addWorkAnswer(@RequestBody Map params, @RequestHeader String Authorization) throws Exception {
        try {
            int schoolId = getIntParam(params, "schoolId");
            Integer courseId = getIntegerParam(params, "courseId");
            Integer userId = getIntegerParam(params, "userId");
            List<String> answer = getListParam(params, "answer");
            Integer topicId = getIntegerParam(params, "topicId");
            Integer examId = getIntegerParam(params, "examId");
            Integer recordId = getIntegerParam(params, "recordId");
            Integer type = getIntegerParam(params, "type");
            log.info("提交客观题答案: schoolId={}, examId={}, recordId={}, topicId={}, type={}",
                    schoolId, examId, recordId, topicId, type);
            if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
                return yeeStudentCourseExamService.addExamAnswer(schoolId, courseId, userId, answer, topicId, examId, recordId, type);
            } else return Result.error("非法访问");
        } catch (Exception e) {
            log.error("提交客观题答案失败: params={}", params, e);
            return Result.error("提交答案失败：" + e.getMessage());
        }
    }

    /**
     * 提交 简答题 题目答案
     * @return
     * @throws Exception
     */
    @PostMapping("/yee_exam_answer_addText")
    public Result addWorkAnswerText(@RequestBody Map params, @RequestHeader String Authorization) throws Exception {
        try {
            int schoolId = getIntParam(params, "schoolId");
            Integer courseId = getIntegerParam(params, "courseId");
            Integer userId = getIntegerParam(params, "userId");
            String answer = (String) params.get("answer");
            Integer topicId = getIntegerParam(params, "topicId");
            Integer examId = getIntegerParam(params, "examId");
            Integer recordId = getIntegerParam(params, "recordId");
            Integer type = getIntegerParam(params, "type");
            List<FileInfo> images = getFileInfoListParam(params, "images");
            List<FileInfo> files = getFileInfoListParam(params, "files");
            log.info("提交主观题答案: schoolId={}, examId={}, recordId={}, topicId={}, type={}",
                    schoolId, examId, recordId, topicId, type);
            if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
                return yeeStudentCourseExamService.addExamAnswerText(schoolId, courseId, userId, answer, topicId, examId, recordId, type, images, files);
            } else return Result.error("非法访问");
        } catch (Exception e) {
            log.error("提交主观题答案失败: params={}", params, e);
            return Result.error("提交答案失败：" + e.getMessage());
        }
    }

    /**
     * 提交 填空题 题目答案
     * @return
     * @throws Exception
     */
    @PostMapping("/yee_exam_answer_addBlank")
    public Result addWorkAnswerBlank(@RequestBody Map params, @RequestHeader String Authorization) throws Exception {
        try {
            int schoolId = getIntParam(params, "schoolId");
            Integer courseId = getIntegerParam(params, "courseId");
            Integer userId = getIntegerParam(params, "userId");
            Map<String, String> answer = (Map<String, String>) params.get("answer");
            Integer topicId = getIntegerParam(params, "topicId");
            Integer examId = getIntegerParam(params, "examId");
            Integer recordId = getIntegerParam(params, "recordId");
            Integer type = getIntegerParam(params, "type");
            log.info("提交填空题答案: schoolId={}, examId={}, recordId={}, topicId={}, type={}",
                    schoolId, examId, recordId, topicId, type);
            if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
                return yeeStudentCourseExamService.addExamAnswerBlank(schoolId, courseId, userId, answer, topicId, examId, recordId, type);
            } else return Result.error("非法访问");
        } catch (Exception e) {
            log.error("提交填空题答案失败: params={}", params, e);
            return Result.error("提交答案失败：" + e.getMessage());
        }
    }

    /**
     * 完成答题
     * @param schoolId
     * @param courseId
     * @param examId
     * @param Authorization
     * @return
     * @throws Exception
     */
    @GetMapping("/yee_exam_answer_finish")
    public Result finishExamAnswer(@RequestParam int schoolId,
                                @RequestParam Integer courseId,
                                @RequestParam Integer userId,
                                @RequestParam Integer examId,
                                @RequestParam Integer recordId,
                                @RequestHeader String Authorization) throws Exception {
        log.info("交卷请求: schoolId={}, courseId={}, userId={}, examId={}, recordId={}",
                schoolId, courseId, userId, examId, recordId);
        try {
            if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
                return yeeStudentCourseExamService.finishExamAnswer(schoolId, courseId, userId, examId, recordId);
            } else return Result.error("非法访问");
        } catch (Exception e) {
            log.error("交卷失败: schoolId={}, examId={}, recordId={}", schoolId, examId, recordId, e);
            return Result.error("交卷失败：" + e.getMessage());
        }
    }

    @GetMapping("/yee_exam_record_consult_list")
    public Result selectWorkRecordConsult(@RequestParam int schoolId, @RequestParam Integer userId, @RequestParam Integer examId, @RequestParam(required = false) Integer courseId, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectWorkRecordConsult(schoolId, userId, examId, courseId);
        } else return Result.error("非法访问");
    }

    // ---- 安全参数提取 ----

    /** 从 Map 中安全提取 int 参数 */
    private int getIntParam(Map params, String key) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new IllegalArgumentException("缺少参数或类型错误: " + key + "=" + value);
    }

    /** 从 Map 中安全提取 Integer 参数，允许为 null */
    private Integer getIntegerParam(Map params, String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            return Integer.parseInt((String) value);
        }
        throw new IllegalArgumentException("参数类型错误: " + key + "=" + value);
    }

    @SuppressWarnings("unchecked")
    private List<String> getListParam(Map params, String key) {
        Object value = params.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        throw new IllegalArgumentException("缺少参数或类型错误: " + key + "=" + value);
    }

    @SuppressWarnings("unchecked")
    private List<FileInfo> getFileInfoListParam(Map params, String key) {
        Object value = params.get(key);
        if (value == null) return null;
        if (value instanceof List) {
            return (List<FileInfo>) value;
        }
        throw new IllegalArgumentException("参数类型错误: " + key + "=" + value);
    }

}
