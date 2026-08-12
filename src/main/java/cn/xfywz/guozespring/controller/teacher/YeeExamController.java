package cn.xfywz.guozespring.controller.teacher;


import cn.xfywz.guozespring.entity.dto.YeeExamExportDTO;
import cn.xfywz.guozespring.entity.file.AsyncQueryTask;
import cn.xfywz.guozespring.entity.mhsch.YeeExam;
import cn.xfywz.guozespring.service.file.ExamTopicAsyncService;
import cn.xfywz.guozespring.service.student.YeeStudentCourseExamService;
import cn.xfywz.guozespring.service.teacher.YeeExamService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: ChengLin
 * yee_work
 */
@RestController
@RequestMapping("/school")
public class YeeExamController {

    private static final Logger log = LoggerFactory.getLogger(YeeExamController.class);

    @Autowired
    private YeeExamService yeeExamService;
    @Autowired
    private YeeStudentCourseExamService yeeStudentCourseExamService;
    @Resource
    private ExamTopicAsyncService examTopicAsyncService;

    /**
     * 提交【异步导出PDF/ZIP】任务 立即返回taskId
     */
    @PostMapping("/async_export_create")
    public Result createAsyncExportTask(
            @RequestParam Integer schoolId,
            @RequestParam Integer courseId,
            @RequestParam Integer examId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer subState,
            @RequestParam(required = false) Integer reviewState,
            @RequestParam(required = false) Integer scoredState,
            @RequestHeader("Authorization") String authorization
    ) throws Exception{
        // 1. 权限校验
        if (!AuthTokenUtil.verifyToken(authorization, schoolId)) {
            return Result.error("非法访问，权限不足");
        }

        // 从 JWT 提取用户 ID（不再依赖 Sa-Token 会话，避免 Sa-Token 过期导致请求失败）
        String operateUserId = AuthTokenUtil.extractUserId(authorization).toString();

        // 2. 创建异步任务
        String taskId = examTopicAsyncService.createExportTask(
                schoolId,
                courseId,
                examId,
                title,
                classId,
                subState,
                reviewState,
                scoredState,
                operateUserId
        );

        return Result.success("任务已开始生成，请稍候", taskId);
    }

    /**
     * 轮询查询导出状态
     */
    @PostMapping("/async_export_result")
    public Result getAsyncExportResult(@RequestParam String taskId) {
        AsyncQueryTask task = examTopicAsyncService.getTask(taskId);

        if (task == null) {
            return Result.error("任务不存在或已过期");
        }

        Integer current = (task.getCurrent() == null) ? 0 : task.getCurrent();
        Integer total = (task.getTotal() == null) ? 0 : task.getTotal();
        String status = task.getStatus();
        String msg;

        // 运行中
        if ("RUNNING".equals(status)) {
            // 防御：进度等于总数，直接返回成功
            if (total > 0 && current >= total) {
                status = "SUCCESS";
                msg = "导出完成";
            } else if (total <= 0) {
                msg = "正在准备数据...";
            } else {
                msg = "正在生成";
            }
        } else if ("FAILED".equals(status)) {
            msg = task.getErrorMsg();
        } else if ("SUCCESS".equals(status)) {
            msg = "导出完成";
        } else {
            msg = "任务状态异常";
        }

        // 组装自定义返回数据
        Map<String, Object> resMap = new HashMap<>();
        resMap.put("status", status);
        resMap.put("current", current);
        resMap.put("total", total);

        // 关键修复：将第二个参数强制转换为Object，消除二义性
        return Result.success(msg, (Object) resMap);
    }

    @GetMapping("/async_export_download")
    public void asyncExportDownload(
            @RequestParam String taskId,
            HttpServletResponse response
    ) {
        try {
            // 1. 获取任务
            AsyncQueryTask task = examTopicAsyncService.getTask(taskId);
            if (task == null || !"SUCCESS".equals(task.getStatus())) {
                response.setContentType("application/json;charset=utf-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"文件未生成或已过期\"}");
                return;
            }

            // 2. 从磁盘读取文件（不再从Redis拿byte[]）
            String filePath = task.getFilePath();
            java.io.File file = new java.io.File(filePath);

            // 3. 设置下载响应头
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment;filename=" +
                    java.net.URLEncoder.encode(task.getFileName(), "UTF-8"));

            // 4. 磁盘文件 → 直接写出到浏览器（零内存拷贝）
            try (java.io.InputStream in = Files.newInputStream(file.toPath());
                 java.io.OutputStream out = response.getOutputStream()) {

                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                out.flush();
            }

            if (file.exists()) {
                file.delete();
            }

        } catch (Exception e) {
            log.error("异步导出下载失败: taskId={}", taskId, e);
        }
    }

    @GetMapping("/yee_exam_list")
    public Result selectAll(@RequestParam int schoolId,
                            @RequestParam Integer courseId,
                            @RequestParam(required = false) Integer classId,
                            @RequestParam(required = false) String title,
                            @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectAll(schoolId, courseId, classId, title);
        } else return Result.error("非法访问");
    }

    @GetMapping("/yee_exam_record_list")
    public Result selectRecordAll(@RequestParam int schoolId,
                                  @RequestParam Integer courseId,
                                  @RequestParam Integer nodeId,
                                  @RequestParam(required = false) Integer classId,
                                  @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectRecordAll(schoolId, courseId, nodeId, classId);
        } else return Result.error("非法访问");
    }


    @GetMapping("/yee_exam_record_list_exam_id")
    public Result selectRecordAllWorkId(@RequestParam int schoolId,
                                        @RequestParam Integer courseId,
                                        @RequestParam Integer examId,
                                        @RequestParam(required = false) Integer classId,
                                        @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectRecordAllExamId(schoolId, courseId, examId, classId);
        } else return Result.error("非法访问");
    }

    @GetMapping("/yee_exam_record_search_list")
    public Result selectWorkRecordAll(@RequestParam int schoolId,
                                      @RequestParam Integer courseId,
                                      @RequestParam Integer examId,
                                      @RequestParam(required = false) String title,
                                      @RequestParam(required = false) Integer classId,
                                      @RequestParam(required = false) Integer subState,
                                      @RequestParam(required = false) Integer reviewState,
                                      @RequestParam(required = false) Integer scoredState,
                                      @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                      @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                      @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectSearchRecordAll(schoolId, courseId, examId, title, classId, subState, reviewState, scoredState, pageNum, pageSize);
        } else return Result.error("非法访问");
    }

    @GetMapping("/yee_exam_record_consult_list")
    public Result selectWorkRecordConsult(@RequestParam int schoolId, @RequestParam Integer userId, @RequestParam Integer examId, @RequestParam(required = false) Integer courseId, @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectWorkRecordConsult(schoolId, userId, examId, courseId);
        } else return Result.error("非法访问");
    }

    @GetMapping("/yee_exam_record_consult_list_pre")
    public Result selectWorkRecordConsultPre(@RequestParam int schoolId,  @RequestParam Integer examId, @RequestParam(required = false) Integer courseId,@RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectWorkRecordConsultPre(schoolId, examId, courseId);
        } else return Result.error("非法访问");
    }

//    @GetMapping("/yee_exam_record_recheck_update")
//    public Result selectWorkRecordRecheck(@RequestParam int schoolId, @RequestParam Integer userId, @RequestParam Integer examId, @RequestParam BigDecimal recheckScore, @RequestParam(required = false) Integer courseId,@RequestHeader String Authorization) throws Exception {
//        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
//            return yeeExamService.selectWorkRecordRecheck(schoolId, userId, examId, recheckScore, courseId);
//        } else return Result.error("非法访问");
//    }

    @PostMapping("/yee_exam_record_recheck_update_new")
    public Result selectWorkRecordRecheckNew(@RequestBody Map<String, Object> requestData, @RequestHeader String Authorization) throws Exception {
        Integer schoolId = Integer.parseInt(requestData.get("schoolId").toString());
        Integer userId = Integer.parseInt(requestData.get("userId").toString());
        Integer examId = Integer.parseInt(requestData.get("examId").toString());
        Integer courseId = Integer.parseInt(requestData.get("courseId").toString());
        Integer teacherId = Integer.parseInt(requestData.get("teacherId").toString());
        BigDecimal recheckScore = new BigDecimal(requestData.get("recheckScore").toString());
        List<Map<String, Object>> examResult = (List<Map<String, Object>>) requestData.get("examResult");

        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectWorkRecordRecheckNew(schoolId, userId, examId, courseId,recheckScore, teacherId, examResult);
        } else return Result.error("非法访问");
    }

    @GetMapping("/yee_exam_record_manual_list")
    public Result selectWorkRecordManualList(@RequestParam int schoolId, @RequestParam Integer userId, @RequestParam Integer examId, @RequestParam(required = false) Integer courseId,@RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectWorkRecordManualList(schoolId, userId, examId, courseId);
        } else return Result.error("非法访问");
    }

    @GetMapping("/yee_exam_record_manual_update")
    public Result selectWorkRecordManual(@RequestParam int schoolId, @RequestParam Integer userId, @RequestParam Integer examId, @RequestParam BigDecimal manualScore, @RequestParam(required = false) Integer courseId,@RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectWorkRecordManual(schoolId, userId, examId, manualScore, courseId);
        } else return Result.error("非法访问");
    }

    /**
     * 导出试题数据为Excel
     */
    @GetMapping("/yee_exam_topic_export")
    public void exportQuestions(HttpServletResponse response,
                                @RequestParam Integer schoolId,
                                @RequestParam(required = false) String topic,
                                @RequestParam(required = false) Integer createId,
                                @RequestParam(required = false) Integer type,
                                @RequestParam(required = false) Integer level,
                                @RequestParam(required = false) Integer cateBid,
                                @RequestParam(required = false) Integer cateMid,
                                @RequestParam Integer examId,
                                @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            yeeExamService.exportQuestions(response, schoolId, topic, createId, type, level, cateBid, cateMid, examId);
        } else {
            response.setStatus(403);
            response.getWriter().write("非法访问");
        }
    }

    /**
     * 从试题篮 添加作业
     * @param yeeExam
     * @param Authorization
     * @return
     * @throws Exception
     */
    @PostMapping("/yee_exam_add")
    public Result add(@RequestBody YeeExam yeeExam,
                      @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, yeeExam.getSchoolId())){
            return yeeExamService.add(yeeExam);
        }else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_exam_add_more")
    public Result addMore(@RequestBody YeeExam yeeExam,
                          @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, yeeExam.getSchoolId())){
            return yeeExamService.addMore(yeeExam);
        }else {
            return Result.error("非法访问");
        }
    }



    @GetMapping("/yee_exam_list_node")
    public Result selectAllNode(@RequestParam int schoolId,
                                @RequestParam(required = false) String title,
                                @RequestParam(required = false) Integer classId,
                                @RequestParam(required = false) Integer allow,
                                @RequestParam Integer nodeId,
                                @RequestParam Integer courseId,
                                @RequestHeader String Authorization) throws Exception {
        // Validate required parameters
        if (schoolId <= 0 || nodeId == null || courseId == null) {
            return Result.error("缺少必要参数");
        }

        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectAllNode(schoolId, courseId, classId, title, nodeId, allow);
        } else {
            return Result.error("非法访问");
        }
    }

    @PostMapping("/yee_exam_update")
    public Result update(@RequestBody YeeExam yeeExam,
                         @RequestHeader String Authorization) throws Exception{
        if (AuthTokenUtil.verifyToken(Authorization, yeeExam.getSchoolId())){
            return yeeExamService.update(yeeExam);
        }else {
            return Result.error("非法访问");
        }
    }

    /**
     * 根据ID查询作业
     */
    @GetMapping("/yee_exam_get")
    public Result selectById(@RequestParam Integer schoolId,
                             @RequestParam Integer id,
                             @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.selectById(schoolId, id);
        } else {
            return Result.error("非法访问");
        }
    }

    /**
     * 测回功能 - 更新作业表allow字段，并删除相关的记录和答案
     */
    @GetMapping("/yee_exam_recover")
    public Result recoverWork(@RequestParam Integer schoolId,
                              @RequestParam Integer examId,
                              @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.recoverWork(schoolId, examId);
        } else {
            return Result.error("非法访问");
        }
    }

    /**
     * 删除作业功能 - 根据ID删除作业及相关记录
     */
    @GetMapping("/yee_exam_delete")
    public Result deleteWork(@RequestParam Integer schoolId,
                             @RequestParam Integer examId,
                             @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.deleteWork(schoolId, examId);
        } else {
            return Result.error("非法访问");
        }
    }

    /**
     * 打回重做功能 - 删除学生的考试记录和答案，允许重新答题
     */
    @GetMapping("/yee_exam_redo")
    public Result redoExam(@RequestParam Integer schoolId,
                           @RequestParam Integer examId,
                           @RequestParam(required = false) Integer userId,
                           @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
            return yeeExamService.redoExam(schoolId, examId, userId);
        } else {
            return Result.error("非法访问");
        }
    }

//    @GetMapping("/yee_exam_record_search_list_export_pdf")
//    public void selectExamRecordAllPdf(HttpServletResponse response,
//                                      @RequestParam int schoolId,
//                                      @RequestParam Integer courseId,
//                                      @RequestParam Integer examId,
//                                      @RequestParam(required = false) String title,
//                                      @RequestParam(required = false) Integer classId,
//                                      @RequestParam(required = false) Integer subState,
//                                      @RequestParam(required = false) Integer reviewState,
//                                      @RequestParam(required = false) Integer scoredState,
//                                      @RequestHeader String Authorization) throws Exception {
//        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
//            yeeExamService.selectSearchRecordAllPdf(response, schoolId, courseId, examId, title, classId, subState, reviewState, scoredState);
//        } else {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            response.setContentType("application/json;charset=UTF-8");
//            response.getWriter().write("{\"error\":\"非法访问\"}");
//        }
//    }

//    @GetMapping("/yee_exam_record_search_list_export_pdf_without_answers")
//    public void selectExamRecordAllPdfWithoutAnswers(HttpServletResponse response,
//                                      @RequestParam int schoolId,
//                                      @RequestParam Integer courseId,
//                                      @RequestParam Integer examId,
//                                      @RequestParam(required = false) String title,
//                                      @RequestParam(required = false) Integer classId,
//                                      @RequestParam(required = false) Integer subState,
//                                      @RequestParam(required = false) Integer reviewState,
//                                      @RequestParam(required = false) Integer scoredState,
//                                      @RequestHeader String Authorization) throws Exception {
//        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
//            yeeExamService.selectSearchRecordAllPdfWithoutAnswers(response, schoolId, courseId, examId, title, classId, subState, reviewState, scoredState);
//        } else {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            response.setContentType("application/json;charset=UTF-8");
//            response.getWriter().write("{\"error\":\"非法访问\"}");
//        }
//    }

    /**
     * 导出作业成绩为Excel
     */
    @PostMapping("/yee_exam_score_export")
    public void exportWorkScore(HttpServletResponse response,
                                @RequestBody YeeExamExportDTO queryDTO,
                                @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, queryDTO.getSchoolId())) {
            yeeExamService.exportWorkScore(response, queryDTO);
        } else {
            response.setStatus(403);
            response.getWriter().write("非法访问");
        }
    }

    /**
     *
     *  教师收卷
     */
//    @PostMapping("/yee_exam_collect_exam")
//    public Result teacherBatchCollectExam(@RequestParam Integer schoolId,
//                                          @RequestParam Integer examId,
//                                          @RequestHeader String Authorization) throws Exception {
//        if (AuthTokenUtil.verifyToken(Authorization, schoolId)) {
//            return yeeStudentCourseExamService.teacherBatchCollectExam(schoolId, examId);
//        } else return Result.error("非法访问");
//    }
}
