package cn.xfywz.guozespring.service.student;


import cn.xfywz.guozespring.util.Result;

public interface YeeWorkEvaluationService {

    Result selectList(int schoolId, long studentId,int type, int pageSize, int pageNum) throws Exception;

    Result selectById(int schoolId,int evaluationId) throws Exception;
}
