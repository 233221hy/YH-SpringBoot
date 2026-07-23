package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.YeeWorkExportDTO;
import cn.xfywz.guozespring.entity.mhsch.YeePaper;
import cn.xfywz.guozespring.entity.mhsch.YeeWork;
import cn.xfywz.guozespring.service.teacher.YeeWorkService;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @Author: ChengLin
 * yee_work
 */
@RequireAuth
@RestController
@RequestMapping("/school")
public class YeeWorkController {

    @Autowired
    private YeeWorkService yeeWorkService;

    @GetMapping("/yee_work_list")
    public Result selectAll(@RequestParam int schoolId,
                            @RequestParam Integer courseId,
                            @RequestParam(required = false) Integer classId,
                            @RequestParam(required = false) String title) throws Exception {
        return yeeWorkService.selectAll(schoolId, courseId, classId, title);
    }

    @GetMapping("/yee_work_record_list")
    public Result selectRecordAll(@RequestParam int schoolId,
                                  @RequestParam Integer courseId,
                                  @RequestParam Integer nodeId,
                                  @RequestParam(required = false) Integer classId) throws Exception {
        return yeeWorkService.selectRecordAll(schoolId, courseId, nodeId, classId);
    }

    @GetMapping("/yee_work_record_list_work_id")
    public Result selectRecordAllWorkId(@RequestParam int schoolId,
                                        @RequestParam Integer courseId,
                                        @RequestParam Integer workId,
                                        @RequestParam(required = false) Integer classId) throws Exception {
        return yeeWorkService.selectRecordAllWorkId(schoolId, courseId, workId, classId);
    }

    @GetMapping("/yee_work_record_search_list")
    public Result selectWorkRecordAll(@RequestParam int schoolId,
                                      @RequestParam Integer courseId,
                                      @RequestParam Integer workId,
                                      @RequestParam(required = false) String title,
                                      @RequestParam(required = false) Integer classId,
                                      @RequestParam(required = false) Integer subState,
                                      @RequestParam(required = false) Integer reviewState,
                                      @RequestParam(required = false) Integer scoredState,
                                      @RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                      @RequestParam(required = false, defaultValue = "10") Integer pageSize) throws Exception {
        return yeeWorkService.selectSearchRecordAll(schoolId, courseId, workId, title, classId, subState, reviewState, scoredState, pageNum, pageSize);
    }

    @GetMapping("/yee_work_record_consult_list")
    public Result selectWorkRecordConsult(@RequestParam int schoolId, @RequestParam Integer userId, @RequestParam Integer workId) throws Exception {
        return yeeWorkService.selectWorkRecordConsult(schoolId, userId, workId);
    }

    @GetMapping("/yee_work_record_consult_list_pre")
    public Result selectWorkRecordConsultPre(@RequestParam int schoolId, @RequestParam Integer workId) throws Exception {
        return yeeWorkService.selectWorkRecordConsultPre(schoolId, workId);
    }

    @PostMapping("/yee_work_record_recheck_update_new")
    public Result selectWorkRecordRecheckNew(@RequestBody Map<String, Object> requestData) throws Exception {
        Integer schoolId = Integer.parseInt(requestData.get("schoolId").toString());
        Integer userId = Integer.parseInt(requestData.get("userId").toString());
        Integer workId = Integer.parseInt(requestData.get("workId").toString());
        Integer teacherId = Integer.parseInt(requestData.get("teacherId").toString());
        BigDecimal recheckScore = new BigDecimal(requestData.get("recheckScore").toString());
        List<Map<String, Object>> workResult = (List<Map<String, Object>>) requestData.get("workResult");
        return yeeWorkService.selectWorkRecordRecheckNew(schoolId, userId, workId, recheckScore, teacherId, workResult);
    }

    @GetMapping("/yee_work_record_manual_list")
    public Result selectWorkRecordManualList(@RequestParam int schoolId, @RequestParam Integer userId, @RequestParam Integer workId) throws Exception {
        return yeeWorkService.selectWorkRecordManualList(schoolId, userId, workId);
    }

    @GetMapping("/yee_work_record_manual_update")
    public Result selectWorkRecordManual(@RequestParam int schoolId, @RequestParam Integer userId, @RequestParam Integer workId, @RequestParam BigDecimal manualScore) throws Exception {
        return yeeWorkService.selectWorkRecordManual(schoolId, userId, workId, manualScore);
    }

    /**
     * 导出试题数据为Excel
     */
    @GetMapping("/yee_work_topic_export")
    public void exportQuestions(HttpServletResponse response,
                                @RequestParam Integer schoolId,
                                @RequestParam(required = false) String topic,
                                @RequestParam(required = false) Integer createId,
                                @RequestParam(required = false) Integer type,
                                @RequestParam(required = false) Integer level,
                                @RequestParam(required = false) Integer cateBid,
                                @RequestParam(required = false) Integer cateMid,
                                @RequestParam Integer workId) throws Exception {
        yeeWorkService.exportQuestions(response, schoolId, topic, createId, type, level, cateBid, cateMid, workId);
    }

    /**
     * 导出作业成绩为Excel
     */
    @PostMapping("/yee_work_score_export")
    public void exportWorkScore(HttpServletResponse response,
                                @RequestBody YeeWorkExportDTO queryDTO) throws Exception {
        yeeWorkService.exportWorkScore(response, queryDTO);
    }

    /**
     * 从试题篮 添加作业
     */
    @PostMapping("/yee_work_add")
    public Result add(@RequestBody YeeWork yeeWork) throws Exception {
        return yeeWorkService.add(yeeWork);
    }

    @PostMapping("/yee_work_add_more")
    public Result addMore(@RequestBody YeeWork yeeWork) throws Exception {
        return yeeWorkService.addMore(yeeWork);
    }

    @GetMapping("/yee_work_list_node")
    public Result selectAllNode(@RequestParam int schoolId,
                                @RequestParam(required = false) String title,
                                @RequestParam(required = false) Integer classId,
                                @RequestParam(required = false) Integer allow,
                                @RequestParam Integer nodeId,
                                @RequestParam Integer courseId) throws Exception {
        // Validate required parameters
        if (schoolId <= 0 || nodeId == null || courseId == null) {
            return Result.error("缺少必要参数");
        }
        return yeeWorkService.selectAllNode(schoolId, courseId, classId, title, nodeId, allow);
    }

    @PostMapping("/yee_work_update")
    public Result update(@RequestBody YeeWork yeeWork) throws Exception {
        return yeeWorkService.update(yeeWork);
    }

    /**
     * 根据ID查询作业
     */
    @GetMapping("/yee_work_get")
    public Result selectById(@RequestParam Integer schoolId,
                             @RequestParam Integer id) throws Exception {
        return yeeWorkService.selectById(schoolId, id);
    }

    /**
     * 测回功能 - 更新作业表allow字段，并删除相关的记录和答案
     */
    @GetMapping("/yee_work_recover")
    public Result recoverWork(@RequestParam Integer schoolId,
                              @RequestParam Integer workId) throws Exception {
        return yeeWorkService.recoverWork(schoolId, workId);
    }

    /**
     * 删除作业功能 - 根据ID删除作业及相关记录
     */
    @GetMapping("/yee_work_delete")
    public Result deleteWork(@RequestParam Integer schoolId,
                             @RequestParam Integer workId) throws Exception {
        return yeeWorkService.deleteWork(schoolId, workId);
    }

    /**
     * 打回重做功能 - 删除学生的作业记录和答案，允许重新答题
     */
    @GetMapping("/yee_work_redo")
    public Result redoWork(@RequestParam Integer schoolId,
                           @RequestParam Integer workId,
                           @RequestParam(required = false) Integer userId) throws Exception {
        return yeeWorkService.redoWork(schoolId, workId, userId);
    }
}