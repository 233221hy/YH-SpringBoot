package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.dto.YeeWorkExportDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeWork;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface YeeWorkService {
    Result selectAll(int schoolId, Integer courseId, Integer classId, String title) throws Exception;

    Result selectRecordAll(int schoolId, Integer courseId, Integer nodeId, Integer classId) throws Exception;

    Result selectRecordAllWorkId(int schoolId, Integer courseId, Integer workId, Integer classId) throws Exception;

    Result selectSearchRecordAll(int schoolId, Integer courseId, Integer workId, String title, Integer classId, Integer subState, Integer reviewState, Integer scoredState, Integer pageNum, Integer pageSize) throws Exception;

    Result selectWorkRecordConsult(int schoolId, Integer userId, Integer workId) throws  Exception;

//    Result selectWorkRecordRecheck(int schoolId, Integer userId, Integer workId, BigDecimal recheckScore) throws Exception;

    Result selectWorkRecordRecheckNew(int schoolId, Integer userId, Integer workId, BigDecimal recheckScore, Integer teacherId, List<Map<String, Object>> workResult) throws Exception;

    Result selectWorkRecordManual(int schoolId, Integer userId, Integer workId, BigDecimal manualScore) throws Exception;

    Result selectWorkRecordManualList(int schoolId, Integer userId, Integer workId) throws Exception;

    Result selectWorkRecordConsultPre(int schoolId, Integer workId) throws Exception;

    void exportQuestions(HttpServletResponse response, Integer schoolId, String topic, Integer createId, Integer type, Integer level, Integer cateBid, Integer cateMid, Integer workId) throws Exception;

    Result add(YeeWork yeeWork) throws Exception;

    Result selectAllNode(int schoolId, Integer courseId, Integer classId, String title, Integer nodeId, Integer allow) throws Exception;

    Result addMore(YeeWork yeeWork) throws Exception;

    Result update(YeeWork yeeWork) throws Exception;

    Result selectById(Integer schoolId, Integer id) throws Exception;

    Result recoverWork(Integer schoolId, Integer workId) throws Exception;

    Result deleteWork(Integer schoolId, Integer workId) throws Exception;

    Result redoWork(Integer schoolId, Integer workId, Integer userId) throws Exception;

    void exportWorkScore(HttpServletResponse response, YeeWorkExportDTO queryDTO);
}
