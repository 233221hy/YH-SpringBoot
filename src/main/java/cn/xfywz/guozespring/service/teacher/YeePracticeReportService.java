package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.util.Result;

public interface YeePracticeReportService {

    Result stats(int schoolId, int courseId) throws Exception;

    Result list(int schoolId, int courseId, Integer classId,
                String studentNumber, String studentName, Integer status,
                int pageNum, int pageSize) throws Exception;

    Result detail(int schoolId, long reportId) throws Exception;

    Result review(int schoolId, long reportId, String result,
                  long reviewerId, String remark) throws Exception;

    Result allReportsForExport(int schoolId, int courseId,
                               Integer classId, Integer status) throws Exception;
}