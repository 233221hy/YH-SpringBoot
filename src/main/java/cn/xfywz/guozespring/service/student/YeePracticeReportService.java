package cn.xfywz.guozespring.service.student;

import cn.xfywz.guozespring.util.Result;

public interface YeePracticeReportService {

    Result myReport(int schoolId, int courseId, int studentId) throws Exception;

    Result submit(int schoolId, int courseId, int studentId,
                  String title, String content, String files) throws Exception;
}