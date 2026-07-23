package cn.xfywz.guozespring.service.student;


import cn.xfywz.guozespring.entity.mhsch.FileInfo;
import cn.xfywz.guozespring.util.Result;

import java.util.List;
import java.util.Map;

public interface YeeStudentCourseWorkService {

    Result selectStudentWorkList(int schoolId, Integer courseId, Integer studentId, Integer nodeId) throws Exception;

    Result selectStudentWorkDetail(int schoolId, Integer courseId, Integer studentId, Integer workId, String title) throws Exception;

    Result addCollectionTopic(int schoolId, Integer userId, Integer workId, Integer topicId, Integer courseId) throws Exception;

    Result deleteCollectionTopic(int schoolId, Integer userId, Integer workId, Integer topicId, Integer courseId) throws Exception;

    Result startWork(int schoolId, Integer courseId, Integer studentId, Integer workId, Integer createUserId, String platform, Integer classId, Integer paperId) throws  Exception;

    Result addWorkAnswer(int schoolId, Integer courseId, Integer userId, List<String> answer, Integer topicId, Integer workId, Integer recordId, Integer type) throws Exception;

    Result addWorkAnswerText(int schoolId, Integer courseId, Integer userId, String answer, Integer topicId, Integer workId, Integer recordId, Integer type, List<FileInfo> images, List<FileInfo> files) throws Exception;

    Result addWorkAnswerBlank(int schoolId, Integer courseId, Integer userId, Map<String, String> answer, Integer topicId, Integer workId, Integer recordId, Integer type) throws Exception;

    Result finishWorkAnswer(int schoolId, Integer courseId, Integer userId, Integer workId, Integer recordId) throws Exception;

    Result reStartWork(int schoolId, Integer courseId, Integer studentId, Integer workId, Integer createUserId, String platform, Integer classId, Integer paperId, Integer recordId) throws  Exception;
}
