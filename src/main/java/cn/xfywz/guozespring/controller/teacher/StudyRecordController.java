package cn.xfywz.guozespring.controller.teacher;

import cn.xfywz.guozespring.annotation.RequireAuth;
import cn.xfywz.guozespring.entity.dto.StudyRecordQuery;
import cn.xfywz.guozespring.service.teacher.StudyRecordService;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequireAuth
@RestController
@RequestMapping("/school")
public class StudyRecordController {

    @Autowired
    private StudyRecordService studyRecordService;

    // 学习记录列表（支持按课程、可选班级、关键字模糊：学生姓名/学号）
    @PostMapping("/study_record_list")
    public Result list(@RequestBody StudyRecordQuery param) throws Exception {
        return studyRecordService.list(param);
    }

    // 学习记录导出
    @PostMapping("/study_record_export")
    public void exportData(@RequestBody StudyRecordQuery param,
                           HttpServletResponse response) throws Exception {
        studyRecordService.exportData(param, response);
    }

    // 学生考试记录详情
    @PostMapping("/study_record_exam_list")
    public Result examList(@RequestBody StudyRecordQuery param) throws Exception {
        return studyRecordService.examList(param);
    }

    // 学生作业记录详情
    @PostMapping("/study_record_work_list")
    public Result workList(@RequestBody StudyRecordQuery param) throws Exception {
        return studyRecordService.workList(param);
    }

    // 学生讨论记录详情
    @PostMapping("/study_record_discuss_list")
    public Result discussList(@RequestBody StudyRecordQuery param) throws Exception {
        return studyRecordService.discussList(param);
    }

    // 学生视频记录详情
    @PostMapping("/study_record_video_list")
    public Result videoList(@RequestBody StudyRecordQuery param) throws Exception {
        return studyRecordService.videoList(param);
    }

    // 退回重学：清空指定学生在课程下的学习记录
    @PostMapping("/study_record_reset")
    public Result reset(@RequestBody StudyRecordQuery param) throws Exception {
        return studyRecordService.resetStudyRecord(param);
    }
}