package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.dto.StudyRecordQuery;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface StudyRecordService {
    Result list(StudyRecordQuery param);

//    void exportData(Integer schoolId, long courseId, Long classId, String keyword, HttpServletResponse response) throws IOException;

    void exportData(StudyRecordQuery param, HttpServletResponse response) throws IOException;

    // 学生考试记录详情列表
    Result examList(StudyRecordQuery param);

    // 学生作业记录详情列表
    Result workList(StudyRecordQuery param);

    // 学生讨论记录详情列表
    Result discussList(StudyRecordQuery param);

    // 学生视频记录详情列表
    Result videoList(StudyRecordQuery param);

    // 重置学习记录（退回重学）
    Result resetStudyRecord(StudyRecordQuery param);


}