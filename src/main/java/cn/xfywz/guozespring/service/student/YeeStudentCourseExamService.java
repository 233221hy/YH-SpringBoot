package cn.xfywz.guozespring.service.student;


import cn.xfywz.guozespring.entity.mhsch.FileInfo;
import cn.xfywz.guozespring.util.Result;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

public interface YeeStudentCourseExamService {

    Result selectStudentExamList(int schoolId, Integer courseId, Integer studentId, Integer nodeId) throws Exception;

    Result selectStudentExamDetail(int schoolId, Integer courseId, Integer studentId, Integer examId, String title) throws Exception;

    Result startExam(int schoolId, Integer courseId, Integer studentId, Integer examId, Integer createUserId, String platform, Integer classId, Integer paperId, Integer random, String randData, Integer randNumber) throws  Exception;

    Result addExamAnswer(int schoolId, Integer courseId, Integer userId, List<String> answer, Integer topicId, Integer examId, Integer recordId, Integer type) throws Exception;

    Result addExamAnswerText(int schoolId, Integer courseId, Integer userId, String answer, Integer topicId, Integer examId, Integer recordId, Integer type, List<FileInfo> images, List<FileInfo> files) throws Exception;

    Result addExamAnswerBlank(int schoolId, Integer courseId, Integer userId, Map<String, String> answer, Integer topicId, Integer examId, Integer recordId, Integer type) throws Exception;

    Result finishExamAnswer(int schoolId, Integer courseId, Integer userId, Integer examId, Integer recordId) throws Exception;

    Result teacherBatchCollectExam(int schoolId, Integer examId) throws Exception;
}

