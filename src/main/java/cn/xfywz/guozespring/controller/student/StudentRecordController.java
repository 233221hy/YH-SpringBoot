package cn.xfywz.guozespring.controller.student;


import cn.xfywz.guozespring.entity.dto.StudyRecordQuery;
import cn.xfywz.guozespring.service.teacher.StudyRecordService;
import cn.xfywz.guozespring.util.AuthTokenUtil;
import cn.xfywz.guozespring.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class StudentRecordController {

    @Autowired
    private StudyRecordService studyRecordService;
    // 学生考试记录详情
    @PostMapping("/study_record_exam_list")
    public Result examList(@RequestBody StudyRecordQuery param,
                           @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) param.getSchoolId())) {
            return studyRecordService.examList(param);
        } else return Result.error("非法访问");
    }

    // 学生作业记录详情
    @PostMapping("/study_record_work_list")
    public Result workList(@RequestBody StudyRecordQuery param,
                           @RequestHeader String Authorization) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) param.getSchoolId())) {
            return studyRecordService.workList(param);
        } else return Result.error("非法访问");
    }

    // 学生讨论记录详情
    @PostMapping("/study_record_discuss_list")
    public Result discussList(@RequestHeader String Authorization,
                              @RequestBody StudyRecordQuery param) throws Exception {
        if (AuthTokenUtil.verifyToken(Authorization, (int) param.getSchoolId())) {
            return studyRecordService.discussList(param);
        } else return Result.error("非法访问");
    }

}
