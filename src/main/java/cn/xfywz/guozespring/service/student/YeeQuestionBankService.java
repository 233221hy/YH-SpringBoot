package cn.xfywz.guozespring.service.student;

import cn.xfywz.guozespring.util.Result;

public interface YeeQuestionBankService {

    Result selectAll(int schoolId,long studentId, int pageSize, int pageNum) throws Exception;

    Result selectById(int schoolId,int chapterId,long studentId) throws Exception;

}
