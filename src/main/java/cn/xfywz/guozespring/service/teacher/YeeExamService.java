package cn.xfywz.guozespring.service.teacher;

import cn.xfywz.guozespring.entity.dto.YeeExamExportDTO;
import cn.xfywz.guozespring.entity.mhsch.YeeExam;
import cn.xfywz.guozespring.entity.mhsch.YeeWork;
import cn.xfywz.guozespring.util.Result;
import jakarta.servlet.http.HttpServletResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface YeeExamService {
    Result selectAll(int schoolId, Integer courseId, Integer classId, String title) throws Exception;

    Result selectRecordAll(int schoolId, Integer courseId, Integer nodeId, Integer classId) throws Exception;

    Result selectRecordAllExamId(int schoolId, Integer courseId, Integer examId, Integer classId);

    Result selectSearchRecordAll(int schoolId, Integer courseId, Integer examId, String title, Integer classId, Integer subState, Integer reviewState, Integer scoredState, Integer pageNum, Integer pageSize) throws Exception;

    Result selectWorkRecordConsult(int schoolId, Integer userId, Integer examId, Integer courseId) throws  Exception;
    
//    Result selectWorkRecordRecheck(int schoolId, Integer userId, Integer examId, BigDecimal recheckScore, Integer courseId) throws Exception;

    Result selectWorkRecordRecheckNew(int schoolId, Integer userId, Integer examId,Integer courseId, BigDecimal recheckScore, Integer teacherId, List<Map<String, Object>> examResult) throws Exception;

    Result selectWorkRecordManual(int schoolId, Integer userId, Integer examId, BigDecimal manualScore, Integer courseId) throws Exception;

    Result selectWorkRecordManualList(int schoolId, Integer userId, Integer examId, Integer courseId) throws Exception;

    Result selectWorkRecordConsultPre(int schoolId, Integer examId, Integer courseId) throws Exception;

    void exportQuestions(HttpServletResponse response, Integer schoolId, String topic, Integer createId, Integer type, Integer level, Integer cateBid, Integer cateMid, Integer examId) throws Exception;

    Result add(YeeExam yeeExam) throws Exception;

    Result addMore(YeeExam yeeExam) throws Exception;

    Result selectAllNode(int schoolId, Integer courseId, Integer classId, String title, Integer nodeId, Integer allow) throws Exception;

    Result update(YeeExam yeeExam) throws Exception;

    Result selectById(Integer schoolId, Integer id) throws Exception;

    Result recoverWork(Integer schoolId, Integer examId) throws Exception;

    Result deleteWork(Integer schoolId, Integer examId) throws Exception;

    Result redoExam(Integer schoolId, Integer examId, Integer userId) throws Exception;

//    Result selectSearchRecordAllPdf(HttpServletResponse response, int schoolId, Integer courseId, Integer examId, String title, Integer classId, Integer subState, Integer reviewState, Integer scoredState) throws Exception;

//    Result selectSearchRecordAllPdfWithoutAnswers(HttpServletResponse response, int schoolId, Integer courseId, Integer examId, String title, Integer classId, Integer subState, Integer reviewState, Integer scoredState) throws Exception;

    void exportWorkScore(HttpServletResponse response, YeeExamExportDTO queryDTO);
}
